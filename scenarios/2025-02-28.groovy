import org.jenkinsci.plugins.pipeline.modeldefinition.Utils

def getStages() {
    rc_testing = load("rollout_stage/jenkins/rc_testing.groovy")

    def getDynamicStagesResults = {
        if (env.dynamicStagesResults) {
            return readJSON(text: env.dynamicStagesResults)
        }
        return [:]
    }

    def saveFailedStages = {failedStageMessage ->
        def failedRCStages = env.failedRCStages ? env.failedRCStages.split(',').toList() : []

        failedRCStages.add(failedStageMessage)
        env.failedRCStages = failedRCStages.join(",")
    }

    return [
        [
            name: "Deploy user-svc 1.32.0*.RELEASE | a911-svc 1.130.0*.RELEASE | cms-gw-svc 1.39.0*.RELEASE | external-device-svc 1.43.0*.RELEASE",
            steps: {
                script {
                    parallel (
                        "user-svc 1.32.0*.RELEASE": {
                            dynamicStagesResults = getDynamicStagesResults()

                            def serviceVersionFromPattern = rc_testing.getLatestServiceVersionByPattern(
                                serviceName="user-svc",
                                serviceVersionPattern="1.32.0*.RELEASE"
                            )
    
                            dynamicStagesResults['deploy_user_svc_passed'] = rc_testing.deployService(
                                serviceName="user-svc",
                                serviceVersion=serviceVersionFromPattern,
                                deploymentDestination="null",
                            )
                            if (dynamicStagesResults['deploy_user_svc_passed'] == false) {
                                saveFailedStages("Deploy user-svc with version 1.32.0*.RELEASE")
                            }
        
                            env.dynamicStagesResults = groovy.json.JsonOutput.toJson(dynamicStagesResults)
                        },
                        "a911-svc 1.130.0*.RELEASE": {
                            dynamicStagesResults = getDynamicStagesResults()

                            def serviceVersionFromPattern = rc_testing.getLatestServiceVersionByPattern(
                                serviceName="a911-svc",
                                serviceVersionPattern="1.130.0*.RELEASE"
                            )
    
                            dynamicStagesResults['deploy_a911_svc_passed'] = rc_testing.deployService(
                                serviceName="a911-svc",
                                serviceVersion=serviceVersionFromPattern,
                                deploymentDestination="null",
                            )
                            if (dynamicStagesResults['deploy_a911_svc_passed'] == false) {
                                saveFailedStages("Deploy a911-svc with version 1.130.0*.RELEASE")
                            }
        
                            env.dynamicStagesResults = groovy.json.JsonOutput.toJson(dynamicStagesResults)
                        },
                        "cms-gw-svc 1.39.0*.RELEASE": {
                            dynamicStagesResults = getDynamicStagesResults()

                            def serviceVersionFromPattern = rc_testing.getLatestServiceVersionByPattern(
                                serviceName="cms-gw-svc",
                                serviceVersionPattern="1.39.0*.RELEASE"
                            )
    
                            dynamicStagesResults['deploy_cms_gw_svc_passed'] = rc_testing.deployService(
                                serviceName="cms-gw-svc",
                                serviceVersion=serviceVersionFromPattern,
                                deploymentDestination="null",
                            )
                            if (dynamicStagesResults['deploy_cms_gw_svc_passed'] == false) {
                                saveFailedStages("Deploy cms-gw-svc with version 1.39.0*.RELEASE")
                            }
        
                            env.dynamicStagesResults = groovy.json.JsonOutput.toJson(dynamicStagesResults)
                        },
                        "external-device-svc 1.43.0*.RELEASE": {
                            dynamicStagesResults = getDynamicStagesResults()

                            def serviceVersionFromPattern = rc_testing.getLatestServiceVersionByPattern(
                                serviceName="external-device-svc",
                                serviceVersionPattern="1.43.0*.RELEASE"
                            )
    
                            dynamicStagesResults['deploy_external_device_svc_passed'] = rc_testing.deployService(
                                serviceName="external-device-svc",
                                serviceVersion=serviceVersionFromPattern,
                                deploymentDestination="null",
                            )
                            if (dynamicStagesResults['deploy_external_device_svc_passed'] == false) {
                                saveFailedStages("Deploy external-device-svc with version 1.43.0*.RELEASE")
                            }
        
                            env.dynamicStagesResults = groovy.json.JsonOutput.toJson(dynamicStagesResults)
                        },
                    )
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
                            test_plan_name='RC [user-svc 1.32.0*.RELEASE | a911-svc 1.130.0*.RELEASE | cms-gw-svc 1.39.0*.RELEASE | external-device-svc 1.43.0*.RELEASE]',
                            test_plan_description='RC Testing'
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
        [
            name: "Deploy cloud-signaling-svc 1.109.0*.RELEASE",
            steps: {
                script {
                    parallel (
                        "cloud-signaling-svc 1.109.0*.RELEASE": {
                            dynamicStagesResults = getDynamicStagesResults()

                            def serviceVersionFromPattern = rc_testing.getLatestServiceVersionByPattern(
                                serviceName="cloud-signaling-svc",
                                serviceVersionPattern="1.109.0*.RELEASE"
                            )
    
                            dynamicStagesResults['deploy_cloud_signaling_svc_passed'] = rc_testing.deployService(
                                serviceName="cloud-signaling-svc",
                                serviceVersion=serviceVersionFromPattern,
                                deploymentDestination="null",
                            )
                            if (dynamicStagesResults['deploy_cloud_signaling_svc_passed'] == false) {
                                saveFailedStages("Deploy cloud-signaling-svc with version 1.109.0*.RELEASE")
                            }
        
                            env.dynamicStagesResults = groovy.json.JsonOutput.toJson(dynamicStagesResults)
                        },
                    )
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
                            test_plan_name='RC [user-svc 1.32.0*.RELEASE | a911-svc 1.130.0*.RELEASE | cms-gw-svc 1.39.0*.RELEASE | external-device-svc 1.43.0*.RELEASE | cloud-signaling-svc 1.109.0*.RELEASE]',
                            test_plan_description='RC Testing'
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
        [
            name: "Deploy company-svc 1.98.0*.RELEASE",
            steps: {
                script {
                    parallel (
                        "company-svc 1.98.0*.RELEASE": {
                            dynamicStagesResults = getDynamicStagesResults()

                            def serviceVersionFromPattern = rc_testing.getLatestServiceVersionByPattern(
                                serviceName="company-svc",
                                serviceVersionPattern="1.98.0*.RELEASE"
                            )
    
                            dynamicStagesResults['deploy_company_svc_passed'] = rc_testing.deployService(
                                serviceName="company-svc",
                                serviceVersion=serviceVersionFromPattern,
                                deploymentDestination="null",
                            )
                            if (dynamicStagesResults['deploy_company_svc_passed'] == false) {
                                saveFailedStages("Deploy company-svc with version 1.98.0*.RELEASE")
                            }
        
                            env.dynamicStagesResults = groovy.json.JsonOutput.toJson(dynamicStagesResults)
                        },
                    )
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
                            test_plan_name='RC [user-svc 1.32.0*.RELEASE | a911-svc 1.130.0*.RELEASE | cms-gw-svc 1.39.0*.RELEASE | external-device-svc 1.43.0*.RELEASE | cloud-signaling-svc 1.109.0*.RELEASE | company-svc 1.98.0*.RELEASE]',
                            test_plan_description='RC Testing'
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
        [
            name: "Deploy communication-svc 1.73.0*.RELEASE",
            steps: {
                script {
                    parallel (
                        "communication-svc 1.73.0*.RELEASE": {
                            dynamicStagesResults = getDynamicStagesResults()

                            def serviceVersionFromPattern = rc_testing.getLatestServiceVersionByPattern(
                                serviceName="communication-svc",
                                serviceVersionPattern="1.73.0*.RELEASE"
                            )
    
                            dynamicStagesResults['deploy_communication_svc_passed'] = rc_testing.deployService(
                                serviceName="communication-svc",
                                serviceVersion=serviceVersionFromPattern,
                                deploymentDestination="null",
                            )
                            if (dynamicStagesResults['deploy_communication_svc_passed'] == false) {
                                saveFailedStages("Deploy communication-svc with version 1.73.0*.RELEASE")
                            }
        
                            env.dynamicStagesResults = groovy.json.JsonOutput.toJson(dynamicStagesResults)
                        },
                    )
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
                            test_plan_name='RC [user-svc 1.32.0*.RELEASE | a911-svc 1.130.0*.RELEASE | cms-gw-svc 1.39.0*.RELEASE | external-device-svc 1.43.0*.RELEASE | cloud-signaling-svc 1.109.0*.RELEASE | company-svc 1.98.0*.RELEASE | communication-svc 1.73.0*.RELEASE]',
                            test_plan_description='RC Testing'
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
        [
            name: "Restore toggles ENABLE_PROCESSING_HUB_COMMAND_QUEUE_MESSAGE_TYPE [a911-svc]: disabled",
            steps: {
                script {
                    dynamicStagesResults = getDynamicStagesResults()
                    if (dynamicStagesResults.every { stage_passed -> stage_passed.value == true }) {
                        parallel (
                        "ENABLE_PROCESSING_HUB_COMMAND_QUEUE_MESSAGE_TYPE [a911-svc]": {
                            dynamicStagesResults = getDynamicStagesResults()
    
                            dynamicStagesResults['restore_enable_processing_hub_command_queue_message_type_passed'] = rc_testing.setToggle(
                                serviceName='a911-svc',
                                featureFlagName='ENABLE_PROCESSING_HUB_COMMAND_QUEUE_MESSAGE_TYPE',
                                featureFlagState='False',
                                additionalData='{"pattern": "^(002714EE|002D02C8)"}'
                            )
                            if (dynamicStagesResults['restore_enable_processing_hub_command_queue_message_type_passed'] == false) {
                                saveFailedStages("Set toggle ENABLE_PROCESSING_HUB_COMMAND_QUEUE_MESSAGE_TYPE on a911-svc")
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
                            test_plan_description='RC Testing. Updated toggles: ENABLE_PROCESSING_HUB_COMMAND_QUEUE_MESSAGE_TYPE [a911-svc]: disabled'
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