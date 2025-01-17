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
            name: "Deploy communication-svc 1.64.0*.RELEASE | space-svc 1.79.0-2159.RELEASE | video-svc 1.66.0-1314.RELEASE",
            steps: {
                script {
                    parallel (
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
        
                            env.dynamicStagesResults = groovy.json.JsonOutput.toJson(dynamicStagesResults)
                        },
                        "space-svc 1.79.0-2159.RELEASE": {
                            dynamicStagesResults = getDynamicStagesResults()

                            def serviceVersionFromPattern = rc_testing.getLatestServiceVersionByPattern(
                                serviceName="space-svc",
                                serviceVersionPattern="1.79.0-2159.RELEASE"
                            )
    
                            dynamicStagesResults['deploy_space_svc_passed'] = rc_testing.deployService(
                                serviceName="space-svc",
                                serviceVersion=serviceVersionFromPattern,
                                deploymentDestination="null",
                            )
        
                            env.dynamicStagesResults = groovy.json.JsonOutput.toJson(dynamicStagesResults)
                        },
                        "video-svc 1.66.0-1314.RELEASE": {
                            dynamicStagesResults = getDynamicStagesResults()

                            def serviceVersionFromPattern = rc_testing.getLatestServiceVersionByPattern(
                                serviceName="video-svc",
                                serviceVersionPattern="1.66.0-1314.RELEASE"
                            )
    
                            dynamicStagesResults['deploy_video_svc_passed'] = rc_testing.deployService(
                                serviceName="video-svc",
                                serviceVersion=serviceVersionFromPattern,
                                deploymentDestination="null",
                            )
        
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
                            test_plan_name='RC [communication-svc 1.64.0*.RELEASE | space-svc 1.79.0-2159.RELEASE | video-svc 1.66.0-1314.RELEASE]'
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
