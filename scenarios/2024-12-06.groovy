import org.jenkinsci.plugins.pipeline.modeldefinition.Utils

def failedDeploys = []

def getStages() {
    rc_testing = load("rollout_stage/jenkins/rc_testing.groovy")

    def getDynamicStagesResults = {
        if (env.dynamicStagesResults) {
            return readJSON(text: env.dynamicStagesResults)
        }
        return [:]
    }

    return [
        [
            name: "Deploy external-device-svc wrong version",
            steps: {
                script {
                    dynamicStagesResults = getDynamicStagesResults()
                    if (dynamicStagesResults.every { stage_passed -> stage_passed.value == true }) {
                        def serviceVersionFromPattern = rc_testing.getLatestServiceVersionByPattern(
                            serviceName="external-device-svc",
                            serviceVersionPattern="wrong version"
                        )

                        dynamicStagesResults['deploy_external_device_svc_passed'] = rc_testing.deployService(
                            serviceName="external-device-svc",
                            serviceVersion=serviceVersionFromPattern
                        )

                        if (dynamicStagesResults['deploy_external_device_svc_passed'] == false) {
                            failedDeploys << "Deploy external-device-svc wrong version"
                        }
                    }
                    else {
                        def failedStage = dynamicStagesResults.find { stage_passed -> stage_passed.value == false }?.key
                        echo "Skip deploy due to failure: ${failedStage} == false"
                        Utils.markStageSkippedForConditional(env.STAGE_NAME)
                    }

                    env.dynamicStagesResults = groovy.json.JsonOutput.toJson(dynamicStagesResults)
                }
            }
        ],
    ]
}

return this