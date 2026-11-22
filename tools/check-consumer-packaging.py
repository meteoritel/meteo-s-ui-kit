"""核对独立宿主的加载器声明和内嵌平台库。"""

import argparse
from io import BytesIO
import json
from pathlib import PurePosixPath
import tomllib
from zipfile import ZipFile


def check(path, platform, minecraft, version):
    artifact = f'meteos-ui-kit-{platform}-{minecraft}'
    expected_name = f'{artifact}-{version}.jar'
    with ZipFile(path) as host:
        nested = [entry for entry in host.namelist()
                  if PurePosixPath(entry).name.startswith('meteos-ui-kit-') and entry.endswith('.jar')]
        if [PurePosixPath(entry).name for entry in nested] != [expected_name]:
            raise ValueError(f'{platform}: 预期仅内嵌 {expected_name}，实际为 {nested}')
        if platform == 'fabric':
            entries = json.loads(host.read('fabric.mod.json'))['jars']
            declared = [entry['file'] for entry in entries]
        else:
            entries = json.loads(host.read('META-INF/jarjar/metadata.json'))['jars']
            declared = [entry['path'] for entry in entries
                        if entry['identifier'] == {'group': 'com.meteorite.meteosuikit', 'artifact': artifact}
                        and entry['version']['artifactVersion'] == version]
        if nested[0] not in declared:
            raise ValueError(f'{platform}: 加载器未声明 {nested[0]}')
        with ZipFile(BytesIO(host.read(nested[0]))) as kit:
            if platform == 'fabric':
                identity = json.loads(kit.read('fabric.mod.json'))
                mod_id = identity['id']
            else:
                identity = tomllib.loads(kit.read('META-INF/neoforge.mods.toml').decode('utf-8'))['mods'][0]
                mod_id = identity['modId']
            if mod_id != 'meteos_ui_kit' or identity['version'] != version:
                raise ValueError(f'{platform}: 内嵌库身份或版本不匹配')
    print(f'{platform}: {expected_name} 内嵌与加载器声明通过')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('version')
    parser.add_argument('minecraft')
    parser.add_argument('fabric')
    parser.add_argument('neoforge')
    args = parser.parse_args()
    check(args.fabric, 'fabric', args.minecraft, args.version)
    check(args.neoforge, 'neoforge', args.minecraft, args.version)
