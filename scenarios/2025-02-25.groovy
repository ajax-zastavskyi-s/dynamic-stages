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
            name: "Deploy csa 1.122.0-7660.MASTER-SNAPSHOT | a911-svc 1.70.0*.RELEASE",
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
                        "a911-svc 1.70.0*.RELEASE": {
                            dynamicStagesResults = getDynamicStagesResults()

                            def serviceVersionFromPattern = rc_testing.getLatestServiceVersionByPattern(
                                serviceName="a911-svc",
                                serviceVersionPattern="1.70.0*.RELEASE"
                            )

                            dynamicStagesResults['deploy_a911_svc_passed'] = rc_testing.deployService(
                                serviceName="a911-svc",
                                serviceVersion=serviceVersionFromPattern,
                                deploymentDestination="k8",
                            )
                            if (dynamicStagesResults['deploy_a911_svc_passed'] == false) {
                                saveFailedDeploy("a911-svc", "1.70.0*.RELEASE")
                            }

                            env.dynamicStagesResults = groovy.json.JsonOutput.toJson(dynamicStagesResults)
                        },
                    )
                }
            }
        ],
        [
            name: "Restore toggles ",
            steps: {
                script {
                    if (dynamicStagesResults.every { stage_passed -> stage_passed.value == true }) {
                        parallel (

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
                            test_plan_name='RC [csa 1.122.0-7660.MASTER-SNAPSHOT | a911-svc 1.70.0*.RELEASE]'
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