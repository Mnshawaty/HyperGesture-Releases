#!/usr/bin/env python3
"""Rebuild with JDK 17, SDK Build Tools 35, Android 35 android.jar, Xposed API 82 (compile-only).
Usage: python3 build.py --build-tools /sdk/build-tools/35.0.0 --android-jar /sdk/platforms/android-35/android.jar --xposed-api /path/api-82.jar
"""
import argparse, pathlib, subprocess, zipfile, shutil
p=argparse.ArgumentParser()
p.add_argument('--build-tools',required=True);p.add_argument('--android-jar',required=True);p.add_argument('--xposed-api',required=True)
a=p.parse_args();root=pathlib.Path(__file__).resolve().parent
bt=pathlib.Path(a.build_tools).resolve();aj=str(pathlib.Path(a.android_jar).resolve());api=str(pathlib.Path(a.xposed_api).resolve())
out=root/'build';out.mkdir(exist_ok=True)
for name in ('classes','dex'):
 d=out/name
 if d.exists():shutil.rmtree(d)
 d.mkdir()
def run(*cmd):subprocess.run([str(x) for x in cmd],check=True,timeout=120)
import os
run('java','com.sun.tools.javac.Main','-encoding','UTF-8','-source','8','-target','8','-cp',aj+os.pathsep+api,'-d',out/'classes',*root.glob('src/com/mn/hypergestures/*.java'))
with zipfile.ZipFile(out/'classes.jar','w') as z:
 for f in (out/'classes').rglob('*.class'):z.write(f,f.relative_to(out/'classes'))
# DEX compatibility floor 34; the manifest and runtime gate require Android 15 / API 35.
run('java','-cp',bt/'lib/d8.jar','com.android.tools.r8.D8','--min-api','34','--lib',aj,'--classpath',api,'--output',out/'dex',out/'classes.jar')
run(bt/'aapt2','compile','--dir',root/'res','-o',out/'res.zip')
run(bt/'aapt2','link','-o',out/'base.apk','-I',aj,'--manifest',root/'AndroidManifest.xml','-A',root/'assets',out/'res.zip')
shutil.copyfile(out/'base.apk',out/'unsigned.apk')
with zipfile.ZipFile(out/'unsigned.apk','a') as z:z.write(out/'dex/classes.dex','classes.dex')
run(bt/'zipalign','-f','-p','4',out/'unsigned.apk',out/'aligned.apk')
# Official signing credentials are supplied externally, never committed.
release_key=os.environ.get('HG_KEYSTORE')
keystore=pathlib.Path(release_key).resolve() if release_key else out/'debug.p12'
alias=os.environ.get('HG_KEY_ALIAS','androiddebugkey')
if release_key:
 if not keystore.is_file():raise SystemExit('HG_KEYSTORE does not exist')
 if not os.environ.get('HG_STORE_PASSWORD'):raise SystemExit('HG_STORE_PASSWORD is required')
else:
 os.environ['HG_STORE_PASSWORD']='android'
 os.environ['HG_KEY_PASSWORD']='android'
 if not keystore.exists():
  run('keytool','-genkeypair','-keystore',keystore,'-storepass:env','HG_STORE_PASSWORD','-keypass:env','HG_KEY_PASSWORD','-alias',alias,'-keyalg','RSA','-keysize','2048','-validity','3650','-dname','CN=HyperGesture Local Development')
os.environ.setdefault('HG_KEY_PASSWORD',os.environ['HG_STORE_PASSWORD'])
import xml.etree.ElementTree as ET
version=ET.parse(root/'AndroidManifest.xml').getroot().attrib['{http://schemas.android.com/apk/res/android}versionName']
apk=root/('HyperGesture-'+version+'.apk')
run('java','-jar',bt/'lib/apksigner.jar','sign','--ks',keystore,'--ks-pass','env:HG_STORE_PASSWORD','--key-pass','env:HG_KEY_PASSWORD','--ks-key-alias',alias,'--v2-signing-enabled','true','--v3-signing-enabled','true','--out',apk,out/'aligned.apk')
run('java','-jar',bt/'lib/apksigner.jar','verify','--verbose',apk)
run(bt/'zipalign','-c','-p','4',apk)
print(apk)

