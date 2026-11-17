"""核对本地 Maven 产物、附件、映射与元数据。"""
import argparse
import hashlib
import json
from pathlib import Path
from xml.etree import ElementTree
from zipfile import ZipFile

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--repository', required=True)
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
properties = dict(line.split('=', 1) for line in (root / 'gradle.properties').read_text(encoding='utf-8').splitlines()
                  if '=' in line and not line.startswith('#'))
version = properties['version']
for module in ('common', 'fabric', 'neoforge'):
    artifact = f'meteos-ui-kit-{module}-{properties["minecraft_version"]}'
    folder = Path(args.repository) / 'com/meteorite/meteosuikit' / artifact / version
    stem = f'{artifact}-{version}'
    metadata = json.loads((folder / f'{stem}.module').read_text())
    assert not ElementTree.parse(folder / f'{stem}.pom').findall('.//{*}dependency'), f'{module}: 意外 POM 依赖'
    for variant in metadata['variants']:
        assert not variant.get('dependencies') and not variant.get('dependencyConstraints'), f'{module}: 意外 module 依赖'
        for entry in variant.get('files', []):
            data = (folder / entry['url']).read_bytes()
            assert hashlib.sha512(data).hexdigest() == entry['sha512'], f'{module}: 元数据摘要与归档不一致'
    for suffix in ('', '-sources', '-javadoc'):
        published = folder / f'{stem}{suffix}.jar'
        built = root / module / 'build/libs' / published.name
        assert published.read_bytes() == built.read_bytes(), f'{module}: 发布的附件与构建产物不同'
        with ZipFile(published) as jar:
            names = jar.namelist()
            assert any(n.startswith('LICENSE.txt') for n in names), f'{module}: 缺少许可'
            assert not any('/samples/' in n or 'meteos_ui_kit_samples' in n for n in names), f'{module}: 开发内容泄漏'
            if module == 'fabric' and not suffix:
                mixin = json.loads(jar.read('meteos_ui_kit.mixins.json'))
                assert mixin['refmap'] in names, 'Fabric refmap 缺失'
                # 运行产物的原版类型引用必须使用 intermediary。
                assert b'net/minecraft/class_' in jar.read('com/meteorite/meteosuikit/api/control/UiTextInput.class'), 'Fabric 运行产物未 remap'
    print(f'[通过] {artifact}:{version} 归档、附件、摘要、映射与元数据')
