import argparse
import json
from helpers import render_template, get_logger
from templates import DEPLOY_SVC_TEMPLATE, RUN_BDD_TESTS_TEMPLATE, BASE_GROOVY_TEMPLATE
from loguru import logger as LoguruLogger

SCENARIOS_DIR = "scenarios"


class RCGroovyScenarioBuilder:
    def __init__(self, scenario_name, scenario_date, logger):
        self.scenario_name = scenario_name
        self.scenario_date = scenario_date
        self.stages = []
        self.logger = logger

    def get_builder_method(self, stage_name):
        return {
            "deployService": self.build_deploy_service_stage,
            "runTests": self.build_run_bdd_tests_stage,
        }.get(stage_name)

    def build_deploy_service_stage(self, serviceName, serviceVersion):
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
        """Generate setups, respawn actors, unlock setups and run BDD tests with specified marks"""
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

        groovy_file_path = f"{SCENARIOS_DIR}/{self.scenario_date}.groovy"
        with open(groovy_file_path, "w") as f:
            f.write(groovy)

        self.logger.success(f"File '{groovy_file_path}' successfully generated")

def generate_groovy_file(json_scenario: dict, logger: LoguruLogger):
    builder = RCGroovyScenarioBuilder(
        scenario_name=json_scenario["scenarioName"],
        scenario_date=json_scenario["date"],
        logger=logger,
    )

    for stage in json_scenario["stages"]:
        builder_method = builder.get_builder_method(stage_name=stage["name"])
        builder_method(**stage["parameters"])

    builder.save_groovy_file()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--json_scenario_path",
        type=str,
        help="Path to json file with RC testing scenario",
        required=True
    )
    args = parser.parse_args()
    logger = get_logger()

    try:
        with open(args.json_scenario_path) as json_scenario_file:
            scenario = json.load(fp=json_scenario_file)
    except json.decoder.JSONDecodeError:
        logger.error("Failed to parse JSON scenario")
        exit(1)

    generate_groovy_file(json_scenario=scenario, logger=logger)


if __name__ == "__main__":
    main()

