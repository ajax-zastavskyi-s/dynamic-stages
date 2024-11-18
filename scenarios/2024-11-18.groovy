import org.jenkinsci.plugins.pipeline.modeldefinition.Utils

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
            name: "Deploy space-svc 123.RELEASE",
            steps: {
                script {
                    dynamicStagesResults = getDynamicStagesResults()
                    if (dynamicStagesResults.every { stage_passed -> stage_passed.value == true }) {
                        dynamicStagesResults['deploy_space_svc_passed'] = rc_testing.deployService(
                            serviceName="space-svc",
                            serviceVersion="123.RELEASE"
                        )
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
        [
            name: "Deploy external-device-svc 124.RELEASE",
            steps: {
                script {
                    dynamicStagesResults = getDynamicStagesResults()
                    if (dynamicStagesResults.every { stage_passed -> stage_passed.value == true }) {
                        dynamicStagesResults['deploy_external_device_svc_passed'] = rc_testing.deployService(
                            serviceName="external-device-svc",
                            serviceVersion="124.RELEASE"
                        )
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
        [
            name: "Run BDD tests with marks: smart_home, settings_and_statuses",
            steps: {
                script {
                    dynamicStagesResults = getDynamicStagesResults()
                    
                    if (dynamicStagesResults.every { stage_passed -> stage_passed.value == true }) {
                        dynamicStagesResults['setup_generation_passed'] = rc_testing.generateSetups()
                        if (dynamicStagesResults['setup_generation_passed']) {
                                rc_testing.respawnActors()
                                rc_testing.unlockRegressSetups()
                        }
                    }
                    else {
                        def failedStage = dynamicStagesResults.find { stage_passed -> stage_passed.value == false }?.key
                        echo "Skip setup generation due to failure: ${failedStage} == false"
                        Utils.markStageSkippedForConditional(env.STAGE_NAME)
                    }

                    if (dynamicStagesResults.every { stage_passed -> stage_passed.value == true }) {
                        rc_testing.runBDDTests(
                            marks='smart_home, settings_and_statuses'
                        )
                    }
                    else {
                        def failedStage = dynamicStagesResults.find { stage_passed -> stage_passed.value == false }?.key 
                        echo "Skip running BDD tests due to failure: ${failedStage} == false"
                        Utils.markStageSkippedForConditional(env.STAGE_NAME)
                    }

                    env.dynamicStagesResults = ""
                }
            }
        ],
        [
            name: "Deploy user-svc 125.RELEASE",
            steps: {
                script {
                    dynamicStagesResults = getDynamicStagesResults()
                    if (dynamicStagesResults.every { stage_passed -> stage_passed.value == true }) {
                        dynamicStagesResults['deploy_user_svc_passed'] = rc_testing.deployService(
                            serviceName="user-svc",
                            serviceVersion="125.RELEASE"
                        )
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
        [
            name: "Run BDD tests",
            steps: {
                script {
                    dynamicStagesResults = getDynamicStagesResults()
                    
                    if (dynamicStagesResults.every { stage_passed -> stage_passed.value == true }) {
                        dynamicStagesResults['setup_generation_passed'] = rc_testing.generateSetups()
                        if (dynamicStagesResults['setup_generation_passed']) {
                                rc_testing.respawnActors()
                                rc_testing.unlockRegressSetups()
                        }
                    }
                    else {
                        def failedStage = dynamicStagesResults.find { stage_passed -> stage_passed.value == false }?.key
                        echo "Skip setup generation due to failure: ${failedStage} == false"
                        Utils.markStageSkippedForConditional(env.STAGE_NAME)
                    }

                    if (dynamicStagesResults.every { stage_passed -> stage_passed.value == true }) {
                        rc_testing.runBDDTests(
                            marks='Empty'
                        )
                    }
                    else {
                        def failedStage = dynamicStagesResults.find { stage_passed -> stage_passed.value == false }?.key 
                        echo "Skip running BDD tests due to failure: ${failedStage} == false"
                        Utils.markStageSkippedForConditional(env.STAGE_NAME)
                    }

                    env.dynamicStagesResults = ""
                }
            }
        ],
    ]
}

return this