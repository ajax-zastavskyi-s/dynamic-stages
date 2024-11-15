import sys
from string import Template
from loguru import logger as _logger


def render_template(template: Template, **kwargs):
    return template.safe_substitute(**kwargs).strip("\n")

def get_logger():
    logs_format = " ".join([
        "<level>{level}</level>",
        "<green>{time:YYYY-DD-MM HH:mm:ss.ms}</green>",
        "<level>{message}</level>"])

    _logger.remove()
    _logger.add(sink=sys.stdout, format=logs_format)

    return _logger

