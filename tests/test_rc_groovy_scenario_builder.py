from unittest.mock import mock_open, patch

from rc_groovy_scenario_builder import RCGroovyScenarioBuilder, SCENARIOS_DIR
from templates import DEPLOY_SVC_TEMPLATE, RUN_BDD_TESTS_TEMPLATE


def test_build_deploy_service_stage(mock_logger, mock_render_template):
    builder = RCGroovyScenarioBuilder("TestScenario", "2024-11-19", mock_logger)
    mock_render_template.return_value = "Filled template to deploy service"
    builder.build_deploy_service_stage(serviceName="a-svc", serviceVersion="1.0")

    mock_render_template.assert_called_once_with(
        template=DEPLOY_SVC_TEMPLATE,
        stage_name="Deploy a-svc 1.0",
        service_name="a-svc",
        service_version="1.0",
        stage_passed_variable="deploy_a_svc_passed"
    )

    assert "Filled template to deploy service" in builder.stages
    mock_logger.info.assert_called_with("Deploy a-svc 1.0")


def test_build_run_bdd_tests_stage(mock_logger, mock_render_template):
    builder = RCGroovyScenarioBuilder("TestScenario", "2024-11-19", mock_logger)
    mock_render_template.return_value = "Filled template to run tests"

    builder.build_run_bdd_tests_stage(marks=["smoke", "regression"])

    mock_render_template.assert_called_once_with(
        template=RUN_BDD_TESTS_TEMPLATE,
        stage_name="Run BDD tests with marks: smoke, regression",
        marks="smoke, regression"
    )
    assert "Filled template to run tests" in builder.stages
    mock_logger.info.assert_called_with("Run BDD tests with marks: smoke, regression")


def test_save_groovy_file(mock_logger, mock_render_template):
    builder = RCGroovyScenarioBuilder("TestScenario", "2024-11-19", mock_logger)
    builder.stages = ["Deploy Stage", "Run Tests Stage"]
    mock_render_template.return_value = "groovy file content"

    with patch("rc_groovy_scenario_builder.open", mock_open()) as mock_file:
        builder.save_groovy_file()
        mock_file.assert_called_once_with(f"{SCENARIOS_DIR}/2024-11-19.groovy", "w")
        mock_file().write.assert_called_once_with("groovy file content")

    mock_logger.success.assert_called_with(f"File '{SCENARIOS_DIR}/2024-11-19.groovy' successfully generated")
