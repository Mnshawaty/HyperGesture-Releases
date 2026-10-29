package com.mn.hypergestures;
import android.content.*;
import android.content.pm.*;
import android.os.Build;
import org.json.*;
import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
final class UpdateClient {
 static final String API="https://api.github.com/repos/"+UpdatePolicy.REPOSITORY+"/releases/latest";
 static final long MAX_APK=50L*1024*1024;
 static final AtomicBoolean DOWNLOADING=new AtomicBoolean();
 static class Release {long code,size;String name,notes,url,digest;JSONObject json()throws JSONException{return new JSONObject().put("code",code).put("size",size).put("name",name).put("notes",notes).put("url",url).put("digest",digest);}}
 interface Progress{void update(int p);}
 static SharedPreferences prefs(Context c){return c.getSharedPreferences("updates",0);}
 static long installed(Context c)throws Exception{return c.getPackageManager().getPackageInfo(c.getPackageName(),0).getLongVersionCode();}
 static File apk(Context c){return new File(c.getFilesDir(),"update.apk");}
 static Release cached(Context c){try{JSONObject j=new JSONObject(prefs(c).getString("release",""));Release r=new Release();r.code=j.getLong("code");r.size=j.getLong("size");r.name=j.getString("name");r.notes=j.getString("notes");r.url=j.getString("url");r.digest=j.getString("digest");return UpdatePolicy.isAssetUrl(r.url)&&UpdatePolicy.validDigest(r.digest)?r:null;}catch(Exception e){return null;}}
 static HttpURLConnection connect(String address)throws IOException{
  for(int i=0;i<6;i++){if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();if(!UpdatePolicy.allowedNetworkUrl(address))throw new IOException("Untrusted update host");HttpURLConnection h=(HttpURLConnection)new URL(address).openConnection();h.setConnectTimeout(15000);h.setReadTimeout(20000);h.setInstanceFollowRedirects(false);h.setRequestProperty("User-Agent","HyperGesture-Updater");int status=h.getResponseCode();
   if(status==301||status==302||status==303||status==307||status==308){String next=h.getHeaderField("Location");h.disconnect();if(next==null)throw new IOException("Missing download location");address=new URL(new URL(address),next).toString();continue;}
   if(status!=200){h.disconnect();if(status==404)throw new IOException("Public update feed is unavailable.");if(status==403||status==429)throw new IOException("GitHub request limit. Try again later.");throw new IOException("GitHub HTTP "+status);}return h;
  }throw new IOException("Too many redirects");
 }
 static byte[] read(String url,int limit)throws IOException{HttpURLConnection h=connect(url);try(InputStream in=h.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();if(out.size()+n>limit)throw new IOException("Metadata too large");out.write(b,0,n);}return out.toByteArray();}finally{h.disconnect();}}
 static Release check(Context c)throws Exception{
  JSONObject j=new JSONObject(new String(read(API,1024*1024),"UTF-8"));if(j.optBoolean("draft")||j.optBoolean("prerelease"))throw new IOException("No stable release");JSONArray assets=j.getJSONArray("assets");String manifest=null;
  for(int i=0;i<assets.length();i++){JSONObject a=assets.getJSONObject(i);if("update.json".equals(a.optString("name")))manifest=a.getString("browser_download_url");}
  if(!UpdatePolicy.isAssetUrl(manifest))throw new IOException("This release has no valid update.json metadata.");JSONObject m=new JSONObject(new String(read(manifest,65536),"UTF-8"));
  if(m.getInt("schemaVersion")!=1||!UpdatePolicy.PACKAGE.equals(m.getString("packageName")))throw new IOException("Incompatible metadata");if(m.getInt("minSdk")>Build.VERSION.SDK_INT)throw new IOException("Update needs newer Android");
  Release r=new Release();r.code=m.getLong("versionCode");r.name=m.getString("versionName");r.size=m.getLong("size");r.digest=m.getString("sha256");if(r.code<=0||r.size<=0||r.size>MAX_APK||!UpdatePolicy.validDigest(r.digest))throw new IOException("Invalid metadata");String filename=m.getString("apkAsset");if(!filename.endsWith(".apk")||filename.contains("/"))throw new IOException("Invalid APK name");
  for(int i=0;i<assets.length();i++){JSONObject a=assets.getJSONObject(i);if(filename.equals(a.optString("name"))){r.url=a.getString("browser_download_url");if(a.getLong("size")!=r.size)throw new IOException("APK size mismatch");}}
  if(!UpdatePolicy.isAssetUrl(r.url))throw new IOException("Release APK missing");r.notes=j.optString("body","");if(r.notes.length()>16000)r.notes=r.notes.substring(0,16000);prefs(c).edit().putString("release",r.json().toString()).putLong("checked",System.currentTimeMillis()).apply();return r;
 }
 static String digest(File f)throws Exception{MessageDigest d=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(f)){byte[] b=new byte[32768];int n;while((n=in.read(b))!=-1)d.update(b,0,n);}StringBuilder s=new StringBuilder();for(byte b:d.digest())s.append(String.format(Locale.US,"%02x",b&255));return s.toString();}
 static void verify(Context c,File f,Release r)throws Exception{
  if(f.length()!=r.size||!digest(f).equalsIgnoreCase(r.digest))throw new IOException("Download integrity check failed");PackageManager pm=c.getPackageManager();PackageInfo local=pm.getPackageInfo(c.getPackageName(),PackageManager.GET_SIGNING_CERTIFICATES),remote=pm.getPackageArchiveInfo(f.getAbsolutePath(),PackageManager.GET_SIGNING_CERTIFICATES);
  if(remote==null||!local.packageName.equals(remote.packageName)||remote.getLongVersionCode()!=r.code||!UpdatePolicy.newer(local.getLongVersionCode(),r.code)||remote.applicationInfo==null||remote.applicationInfo.minSdkVersion>Build.VERSION.SDK_INT)throw new IOException("APK package or version check failed");
  if(remote.signingInfo==null||local.signingInfo==null)throw new IOException("Missing signer");Set<String> expected=new HashSet<>(),actual=new HashSet<>();for(Signature s:local.signingInfo.getApkContentsSigners())expected.add(s.toCharsString());for(Signature s:remote.signingInfo.getApkContentsSigners())actual.add(s.toCharsString());if(expected.isEmpty()||!expected.equals(actual))throw new IOException("APK signer does not match this app");
  // Android installer performs authoritative cryptographic signature verification.
 }
 static File download(Context c,Release r,Progress progress)throws Exception{
  if(!DOWNLOADING.compareAndSet(false,true))throw new IOException("Download already running");File part=new File(c.getFilesDir(),"update.part");HttpURLConnection h=null;
  try{if(!UpdatePolicy.newer(installed(c),r.code))throw new IOException("Version already installed");h=connect(r.url);long bytes=0;int last=-1;
   try(InputStream in=h.getInputStream();FileOutputStream out=new FileOutputStream(part)){byte[] b=new byte[32768];int n;while((n=in.read(b))!=-1){if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();bytes+=n;if(bytes>r.size||bytes>MAX_APK)throw new IOException("Unexpected size");out.write(b,0,n);int p=(int)(bytes*100/r.size);if(p!=last){last=p;progress.update(p);}}out.getFD().sync();}
   verify(c,part,r);File target=apk(c);if(target.exists()&&!target.delete())throw new IOException("Cannot replace download");if(!part.renameTo(target))throw new IOException("Cannot save APK");return target;
  }finally{if(h!=null)h.disconnect();part.delete();DOWNLOADING.set(false);}
 }
}

