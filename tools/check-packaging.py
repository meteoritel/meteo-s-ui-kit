"""核对独立库归档身份、许可、平台实现与开发内容隔离。"""
from pathlib import Path
from zipfile import ZipFile
import json
import sys

from toolslib.checks import CheckResult, Finding, run


def check(root: Path) -> CheckResult:
    properties = dict(line.split('=', 1) for line in
                      (root / 'gradle.properties').read_text(encoding='utf-8').splitlines()
                      if '=' in line and not line.startswith('#'))
    findings = []
    prefix = 'com/meteorite/meteosuikit/'
    for module in ('common', 'fabric', 'neoforge'):
        stem = f"meteos-ui-kit-{module}-1.21.1-{properties['version']}"
        for suffix in ('', '-sources', '-javadoc'):
            path = root / module / 'build/libs' / f'{stem}{suffix}.jar'
            with ZipFile(path) as jar:
                names = set(jar.namelist())
                for name in names:
                    if '/samples/' in name or name.startswith('assets/meteos_ui_kit_samples/'):
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
                if module != 'common':
                    mixins = json.loads(jar.read('meteos_ui_kit.mixins.json'))
                    if mixins.get('mixins') or mixins.get('client') != ['DisplayPreviewAccessor']:
                        findings.append(Finding(str(path), 'Mixin 端隔离', str(mixins)))
                    accessor = prefix + module + '/client/mixin/DisplayPreviewAccessor.class'
                    if accessor not in names:
                        findings.append(Finding(str(path), '平台装配缺失', accessor))
    return CheckResult(findings, success='库归档、许可、平台装配和开发内容隔离检查通过。')


if __name__ == '__main__':
    sys.exit(run(check, __doc__))
