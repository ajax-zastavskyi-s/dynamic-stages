import argparse
import json
from helpers import render_template, get_logger
from templates import DEPLOY_SVC_TEMPLATE, RUN_BDD_TESTS_TEMPLATE, BASE_GROOVY_TEMPLATE


class RCGroovyScenarioBuilder:
    def __init__(self, scenario_name, scenario_date):
        self.scenario_name = scenario_name
        self.scenario_date = scenario_date
        self.stages = []
        self.logger = get_logger()

    def get_builder_method(self, stage_name):
        return {
            "deploy_service": self.build_deploy_service_stage,
            "run_tests": self.build_run_bdd_tests_stage,
        }.get(stage_name)

    def build_deploy_service_stage(self, service_name, service_version):
        """
            Deploy service with specified name and version
            a911-svc {version": "1.111.0-7151.MASTER-SNAPSHOT"}
        """
        stage_passed_variable = f"deploy_{service_name.lower().replace('-', '_')}_passed"

        stage = render_template(
            template=DEPLOY_SVC_TEMPLATE,
            stage_name=f"Deploy {service_name} {service_version}",
            service_name=service_name,
            service_version=service_version,
            stage_passed_variable=stage_passed_variable,
        )

        self.stages.append(stage)
        self.logger.info(f"Deploy {service_name} {service_version}")

    def build_run_bdd_tests_stage(self, marks):
        """
            Generate setups, respawn actors, unlock setups and run BDD tests with specified marks
        """
        stage_name = "Run BDD tests"
        if marks:
            marks = ", ".join(marks)
            stage_name = f"{stage_name} with marks: {marks}"
        else:
            marks = "Empty"

        run_tests_stage = render_template(
            template=RUN_BDD_TESTS_TEMPLATE,
            stage_name=stage_name,
            marks=marks
        )

        self.stages.append(run_tests_stage)
        self.logger.info(f"{stage_name} {marks}")

    def save_groovy_file(self):
        groovy = render_template(
            template=BASE_GROOVY_TEMPLATE,
            stages="\n".join((str(stage) for stage in self.stages))
        )

        with open(f"scenarios/{self.scenario_date}.groovy", "w") as f:
            f.write(groovy)
        self.logger.success(f"File 'scenarios/{self.scenario_date}.groovy successfully generated")

def generate_groovy_file(json_scenario: dict):
    builder = RCGroovyScenarioBuilder(
        scenario_name=json_scenario["name"],
        scenario_date=json_scenario["date"],
    )

    for stage in json_scenario["stages"]:
        builder_method = builder.get_builder_method(stage_name=stage["name"])
        builder_method(**stage["parameters"])

    builder.save_groovy_file()


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--json_scenario_path",
        type=str,
        help="Path to json file with RC testing scenario",
        required=True
    )
    args = parser.parse_args()

    with open(args.json_scenario_path) as json_scenario_file:
        scenario = json.load(fp=json_scenario_file)

    generate_groovy_file(json_scenario=scenario)
