import zipfile
from pathlib import Path

base_dir = Path('C:/Project Work/Swari Sadhan/swari-sewa-backend')
zip_path = base_dir / 'swari-sadhan-aws.zip'

files_to_add = [
    base_dir / 'target' / 'swari-sewa-backend-0.0.1-SNAPSHOT.jar',
    base_dir / 'Procfile',
    base_dir / 'start.sh',
]

ebextensions_dir = base_dir / '.ebextensions'
platform_dir = base_dir / '.platform'

with zipfile.ZipFile(zip_path, 'w', zipfile.ZIP_DEFLATED) as zf:
    for f in files_to_add:
        if not f.exists():
            raise FileNotFoundError(f)
        zf.write(f, f.name)
        print(f'Added {f.name}')

    if ebextensions_dir.exists():
        for f in ebextensions_dir.rglob('*'):
            if f.is_file():
                arcname = '.ebextensions/' + str(f.relative_to(ebextensions_dir)).replace('\\', '/')
                zf.write(f, arcname)
                print(f'Added {arcname}')

    if platform_dir.exists():
        for f in platform_dir.rglob('*'):
            if f.is_file():
                arcname = '.platform/' + str(f.relative_to(platform_dir)).replace('\\', '/')
                zf.write(f, arcname)
                print(f'Added {arcname}')

print('Done:', zip_path)
