#!/usr/bin/env python3
"""Validate the signed APK and generate the two files required by the updater."""
import argparse, hashlib, json, pathlib, re, shutil, subprocess
p=argparse.ArgumentParser()
p.add_argument('apk',type=pathlib.Path)
p.add_argument('--build-tools',required=True,type=pathlib.Path)
p.add_argument('--out-dir',type=pathlib.Path,default=pathlib.Path('build/release'))
a=p.parse_args()
verify=subprocess.check_output(['java','-jar',str(a.build_tools/'lib/apksigner.jar'),'verify','--print-certs',str(a.apk)],text=True)
expected='3d54dc62336f434fe011e56c9c25fbbdeca333045941702ed97051fd55d03983'
if 'certificate SHA-256 digest: '+expected not in verify:raise SystemExit('Wrong signing key: do not publish this APK.')
badging=subprocess.check_output([str(a.build_tools/'aapt'),'dump','badging',str(a.apk)],text=True)
package=re.search(r"package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'",badging)
if not package or package[1]!='com.mn.hypergestures':raise SystemExit('Unexpected package')
code=int(package[2]);version=package[3]
if not re.fullmatch(r'[A-Za-z0-9._-]+',version):raise SystemExit('Unsafe version name')
minsdk=int(re.search(r"sdkVersion:'(\d+)'",badging)[1])
a.out_dir.mkdir(parents=True,exist_ok=True)
name=f'HyperGesture-{version}-build{code}-signed.apk'
target=a.out_dir/name
if target.resolve()!=a.apk.resolve():shutil.copyfile(a.apk,target)
data={'schemaVersion':1,'packageName':package[1],'versionCode':code,'versionName':version,'minSdk':minsdk,'apkAsset':name,'size':target.stat().st_size,'sha256':hashlib.sha256(target.read_bytes()).hexdigest()}
(a.out_dir/'update.json').write_text(json.dumps(data,indent=2)+'\n')
print(json.dumps(data,indent=2))
print('Upload BOTH files to a draft release in Mnshawaty/HyperGesture-Releases, then publish as Latest.')

