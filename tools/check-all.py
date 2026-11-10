"""按固定顺序检查库文档、语言、依赖与平台隔离。"""
from pathlib import Path
import subprocess
import sys
root = Path(__file__).resolve().parent.parent
for script in ('check-docs.py', 'check-i18n.py', 'check-ui-kit-boundaries.py', 'check-platform.py'):
    result = subprocess.run([sys.executable, '-B', str(root / 'tools' / script)], cwd=root)
    if result.returncode:
        raise SystemExit(result.returncode)
print('Library source checks passed.')
