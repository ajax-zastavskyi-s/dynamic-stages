from behave import step, then
import json

from templates.templates import DEPLOY_SVC_TEMPLATE, SET_FF_TEMPLATE, RUN_BDD_TESTS_TEMPLATE


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

@step('Set FF with parameters {params}')
def step_set_toggle(context, params):
    """{service": "a911-svc", "name": "amazing FF", "state": "ON"} """
    params = json.loads(params)
    name = params.get("name")
    state = params.get("state")
    stage = SET_FF_TEMPLATE.safe_substitute(
        stage_name=f"Set toggle {name} as {state} ",
        **params
    ).strip("\n")
    context.stages.append(stage)

@then('Run BDD tests')
def step_run_bdd_tests(context):
    context.stages.append(RUN_BDD_TESTS_TEMPLATE.safe_substitute(
        stage_name="Run BDD tests"
    ).strip("\n"))
