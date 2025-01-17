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
            name: "Deploy communication-svc 1.64.0*.RELEASE | facility-svc 1.37.1-988.RELEASE | external-device-svc 1.38.0-322.RELEASE",
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
                        }
                        "facility-svc 1.37.1-988.RELEASE": {
                            dynamicStagesResults = getDynamicStagesResults()

                            def serviceVersionFromPattern = rc_testing.getLatestServiceVersionByPattern(
                                serviceName="facility-svc",
                                serviceVersionPattern="1.37.1-988.RELEASE"
                            )
    
                            dynamicStagesResults['deploy_facility_svc_passed'] = rc_testing.deployService(
                                serviceName="facility-svc",
                                serviceVersion=serviceVersionFromPattern,
                                deploymentDestination="null",
                            )
        
                            env.dynamicStagesResults = groovy.json.JsonOutput.toJson(dynamicStagesResults)
                        }
                        "external-device-svc 1.38.0-322.RELEASE": {
                            dynamicStagesResults = getDynamicStagesResults()

                            def serviceVersionFromPattern = rc_testing.getLatestServiceVersionByPattern(
                                serviceName="external-device-svc",
                                serviceVersionPattern="1.38.0-322.RELEASE"
                            )
    
                            dynamicStagesResults['deploy_external_device_svc_passed'] = rc_testing.deployService(
                                serviceName="external-device-svc",
                                serviceVersion=serviceVersionFromPattern,
                                deploymentDestination="null",
                            )
        
                            env.dynamicStagesResults = groovy.json.JsonOutput.toJson(dynamicStagesResults)
                        }
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
                            test_plan_name='RC [communication-svc 1.64.0*.RELEASE | facility-svc 1.37.1-988.RELEASE | external-device-svc 1.38.0-322.RELEASE]'
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
            name: "Deploy image-svc 1.25.0*.RELEASE | telemetry-svc 1.13.0-170.RELEASE",
            steps: {
                script {
                    parallel (
                        "image-svc 1.25.0*.RELEASE": {
                            dynamicStagesResults = getDynamicStagesResults()

                            def serviceVersionFromPattern = rc_testing.getLatestServiceVersionByPattern(
                                serviceName="image-svc",
                                serviceVersionPattern="1.25.0*.RELEASE"
                            )
    
                            dynamicStagesResults['deploy_image_svc_passed'] = rc_testing.deployService(
                                serviceName="image-svc",
                                serviceVersion=serviceVersionFromPattern,
                                deploymentDestination="null",
                            )
        
                            env.dynamicStagesResults = groovy.json.JsonOutput.toJson(dynamicStagesResults)
                        }
                        "telemetry-svc 1.13.0-170.RELEASE": {
                            dynamicStagesResults = getDynamicStagesResults()

                            def serviceVersionFromPattern = rc_testing.getLatestServiceVersionByPattern(
                                serviceName="telemetry-svc",
                                serviceVersionPattern="1.13.0-170.RELEASE"
                            )
    
                            dynamicStagesResults['deploy_telemetry_svc_passed'] = rc_testing.deployService(
                                serviceName="telemetry-svc",
                                serviceVersion=serviceVersionFromPattern,
                                deploymentDestination="null",
                            )
        
                            env.dynamicStagesResults = groovy.json.JsonOutput.toJson(dynamicStagesResults)
                        }
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
                            test_plan_name='RC [image-svc 1.25.0*.RELEASE | telemetry-svc 1.13.0-170.RELEASE]'
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
