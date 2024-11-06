def getStages() {
    return [
        [
            name: 'Dynamic stage 1',
            steps: {}
        ],
        [
            name: 'Dynamic stage 2',
            steps: {}
        ],
        [
            name: 'Dynamic stage 3',
            steps: {
                error "DS 3 failed"
            }
        ],
    [
       name: "Dynamic stage 4",
       steps: {
        echo "dynamic stage 4"
        echo "one more step here"
        script {
                    catchError(buildResult: 'FAILURE', stageResult: 'FAILURE') {
                        echo "here can be your script"
            sh "python3 --version"
                    }
                }
       }
    ]
    ]
}

return this
