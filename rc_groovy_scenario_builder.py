import os.path

from helpers import render_template
from templates import (BASE_GROOVY_TEMPLATE, DEPLOY_SVC_TEMPLATE,
                       PARALLEL_DEPLOY_TEMPLATE, RUN_BDD_TESTS_TEMPLATE, SET_FF_TEMPLATE, PARALLEL_RESTORE_TEMPLATE)

SCENARIOS_DIR = "scenarios"


class RCGroovyScenarioBuilder:
    def __init__(self, scenario_name, scenario_id, scenario_date, logger):
        self.scenario_id = scenario_id
        self.scenario_name = scenario_name
        self.scenario_date = scenario_date
        self.stages = []
        self.logger = logger
        self.deployments = []
        self.deployed_services = []
        self.ff_toggles = []

    def get_builder_method(self, stage_name):
        return {
            "deployService": self.build_deploy_service_stage,
            "runTests": self.build_run_bdd_tests_stage,
            "setFeatureFlag": self.build_set_ff_stage
        }.get(stage_name)

    def build_deploy_service_stage(self, serviceName, serviceVersion, deploymentDestination="null"):
        stage_passed_variable = f"deploy_{serviceName.lower().replace('-', '_')}_passed"
        stage_name = f"{serviceName} {serviceVersion}"

        self.deployments.append({
            "stage_name": stage_name,
            "service_name": serviceName,
            "service_version": serviceVersion,
            "deployment_destination": deploymentDestination,
            "stage_passed_variable": stage_passed_variable,
        })
        self.deployed_services.append(stage_name)

    def build_run_bdd_tests_stage(self, marks):
        """Generate setups, respawn actors, unlock setups and run BDD tests with specified marks"""
        stage_name = "Run BDD tests"
        if marks:
            marks = ", ".join(marks)
            stage_name = f"{stage_name} with marks: {marks}"
        else:
            marks = "Empty"
        if self.deployments:
            self._build_parallel_deployments_stage()
        if self.ff_toggles:
            self._build_ff_stage()

        test_plan_name = "RC [Undefined]"
        test_plan_description = "RC Testing"
        services = [service.replace("*.RELEASE", "") for service in self.deployed_services]

        if self.deployments:
            while True:
                test_plan_name = f"RC [{' | '.join(services)}]"
                if len(test_plan_name) <= 250:
                    break
                services.pop(0)

        if self.ff_toggles:
            test_plan_description = f"RC Testing. Updated toggles: {self.rc_toggles}"

        run_tests_stage = render_template(
            template=RUN_BDD_TESTS_TEMPLATE,
            stage_name=stage_name,
            marks=marks,
            test_plan_name=test_plan_name,
            test_plan_description=test_plan_description,
        )

        self.stages.append(run_tests_stage)
        self.logger.info(f"Run BDD tests with marks: {marks}")
        self.deployments = []
        self.ff_toggles = []

    def build_set_ff_stage(self, serviceName, featureFlagName, featureFlagState, additionalData="null"):
        stage_name = f"{featureFlagName} [{serviceName}]"
        stage_passed_variable = f"restore_{featureFlagName.lower().replace('-', '_')}_passed"

        self.ff_toggles.append({
            "stage_name": stage_name,
            "service_name": serviceName,
            "feature_flag_name": featureFlagName,
            "feature_flag_state": featureFlagState,
            "additional_data": str(additionalData).replace("'", '"'),
            "stage_passed_variable": stage_passed_variable,
        })

    def save_groovy_file(self):
        groovy = render_template(
            template=BASE_GROOVY_TEMPLATE,
            stages="\n".join((str(stage) for stage in self.stages))
        )

        groovy_file_path = os.path.join(SCENARIOS_DIR, self.scenario_date, f"{self.scenario_id}.groovy")
        os.makedirs(os.path.dirname(groovy_file_path), exist_ok=True)

        with open(groovy_file_path, "w") as f:
            f.write(groovy)

        self.logger.success(f"File '{groovy_file_path}' successfully generated")

    def _build_parallel_deployments_stage(self):
        parallel_deployments_stage = render_template(
            template=PARALLEL_DEPLOY_TEMPLATE,
            stage_name=f"Deploy {self.rc_deployments}",
            parallel_deployments="\n".join([render_template(template=DEPLOY_SVC_TEMPLATE, **deploy)
                                            for deploy in self.deployments])
        )
        self.stages.append(parallel_deployments_stage)

        self.logger.success(f"Deploy {self.rc_deployments}")

    def _build_ff_stage(self):
        build_ff_stage = render_template(
            template=PARALLEL_RESTORE_TEMPLATE,
            stage_name=f"Restore toggles {self.rc_toggles}",
            parallel_toggles_setting="\n".join([render_template(template=SET_FF_TEMPLATE, **toggle)
                                                for toggle in self.ff_toggles])
        )
        self.stages.append(build_ff_stage)
        self.logger.success(f"Set {self.rc_toggles}")

    @property
    def rc_deployments(self):
        return " | ".join([deploy['stage_name'] for deploy in self.deployments])

    @property
    def rc_toggles(self):
        return " | ".join([f"{toggle['stage_name']}: {'enabled' if toggle['feature_flag_state'] else 'disabled'}"
                           for toggle in self.ff_toggles])
