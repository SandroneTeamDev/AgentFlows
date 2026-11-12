import os
import re
import shutil
import subprocess
import tarfile
import tempfile
from pathlib import Path

root = Path(__file__).resolve().parents[1]
match = re.search(r"\*\*v([0-9]+\.[0-9]+\.[0-9]+-rc[0-9]+-alpha[0-9]+)", (root / "README.md").read_text())
if match is None:
    raise SystemExit("README.md does not declare the release version")
version = match.group(1).replace("-", "_")
images = root / "desktop/build/compose/binaries/main/app"
launchers = [p for p in images.glob("*/bin/*") if p.is_file() and p.name in {"AgentFlows", "agentflows"}]
if len(launchers) != 1:
    raise SystemExit("Create exactly one desktop distributable before packaging")
launcher = launchers[0]
source = launcher.parent.parent
artifacts = root / "artifacts"
artifacts.mkdir(exist_ok=True)
with tempfile.TemporaryDirectory(prefix="agentflows-arch-") as temporary:
    stage = Path(temporary) / "package"
    shutil.copytree(source, stage / "opt/agentflows", symlinks=True)
    (stage / "usr/bin").mkdir(parents=True)
    (stage / "usr/bin/agentflows").symlink_to("/opt/agentflows/bin/" + launcher.name)
    applications = stage / "usr/share/applications"
    applications.mkdir(parents=True)
    (applications / "agentflows.desktop").write_text("[Desktop Entry]\nType=Application\nName=AgentFlows\nExec=/usr/bin/agentflows\nTerminal=false\nCategories=Development;\n")
    licenses = stage / "usr/share/licenses/agentflows"
    licenses.mkdir(parents=True)
    shutil.copy2(root / "LICENSE", licenses / "LICENSE")
    size = sum(p.stat().st_size for p in stage.rglob("*") if p.is_file() and not p.is_symlink())
    builddate = int(os.environ.get("SOURCE_DATE_EPOCH", "0"))
    (stage / ".PKGINFO").write_text(
        f"pkgname = agentflows\npkgbase = agentflows\npkgver = {version}-1\n"
        f"pkgdesc = A local-first personal Super-agent workspace\nurl = https://github.com/SandroneTeamDev/AgentFlows\n"
        f"builddate = {builddate}\npackager = SandroneTeamDev\nsize = {size}\narch = x86_64\n"
        "license = AGPL-3.0-only\ndepend = glibc\ndepend = gcc-libs\ndepend = zlib\n"
        "depend = libx11\ndepend = libxext\ndepend = libxi\ndepend = libxrender\ndepend = libxtst\n"
        "depend = fontconfig\ndepend = freetype2\ndepend = alsa-lib\n"
    )
    archive = Path(temporary) / "package.tar"
    def owner(info):
        info.uid = info.gid = 0
        info.uname = info.gname = "root"
        return info
    with tarfile.open(archive, "w", format=tarfile.PAX_FORMAT) as tar:
        for path in sorted(stage.iterdir(), key=lambda p: p.name):
            tar.add(path, arcname=path.name, filter=owner)
    output = artifacts / f"agentflows-{version}-1-x86_64.pkg.tar.zst"
    subprocess.run(["zstd", "--force", "-T0", str(archive), "-o", str(output)], check=True)
    print(output)
