Feature: Showing off behave
  Scenario: 14_11_24
    When Deploy service "space-svc" with parameters {"version": "123.RELEASE"}
    When Deploy service "external-device-svc" with parameters {"version": "123.RELEASE"}
    Then Run BDD tests with parameters {"marks": ["settings_and_statuses"]}
