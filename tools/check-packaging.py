"""核对独立库归档身份、许可、平台实现与开发内容隔离。"""
from pathlib import Path
from zipfile import ZipFile
import json
import sys
import tomllib

from toolslib.checks import CheckResult, Finding, run


def check(root: Path) -> CheckResult:
    properties = dict(line.split('=', 1) for line in
                      (root / 'gradle.properties').read_text(encoding='utf-8').splitlines()
                      if '=' in line and not line.startswith('#'))
    findings = []
    prefix = 'com/meteorite/meteosuikit/'
    for module in ('common', 'fabric', 'neoforge'):
        stem = f"meteos-ui-kit-{module}-{properties['minecraft_version']}-{properties['version']}"
        for suffix in ('', '-sources', '-javadoc'):
            path = root / module / 'build/libs' / f'{stem}{suffix}.jar'
            with ZipFile(path) as jar:
                names = set(jar.namelist())
                for name in names:
                    if '/samples/' in name or '/contract/' in name or name.startswith('assets/meteos_ui_kit_samples/'):
                        findings.append(Finding(str(path), '开发内容泄漏', name))
                if not any(name.startswith('LICENSE.txt') for name in names):
                    findings.append(Finding(str(path), '许可', '缺少 MIT 许可附件'))
                if suffix:
                    continue
                for required in ('api/input/UiInputBridge.class', 'api/layout/UiGridMetrics.class'):
                    if prefix + required not in names:
                        findings.append(Finding(str(path), '通用实现缺失', required))
                if module == 'fabric':
                    metadata = json.loads(jar.read('fabric.mod.json'))
                    if metadata['id'] != 'meteos_ui_kit' or metadata['version'] != properties['version']:
                        findings.append(Finding(str(path), 'Fabric 身份', str(metadata)))
                    for declaration in metadata.get('mixins', []):
                        config = declaration if isinstance(declaration, str) else declaration['config']
                        mixin_package = json.loads(jar.read(config)).get('package')
                        if not mixin_package:
                            continue
                        for entry in metadata.get('entrypoints', {}).get('client', []):
                            value = entry if isinstance(entry, str) else entry['value']
                            if value.split('::', 1)[0].startswith(mixin_package + '.'):
                                findings.append(Finding(str(path), 'Fabric 客户端入口位于 Mixin 包', f'{value} ({config})'))
                if module != 'common':
                    resource = 'fabric.mod.json' if module == 'fabric' else 'META-INF/neoforge.mods.toml'
                    text = jar.read(resource).decode('utf-8')
                    if '${' in text:
                        findings.append(Finding(str(path), '资源占位符未展开', resource))
                    if module == 'fabric':
                        data = json.loads(text)
                        expected = {'fabricloader': '>=' + properties['fabric_loader_version'],
                                    'minecraft': properties['minecraft_version'], 'java': '>=' + properties['java_version']}
                        if any(data['depends'].get(key) != value for key, value in expected.items()):
                            findings.append(Finding(str(path), 'Fabric 版本元数据', str(data['depends'])))
                    else:
                        data = tomllib.loads(text)
                        mods = data['mods'][0]
                        ranges = {entry['modId']: entry['versionRange'] for entry in data['dependencies'][properties['mod_id']]}
                        if (data['loaderVersion'] != properties['neoforge_loader_version_range']
                                or mods['modId'] != properties['mod_id'] or mods['version'] != properties['version']
                                or ranges.get('minecraft') != properties['minecraft_version_range']
                                or ranges.get('neoforge') != '[' + properties['neoforge_version'] + ',)'):
                            findings.append(Finding(str(path), 'NeoForge 版本元数据', str(ranges)))
                if module != 'common':
                    mixins = json.loads(jar.read('meteos_ui_kit.mixins.json'))
                    if mixins.get('mixins') or mixins.get('client') != ['DisplayPreviewAccessor']:
                        findings.append(Finding(str(path), 'Mixin 端隔离', str(mixins)))
                    accessor = prefix + 'internal/preview/mixin/DisplayPreviewAccessor.class'
                    if mixins.get('package') != 'com.meteorite.meteosuikit.internal.preview.mixin':
                        findings.append(Finding(str(path), '共享 Mixin 包', str(mixins)))
                    if accessor not in names:
                        findings.append(Finding(str(path), '平台装配缺失', accessor))
                        continue
                    if module == 'fabric':
                        refmap = mixins.get('refmap')
                        if not refmap or refmap not in names:
                            findings.append(Finding(str(path), 'Fabric refmap', '共享 Mixin 配置未引用有效 refmap'))
                        elif accessor.removesuffix('.class') not in json.loads(jar.read(refmap)).get('mappings', {}):
                            findings.append(Finding(str(path), 'Fabric refmap', '缺少共享 Display accessor 映射'))
                        if b'net/minecraft/class_' not in jar.read(accessor):
                            findings.append(Finding(str(path), 'Fabric remap', '共享 Display accessor 未使用 intermediary 类型'))
    return CheckResult(findings, success='库归档、许可、平台装配和开发内容隔离检查通过。')


if __name__ == '__main__':
    sys.exit(run(check, __doc__))
