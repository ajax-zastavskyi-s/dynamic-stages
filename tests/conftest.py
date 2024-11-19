from unittest.mock import MagicMock, patch

import pytest


@pytest.fixture
def mock_logger():
    return MagicMock()


@pytest.fixture
def mock_builder():
    return MagicMock()


@pytest.fixture
def json_scenario():
    return {
        "scenarioName": "TestScenario",
        "date": "2024-11-19",
        "stages": [
            {"name": "deployService", "parameters": {"serviceName": "serviceA", "serviceVersion": "1.0"}},
            {"name": "runTests", "parameters": {"marks": ["smoke"]}}
        ]
    }


@pytest.fixture
def mock_render_template():
    with patch("rc_groovy_scenario_builder.render_template") as mock:
        yield mock
