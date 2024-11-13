def getStages() {
    return [
        [
            name: "Deploy user-svc 123.RELEASE",
            steps: {
                script {
                    rc_testing.deployService(
                        serviceName: "user-svc",
                        serviceVersion: "123.RELEASE"
                    )
                }
            }
        ],
        [
            name: "Set toggle amazing FF as ON ",
            steps: {
                script {
                    rc_testing.setToggleState(
                        toggleName: "amazing FF",
                        serviceName: "service",
                        toggleState: "ON"
                    )
                }
            }
        ],
        [
            name: "Run BDD tests",
            steps: {
                script {
                    rc_testing.runBDDTests()
                }
            }
        ],
    ]
}

return this