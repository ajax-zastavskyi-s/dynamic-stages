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
            name: "Deploy a911-svc 1.122.0-7660.MASTER-SNAPSHOT",
            steps: {
                script {
                    parallel (
                        "a911-svc 1.122.0-7660.MASTER-SNAPSHOT": {
                            dynamicStagesResults = getDynamicStagesResults()

                            def serviceVersionFromPattern = rc_testing.getLatestServiceVersionByPattern(
                                serviceName="a911-svc",
                                serviceVersionPattern="1.122.0-7660.MASTER-SNAPSHOT"
                            )

                            dynamicStagesResults['deploy_a911_svc_passed'] = rc_testing.deployService(
                                serviceName="a911-svc",
                                serviceVersion=serviceVersionFromPattern,
                                deploymentDestination="ec2",
                            )
                            if (dynamicStagesResults['deploy_a911_svc_passed'] == false) {
                                saveFailedStages("a911-svc", "1.122.0-7660.MASTER-SNAPSHOT")
                            }

                            env.dynamicStagesResults = groovy.json.JsonOutput.toJson(dynamicStagesResults)
                        },
                    )
                }
            }
        ],
        [
            name: "Restore toggles REDIRECT_RESEND_CONFIRMATION_CODES_TO_USER_SVC [a911-svc]",
            steps: {
                script {
                    dynamicStagesResults = getDynamicStagesResults()
                    if (dynamicStagesResults.every { stage_passed -> stage_passed.value == true }) {
                        parallel (
                        "REDIRECT_RESEND_CONFIRMATION_CODES_TO_USER_SVC [a911-svc]": {
                            dynamicStagesResults = getDynamicStagesResults()

                            dynamicStagesResults['restore_redirect_resend_confirmation_codes_to_user_svc_passed'] = rc_testing.setToggle(
                                serviceName='a911-svc',
                                featureFlagName='REDIRECT_RESEND_CONFIRMATION_CODES_TO_USER_SVC',
                                featureFlagState='False',
                                additionalData='{"cliend-id": "+"}'
                            )
                            if (dynamicStagesResults['restore_redirect_resend_confirmation_codes_to_user_svc_passed'] == false) {
                                saveFailedStages("REDIRECT_RESEND_CONFIRMATION_CODES_TO_USER_SVC", "a911-svc")
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
                            test_plan_name='RC [a911-svc 1.122.0-7660.MASTER-SNAPSHOT]',
                            test_plan_description='RC Testing. Updated toggles: REDIRECT_RESEND_CONFIRMATION_CODES_TO_USER_SVC [a911-svc]'
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