"""Copy a built mod to dist, omitting opt-in development test fixtures."""
from pathlib import Path
import hashlib, zipfile
root=Path(__file__).resolve().parent.parent
sources=[p for p in (root/'neoforge/build/libs').glob('Galacticraft-neoforge-*.jar')
         if not any(s in p.name for s in ('-sources','-javadoc','-dev'))]
if not sources:raise SystemExit('Build Galacticraft first.')
source=max(sources,key=lambda p:p.stat().st_mtime)
output=root/'dist';output.mkdir(exist_ok=True);target=output/source.name
with zipfile.ZipFile(source) as src,zipfile.ZipFile(target,'w',zipfile.ZIP_DEFLATED) as dst:
    for item in src.infolist():
        if not item.filename.startswith('dev/galacticraft/neoforge/gametest/'):
            dst.writestr(item,src.read(item.filename))
with zipfile.ZipFile(target) as jar:assert jar.testzip() is None
(output/'SHA256SUMS.txt').write_text(hashlib.sha256(target.read_bytes()).hexdigest()+'  '+target.name+'\n',encoding='utf-8')
print(target.name)
