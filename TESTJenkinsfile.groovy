import org.jenkinsci.plugins.pipeline.modeldefinition.Utils

pipeline {
    agent any
    stages {
        stage('ROLLOUT') {
                steps {
                    sh "echo Rollout!"
                    rollout_stage_passed = true
                }
        }
        stage("DYNAMIC_STAGES") {
            steps {
                script {
                    if (rollout_stage_passed == true) {
                        getDynamicStages().each { dynamicStage ->
                            stage(dynamicStage.name) {
                                dynamicStage.steps.each { step ->
                                    step.call()
                                }
                            }
                        }
                    }
                    else {
                        echo "Skip running dynamic stages due to failed rollout process"
                    }
                }
            }
        }
    }
}



def getDynamicStages() {
    try {
        dynamicStagesFile = "dynamic-stages/scenarios/2024-01-07.groovy"
        if (fileExists(dynamicStagesFile)) {
            return load(dynamicStagesFile).getStages()
        }
        return []

        }
    }
    catch (Exception exc) {
        currentBuild.result = "UNSTABLE"
        unstable("Failed loading dynamic stages from remote repository. Error: ${exc}")

        return []
    }
}