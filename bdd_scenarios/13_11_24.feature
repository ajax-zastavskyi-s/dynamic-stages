Feature: Showing off behave
  Scenario: 13_11_24
    When Deploy service "user-svc" with parameters {"version": "123.RELEASE"}
    And Deploy service "user-svc" with parameters {"version": "124.RELEASE"}
    Then Run BDD tests with parameters {"marks": ["settings_and_statuses"]}
