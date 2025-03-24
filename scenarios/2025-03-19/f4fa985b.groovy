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
            name: "Deploy accounting-svc 1.42.0*.RELEASE | cloud-api-svc 0.51.0*.RELEASE",
            steps: {
                script {
                    parallel (
                        "accounting-svc 1.42.0*.RELEASE": {
                            dynamicStagesResults = getDynamicStagesResults()

                            def serviceVersionFromPattern = rc_testing.getLatestServiceVersionByPattern(
                                serviceName="accounting-svc",
                                serviceVersionPattern="1.42.0*.RELEASE"
                            )
    
                            dynamicStagesResults['deploy_accounting_svc_passed'] = rc_testing.deployService(
                                serviceName="accounting-svc",
                                serviceVersion=serviceVersionFromPattern,
                                deploymentDestination="null",
                            )
                            if (dynamicStagesResults['deploy_accounting_svc_passed'] == false) {
                                saveFailedStages("Deploy accounting-svc with version 1.42.0*.RELEASE")
                            }
        
                            env.dynamicStagesResults = groovy.json.JsonOutput.toJson(dynamicStagesResults)
                        },
                        "cloud-api-svc 0.51.0*.RELEASE": {
                            dynamicStagesResults = getDynamicStagesResults()

                            def serviceVersionFromPattern = rc_testing.getLatestServiceVersionByPattern(
                                serviceName="cloud-api-svc",
                                serviceVersionPattern="0.51.0*.RELEASE"
                            )
    
                            dynamicStagesResults['deploy_cloud_api_svc_passed'] = rc_testing.deployService(
                                serviceName="cloud-api-svc",
                                serviceVersion=serviceVersionFromPattern,
                                deploymentDestination="null",
                            )
                            if (dynamicStagesResults['deploy_cloud_api_svc_passed'] == false) {
                                saveFailedStages("Deploy cloud-api-svc with version 0.51.0*.RELEASE")
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
                            test_plan_name='RC [accounting-svc 1.42.0 | cloud-api-svc 0.51.0]',
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
            name: "Deploy video-svc 1.75.0*.RELEASE",
            steps: {
                script {
                    parallel (
                        "video-svc 1.75.0*.RELEASE": {
                            dynamicStagesResults = getDynamicStagesResults()

                            def serviceVersionFromPattern = rc_testing.getLatestServiceVersionByPattern(
                                serviceName="video-svc",
                                serviceVersionPattern="1.75.0*.RELEASE"
                            )
    
                            dynamicStagesResults['deploy_video_svc_passed'] = rc_testing.deployService(
                                serviceName="video-svc",
                                serviceVersion=serviceVersionFromPattern,
                                deploymentDestination="null",
                            )
                            if (dynamicStagesResults['deploy_video_svc_passed'] == false) {
                                saveFailedStages("Deploy video-svc with version 1.75.0*.RELEASE")
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
                            test_plan_name='RC [accounting-svc 1.42.0 | cloud-api-svc 0.51.0 | video-svc 1.75.0]',
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
            name: "Deploy space-svc 1.88.0*.RELEASE",
            steps: {
                script {
                    parallel (
                        "space-svc 1.88.0*.RELEASE": {
                            dynamicStagesResults = getDynamicStagesResults()

                            def serviceVersionFromPattern = rc_testing.getLatestServiceVersionByPattern(
                                serviceName="space-svc",
                                serviceVersionPattern="1.88.0*.RELEASE"
                            )
    
                            dynamicStagesResults['deploy_space_svc_passed'] = rc_testing.deployService(
                                serviceName="space-svc",
                                serviceVersion=serviceVersionFromPattern,
                                deploymentDestination="null",
                            )
                            if (dynamicStagesResults['deploy_space_svc_passed'] == false) {
                                saveFailedStages("Deploy space-svc with version 1.88.0*.RELEASE")
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
                            test_plan_name='RC [accounting-svc 1.42.0 | cloud-api-svc 0.51.0 | video-svc 1.75.0 | space-svc 1.88.0]',
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
            name: "Deploy mobile-gw-svc 1.103.0*.RELEASE",
            steps: {
                script {
                    parallel (
                        "mobile-gw-svc 1.103.0*.RELEASE": {
                            dynamicStagesResults = getDynamicStagesResults()

                            def serviceVersionFromPattern = rc_testing.getLatestServiceVersionByPattern(
                                serviceName="mobile-gw-svc",
                                serviceVersionPattern="1.103.0*.RELEASE"
                            )
    
                            dynamicStagesResults['deploy_mobile_gw_svc_passed'] = rc_testing.deployService(
                                serviceName="mobile-gw-svc",
                                serviceVersion=serviceVersionFromPattern,
                                deploymentDestination="null",
                            )
                            if (dynamicStagesResults['deploy_mobile_gw_svc_passed'] == false) {
                                saveFailedStages("Deploy mobile-gw-svc with version 1.103.0*.RELEASE")
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
                            test_plan_name='RC [accounting-svc 1.42.0 | cloud-api-svc 0.51.0 | video-svc 1.75.0 | space-svc 1.88.0 | mobile-gw-svc 1.103.0]',
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
            name: "Deploy a911-svc 1.133.0*.RELEASE",
            steps: {
                script {
                    parallel (
                        "a911-svc 1.133.0*.RELEASE": {
                            dynamicStagesResults = getDynamicStagesResults()

                            def serviceVersionFromPattern = rc_testing.getLatestServiceVersionByPattern(
                                serviceName="a911-svc",
                                serviceVersionPattern="1.133.0*.RELEASE"
                            )
    
                            dynamicStagesResults['deploy_a911_svc_passed'] = rc_testing.deployService(
                                serviceName="a911-svc",
                                serviceVersion=serviceVersionFromPattern,
                                deploymentDestination="null",
                            )
                            if (dynamicStagesResults['deploy_a911_svc_passed'] == false) {
                                saveFailedStages("Deploy a911-svc with version 1.133.0*.RELEASE")
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
                            test_plan_name='RC [accounting-svc 1.42.0 | cloud-api-svc 0.51.0 | video-svc 1.75.0 | space-svc 1.88.0 | mobile-gw-svc 1.103.0 | a911-svc 1.133.0]',
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