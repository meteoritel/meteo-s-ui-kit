"""Gradle 的兼容启动层；在载入检查脚本前检查 Python 版本。"""
import sys

if sys.version_info < (3, 11):
    raise SystemExit("需要 Python 3.11+，当前 " + sys.version.split()[0])

import runpy
from pathlib import Path

script = Path(sys.argv[1]).resolve()
sys.argv = sys.argv[1:]
sys.path.insert(0, str(script.parent))
runpy.run_path(str(script), run_name="__main__")
