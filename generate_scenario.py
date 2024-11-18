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
            "deployService": self.build_deploy_service_stage,
            "runTests": self.build_run_bdd_tests_stage,
        }.get(stage_name)

    def build_deploy_service_stage(self, serviceName, serviceVersion):
        """
            Deploy service with specified name and version
            a911-svc {version": "1.111.0-7151.MASTER-SNAPSHOT"}
        """
        stage_passed_variable = f"deploy_{serviceName.lower().replace('-', '_')}_passed"

        stage = render_template(
            template=DEPLOY_SVC_TEMPLATE,
            stage_name=f"Deploy {serviceName} {serviceVersion}",
            service_name=serviceName,
            service_version=serviceVersion,
            stage_passed_variable=stage_passed_variable,
        )

        self.stages.append(stage)
        self.logger.info(f"Deploy {serviceName} {serviceVersion}")

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
        scenario_name=json_scenario["scenarioName"],
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
