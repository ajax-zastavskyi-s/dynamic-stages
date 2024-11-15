Feature: Showing off behave
  Scenario: 15_11_24
    When Deploy service "user-svc" with parameters {"version": "123.RELEASE"}
    And Deploy service "space-svc" with parameters {"version": "123.RELEASE"}
    Then Run BDD tests with parameters {}

    When Deploy service "external-device-svc" with parameters {"version": "124.RELEASE"}
    Then Run BDD tests with parameters {"marks": "smart_home"}
