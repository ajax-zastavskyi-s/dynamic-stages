from helpers import render_template
from templates import DEPLOY_SVC_TEMPLATE, RUN_BDD_TESTS_TEMPLATE, BASE_GROOVY_TEMPLATE

SCENARIOS_DIR = "scenarios"

class RCGroovyScenarioBuilder:
    def __init__(self,
                 scenario,
                 logger,
                 parallel_deployment=False):
        self.scenario_name = scenario["scenarioName"]
        self.scenario_date = scenario["date"]
        self.config_stages = scenario["stages"]
        self.build_stages = []
        self.logger = logger
        self.deployments = []
        self.parallel_deployment = parallel_deployment
        self.current_parallel_stage = []

    def get_builder_method(self, stage_name):
        return {
            "deployService": self.build_deploy_service_stage,
            "runTests": self.build_run_bdd_tests_stage,
        }.get(stage_name)

    def build(self):
        for stage in self.config_stages:
            builder_method = self.get_builder_method(stage_name=stage["name"])
            builder_method(**stage["parameters"])


    def build_deploy_service_stage(self, serviceName, serviceVersion, deploymentDestination="null"):

        stage_passed_variable = f"deploy_{serviceName.lower().replace('-', '_')}_passed"

        stage = render_template(
            template=DEPLOY_SVC_TEMPLATE,
            stage_name=f"Deploy {serviceName} {serviceVersion}",
            service_name=serviceName,
            service_version=serviceVersion,
            deployment_destination=deploymentDestination,
            stage_passed_variable=stage_passed_variable,
        )

        self.build_stages.append(stage)
        self.deployments.append(f"{serviceName} {serviceVersion}")

        self.logger.info(f"Deploy {serviceName} {serviceVersion}")

    def build_run_bdd_tests_stage(self, marks):
        """Generate setups, respawn actors, unlock setups and run BDD tests with specified marks"""
        stage_name = "Run BDD tests"
        if marks:
            marks = ", ".join(marks)
            stage_name = f"{stage_name} with marks: {marks}"
        else:
            marks = "Empty"

        if self.deployments:
            test_plan_name = " | ".join(self.deployments)
            test_plan_name = f"RC [{test_plan_name}]"
        else:
            test_plan_name = "RC [Undefined]"

        run_tests_stage = render_template(
            template=RUN_BDD_TESTS_TEMPLATE,
            stage_name=stage_name,
            marks=marks,
            test_plan_name=test_plan_name
        )

        self.build_stages.append(run_tests_stage)
        self.logger.info(f"Run BDD tests with marks: {marks}")

    def save_groovy_file(self):
        groovy = render_template(
            template=BASE_GROOVY_TEMPLATE,
            stages="\n".join((str(stage) for stage in self.build_stages))
        )

        groovy_file_path = f"{SCENARIOS_DIR}/{self.scenario_date}.groovy"

        with open(groovy_file_path, "w") as f:
            f.write(groovy)

        self.logger.success(f"File '{groovy_file_path}' successfully generated")
