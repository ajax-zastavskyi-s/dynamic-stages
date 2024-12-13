from string import Template

BASE_GROOVY_TEMPLATE = Template("""
import org.jenkinsci.plugins.pipeline.modeldefinition.Utils

def getStages() {
    rc_testing = load("rollout_stage/jenkins/rc_testing.groovy")

    def getDynamicStagesResults = {
        if (env.dynamicStagesResults) {
            return readJSON(text: env.dynamicStagesResults)
        }
        return [:]
    }
def getFailedDeploys() {
  if (env.failedRCDeploys) {
    echo ${env.failedRCDeploys}
    return env.failedRCDeploys.split(',').toList()
  }
  return []
}

def addFailedRCDeploy(service, version) {
  def failedRCDeploys = getFailedDeploys()
  failedDeploys.add("Deploy ${service} ${version}")
  env.failedDeploys = failedDeploys.join(",")
}

    return [
$stages
    ]
}

return this

""")

DEPLOY_SVC_TEMPLATE = Template("""
        [
            name: "$stage_name",
            steps: {
                script {
                    dynamicStagesResults = getDynamicStagesResults()
                    if (dynamicStagesResults.every { stage_passed -> stage_passed.value == true }) {
                        def serviceVersionFromPattern = rc_testing.getLatestServiceVersionByPattern(
                            serviceName="$service_name",
                            serviceVersionPattern="$service_version"
                        )

                        dynamicStagesResults['$stage_passed_variable'] = rc_testing.deployService(
                            serviceName="$service_name",
                            serviceVersion=serviceVersionFromPattern
                        )
                        if (dynamicStagesResults['$stage_passed_variable'] == false) {
                          addFailedRCDeploy($service_name, $service_version)
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
""")

RUN_BDD_TESTS_TEMPLATE = Template("""
        [
            name: "$stage_name",
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
                            marks='$marks',
                            test_plan_name='$test_plan_name'
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
