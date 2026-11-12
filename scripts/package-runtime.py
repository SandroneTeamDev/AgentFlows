import os
import shutil
from pathlib import Path

root = Path(__file__).resolve().parents[1]
engine = root / "engine"
resources = root / "desktop/resources/common"
if not (engine / "dist/main.js").is_file():
    raise SystemExit("Build the engine before packaging its runtime")
node = shutil.which("node")
if node is None:
    raise SystemExit("Node.js is required for runtime packaging")
resources.mkdir(parents=True, exist_ok=True)
for name in ["engine", "runtime"]:
    target = resources / name
    if target.exists():
        shutil.rmtree(target)
    target.mkdir()
shutil.copytree(engine / "dist", resources / "engine/dist")
shutil.copytree(engine / "node_modules", resources / "engine/node_modules", symlinks=True)
shutil.copy2(engine / "package.json", resources / "engine/package.json")
name = "node.exe" if os.name == "nt" else "node"
shutil.copy2(Path(node).resolve(), resources / "runtime" / name)
(resources / "runtime" / name).chmod(0o755)
print("Built engine and native Node.js runtime staged for packaging")
