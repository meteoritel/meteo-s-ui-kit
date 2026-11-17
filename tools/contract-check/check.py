#!/usr/bin/env python3
"""运行真实源集的公开行为契约；资源隔离 fixture 与临时输出写入系统临时目录。"""
from pathlib import Path
import argparse
import os
import re
import subprocess
import tempfile
import sys

sys.stdout.reconfigure(encoding="utf-8")


def java_args(path: Path, args: list[str]) -> str:
    path.write_text("\n".join('"' + value.replace("\\", "/").replace('"', '\\"') + '"' for value in args), encoding="utf-8")
    return "@" + str(path)


def run_java(java: Path, folder: Path, classpath: str, args: list[str], expected_failure: str | None = None) -> None:
    command = [str(java), java_args(folder / "java.args", ["-Duser.language=en", "-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8", "-cp", classpath, *args])]
    result = subprocess.run(command, capture_output=True, text=True, encoding="utf-8", errors="replace", cwd=folder)
    output = result.stdout + result.stderr
    if expected_failure is not None:
        if result.returncode == 0 or expected_failure not in output or not re.search(r"Fixture.java:\d+:", output):
            raise RuntimeError("资源门禁负例未按预期失败：\n" + output)
    elif result.returncode:
        raise RuntimeError(output)
    else:
        print(output.strip())


def resources(java: Path, folder: Path, classpath: str, root: Path) -> None:
    source = folder / "resources"
    source.mkdir()
    fixture = source / "Fixture.java"
    namespaces = (root / "tools/ui-kit-interface/host-namespaces.txt").read_text(encoding="utf-8").split()
    host, other = namespaces[0], namespaces[-1]
    checks = [
        ('Component.translatable("screen.HOST_NAMESPACE.title");', "宿主本地化 key"),
        ('Component.translatable("screen." + HOST + ".title");', "宿主本地化 key"),
        ('ResourceLocation.fromNamespaceAndPath("OTHER_NAMESPACE", "icon");', "宿主资源命名空间"),
        ('ResourceLocation.parse(HOST + ":icon");', "宿主资源命名空间"),
        ('Component.translatable("screen.meteos_ui_kit.title"); ResourceLocation.parse("minecraft:stone"); String harmless = HOST;', None),
    ]
    for body, failure in checks:
        body = body.replace('HOST_NAMESPACE', host).replace('OTHER_NAMESPACE', other)
        fixture.write_text('package com.meteorite.meteosuikit.api.fixture;\n'
                           'import net.minecraft.network.chat.Component;\n'
                           'import net.minecraft.resources.ResourceLocation;\n'
                           f'public final class Fixture {{ static final String HOST = "{host}"; void use() {{ '
                           + body + ' } }\n', encoding="utf-8")
        run_java(java, folder, classpath, ["ExportInterface", str(source), str(folder / "fixture-api.txt"), "write",
                 str(root / "tools/ui-kit-interface/dependencies.txt"), str(root / "tools/ui-kit-interface/host-namespaces.txt")], failure)
    print("PASS: host resource boundary positive/negative cases")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--classpath-file", required=True)
    parser.add_argument("--java-home", required=True)
    parser.add_argument("--contract", action="append", default=[], help="只运行指定 Java 契约，可重复")
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[2]
    classpath = Path(args.classpath_file).read_text(encoding="utf-8").strip()
    suffix = ".exe" if os.name == "nt" else ""
    java = Path(args.java_home) / "bin" / ("java" + suffix)
    with tempfile.TemporaryDirectory(prefix="meteos-kit-contract-") as temp:
        folder = Path(temp)
        run_java(java, folder, classpath, ["com.meteorite.meteosuikit.contract.ContractRunner", *args.contract])
        resources(java, folder, classpath, root)


if __name__ == "__main__":
    main()
