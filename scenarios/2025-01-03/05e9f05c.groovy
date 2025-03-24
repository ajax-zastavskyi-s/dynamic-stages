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
            name: "Deploy user-svc 1.24.0-*.RELEASE",
            steps: {
                script {
                    parallel (
                        "user-svc 1.24.0-*.RELEASE": {
                            dynamicStagesResults = getDynamicStagesResults()

                            def serviceVersionFromPattern = rc_testing.getLatestServiceVersionByPattern(
                                serviceName="user-svc",
                                serviceVersionPattern="1.24.0-*.RELEASE"
                            )
    
                            dynamicStagesResults['deploy_user_svc_passed'] = rc_testing.deployService(
                                serviceName="user-svc",
                                serviceVersion=serviceVersionFromPattern,
                                deploymentDestination="null",
                            )
                            if (dynamicStagesResults['deploy_user_svc_passed'] == false) {
                                saveFailedStages("Deploy user-svc with version 1.24.0-*.RELEASE")
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
                            test_plan_name='RC [user-svc 1.24.0-*.RELEASE]',
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
            name: "Deploy a911-svc 1.122.0-*.RELEASE",
            steps: {
                script {
                    parallel (
                        "a911-svc 1.122.0-*.RELEASE": {
                            dynamicStagesResults = getDynamicStagesResults()

                            def serviceVersionFromPattern = rc_testing.getLatestServiceVersionByPattern(
                                serviceName="a911-svc",
                                serviceVersionPattern="1.122.0-*.RELEASE"
                            )
    
                            dynamicStagesResults['deploy_a911_svc_passed'] = rc_testing.deployService(
                                serviceName="a911-svc",
                                serviceVersion=serviceVersionFromPattern,
                                deploymentDestination="null",
                            )
                            if (dynamicStagesResults['deploy_a911_svc_passed'] == false) {
                                saveFailedStages("Deploy a911-svc with version 1.122.0-*.RELEASE")
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
                            test_plan_name='RC [user-svc 1.24.0-*.RELEASE | a911-svc 1.122.0-*.RELEASE]',
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
            name: "Deploy hub-svc 1.37.0-*.RELEASE",
            steps: {
                script {
                    parallel (
                        "hub-svc 1.37.0-*.RELEASE": {
                            dynamicStagesResults = getDynamicStagesResults()

                            def serviceVersionFromPattern = rc_testing.getLatestServiceVersionByPattern(
                                serviceName="hub-svc",
                                serviceVersionPattern="1.37.0-*.RELEASE"
                            )
    
                            dynamicStagesResults['deploy_hub_svc_passed'] = rc_testing.deployService(
                                serviceName="hub-svc",
                                serviceVersion=serviceVersionFromPattern,
                                deploymentDestination="null",
                            )
                            if (dynamicStagesResults['deploy_hub_svc_passed'] == false) {
                                saveFailedStages("Deploy hub-svc with version 1.37.0-*.RELEASE")
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
                            test_plan_name='RC [user-svc 1.24.0-*.RELEASE | a911-svc 1.122.0-*.RELEASE | hub-svc 1.37.0-*.RELEASE]',
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
            name: "Deploy cloud-signaling-svc 1.101.0.*.RELEASE | communication-svc 1.65.0-*.RELEASE",
            steps: {
                script {
                    parallel (
                        "cloud-signaling-svc 1.101.0.*.RELEASE": {
                            dynamicStagesResults = getDynamicStagesResults()

                            def serviceVersionFromPattern = rc_testing.getLatestServiceVersionByPattern(
                                serviceName="cloud-signaling-svc",
                                serviceVersionPattern="1.101.0.*.RELEASE"
                            )
    
                            dynamicStagesResults['deploy_cloud_signaling_svc_passed'] = rc_testing.deployService(
                                serviceName="cloud-signaling-svc",
                                serviceVersion=serviceVersionFromPattern,
                                deploymentDestination="null",
                            )
                            if (dynamicStagesResults['deploy_cloud_signaling_svc_passed'] == false) {
                                saveFailedStages("Deploy cloud-signaling-svc with version 1.101.0.*.RELEASE")
                            }
        
                            env.dynamicStagesResults = groovy.json.JsonOutput.toJson(dynamicStagesResults)
                        },
                        "communication-svc 1.65.0-*.RELEASE": {
                            dynamicStagesResults = getDynamicStagesResults()

                            def serviceVersionFromPattern = rc_testing.getLatestServiceVersionByPattern(
                                serviceName="communication-svc",
                                serviceVersionPattern="1.65.0-*.RELEASE"
                            )
    
                            dynamicStagesResults['deploy_communication_svc_passed'] = rc_testing.deployService(
                                serviceName="communication-svc",
                                serviceVersion=serviceVersionFromPattern,
                                deploymentDestination="null",
                            )
                            if (dynamicStagesResults['deploy_communication_svc_passed'] == false) {
                                saveFailedStages("Deploy communication-svc with version 1.65.0-*.RELEASE")
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
                            test_plan_name='RC [user-svc 1.24.0-*.RELEASE | a911-svc 1.122.0-*.RELEASE | hub-svc 1.37.0-*.RELEASE | cloud-signaling-svc 1.101.0.*.RELEASE | communication-svc 1.65.0-*.RELEASE]',
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
            name: "Deploy csa 1.107.0.*.RELEASE",
            steps: {
                script {
                    parallel (
                        "csa 1.107.0.*.RELEASE": {
                            dynamicStagesResults = getDynamicStagesResults()

                            def serviceVersionFromPattern = rc_testing.getLatestServiceVersionByPattern(
                                serviceName="csa",
                                serviceVersionPattern="1.107.0.*.RELEASE"
                            )
    
                            dynamicStagesResults['deploy_csa_passed'] = rc_testing.deployService(
                                serviceName="csa",
                                serviceVersion=serviceVersionFromPattern,
                                deploymentDestination="null",
                            )
                            if (dynamicStagesResults['deploy_csa_passed'] == false) {
                                saveFailedStages("Deploy csa with version 1.107.0.*.RELEASE")
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
                            test_plan_name='RC [user-svc 1.24.0-*.RELEASE | a911-svc 1.122.0-*.RELEASE | hub-svc 1.37.0-*.RELEASE | cloud-signaling-svc 1.101.0.*.RELEASE | communication-svc 1.65.0-*.RELEASE | csa 1.107.0.*.RELEASE]',
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