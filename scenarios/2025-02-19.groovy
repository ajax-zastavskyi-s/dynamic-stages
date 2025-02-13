import org.jenkinsci.plugins.pipeline.modeldefinition.Utils

def getStages() {
    rc_testing = load("rollout_stage/jenkins/rc_testing.groovy")

    def getDynamicStagesResults = {
        if (env.dynamicStagesResults) {
            return readJSON(text: env.dynamicStagesResults)
        }
        return [:]
    }

    def saveFailedDeploy = {service, version ->
        def failedRCDeploys = env.failedRCDeploys ? env.failedRCDeploys.split(',').toList() : []

        failedRCDeploys.add("Deploy ${service} ${version}")
        env.failedRCDeploys = failedRCDeploys.join(",")
    }

    return [
        [
            name: "Deploy csa 1.122.0-7660.MASTER-SNAPSHOT",
            steps: {
                script {
                    parallel (
                        "csa 1.122.0-7660.MASTER-SNAPSHOT": {
                            dynamicStagesResults = getDynamicStagesResults()

                            def serviceVersionFromPattern = rc_testing.getLatestServiceVersionByPattern(
                                serviceName="csa",
                                serviceVersionPattern="1.122.0-7660.MASTER-SNAPSHOT"
                            )

                            dynamicStagesResults['deploy_csa_passed'] = rc_testing.deployService(
                                serviceName="csa",
                                serviceVersion=serviceVersionFromPattern,
                                deploymentDestination="k8",
                            )
                            if (dynamicStagesResults['deploy_csa_passed'] == false) {
                                saveFailedDeploy("csa", "1.122.0-7660.MASTER-SNAPSHOT")
                            }

                            env.dynamicStagesResults = groovy.json.JsonOutput.toJson(dynamicStagesResults)
                        },
                    )
                }
            }
        ],
        [
            name: "Restore toggles ADD_SERVICE_STATE_TO_HUB_IN_DESKTOP_GW_SPACE_STREAM [a911-svc] | ENABLE_FALLBACK_ARM_PROCESSING_VIA_HUB_SVC [cloud-api-svc] | NotificationHandlingErrorRoute [communication-svc]",
            steps: {
                script {
                    if (dynamicStagesResults.every { stage_passed -> stage_passed.value == true }) {
                        parallel (
                        "ADD_SERVICE_STATE_TO_HUB_IN_DESKTOP_GW_SPACE_STREAM [a911-svc]": {
                            dynamicStagesResults = getDynamicStagesResults()

                            dynamicStagesResults['restore_add_service_state_to_hub_in_desktop_gw_space_stream_passed'] = rc_testing.restoreToggle(
                                serviceName='a911-svc',
                                featureFlagName='ADD_SERVICE_STATE_TO_HUB_IN_DESKTOP_GW_SPACE_STREAM',
                                featureFlagState='True',
                                additionalData='{}'
                            )
                            if (dynamicStagesResults['restore_add_service_state_to_hub_in_desktop_gw_space_stream_passed'] == false) {
                                saveFailedDeploy("ADD_SERVICE_STATE_TO_HUB_IN_DESKTOP_GW_SPACE_STREAM", "a911-svc")
                            }

                            env.dynamicStagesResults = groovy.json.JsonOutput.toJson(dynamicStagesResults)
                        },
                        "ENABLE_FALLBACK_ARM_PROCESSING_VIA_HUB_SVC [cloud-api-svc]": {
                            dynamicStagesResults = getDynamicStagesResults()

                            dynamicStagesResults['restore_enable_fallback_arm_processing_via_hub_svc_passed'] = rc_testing.restoreToggle(
                                serviceName='cloud-api-svc',
                                featureFlagName='ENABLE_FALLBACK_ARM_PROCESSING_VIA_HUB_SVC',
                                featureFlagState='False',
                                additionalData='{"client-ids": ""}'
                            )
                            if (dynamicStagesResults['restore_enable_fallback_arm_processing_via_hub_svc_passed'] == false) {
                                saveFailedDeploy("ENABLE_FALLBACK_ARM_PROCESSING_VIA_HUB_SVC", "cloud-api-svc")
                            }

                            env.dynamicStagesResults = groovy.json.JsonOutput.toJson(dynamicStagesResults)
                        },
                        "NotificationHandlingErrorRoute [communication-svc]": {
                            dynamicStagesResults = getDynamicStagesResults()

                            dynamicStagesResults['restore_notificationhandlingerrorroute_passed'] = rc_testing.restoreToggle(
                                serviceName='communication-svc',
                                featureFlagName='NotificationHandlingErrorRoute',
                                featureFlagState='True',
                                additionalData='{"config": "[\\n {\\n "expression", "\\n "team": "video"\\n }\\n]", "\\n {\\n "expression": "message is too big"}'
                            )
                            if (dynamicStagesResults['restore_notificationhandlingerrorroute_passed'] == false) {
                                saveFailedDeploy("NotificationHandlingErrorRoute", "communication-svc")
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
                            test_plan_name='RC [csa 1.122.0-7660.MASTER-SNAPSHOT]'
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