import org.jenkinsci.plugins.pipeline.modeldefinition.Utils

def getStages() {
    def rc_testing = load("rollout_stage/jenkins/rc_testing.groovy")

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
            name: "Deploy company-svc 1.90.0*.RELEASE | communication-svc 1.64.0*.RELEASE | cloud-signaling-svc 1.100.0*.RELEASE | mobile-gw-svc 1.92.0*.RELEASE | space-svc 1.77.0*.RELEASE | video-svc 1.64.0*.RELEASE",
            steps: {
                script {
                    parallel (
                        "company-svc 1.90.0*.RELEASE": {
                            dynamicStagesResults = getDynamicStagesResults()

                            def serviceVersionFromPattern = rc_testing.getLatestServiceVersionByPattern(
                                serviceName="company-svc",
                                serviceVersionPattern="1.90.0*.RELEASE"
                            )
    
                            dynamicStagesResults['deploy_company_svc_passed'] = rc_testing.deployService(
                                serviceName="company-svc",
                                serviceVersion=serviceVersionFromPattern,
                                deploymentDestination="null",
                            )
                            if (dynamicStagesResults['deploy_company_svc_passed'] == false) {
                                saveFailedStages("Deploy company-svc with version 1.90.0*.RELEASE")
                            }
        
                            env.dynamicStagesResults = groovy.json.JsonOutput.toJson(dynamicStagesResults)
                        },
                        "communication-svc 1.64.0*.RELEASE": {
                            dynamicStagesResults = getDynamicStagesResults()

                            def serviceVersionFromPattern = rc_testing.getLatestServiceVersionByPattern(
                                serviceName="communication-svc",
                                serviceVersionPattern="1.64.0*.RELEASE"
                            )
    
                            dynamicStagesResults['deploy_communication_svc_passed'] = rc_testing.deployService(
                                serviceName="communication-svc",
                                serviceVersion=serviceVersionFromPattern,
                                deploymentDestination="null",
                            )
                            if (dynamicStagesResults['deploy_communication_svc_passed'] == false) {
                                saveFailedStages("Deploy communication-svc with version 1.64.0*.RELEASE")
                            }
        
                            env.dynamicStagesResults = groovy.json.JsonOutput.toJson(dynamicStagesResults)
                        },
                        "cloud-signaling-svc 1.100.0*.RELEASE": {
                            dynamicStagesResults = getDynamicStagesResults()

                            def serviceVersionFromPattern = rc_testing.getLatestServiceVersionByPattern(
                                serviceName="cloud-signaling-svc",
                                serviceVersionPattern="1.100.0*.RELEASE"
                            )
    
                            dynamicStagesResults['deploy_cloud_signaling_svc_passed'] = rc_testing.deployService(
                                serviceName="cloud-signaling-svc",
                                serviceVersion=serviceVersionFromPattern,
                                deploymentDestination="null",
                            )
                            if (dynamicStagesResults['deploy_cloud_signaling_svc_passed'] == false) {
                                saveFailedStages("Deploy cloud-signaling-svc with version 1.100.0*.RELEASE")
                            }
        
                            env.dynamicStagesResults = groovy.json.JsonOutput.toJson(dynamicStagesResults)
                        },
                        "mobile-gw-svc 1.92.0*.RELEASE": {
                            dynamicStagesResults = getDynamicStagesResults()

                            def serviceVersionFromPattern = rc_testing.getLatestServiceVersionByPattern(
                                serviceName="mobile-gw-svc",
                                serviceVersionPattern="1.92.0*.RELEASE"
                            )
    
                            dynamicStagesResults['deploy_mobile_gw_svc_passed'] = rc_testing.deployService(
                                serviceName="mobile-gw-svc",
                                serviceVersion=serviceVersionFromPattern,
                                deploymentDestination="null",
                            )
                            if (dynamicStagesResults['deploy_mobile_gw_svc_passed'] == false) {
                                saveFailedStages("Deploy mobile-gw-svc with version 1.92.0*.RELEASE")
                            }
        
                            env.dynamicStagesResults = groovy.json.JsonOutput.toJson(dynamicStagesResults)
                        },
                        "space-svc 1.77.0*.RELEASE": {
                            dynamicStagesResults = getDynamicStagesResults()

                            def serviceVersionFromPattern = rc_testing.getLatestServiceVersionByPattern(
                                serviceName="space-svc",
                                serviceVersionPattern="1.77.0*.RELEASE"
                            )
    
                            dynamicStagesResults['deploy_space_svc_passed'] = rc_testing.deployService(
                                serviceName="space-svc",
                                serviceVersion=serviceVersionFromPattern,
                                deploymentDestination="null",
                            )
                            if (dynamicStagesResults['deploy_space_svc_passed'] == false) {
                                saveFailedStages("Deploy space-svc with version 1.77.0*.RELEASE")
                            }
        
                            env.dynamicStagesResults = groovy.json.JsonOutput.toJson(dynamicStagesResults)
                        },
                        "video-svc 1.64.0*.RELEASE": {
                            dynamicStagesResults = getDynamicStagesResults()

                            def serviceVersionFromPattern = rc_testing.getLatestServiceVersionByPattern(
                                serviceName="video-svc",
                                serviceVersionPattern="1.64.0*.RELEASE"
                            )
    
                            dynamicStagesResults['deploy_video_svc_passed'] = rc_testing.deployService(
                                serviceName="video-svc",
                                serviceVersion=serviceVersionFromPattern,
                                deploymentDestination="null",
                            )
                            if (dynamicStagesResults['deploy_video_svc_passed'] == false) {
                                saveFailedStages("Deploy video-svc with version 1.64.0*.RELEASE")
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
                            test_plan_name='RC [company-svc 1.90.0*.RELEASE | communication-svc 1.64.0*.RELEASE | cloud-signaling-svc 1.100.0*.RELEASE | mobile-gw-svc 1.92.0*.RELEASE | space-svc 1.77.0*.RELEASE | video-svc 1.64.0*.RELEASE]',
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
    ]
}

return this