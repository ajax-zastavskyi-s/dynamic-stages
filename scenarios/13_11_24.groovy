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
            name: "Deploy user-svc 124.RELEASE",
            steps: {
                script {
                    rc_testing.deployService(
                        serviceName: "user-svc",
                        serviceVersion: "124.RELEASE"
                    )
                }
            }
        ],
        [
            name: "Run BDD tests ['settings_and_statuses']",
            steps: {
                script {
                    rc_testing.runBDDTests(
                        marks: ['settings_and_statuses']
                    )
                }
            }
        ],
    ]
}

return this