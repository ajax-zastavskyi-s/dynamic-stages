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
            name: "Deploy cloud-signaling-svc 1.1.1*.RELEASE",
            steps: {
                script {
                    parallel (
                        "cloud-signaling-svc 1.1.1*.RELEASE": {
                            dynamicStagesResults = getDynamicStagesResults()

                            def serviceVersionFromPattern = rc_testing.getLatestServiceVersionByPattern(
                                serviceName="cloud-signaling-svc",
                                serviceVersionPattern="1.1.1*.RELEASE"
                            )
    
                            dynamicStagesResults['deploy_cloud_signaling_svc_passed'] = rc_testing.deployService(
                                serviceName="cloud-signaling-svc",
                                serviceVersion=serviceVersionFromPattern,
                                deploymentDestination="ec2",
                            )
                            if (dynamicStagesResults['deploy_cloud_signaling_svc_passed'] == false) {
                                saveFailedDeploy("cloud-signaling-svc", "1.1.1*.RELEASE")
                            }
        
                            env.dynamicStagesResults = groovy.json.JsonOutput.toJson(dynamicStagesResults)
                        },
                    )
                }
            }
        ],
        [
            name: "Restore toggles amazing [a911-svc] | amazing-ff [a911-svc]",
            steps: {
                script {
                    if (dynamicStagesResults.every { stage_passed -> stage_passed.value == true }) {
                        parallel (
                        "amazing [a911-svc]": {
                            dynamicStagesResults = getDynamicStagesResults()

                            dynamicStagesResults['restore_amazing_passed'] = rc_testing.restoreToggle(
                                serviceName="a911-svc",
                                featureFlagName="amazing",
                                featureFlagState="True",
                                additionalData="{'cliend-id': '+'}"
                            )
                            if (dynamicStagesResults['restore_amazing_passed'] == false) {
                                saveFailedDeploy("amazing", "a911-svc")
                            }

                            env.dynamicStagesResults = groovy.json.JsonOutput.toJson(dynamicStagesResults)
                        },
                        "amazing-ff [a911-svc]": {
                            dynamicStagesResults = getDynamicStagesResults()

                            dynamicStagesResults['restore_amazing_ff_passed'] = rc_testing.restoreToggle(
                                serviceName="a911-svc",
                                featureFlagName="amazing-ff",
                                featureFlagState="True",
                                additionalData="{'cliend-id': '+'}"
                            )
                            if (dynamicStagesResults['restore_amazing_ff_passed'] == false) {
                                saveFailedDeploy("amazing-ff", "a911-svc")
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
            name: "Run BDD tests with marks: regress",
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
                            marks='regress',
                            test_plan_name='RC [cloud-signaling-svc 1.1.1*.RELEASE]'
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