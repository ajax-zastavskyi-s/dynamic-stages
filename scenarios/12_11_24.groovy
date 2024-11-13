def getStages() {
    rc_testing = load("rollout_stage/jenkins/rc_testing.groovy")

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
            name: "Run BDD tests",
            steps: {
                script {
                    rc_testing.runBDDTests(
                        marks: Empty
                    )
                }
            }
        ],
    ]
}

return this