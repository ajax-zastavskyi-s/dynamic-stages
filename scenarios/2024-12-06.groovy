import org.jenkinsci.plugins.pipeline.modeldefinition.Utils

def getStages() {
    rc_testing = load("rollout_stage/jenkins/rc_testing.groovy")

    def getDynamicStagesResults = {
        if (env.dynamicStagesResults) {
            return readJSON(text: env.dynamicStagesResults)
        }
        return [:]
    }

    def getFailedDeploys = {
      if (env.failedRCDeploys) {
        echo ${env.failedRCDeploys}
        return env.failedRCDeploys.split(',').toList()
      }
      return []
    }

    def addFailedRCDeploy = {service, version ->
      def failedRCDeploys = getFailedDeploys()
      failedDeploys.add("Deploy ${service} ${version}")
      env.failedDeploys = failedDeploys.join(",")
    }

    return [
        [
            name: "Deploy external-device-svc wrong version 1",
            steps: {
                script {
                    dynamicStagesResults = getDynamicStagesResults()
                    if (dynamicStagesResults.every { stage_passed -> stage_passed.value == true }) {
                        def serviceVersionFromPattern = rc_testing.getLatestServiceVersionByPattern(
                            serviceName="external-device-svc",
                            serviceVersionPattern="wrong version 1"
                        )

                        dynamicStagesResults['deploy_external_device_svc_passed'] = rc_testing.deployService(
                            serviceName="external-device-svc",
                            serviceVersion=serviceVersionFromPattern
                        )
                        if (dynamicStagesResults['deploy_external_device_svc_passed'] == false) {
                          addFailedRCDeploy("external-device-svc", "wrong version 1")
                        }
                    }
                    else {
                        def failedStage = dynamicStagesResults.find { stage_passed -> stage_passed.value == false }?.key
                        echo "Skip deploy due to failure: ${failedStage} == false"
                        Utils.markStageSkippedForConditional(env.STAGE_NAME)
                    }

                    env.dynamicStagesResults = groovy.json.JsonOutput.toJson(dynamicStagesResults)
                }
            }
        ],
        [
            name: "Run BDD tests with marks: regress",
            steps: {
                script {
                    dynamicStagesResults = getDynamicStagesResults()

                    if (dynamicStagesResults.every { stage_passed -> stage_passed.value == true }) {
                        rc_testing.clearDatabases()
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
                            test_plan_name='RC [external-device-svc wrong version 1]'
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
            name: "Deploy external-device-svc wrong version 2",
            steps: {
                script {
                    dynamicStagesResults = getDynamicStagesResults()
                    if (dynamicStagesResults.every { stage_passed -> stage_passed.value == true }) {
                        def serviceVersionFromPattern = rc_testing.getLatestServiceVersionByPattern(
                            serviceName="external-device-svc",
                            serviceVersionPattern="wrong version 2"
                        )

                        dynamicStagesResults['deploy_external_device_svc_passed'] = rc_testing.deployService(
                            serviceName="external-device-svc",
                            serviceVersion=serviceVersionFromPattern
                        )
                        if (dynamicStagesResults['deploy_external_device_svc_passed'] == false) {
                          addFailedRCDeploy("external-device-svc", "wrong version 2")
                        }
                    }
                    else {
                        def failedStage = dynamicStagesResults.find { stage_passed -> stage_passed.value == false }?.key
                        echo "Skip deploy due to failure: ${failedStage} == false"
                        Utils.markStageSkippedForConditional(env.STAGE_NAME)
                    }

                    env.dynamicStagesResults = groovy.json.JsonOutput.toJson(dynamicStagesResults)
                }
            }
        ],
    ]
}

return this