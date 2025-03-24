from string import Template

BASE_GROOVY_TEMPLATE = Template("""
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
$stages
    ]
}

return this

""")

PARALLEL_DEPLOY_TEMPLATE = Template("""
        [
            name: "$stage_name",
            steps: {
                script {
                    parallel (
$parallel_deployments
                    )
                }
            }
        ],
""")

PARALLEL_RESTORE_TEMPLATE = Template("""
        [
            name: "$stage_name",
            steps: {
                script {
                    def dynamicStagesResults = getDynamicStagesResults()
                    if (dynamicStagesResults.every { stage_passed -> stage_passed.value == true }) {
                        parallel (
$parallel_toggles_setting
                        )
                    } else {
                        def failedStage = dynamicStagesResults.find { stage_passed -> stage_passed.value == false }?.key
                        echo "Skip restore toggles due to failure: ${failedStage} == false"
                    }
                }
            }
        ],
""")

DEPLOY_SVC_TEMPLATE = Template("""
                        "$stage_name": {
                            dynamicStagesResults = getDynamicStagesResults()

                            def serviceVersionFromPattern = rc_testing.getLatestServiceVersionByPattern(
                                serviceName="$service_name",
                                serviceVersionPattern="$service_version"
                            )
    
                            dynamicStagesResults['$stage_passed_variable'] = rc_testing.deployService(
                                serviceName="$service_name",
                                serviceVersion=serviceVersionFromPattern,
                                deploymentDestination="$deployment_destination",
                            )
                            if (dynamicStagesResults['$stage_passed_variable'] == false) {
                                saveFailedStages("Deploy ${service_name} with version ${service_version}")
                            }
        
                            env.dynamicStagesResults = groovy.json.JsonOutput.toJson(dynamicStagesResults)
                        },
""")

RUN_BDD_TESTS_TEMPLATE = Template("""
        [
            name: "$stage_name",
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
                            marks='$marks',
                            test_plan_name='$test_plan_name',
                            test_plan_description='$test_plan_description'
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
""")

SET_FF_TEMPLATE = Template("""
                        "$stage_name": {
                            dynamicStagesResults = getDynamicStagesResults()
    
                            dynamicStagesResults['$stage_passed_variable'] = rc_testing.setToggle(
                                serviceName='$service_name',
                                featureFlagName='$feature_flag_name',
                                featureFlagState='$feature_flag_state',
                                additionalData='$additional_data'
                            )
                            if (dynamicStagesResults['$stage_passed_variable'] == false) {
                                saveFailedStages("Set toggle ${feature_flag_name} on ${service_name}")
                            }
        
                            env.dynamicStagesResults = groovy.json.JsonOutput.toJson(dynamicStagesResults)
                        },
""")
