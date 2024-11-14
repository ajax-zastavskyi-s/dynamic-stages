from behave import step, then
import json

from helpers import render_template
from templates import DEPLOY_SVC_TEMPLATE, RUN_BDD_TESTS_TEMPLATE, GENERATE_SETUPS_TEMPLATE, RESPAWN_ACTORS_TEMPLATE, \
    UNLOCK_SETUPS_TEMPLATE


@step('Deploy service "{service_name}" with parameters {params}')
def step_deploy_service(context, service_name, params):
    """a911-svc {version": "1.111.0-7151.MASTER-SNAPSHOT"} """
    params = json.loads(params)
    version = params.get("version")

    stage_passed_variable = f"deploy_{service_name.lower().replace('-', '_')}_passed"

    stage = render_template(
        template=DEPLOY_SVC_TEMPLATE,
        stage_name=f"Deploy {service_name} {version}",
        service_name=service_name,
        service_version=version,
        stage_passed_variable=stage_passed_variable,
    )
    context.stages.append(stage)

    context.dependent_stages_results_variables.append(stage_passed_variable)


@then('Run BDD tests with parameters {params}')
def step_run_bdd_tests(context, params):
    params = json.loads(params)
    stage_name = "Run BDD tests"
    if marks := params.get("marks"):
        stage_name = f"{stage_name} {marks}"
    else:
        marks = "Empty"

    generate_setups_stage = render_template(
        GENERATE_SETUPS_TEMPLATE,
        stage_name="Generate setups",
        stage_passed_variable="generation_passed",
    )
    respawn_actors_stage = render_template(
        template=RESPAWN_ACTORS_TEMPLATE,
        stage_name="Respawn actors",
    )
    unlock_setups_stage = render_template(
        template=UNLOCK_SETUPS_TEMPLATE,
        stage_name="Unlock setups",
    )
    run_tests_stage = render_template(
        template=RUN_BDD_TESTS_TEMPLATE,
        stage_name=stage_name,
        marks=marks
    )

    context.stages.extend(
        (
            generate_setups_stage,
            respawn_actors_stage,
            unlock_setups_stage,
            run_tests_stage,
        )
    )

    context.dependent_stages_results_variables = []
