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
            name: "Deploy communication-svc 1.64.0*.RELEASE| company-svc 1.90.0*.RELEASE| cloud-signaling-svc 1.100.0.*.RELEASE| a911-svc 1.121.0*.RELEASE| user-svc 1.23.0*.RELEASE| image-svc 1.24.0*.RELEASE",
            steps: {
                script {
                    parallel (
                        
                                "Deploy communication-svc 1.64.0*.RELEASE":{
                                echo "DEPLOY communication-svc 1.64.0*.RELEASE"
                                },


                                "Deploy company-svc 1.90.0*.RELEASE":{
                                echo "DEPLOY company-svc 1.90.0*.RELEASE"
                                },


                                "Deploy cloud-signaling-svc 1.100.0.*.RELEASE":{
                                echo "DEPLOY cloud-signaling-svc 1.100.0.*.RELEASE"
                                },


                                "Deploy a911-svc 1.121.0*.RELEASE":{
                                echo "DEPLOY a911-svc 1.121.0*.RELEASE"
                                },


                                "Deploy user-svc 1.23.0*.RELEASE":{
                                echo "DEPLOY user-svc 1.23.0*.RELEASE"
                                },


                                "Deploy image-svc 1.24.0*.RELEASE":{
                                echo "DEPLOY image-svc 1.24.0*.RELEASE"
                                },

                    )
                }
            }
        ],
    ]
}

return this
