import argparse
import json

from helpers import get_logger
from rc_groovy_scenario_builder import RCGroovyScenarioBuilder
from loguru import logger as LoguruLogger


def generate_groovy_file(json_scenario: dict, logger: LoguruLogger):
    builder = RCGroovyScenarioBuilder(
        scenario_name=json_scenario,
        logger=logger,

    )

    builder.save_groovy_file()

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--json_scenario_path",
        type=str,
        help="Path to json file with RC testing scenario",
        required=True
    )
    args = parser.parse_args()
    logger = get_logger()

    try:
        with open(args.json_scenario_path) as json_scenario_file:
            scenario = json.load(fp=json_scenario_file)
    except json.decoder.JSONDecodeError:
        logger.error("Failed to parse JSON scenario")
        exit(1)
    else:
        generate_groovy_file(json_scenario=scenario, logger=logger)


if __name__ == "__main__":
    main()
