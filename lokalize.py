# Steps to use:
# 1) Install Python (depends on OS)
# 2) Install Lokalise API for python:
# > pip3 install python-lokalise-api --break-system-packages
# 3) Obtain token here: https://app.lokalise.com/profile#apitokens
# 4) Create file local.properties (if not exists) and define variables there:
# > lokalise.project.id=[PROJECT_ID]
# > lokalise.token=[YOUR_TOKEN]
# 5) Use script when you need to download translations:
# > python3 lokalize.py
# In case you want to filter languages, just add --langs argument with specific values
# > python3 lokalize.py --langs en,ru

import os
import urllib.request
import zipfile
import lokalise
import configparser
import argparse
import sys
import shutil

config = configparser.ConfigParser()

parser = argparse.ArgumentParser()
parser.add_argument(
    "--langs",
    nargs='?',
    type=str,
    help="--langs en,ru",
    default='',
)

config_file_path = "local.properties"
section_default = "default"
project_id_key = "lokalise.project.id"
token_key = "lokalise.token"

directory_prefix = "core/res/src/main/res/values-%LANG_ISO%/"

if not os.path.isfile(config_file_path):
    print(f"{config_file_path} not found!")
else:
    with open(config_file_path) as stream:
        config.read_string(f'[{section_default}]\n' + stream.read())
    api_token = config.get(section_default, token_key, fallback=None)
    project_id = config.get(section_default, project_id_key, fallback=None)
    if project_id is None:
        print(f"Key '{project_id_key}' not found in file {config_file_path}")
    elif api_token is None:
        print(f"Key '{token_key}' not found in file {config_file_path}")
    else:
        print(f"Found project: {project_id}")
        print(f"Found token: {api_token}")

        folder_path = "."
        file_name = "lokalize.zip"

        client = lokalise.Client(api_token)
        project = client.project(project_id)

        print('Found project: ' + project.name)

        print('Read args...')
        args = parser.parse_args()

        print("Generating bundle...")
        response = client.download_files(project_id, {
            "format": "xml",
            "original_filenames": True,
            "filter_langs": list(filter(None, args.langs.split(",") if args.langs is not None else "")),
            "directory_prefix": directory_prefix,
            "filter_data": ["translated"]
        })

        print("Downloading translation file...")
        bundle_url = response['bundle_url']
        urllib.request.urlretrieve(bundle_url, file_name)

        print("Unzipping archive...")
        with zipfile.ZipFile(file_name, 'r') as zip_ref:
            zip_ref.extractall(folder_path)

        print("Removing archive...")
        os.remove(file_name)

        print("All done!")
