"""核对本地 Maven 产物、附件、映射与元数据。"""
import argparse
import hashlib
import json
from pathlib import Path
import re
from xml.etree import ElementTree
from zipfile import ZipFile

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--repository', required=True)
parser.add_argument('--remote-staging', action='store_true', help='校验远程分支只包含 RC 和正式坐标')
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
properties = dict(line.split('=', 1) for line in (root / 'gradle.properties').read_text(encoding='utf-8').splitlines()
                  if '=' in line and not line.startswith('#'))
version = properties['version']
modules = ('common', 'fabric', 'neoforge')
group_dir = Path(args.repository) / 'com/meteorite/meteosuikit'
if args.remote_staging:
    release_version = re.compile(r'\d+\.\d+\.\d+(?:-rc[1-9]\d*)?')
    if not release_version.fullmatch(version):
        parser.error(f'远程只接受 RC 和正式坐标：{version}')
    for artifact_dir in group_dir.iterdir():
        if not artifact_dir.is_dir():
            continue
        versions = ElementTree.parse(artifact_dir / 'maven-metadata.xml').findall('./versioning/versions/version')
        published = {entry.text for entry in versions}
        directories = {entry.name for entry in artifact_dir.iterdir() if entry.is_dir()}
        if published != directories or any(not release_version.fullmatch(entry) for entry in directories):
            parser.error(f'{artifact_dir.name}: 远程版本目录或 Maven 元数据不符合发布规则')

for module in modules:
    artifact = f'meteos-ui-kit-{module}-{properties["minecraft_version"]}'
    folder = group_dir / artifact / version
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
