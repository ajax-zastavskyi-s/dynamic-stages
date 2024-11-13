Feature: Showing off behave
  Scenario: 12_11_24
    When Deploy service "user-svc" with parameters {"version": "123.RELEASE"}
    Then Run BDD tests with parameters {}
