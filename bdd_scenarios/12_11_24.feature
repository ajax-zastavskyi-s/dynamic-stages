Feature: Showing off behave
  Scenario: 12_11_24
    When Deploy service "user-svc" with parameters {"version": "123.RELEASE"}
    And Set FF with parameters {"service": "a911-svc", "name": "amazing FF", "state": "ON"}
    Then Run BDD tests
