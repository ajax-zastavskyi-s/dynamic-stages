import org.jenkinsci.plugins.pipeline.modeldefinition.Utils

def getStages() {

    return [
        [
            name: "Deploy a911-svc 1.118.0-7447.RELEASE",
            steps: {
                script {
                    echo "In deploy a911-svc"
                }
            }
        ],
        [
            name: "Deploy mobile-gw-svc 1.29.0-8984.RELEASE",
            steps: {
                script {
                    echo "In deploy mobile-gw-svc"
                }
            }
        ],
        [
            name: "Run BDD tests",
            steps: {
                script {
                    echo "In BDD Tests"

                }
            }
        ],
    ]
}

return this