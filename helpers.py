from string import Template


def render_template(template: Template, **kwargs):
    return template.safe_substitute(**kwargs).strip("\n")
