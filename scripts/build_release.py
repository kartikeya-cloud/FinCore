"""Rebuild the self-contained JAR and frontend assets. Run from any directory."""
from pathlib import Path
import shutil, subprocess, os
root=Path(__file__).resolve().parent.parent
npm='npm.cmd' if os.name=='nt' else 'npm'
mvn='mvn.cmd' if os.name=='nt' else 'mvn'
def run(args,path): subprocess.run(args,cwd=root/path,check=True)
run([npm,'ci'],'frontend')
run([npm,'run','build'],'frontend')
run([mvn,'clean','test'],'backend')
shutil.copytree(root/'frontend/dist',root/'backend/target/classes/static',dirs_exist_ok=True)
run([mvn,'-DskipTests','package'],'backend')
(root/'release').mkdir(exist_ok=True)
shutil.copy2(root/'backend/target/fincore-1.0.0.jar',root/'release/fincore-1.0.0.jar')
shutil.copytree(root/'frontend/dist',root/'release/frontend',dirs_exist_ok=True)
print('Created release/fincore-1.0.0.jar with bundled dashboard.')
