"""检查库的加载器隔离与服务端入口，运行时验收由实际加载器执行。"""
from pathlib import Path
import sys
from toolslib.checks import CheckResult, Finding, run
from toolslib.java_references import Source
from toolslib.repo import collect_files

def check(root: Path) -> CheckResult:
    findings = []
    for path in collect_files(root, '.java'):
        source = Source.read(path, root)
        for line, target in source.imports:
            if target.startswith('net.fabricmc.') and source.module != 'fabric':
                findings.append(Finding(source.relative, 'Fabric 隔离', target, line))
            if target.startswith('net.neoforged.') and source.module != 'neoforge':
                findings.append(Finding(source.relative, 'NeoForge 隔离', target, line))
        if path.name == 'MeteosUiKit.java' and 'net.minecraft.client.' in source.code:
            findings.append(Finding(source.relative, '服务端入口', '入口引用客户端类型'))
    return CheckResult(findings, success='平台与最小入口隔离检查通过。')

if __name__ == '__main__':
    sys.exit(run(check, __doc__))
