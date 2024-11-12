from string import Template

BASE_GROOVY_TEMPLATE = Template("""
def getStages() {
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
SET_FF_TEMPLATE = Template("""
        [
            name: "$stage_name",
            steps: {
                script {
                    rc_testing.setToggleState(
                        toggleName: "$name",
                        serviceName: "service",
                        toggleState: "$state"
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
                    rc_testing.runBDDTests()
                }
            }
        ],
""")
