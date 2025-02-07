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
            name: "Deploy accounting-svc 1.39.0-*.RELEASE | video-svc 1.69.0-*.RELEASE",
            steps: {
                script {
                    parallel (
                        "accounting-svc 1.39.0-*.RELEASE": {
                            dynamicStagesResults = getDynamicStagesResults()

                            def serviceVersionFromPattern = rc_testing.getLatestServiceVersionByPattern(
                                serviceName="accounting-svc",
                                serviceVersionPattern="1.39.0-*.RELEASE"
                            )
    
                            dynamicStagesResults['deploy_accounting_svc_passed'] = rc_testing.deployService(
                                serviceName="accounting-svc",
                                serviceVersion=serviceVersionFromPattern,
                                deploymentDestination="null",
                            )
                            if (dynamicStagesResults['deploy_accounting_svc_passed'] == false) {
                                saveFailedDeploy("accounting-svc", "1.39.0-*.RELEASE")
                            }
        
                            env.dynamicStagesResults = groovy.json.JsonOutput.toJson(dynamicStagesResults)
                        },
                        "video-svc 1.69.0-*.RELEASE": {
                            dynamicStagesResults = getDynamicStagesResults()

                            def serviceVersionFromPattern = rc_testing.getLatestServiceVersionByPattern(
                                serviceName="video-svc",
                                serviceVersionPattern="1.69.0-*.RELEASE"
                            )
    
                            dynamicStagesResults['deploy_video_svc_passed'] = rc_testing.deployService(
                                serviceName="video-svc",
                                serviceVersion=serviceVersionFromPattern,
                                deploymentDestination="null",
                            )
                            if (dynamicStagesResults['deploy_video_svc_passed'] == false) {
                                saveFailedDeploy("video-svc", "1.69.0-*.RELEASE")
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
                            test_plan_name='RC [accounting-svc 1.39.0-*.RELEASE | video-svc 1.69.0-*.RELEASE]',
                            chats_for_notification='accounting-svc 1.39.0-*.RELEASE | video-svc 1.69.0-*.RELEASE'
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
            name: "Deploy accounting-svc 1.39.0-*.RELEASE | video-svc 1.69.0-*.RELEASE | space-svc 1.82.0-*.RELEASE",
            steps: {
                script {
                    parallel (
                        "accounting-svc 1.39.0-*.RELEASE": {
                            dynamicStagesResults = getDynamicStagesResults()

                            def serviceVersionFromPattern = rc_testing.getLatestServiceVersionByPattern(
                                serviceName="accounting-svc",
                                serviceVersionPattern="1.39.0-*.RELEASE"
                            )
    
                            dynamicStagesResults['deploy_accounting_svc_passed'] = rc_testing.deployService(
                                serviceName="accounting-svc",
                                serviceVersion=serviceVersionFromPattern,
                                deploymentDestination="null",
                            )
                            if (dynamicStagesResults['deploy_accounting_svc_passed'] == false) {
                                saveFailedDeploy("accounting-svc", "1.39.0-*.RELEASE")
                            }
        
                            env.dynamicStagesResults = groovy.json.JsonOutput.toJson(dynamicStagesResults)
                        },
                        "video-svc 1.69.0-*.RELEASE": {
                            dynamicStagesResults = getDynamicStagesResults()

                            def serviceVersionFromPattern = rc_testing.getLatestServiceVersionByPattern(
                                serviceName="video-svc",
                                serviceVersionPattern="1.69.0-*.RELEASE"
                            )
    
                            dynamicStagesResults['deploy_video_svc_passed'] = rc_testing.deployService(
                                serviceName="video-svc",
                                serviceVersion=serviceVersionFromPattern,
                                deploymentDestination="null",
                            )
                            if (dynamicStagesResults['deploy_video_svc_passed'] == false) {
                                saveFailedDeploy("video-svc", "1.69.0-*.RELEASE")
                            }
        
                            env.dynamicStagesResults = groovy.json.JsonOutput.toJson(dynamicStagesResults)
                        },
                        "space-svc 1.82.0-*.RELEASE": {
                            dynamicStagesResults = getDynamicStagesResults()

                            def serviceVersionFromPattern = rc_testing.getLatestServiceVersionByPattern(
                                serviceName="space-svc",
                                serviceVersionPattern="1.82.0-*.RELEASE"
                            )
    
                            dynamicStagesResults['deploy_space_svc_passed'] = rc_testing.deployService(
                                serviceName="space-svc",
                                serviceVersion=serviceVersionFromPattern,
                                deploymentDestination="null",
                            )
                            if (dynamicStagesResults['deploy_space_svc_passed'] == false) {
                                saveFailedDeploy("space-svc", "1.82.0-*.RELEASE")
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
                            test_plan_name='RC [accounting-svc 1.39.0-*.RELEASE | video-svc 1.69.0-*.RELEASE | space-svc 1.82.0-*.RELEASE]',
                            chats_for_notification='space-svc 1.82.0-*.RELEASE'
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
            name: "Deploy accounting-svc 1.39.0-*.RELEASE | video-svc 1.69.0-*.RELEASE | space-svc 1.82.0-*.RELEASE | mobile-gw-svc 1.97.0-*.RELEASE",
            steps: {
                script {
                    parallel (
                        "accounting-svc 1.39.0-*.RELEASE": {
                            dynamicStagesResults = getDynamicStagesResults()

                            def serviceVersionFromPattern = rc_testing.getLatestServiceVersionByPattern(
                                serviceName="accounting-svc",
                                serviceVersionPattern="1.39.0-*.RELEASE"
                            )
    
                            dynamicStagesResults['deploy_accounting_svc_passed'] = rc_testing.deployService(
                                serviceName="accounting-svc",
                                serviceVersion=serviceVersionFromPattern,
                                deploymentDestination="null",
                            )
                            if (dynamicStagesResults['deploy_accounting_svc_passed'] == false) {
                                saveFailedDeploy("accounting-svc", "1.39.0-*.RELEASE")
                            }
        
                            env.dynamicStagesResults = groovy.json.JsonOutput.toJson(dynamicStagesResults)
                        },
                        "video-svc 1.69.0-*.RELEASE": {
                            dynamicStagesResults = getDynamicStagesResults()

                            def serviceVersionFromPattern = rc_testing.getLatestServiceVersionByPattern(
                                serviceName="video-svc",
                                serviceVersionPattern="1.69.0-*.RELEASE"
                            )
    
                            dynamicStagesResults['deploy_video_svc_passed'] = rc_testing.deployService(
                                serviceName="video-svc",
                                serviceVersion=serviceVersionFromPattern,
                                deploymentDestination="null",
                            )
                            if (dynamicStagesResults['deploy_video_svc_passed'] == false) {
                                saveFailedDeploy("video-svc", "1.69.0-*.RELEASE")
                            }
        
                            env.dynamicStagesResults = groovy.json.JsonOutput.toJson(dynamicStagesResults)
                        },
                        "space-svc 1.82.0-*.RELEASE": {
                            dynamicStagesResults = getDynamicStagesResults()

                            def serviceVersionFromPattern = rc_testing.getLatestServiceVersionByPattern(
                                serviceName="space-svc",
                                serviceVersionPattern="1.82.0-*.RELEASE"
                            )
    
                            dynamicStagesResults['deploy_space_svc_passed'] = rc_testing.deployService(
                                serviceName="space-svc",
                                serviceVersion=serviceVersionFromPattern,
                                deploymentDestination="null",
                            )
                            if (dynamicStagesResults['deploy_space_svc_passed'] == false) {
                                saveFailedDeploy("space-svc", "1.82.0-*.RELEASE")
                            }
        
                            env.dynamicStagesResults = groovy.json.JsonOutput.toJson(dynamicStagesResults)
                        },
                        "mobile-gw-svc 1.97.0-*.RELEASE": {
                            dynamicStagesResults = getDynamicStagesResults()

                            def serviceVersionFromPattern = rc_testing.getLatestServiceVersionByPattern(
                                serviceName="mobile-gw-svc",
                                serviceVersionPattern="1.97.0-*.RELEASE"
                            )
    
                            dynamicStagesResults['deploy_mobile_gw_svc_passed'] = rc_testing.deployService(
                                serviceName="mobile-gw-svc",
                                serviceVersion=serviceVersionFromPattern,
                                deploymentDestination="null",
                            )
                            if (dynamicStagesResults['deploy_mobile_gw_svc_passed'] == false) {
                                saveFailedDeploy("mobile-gw-svc", "1.97.0-*.RELEASE")
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
                            test_plan_name='RC [accounting-svc 1.39.0-*.RELEASE | video-svc 1.69.0-*.RELEASE | space-svc 1.82.0-*.RELEASE | mobile-gw-svc 1.97.0-*.RELEASE]',
                            chats_for_notification='mobile-gw-svc 1.97.0-*.RELEASE'
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
            name: "Deploy accounting-svc 1.39.0-*.RELEASE | video-svc 1.69.0-*.RELEASE | space-svc 1.82.0-*.RELEASE | mobile-gw-svc 1.97.0-*.RELEASE | cloud-api-svc 0.48.0-*.RELEASE",
            steps: {
                script {
                    parallel (
                        "accounting-svc 1.39.0-*.RELEASE": {
                            dynamicStagesResults = getDynamicStagesResults()

                            def serviceVersionFromPattern = rc_testing.getLatestServiceVersionByPattern(
                                serviceName="accounting-svc",
                                serviceVersionPattern="1.39.0-*.RELEASE"
                            )
    
                            dynamicStagesResults['deploy_accounting_svc_passed'] = rc_testing.deployService(
                                serviceName="accounting-svc",
                                serviceVersion=serviceVersionFromPattern,
                                deploymentDestination="null",
                            )
                            if (dynamicStagesResults['deploy_accounting_svc_passed'] == false) {
                                saveFailedDeploy("accounting-svc", "1.39.0-*.RELEASE")
                            }
        
                            env.dynamicStagesResults = groovy.json.JsonOutput.toJson(dynamicStagesResults)
                        },
                        "video-svc 1.69.0-*.RELEASE": {
                            dynamicStagesResults = getDynamicStagesResults()

                            def serviceVersionFromPattern = rc_testing.getLatestServiceVersionByPattern(
                                serviceName="video-svc",
                                serviceVersionPattern="1.69.0-*.RELEASE"
                            )
    
                            dynamicStagesResults['deploy_video_svc_passed'] = rc_testing.deployService(
                                serviceName="video-svc",
                                serviceVersion=serviceVersionFromPattern,
                                deploymentDestination="null",
                            )
                            if (dynamicStagesResults['deploy_video_svc_passed'] == false) {
                                saveFailedDeploy("video-svc", "1.69.0-*.RELEASE")
                            }
        
                            env.dynamicStagesResults = groovy.json.JsonOutput.toJson(dynamicStagesResults)
                        },
                        "space-svc 1.82.0-*.RELEASE": {
                            dynamicStagesResults = getDynamicStagesResults()

                            def serviceVersionFromPattern = rc_testing.getLatestServiceVersionByPattern(
                                serviceName="space-svc",
                                serviceVersionPattern="1.82.0-*.RELEASE"
                            )
    
                            dynamicStagesResults['deploy_space_svc_passed'] = rc_testing.deployService(
                                serviceName="space-svc",
                                serviceVersion=serviceVersionFromPattern,
                                deploymentDestination="null",
                            )
                            if (dynamicStagesResults['deploy_space_svc_passed'] == false) {
                                saveFailedDeploy("space-svc", "1.82.0-*.RELEASE")
                            }
        
                            env.dynamicStagesResults = groovy.json.JsonOutput.toJson(dynamicStagesResults)
                        },
                        "mobile-gw-svc 1.97.0-*.RELEASE": {
                            dynamicStagesResults = getDynamicStagesResults()

                            def serviceVersionFromPattern = rc_testing.getLatestServiceVersionByPattern(
                                serviceName="mobile-gw-svc",
                                serviceVersionPattern="1.97.0-*.RELEASE"
                            )
    
                            dynamicStagesResults['deploy_mobile_gw_svc_passed'] = rc_testing.deployService(
                                serviceName="mobile-gw-svc",
                                serviceVersion=serviceVersionFromPattern,
                                deploymentDestination="null",
                            )
                            if (dynamicStagesResults['deploy_mobile_gw_svc_passed'] == false) {
                                saveFailedDeploy("mobile-gw-svc", "1.97.0-*.RELEASE")
                            }
        
                            env.dynamicStagesResults = groovy.json.JsonOutput.toJson(dynamicStagesResults)
                        },
                        "cloud-api-svc 0.48.0-*.RELEASE": {
                            dynamicStagesResults = getDynamicStagesResults()

                            def serviceVersionFromPattern = rc_testing.getLatestServiceVersionByPattern(
                                serviceName="cloud-api-svc",
                                serviceVersionPattern="0.48.0-*.RELEASE"
                            )
    
                            dynamicStagesResults['deploy_cloud_api_svc_passed'] = rc_testing.deployService(
                                serviceName="cloud-api-svc",
                                serviceVersion=serviceVersionFromPattern,
                                deploymentDestination="null",
                            )
                            if (dynamicStagesResults['deploy_cloud_api_svc_passed'] == false) {
                                saveFailedDeploy("cloud-api-svc", "0.48.0-*.RELEASE")
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
                            test_plan_name='RC [accounting-svc 1.39.0-*.RELEASE | video-svc 1.69.0-*.RELEASE | space-svc 1.82.0-*.RELEASE | mobile-gw-svc 1.97.0-*.RELEASE | cloud-api-svc 0.48.0-*.RELEASE]',
                            chats_for_notification='cloud-api-svc 0.48.0-*.RELEASE'
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