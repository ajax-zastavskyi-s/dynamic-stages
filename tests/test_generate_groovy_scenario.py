from unittest.mock import mock_open, patch, MagicMock
import json
from generate_groovy_scenario import main, generate_groovy_file


def test_main_valid_json(mocker, json_scenario):
    mocker.patch("generate_groovy_scenario.open", mock_open(read_data=json.dumps(json_scenario)))

    mock_generate_groovy_file = mocker.patch("generate_groovy_scenario.generate_groovy_file")

    with patch("sys.argv", ["script_name", "--json_scenario_path", "scenario.json"]):
        main()

    mock_generate_groovy_file.assert_called_once()


def test_main_invalid_json(mocker, mock_logger):
    mocker.patch("builtins.open", mock_open(read_data='invalid json'))
    mock_sys_exit = mocker.patch("generate_groovy_scenario.exit")
    mock_get_logger = mocker.patch("generate_groovy_scenario.get_logger")
    mock_get_logger.return_value = mock_logger

    with patch("sys.argv", ["script_name", "--json_scenario_path", "scenario.json"]):
        main()

    mock_logger.error.assert_called_with("Failed to parse JSON scenario")
    mock_sys_exit.assert_called_once_with(1)

def test_generate_groovy_file(mock_logger, mock_builder, json_scenario):
    with patch("generate_groovy_scenario.RCGroovyScenarioBuilder", return_value=mock_builder):
        mock_build_deploy_service_stage = MagicMock()
        build_run_bdd_tests_stage = MagicMock()
        mock_builder.get_builder_method.side_effect = [mock_build_deploy_service_stage, build_run_bdd_tests_stage]
        generate_groovy_file(json_scenario, mock_logger)


    assert mock_builder.get_builder_method.call_count == len(json_scenario["stages"])
    mock_build_deploy_service_stage.assert_called_once_with(serviceName="serviceA", serviceVersion="1.0")
    build_run_bdd_tests_stage.assert_called_once_with(marks=["smoke"])
    mock_builder.save_groovy_file.assert_called_once()
