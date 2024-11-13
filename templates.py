from string import Template

BASE_GROOVY_TEMPLATE = Template("""
def getStages() {
    rc_testing = load("rollout_stage/jenkins/rc_testing.groovy")

    return [
$stages
    ]
}

return this

""")

DEPLOY_SVC_TEMPLATE = Template("""
        [
            name: "$stage_name",
            steps: {
                script {
                    rc_testing.deployService(
                        serviceName: "$service_name",
                        serviceVersion: "$service_version"
                    )
                }
            }
        ],
""")


RUN_BDD_TESTS_TEMPLATE = Template("""
        [
            name: "$stage_name",
            steps: {
                script {
                    rc_testing.runBDDTests(
                        marks: $marks
                    )
                }
            }
        ],
""")
