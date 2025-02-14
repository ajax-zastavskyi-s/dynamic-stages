import org.jenkinsci.plugins.pipeline.modeldefinition.Utils

def getStages() {
    rc_testing = load("rollout_stage/jenkins/rc_testing.groovy")

    def getDynamicStagesResults = {
        if (env.dynamicStagesResults) {
            return readJSON(text: env.dynamicStagesResults)
        }
        return [:]
    }

    def saveFailedStages = {stage_name, stage_identifier ->
        def failedRCStages = env.failedRCStages ? env.failedRCStages.split(',').toList() : []

        failedRCStages.add("Stage ${stage_name} ${stage_identifier}")
        env.failedRCStages = failedRCStages.join(",")
    }

    return [
        [
            name: "Restore toggles REDIRECT_RESEND_CONFIRMATION_CODES_TO_USER_SVC [csa] | ADD_SERVICE_STATE_TO_HUB_IN_DESKTOP_GW_SPACE_STREAM [a911-svc]",
            steps: {
                script {
                    dynamicStagesResults = getDynamicStagesResults()
                    if (dynamicStagesResults.every { stage_passed -> stage_passed.value == true }) {
                        parallel (
                        "REDIRECT_RESEND_CONFIRMATION_CODES_TO_USER_SVC [csa]": {
                            dynamicStagesResults = getDynamicStagesResults()

                            dynamicStagesResults['restore_redirect_resend_confirmation_codes_to_user_svc_passed'] = rc_testing.setToggle(
                                serviceName='csa',
                                featureFlagName='REDIRECT_RESEND_CONFIRMATION_CODES_TO_USER_SVC',
                                featureFlagState='True',
                                additionalData='{}'
                            )
                            if (dynamicStagesResults['restore_redirect_resend_confirmation_codes_to_user_svc_passed'] == false) {
                                saveFailedStages("REDIRECT_RESEND_CONFIRMATION_CODES_TO_USER_SVC", "csa")
                            }

                            env.dynamicStagesResults = groovy.json.JsonOutput.toJson(dynamicStagesResults)
                        },
                        "ADD_SERVICE_STATE_TO_HUB_IN_DESKTOP_GW_SPACE_STREAM [a911-svc]": {
                            dynamicStagesResults = getDynamicStagesResults()

                            dynamicStagesResults['restore_add_service_state_to_hub_in_desktop_gw_space_stream_passed'] = rc_testing.setToggle(
                                serviceName='a911-svc',
                                featureFlagName='ADD_SERVICE_STATE_TO_HUB_IN_DESKTOP_GW_SPACE_STREAM',
                                featureFlagState='True',
                                additionalData='{}'
                            )
                            if (dynamicStagesResults['restore_add_service_state_to_hub_in_desktop_gw_space_stream_passed'] == false) {
                                saveFailedStages("ADD_SERVICE_STATE_TO_HUB_IN_DESKTOP_GW_SPACE_STREAM", "a911-svc")
                            }

                            env.dynamicStagesResults = groovy.json.JsonOutput.toJson(dynamicStagesResults)
                        },
                        )
                    } else {
                        def failedStage = dynamicStagesResults.find { stage_passed -> stage_passed.value == false }?.key
                        echo "Skip restore toggles due to failure: ${failedStage} == false"
                    }
                }
            }
        ],
        [
            name: "Run BDD tests",
            steps: {
                script {
                    dynamicStagesResults = getDynamicStagesResults()

                    if (dynamicStagesResults.every { stage_passed -> stage_passed.value == true }) {
                        rc_testing.clearUsedObjects()
                        rc_testing.respawnActors()
                        dynamicStagesResults['setup_generation_passed'] = rc_testing.generateSetups()
                    }
                    else {
                        def failedStage = dynamicStagesResults.find { stage_passed -> stage_passed.value == false }?.key
                        echo "Skip setup generation due to failure: ${failedStage} == false"
                    }

                    if (dynamicStagesResults.every { stage_passed -> stage_passed.value == true }) {
                        rc_testing.runBDDTests(
                            marks='Empty',
                            test_plan_name='RC [Undefined]',
                            test_plan_description='RC Testing. Updated toggles: REDIRECT_RESEND_CONFIRMATION_CODES_TO_USER_SVC [csa] | ADD_SERVICE_STATE_TO_HUB_IN_DESKTOP_GW_SPACE_STREAM [a911-svc]'
                        )
                    }
                    else {
                        def failedStage = dynamicStagesResults.find { stage_passed -> stage_passed.value == false }?.key 
                        echo "Skip running BDD tests due to failure: ${failedStage} == false"
                        if (failedStage != "setup_generation_passed") {
                            Utils.markStageSkippedForConditional(env.STAGE_NAME)
                        }
                    }

                    env.dynamicStagesResults = ""
                }
            }
        ],
    ]
}

return this