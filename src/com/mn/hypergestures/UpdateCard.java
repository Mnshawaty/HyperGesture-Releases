package com.mn.hypergestures;

import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.util.concurrent.*;

final class UpdateCard extends ContextWrapper {
    private TextView status,notes,versionInfo;
    private TextView check,download;
    private final Activity host;
    private LinearLayout content;
    private final Runnable changed;
    private Dialog panel;
    private ProgressBar progress;
    private UpdateClient.Release release;
    private boolean ready,waitingPermission,busy;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private Future<?> task;
    private final Handler main=new Handler(Looper.getMainLooper());
    private void ui(Runnable r){main.post(()->{if(!host.isFinishing()&&!host.isDestroyed())r.run();});}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private static final int TEXT=0xfff3f5fb,MUTED=0xffa4aec2,ACCENT=0xffb0a0ff;
    private GradientDrawable shape(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
    private TextView text(LinearLayout p,String value,int size){
        TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(TEXT);
        t.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));t.setLineSpacing(dp(3),1);p.addView(t);return t;
    }
    private void gap(LinearLayout p,int height){p.addView(new Space(this),new LinearLayout.LayoutParams(1,dp(height)));}
    private TextView button(LinearLayout p,String title,boolean primary,Runnable action){
        TextView b=new TextView(this);b.setText(title);b.setTextSize(15);b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        b.setTextColor(primary?0xff171225:ACCENT);b.setGravity(Gravity.CENTER);b.setPadding(dp(14),dp(12),dp(14),dp(12));
        b.setMinimumHeight(dp(52));b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33ffffff),shape(primary?ACCENT:0xff29243f,16),null));
        b.setFocusable(true);b.setOnClickListener(v->action.run());
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(10);p.addView(b,lp);return b;
    }
    private LinearLayout surface(){
        LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        box.setPadding(dp(24),dp(24),dp(24),dp(20));box.setBackground(shape(0xff171e2c,28));return box;
    }
    private Dialog dialog(LinearLayout box){
        Dialog d=new Dialog(host);d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ScrollView scroll=new ScrollView(host);scroll.setFillViewport(true);scroll.addView(box);d.setContentView(scroll);
        Window w=d.getWindow();w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);WindowManager.LayoutParams lp=w.getAttributes();lp.dimAmount=.65f;w.setAttributes(lp);
        d.show();int width=Math.min(getResources().getDisplayMetrics().widthPixels-dp(32),dp(420));
        w.setLayout(width,WindowManager.LayoutParams.WRAP_CONTENT);return d;
    }
    private void confirm(String title,String message,String action,Runnable accept,Runnable cancel){
        LinearLayout box=surface();TextView heading=text(box,title,23);heading.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        gap(box,12);text(box,message,14).setTextColor(MUTED);gap(box,12);
        final Dialog[] d=new Dialog[1];button(box,action,true,()->{d[0].dismiss();accept.run();});
        button(box,"Cancel",false,()->{d[0].dismiss();if(cancel!=null)cancel.run();});d[0]=dialog(box);
        d[0].setOnCancelListener(x->{if(cancel!=null)cancel.run();});
    }
    UpdateCard(Activity activity,Runnable onChanged){
        super(activity);host=activity;changed=onChanged;content=surface();
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);content.addView(top);
        ImageView logo=new ImageView(this);logo.setImageDrawable(getApplicationInfo().loadIcon(getPackageManager()));
        top.addView(logo,new LinearLayout.LayoutParams(dp(44),dp(44)));
        TextView brand=new TextView(this);brand.setText("HYPERGESTURE");brand.setTextColor(ACCENT);brand.setTextSize(11);brand.setLetterSpacing(.12f);
        LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,-2,1);bp.leftMargin=dp(14);top.addView(brand,bp);
        TextView close=new TextView(this);close.setText("×");close.setTextColor(MUTED);close.setTextSize(26);close.setGravity(Gravity.CENTER);
        close.setBackground(shape(0xff242c3c,22));close.setContentDescription("Close updates");close.setFocusable(true);
        close.setOnClickListener(v->{if(panel!=null)panel.dismiss();});top.addView(close,new LinearLayout.LayoutParams(dp(44),dp(44)));
        gap(content,24);TextView title=text(content,"App updates",27);title.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        gap(content,8);text(content,"The latest HyperGesture, directly from GitHub.",14).setTextColor(MUTED);
        gap(content,22);LinearLayout details=new LinearLayout(this);details.setOrientation(1);details.setPadding(dp(16),dp(16),dp(16),dp(16));details.setBackground(shape(0xff0f1521,18));content.addView(details);
        TextView small=text(details,"RELEASE STATUS",10);small.setTextColor(ACCENT);small.setLetterSpacing(.10f);gap(details,10);
        status=text(details,"Checking for updates…",17);gap(details,8);
        versionInfo=text(details,"",12);versionInfo.setTextColor(MUTED);
        try{versionInfo.setText("Installed  "+getPackageManager().getPackageInfo(getPackageName(),0).versionName);}catch(Exception ignored){}
        gap(details,12);progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(100);
        progress.setProgressTintList(ColorStateList.valueOf(ACCENT));progress.setProgressBackgroundTintList(ColorStateList.valueOf(0xff30394b));
        details.addView(progress,new LinearLayout.LayoutParams(-1,dp(6)));progress.setVisibility(View.GONE);
        gap(content,12);download=button(content,"Download update",true,()->{if(ready)install();else download();});download.setVisibility(View.GONE);
        check=button(content,"Check for updates",false,()->check(true));
        gap(content,16);TextView footer=text(content,"Verified download • Original app signature",11);footer.setTextColor(MUTED);footer.setGravity(Gravity.CENTER);
        notes=text(content,"",13);notes.setVisibility(View.GONE);
        release=UpdateClient.cached(this);if(release!=null)showRelease();check(false);
    }
    void open(){
        if(panel!=null&&panel.isShowing())return;
        if(content.getParent()!=null)((ViewGroup)content.getParent()).removeView(content);
        panel=dialog(content);
    }
    void resume(){
        if(!busy&&!ready){release=UpdateClient.cached(this);showRelease();}
        changed.run();
        if(waitingPermission){waitingPermission=false;if(getPackageManager().canRequestPackageInstalls())install();}
    }
    void permissionGranted(){UpdateClient.Release r=UpdateClient.cached(this);if(r!=null)try{UpdateJobs.notifyUpdate(this,r);}catch(Exception ignored){}}
    private void setBusy(boolean value){busy=value;check.setEnabled(!value);download.setEnabled(!value);check.setAlpha(value?.45f:1f);download.setAlpha(value?.55f:1f);}
    private void showRelease(){
        try{
            changed.run();
            if(release==null)return;
            boolean newer=UpdatePolicy.newer(UpdateClient.installed(this),release.code);
            status.setText(newer?"A new version is available":"You’re up to date");
            status.setTextColor(newer?0xff6ee7a0:TEXT);
            String installed=getPackageManager().getPackageInfo(getPackageName(),0).versionName;
            versionInfo.setText(newer?"Installed "+installed+"  →  "+release.name+"  ·  "+String.format(java.util.Locale.US,"%.1f MB",release.size/1048576.0):"Installed "+installed+" · Latest release");
            notes.setText(release.notes);notes.setVisibility(View.GONE);download.setVisibility(newer?View.VISIBLE:View.GONE);
            download.setText(ready?"Install update":"Download update");
        }catch(Exception e){status.setText("Cannot read installed version");}
    }
    private void check(boolean manual){
        if(busy)return;setBusy(true);status.setTextColor(TEXT);status.setText("Checking GitHub…");
        task=worker.submit(()->{try{
            UpdateClient.Release r=UpdateClient.check(getApplicationContext());UpdateJobs.notifyUpdate(getApplicationContext(),r);
            boolean valid=false;if(UpdateClient.apk(this).isFile())try{UpdateClient.verify(this,UpdateClient.apk(this),r);valid=true;}catch(Exception ignored){}
            final boolean verified=valid;ui(()->{release=r;ready=verified;setBusy(false);showRelease();});
        }catch(Exception e){ui(()->{setBusy(false);status.setText("Couldn’t check for updates. "+e.getMessage());});}});
    }
    private void download(){
        if(busy||release==null)return;setBusy(true);progress.setProgress(0);progress.setVisibility(View.VISIBLE);status.setText("Downloading from GitHub…");
        final UpdateClient.Release selected=release;
        task=worker.submit(()->{try{
            UpdateClient.download(getApplicationContext(),selected,p->ui(()->{progress.setProgress(p);status.setText("Downloading… "+p+"%");}));
            ui(()->{ready=true;setBusy(false);progress.setVisibility(View.GONE);status.setText("Download verified. Ready to install.");download.setText("Install update");});
        }catch(Exception e){ui(()->{setBusy(false);progress.setVisibility(View.GONE);status.setText("Download failed. "+e.getMessage());});}});
    }
    private void install(){
        if(busy||!ready||release==null)return;
        if(!getPackageManager().canRequestPackageInstalls()){
            waitingPermission=true;
            confirm("Allow app updates","Allow HyperGesture to install its downloaded update, then return here.","Open settings",
                ()->startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+getPackageName()))),()->waitingPermission=false);return;
        }
        setBusy(true);task=worker.submit(()->{try{UpdateClient.verify(this,UpdateClient.apk(this),release);ui(()->{
            setBusy(false);
            confirm("Ready to install","Android will ask you to confirm installation. Restart your phone afterwards to load the updated module.","Install update",()->{
                try{Intent i=new Intent(Intent.ACTION_VIEW).setDataAndType(UpdateFileProvider.URI,"application/vnd.android.package-archive")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);i.setClipData(ClipData.newRawUri("HyperGesture update",UpdateFileProvider.URI));startActivity(i);}
                catch(Exception e){status.setText("Cannot open installer: "+e.getMessage());}
            },null);
        });}catch(Exception e){ui(()->{setBusy(false);ready=false;download.setText("Download update");status.setText("Verification failed. Please download again.");});}});
    }
    void destroy(){if(panel!=null)panel.dismiss();if(task!=null)task.cancel(true);worker.shutdownNow();main.removeCallbacksAndMessages(null);}
}
