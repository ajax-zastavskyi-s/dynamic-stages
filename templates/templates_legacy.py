from string import Template

BASE_GROOVY_TEMPLATE = Template("""
def getStages() {
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
                # script {
                #     def deployServiceBuild = build(job:"CSA_QA/UTILS/STAGE_MANIPULATION/common/DEPLOY_SVC", parameters: [
                #         string(name: 'SERVER', value: env.SERVER),
                #         string(name: '$service_name', value: '$service_version',
                #     ], wait: true, propagate: false)
                # 
                #     if (deployServiceBuild.resultIsWorseOrEqualTo("UNSTABLE")) {
                #         error "Failed to deploy $service_name $service_version"
                #     }
                # }
            }
        ],
""")

SET_FF_TEMPLATE = Template("""
        [
            name: "$stage_name",
            steps: {
                # script {
                #     general.runCommandInVenv("python3.9 rollout_stage/manage_toggles/restore_toggles.py --dump_name=clean_dump --config $WORKSPACE/config.yaml")        
                # }
            }
        ],
""")

RUN_BDD_TESTS_TEMPLATE = Template("""
        [
            name: "$stage_name",
            steps: {
                # script {
                #     catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                #         build(
                #             job: "CSA_QA/CSA_QA.cloud_taf_bdd/tiny_arc",
                #             parameters: [
                #                 string(name: "SERVER", value: env.SERVER.replaceAll('-', "_")),
                #                 string(name: "MULTI_SCENARIO", value: "Empty"),
                #                 string(name: "SETUP", value: "regress"),
                #                 string(name: "OBJECTS_OWNER", value: "regress"),
                #                 string(name: "RERUN_FAILED_TESTS", value: "3"),
                #                 booleanParam(name: "MAKE_RUN_GREAT_AGAIN", value: true),
                #                 booleanParam(name: "COMPARE_BDD_TESTS", value: false),
                #                 booleanParam(name: 'NOTIFY_ABOUT_NOT_EXISTING_FEATURE_TOGGLES', value: true),
                #                 booleanParam(name: 'NOTIFY_ABOUT_NOT_EXISTING_SERVICE_OR_INVALID_VERSION', value: true),
                #                 booleanParam(name: 'NOTIFY_ABOUT_RUN_RESULTS', value: true)
                #             ],
                #             wait: true,
                #             propagate: false
                #         )
                #     }
                # }
            }
        ],
""")
