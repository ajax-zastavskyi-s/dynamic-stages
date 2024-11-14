from string import Template

BASE_GROOVY_TEMPLATE = Template("""
def getStages() {
    rc_testing = load("rollout_stage/jenkins/rc_testing.groovy")

    def getDynamicStagesResults = {
        if (env.dynamicStagesResults) {
            return readJSON(text: env.dynamicStagesResults)
        }
        return [:]
    }

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
                    dynamicStagesResults = getDynamicStagesResults()

                    dynamicStagesResults['$stagePassedVariable'] = rc_testing.deployService(
                        serviceName="$service_name",
                        serviceVersion="$service_version"
                    )

                    env.dynamicStagesResults = groovy.json.JsonOutput.toJson(dynamicStagesResults)
                }
            }
        ],
""")


RUN_BDD_TESTS_TEMPLATE = Template("""
        [
            name: "$stage_name",
            steps: {
                script {
                    dynamicStagesResults = getDynamicStagesResults()

                    if (dynamicStagesResults.every { stage_passed.value == true }) {
                        rc_testing.runBDDTests(
                            marks=$marks
                        )
                    }
                    else {
                        def failedStage = dynamicStagesResults.find { stage_passed -> stage_passed.value == false }?.key 
                        echo "Skip running BDD tests due to failure: ${failedStage} == false"
                        Utils.markStageSkippedForConditional(env.STAGE_NAME)
                    }

                    env.dynamicStagesResults = null
                }
            }
        ],
""")
