import uuid
from unittest.mock import mock_open, patch

from rc_groovy_scenario_builder import RCGroovyScenarioBuilder, SCENARIOS_DIR
from templates import RUN_BDD_TESTS_TEMPLATE


def test_build_deploy_service_stage(mock_logger, mock_render_template):
    builder = RCGroovyScenarioBuilder(
        "TestScenario", str(uuid.uuid4())[:8], "2024-11-19", mock_logger)
    mock_render_template.return_value = "Filled template to deploy service"

    builder.build_deploy_service_stage(serviceName="a-svc", serviceVersion="1.0")

    expected_stage = {
        "stage_name": "a-svc 1.0",
        "service_name": "a-svc",
        "service_version": "1.0",
        "deployment_destination": "null",
        "stage_passed_variable": "deploy_a_svc_passed"
    }

    assert expected_stage in builder.deployments
    assert expected_stage['stage_name'] in builder.deployed_services


def test_build_run_bdd_tests_stage(mock_logger, mock_render_template):
    builder = RCGroovyScenarioBuilder(
        "TestScenario", str(uuid.uuid4())[:8], "2024-11-19", mock_logger)
    builder.deployments = [
        {
            "stage_name": "a-svc 1.0",
            "service_name": "a-svc",
            "service_version": "1.0",
            "deployment_destination": "null",
            "stage_passed_variable": "deploy_a_svc_passed"
        }
    ]
    builder.deployed_services = [
        "a-svc 1.0"
    ]

    mock_render_template.side_effect = lambda template, **kwargs: f"Rendered with {kwargs}"

    with patch.object(builder, "_build_parallel_deployments_stage") as mock_build_parallel_deployments_stage:
        builder.build_run_bdd_tests_stage(marks=["smoke", "regression"])

    mock_render_template.assert_any_call(
        template=RUN_BDD_TESTS_TEMPLATE,
        stage_name="Run BDD tests with marks: smoke, regression",
        marks="smoke, regression",
        test_plan_description="RC Testing",
        test_plan_name="RC [a-svc 1.0]"
    )
    mock_build_parallel_deployments_stage.assert_called_once()
    assert ("Rendered with {'stage_name': 'Run BDD tests with marks: "
            + "smoke, regression', 'marks': 'smoke, regression', 'test_plan_name': "
            + "'RC [a-svc 1.0]', 'test_plan_description': 'RC Testing'}") in builder.stages
    mock_logger.info.assert_called_with("Run BDD tests with marks: smoke, regression")


def test_save_groovy_file(mock_logger, mock_render_template):
    scenario_id = str(uuid.uuid4())[:8],
    builder = RCGroovyScenarioBuilder("TestScenario", scenario_id, "2024-11-19", mock_logger)
    builder.stages = ["Deploy Stage", "Run Tests Stage"]
    mock_render_template.return_value = "groovy file content"

    with patch("rc_groovy_scenario_builder.open", mock_open()) as mock_file:
        builder.save_groovy_file()

        mock_file.assert_called_once_with(f"{SCENARIOS_DIR}/2024-11-19/{scenario_id}.groovy", "w")
        mock_file().write.assert_called_once_with("groovy file content")

    mock_logger.success.assert_called_with(
        f"File '{SCENARIOS_DIR}/2024-11-19/{scenario_id}.groovy' successfully generated")
