from behave import step, then
import json

from templates import DEPLOY_SVC_TEMPLATE, RUN_BDD_TESTS_TEMPLATE


@step('Deploy service "{service_name}" with parameters {params}')
def step_deploy_service(context, service_name, params):
    """a911-svc {version": "1.111.0-7151.MASTER-SNAPSHOT"} """
    params = json.loads(params)
    version = params.get("version")

    stage = DEPLOY_SVC_TEMPLATE.safe_substitute(
        stage_name=f"Deploy {service_name} {version}",
        service_name=service_name,
        service_version=version
    ).strip("\n")
    context.stages.append(stage)


@then('Run BDD tests with parameters {params}')
def step_run_bdd_tests(context, params):
    params = json.loads(params)
    stage_name = "Run BDD tests"
    if marks := params.get("marks"):
        stage_name = f"{stage_name} {marks}"
    else:
        marks = "Empty"

    context.stages.append(RUN_BDD_TESTS_TEMPLATE.safe_substitute(
        stage_name=stage_name,
        marks=marks,
    ).strip("\n"))
