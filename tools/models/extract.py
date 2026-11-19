from pathlib import Path
import io, zipfile
root=Path(__file__).parent
import sys
if len(sys.argv)!=2: raise SystemExit('Usage: python extract.py ARCHIVE_DIRECTORY')
downloads=Path(sys.argv[1])
for key,filename in [('standard','jane-doe-zzz.zip'),('nocturne','jane-doe-nocturne-of-light.zip')]:
    dest=root/'source'/key
    dest.mkdir(parents=True,exist_ok=True)
    with zipfile.ZipFile(downloads/filename) as pkg:
        inner=next((n for n in pkg.namelist() if n.startswith('source/') and n.endswith('.zip')),None)
        model=zipfile.ZipFile(io.BytesIO(pkg.read(inner))) if inner else pkg
        for name in model.namelist():
            if name.lower().endswith(('.fbx','_d.png')):
                (dest/Path(name).name).write_bytes(model.read(name))

