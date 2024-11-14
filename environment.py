from templates import BASE_GROOVY_TEMPLATE


def before_scenario(context, scenario):
    context.stages = []
    context.dependent_stages_results_variables = []

def after_scenario(context, scenario):
    groovy = BASE_GROOVY_TEMPLATE.safe_substitute(stages="\n".join((str(stage) for stage in context.stages))).strip("\n")

    with open(f"scenarios/{scenario.name}.groovy", "w") as f:
        f.write(groovy)
