package com.mn.hypergestures;

import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.ResolveInfo;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.Uri;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.io.*;

/** Native dashboard. Device checks are not presented as hook activation. */
public final class MainActivity extends Activity {
    private static final int BG=0xff0c1019, CARD=0xff171e2c, LINE=0xff2a3447,
        TEXT=0xfff3f5fb, MUTED=0xffa4aec2, ACCENT=0xffb0a0ff, CYAN=0xff67dce9;
    private LinearLayout shell, page, tabs;
    private ScrollView scroll;
    private int selected=0;
    private Bitmap scopeImage;
    private UpdateCard updates;
    private View updateBadge;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if(state!=null) selected=state.getInt("tab",0);
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        shell=column();shell.setBackgroundColor(BG);shell.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        setContentView(shell);
        shell.setOnApplyWindowInsetsListener((v,insets)->{
            Insets i=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
            v.setPadding(i.left,i.top,i.right,i.bottom);return insets;
        });shell.requestApplyInsets();
        if(getWindow().getInsetsController()!=null)getWindow().getInsetsController().setSystemBarsAppearance(0,WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
        LinearLayout header=row();header.setPadding(dp(16),dp(16),dp(16),dp(16));
        ImageView logo=new ImageView(this);logo.setImageDrawable(getApplicationInfo().loadIcon(getPackageManager()));
        header.addView(logo,new LinearLayout.LayoutParams(dp(32),dp(32)));
        TextView brand=label("HyperGesture",21,TEXT,true);brand.setSingleLine(true);brand.setAutoSizeTextTypeUniformWithConfiguration(12,21,1,android.util.TypedValue.COMPLEX_UNIT_SP);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,-2,1);bp.leftMargin=dp(8);header.addView(brand,bp);
        TextView version=label("0.4.9",12,ACCENT,true);version.setPadding(dp(12),dp(7),dp(12),dp(7));version.setBackground(shape(0xff28233e,14));header.addView(version);
        LinearLayout coffee=column();coffee.setGravity(Gravity.CENTER);
        coffee.addView(new Glyph("coffee",ACCENT),new LinearLayout.LayoutParams(dp(24),dp(24)));
        clickable(coffee,()->link("https://paypal.me/mhdnashawati/5"));
        coffee.setContentDescription("Buy me a coffee. Donate with PayPal");
        LinearLayout.LayoutParams coffeeParams=new LinearLayout.LayoutParams(dp(44),dp(44));coffeeParams.leftMargin=dp(6);
        header.addView(coffee,coffeeParams);
        LinearLayout badge=column();badge.setGravity(Gravity.CENTER);badge.setBackground(shape(0xff16382b,14));
        badge.addView(new Glyph("download",0xff6ee7a0),new LinearLayout.LayoutParams(dp(23),dp(23)));
        LinearLayout.LayoutParams badgeParams=new LinearLayout.LayoutParams(dp(44),dp(44));badgeParams.leftMargin=dp(8);
        header.addView(badge,badgeParams);updateBadge=badge;badge.setVisibility(View.GONE);
        badge.setFocusable(true);badge.setContentDescription("New update available. Download update");
        badge.setOnClickListener(v->{if(updates!=null)updates.open();});
        version.setMinHeight(dp(44));version.setGravity(Gravity.CENTER);version.setFocusable(true);
        version.setContentDescription("Installed version 0.4.9. Check for updates");
        version.setOnClickListener(v->{if(updates!=null)updates.open();});
        shell.addView(header);
        scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);
        shell.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        View line=new View(this);line.setBackgroundColor(LINE);shell.addView(line,new LinearLayout.LayoutParams(-1,dp(1)));
        tabs=row();tabs.setPadding(dp(12),dp(8),dp(12),dp(8));shell.addView(tabs);
        updates=new UpdateCard(this,this::refreshUpdateBadge);
        show(selected);
        UpdateJobs.schedule(this);
        if(!UpdateClient.prefs(this).getBoolean("notification_permission_requested",false)){
            UpdateClient.prefs(this).edit().putBoolean("notification_permission_requested",true).apply();
            if(checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=android.content.pm.PackageManager.PERMISSION_GRANTED)
                requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},14);
        }
    }
    @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] results){
        super.onRequestPermissionsResult(code,permissions,results);
        if(code==14&&updates!=null)updates.permissionGranted();
    }
    @Override protected void onDestroy(){if(updates!=null)updates.destroy();super.onDestroy();}
    @Override protected void onSaveInstanceState(Bundle out){out.putInt("tab",selected);super.onSaveInstanceState(out);}
    @Override protected void onResume(){super.onResume();if(updates!=null)updates.resume();}
    private void refreshUpdateBadge(){
        if(updateBadge==null)return;
        UpdateClient.Release r=UpdateClient.cached(this);
        boolean available=false;
        try{available=r!=null&&UpdatePolicy.newer(UpdateClient.installed(this),r.code);}catch(Exception ignored){}
        updateBadge.setVisibility(available?View.VISIBLE:View.GONE);
        if(available)updateBadge.setContentDescription("Update "+r.name+" available. Download update");
    }
    private int dp(float x){return Math.round(x*getResources().getDisplayMetrics().density);}
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private GradientDrawable shape(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
    private void clickable(View v,Runnable action){v.setBackground(new RippleDrawable(android.content.res.ColorStateList.valueOf(0x33b0a0ff),shape(CARD,20),null));v.setOnClickListener(w->action.run());v.setFocusable(true);}
    private TextView label(String text,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(text);t.setTextSize(size);t.setTextColor(color);t.setFontFeatureSettings("kern");t.setTypeface(Typeface.create(bold?"sans-serif-medium":"sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));t.setLineSpacing(dp(3),1);return t;}
    private void gap(LinearLayout parent,int size){Space v=new Space(this);parent.addView(v,new LinearLayout.LayoutParams(1,dp(size)));}
    private LinearLayout card(){LinearLayout c=column();c.setPadding(dp(20),dp(20),dp(20),dp(20));c.setBackground(shape(CARD,22));page.addView(c,new LinearLayout.LayoutParams(-1,-2));gap(page,14);return c;}
    private void title(String eyebrow,String title,String sub){page.addView(label(eyebrow,11,CYAN,true));gap(page,10);page.addView(label(title,32,TEXT,true));gap(page,10);page.addView(label(sub,15,MUTED,false));gap(page,26);}
    private void section(String text){gap(page,8);page.addView(label(text,18,TEXT,true));gap(page,14);}
    private void show(int tab){selected=tab;scroll.removeAllViews();page=column();page.setPadding(dp(24),dp(16),dp(24),dp(28));scroll.addView(page,new ScrollView.LayoutParams(-1,-2));
        if(tab==0)home();else if(tab==1)guide();else about();
        tabs.removeAllViews();String[] names={"Home","Guide","About"};String[] icons={"home","book","info"};
        for(int i=0;i<3;i++){final int n=i;LinearLayout b=column();b.setGravity(Gravity.CENTER);b.setPadding(dp(8),dp(8),dp(8),dp(8));
            b.setBackground(shape(i==tab?0xff29243f:BG,18));b.addView(new Glyph(icons[i],i==tab?ACCENT:MUTED),new LinearLayout.LayoutParams(dp(24),dp(24)));gap(b,4);TextView tabLabel=label(names[i],12,i==tab?TEXT:MUTED,i==tab);tabLabel.setGravity(Gravity.CENTER);tabLabel.setIncludeFontPadding(false);b.addView(tabLabel,new LinearLayout.LayoutParams(-1,-2));
            b.setContentDescription(names[i]+(i==tab?", selected":""));b.setSelected(i==tab);b.setFocusable(true);b.setOnClickListener(v->show(n));tabs.addView(b,new LinearLayout.LayoutParams(0,-2,1));}
        scroll.scrollTo(0,0);
    }
    private void home(){
        title("HYPEROS 3  /  ANDROID 15","Your launcher.\nYour gestures.","A familiar way to move between your apps.");
        LinearLayout device=card();device.addView(label("DEVICE COMPATIBILITY",11,ACCENT,true));gap(device,10);
        boolean match=Build.VERSION.SDK_INT==35 && version("com.miui.home")==601052343L && version("com.android.systemui")==202408080L;
        device.addView(label(match?"Supported versions detected":"Check your system versions",19,TEXT,true));gap(device,8);
        device.addView(label("Default launcher · "+homeName(),14,MUTED,false));gap(device,10);
        TextView details=label("View setup guide  →",14,CYAN,true);details.setMinHeight(dp(48));details.setGravity(Gravity.CENTER_VERTICAL);details.setOnClickListener(v->show(1));details.setFocusable(true);device.addView(details);
        section("Make yourself at home");
        action("settings","Navigation settings","Choose full-screen gestures",()->open(new Intent("com.miui.home.action.navigation_bar_type_settings").setPackage("com.miui.home")));
        action("home","Default launcher","Choose your preferred third-party launcher",()->open(new Intent(Settings.ACTION_HOME_SETTINGS)));
        section("Five simple movements");
        LinearLayout gestures=card();gesture(gestures,"up","Go home","Swipe up and release.");divider(gestures);gesture(gestures,"recent","Open Recents","Swipe up, then pause.");divider(gestures);gesture(gestures,"back","Go back","Swipe inward from the left or right edge of the screen.");divider(gestures);gesture(gestures,"switch","Switch apps","Swipe along the bottom edge to return to the previous app.");divider(gestures);gesture(gestures,"press","Circle to Search","Press and hold the full-screen indicator to search on supported systems.");
        LinearLayout indicator=card();indicator.addView(label("Full-screen indicator support",18,TEXT,true));gap(indicator,8);indicator.addView(label("Keep your bottom indicator and its press-and-hold shortcut while using Smart Launcher or another default launcher.",14,MUTED,false));
    }
    private void guide(){
        title("GET STARTED","A little setup.\nA smoother flow.","Set up HyperGesture in three steps.");
        step("01","Enable HyperGesture","Open LSPosed or Vector, find HyperGesture, and enable the module.");
        step("02","Select the two system apps","Select System launcher and System UI, as shown below.");
        LinearLayout imageCard=card();imageCard.addView(label("Your scope checklist",18,TEXT,true));gap(imageCard,8);imageCard.addView(label("System launcher + System UI",14,CYAN,false));gap(imageCard,16);
        ImageView image=new ImageView(this);image.setAdjustViewBounds(true);image.setScaleType(ImageView.ScaleType.FIT_CENTER);image.setContentDescription("LSPosed setup reference: System launcher and System UI are checked.");
        try{if(scopeImage==null){try(InputStream in=getAssets().open("setup_scope.jpg")){scopeImage=BitmapFactory.decodeStream(in);}}image.setImageBitmap(scopeImage);}catch(IOException e){imageCard.addView(label("Setup image unavailable",14,MUTED,false));}
        imageCard.addView(image,new LinearLayout.LayoutParams(-1,-2));image.setOnClickListener(v->showScope());gap(imageCard,12);imageCard.addView(label("Tap the image to enlarge",12,MUTED,false));
        step("03","Restart & choose your launcher","Restart your phone. Set your preferred launcher as your default, then select gestures in Navigation settings.");
        action("settings","Open navigation settings","Finish your setup",()->open(new Intent("com.miui.home.action.navigation_bar_type_settings").setPackage("com.miui.home")));
        section("Good to know");LinearLayout tips=card();tips.addView(label("Keep System launcher installed",16,TEXT,true));gap(tips,7);tips.addView(label("It provides the gesture and Recents engine, even when you use another launcher.",14,MUTED,false));gap(tips,18);tips.addView(label("Use one gesture module",16,TEXT,true));gap(tips,7);tips.addView(label("Disable other gesture modules, then restart after each update. To turn HyperGesture off, switch to button navigation, disable the module, and restart.",14,MUTED,false));gap(tips,18);tips.addView(label("Full-screen indicator & Circle to Search",16,TEXT,true));gap(tips,7);tips.addView(label("Show the full-screen indicator in Navigation settings. Set Press and hold full screen indicator to Circle to Search, then hold the indicator to search. Your system must already provide Circle to Search.",14,MUTED,false));
    }
    private void about(){
        action("coffee","Buy me a coffee","Support HyperGesture via PayPal",()->link("https://paypal.me/mhdnashawati/5"));
        title("MADE FOR YOUR EVERYDAY","Small details.\nBetter navigation.","HyperGesture by Mohammad Alnashawati.");
        LinearLayout identity=card();ImageView logo=new ImageView(this);logo.setImageDrawable(getApplicationInfo().loadIcon(getPackageManager()));identity.addView(logo,new LinearLayout.LayoutParams(dp(64),dp(64)));gap(identity,16);identity.addView(label("HyperGesture",25,TEXT,true));gap(identity,6);identity.addView(label("Version 0.4.9",14,ACCENT,false));gap(identity,12);identity.addView(label("© 2026 Mohammad Alnashawati\nOpen source · Apache License 2.0.",13,MUTED,false));
        section("Stay connected");
        action("reddit","Reddit","u/mnshawaty",()->link("https://www.reddit.com/user/mnshawaty/"));
        action("telegram","Message me","t.me/mnshawaty",()->link("https://t.me/mnshawaty"));
        action("telegram","Let's APK","t.me/letsapk",()->link("https://t.me/letsapk"));
        action("book","Source code","HyperGesture on GitHub",()->link("https://github.com/Mnshawaty/HyperGesture-Releases"));
        section("Details");
        action("info","Compatibility & diagnostics","System versions and module information",()->info());
        action("book","Legal & acknowledgements","App ownership and third-party notices",()->licenses());
        gap(page,12);page.addView(label("Designed for HyperOS 3 • Android 15",12,MUTED,false));
    }
    private void step(String number,String name,String description){LinearLayout c=card();c.addView(label(number,12,ACCENT,true));gap(c,8);c.addView(label(name,19,TEXT,true));gap(c,8);c.addView(label(description,15,MUTED,false));}
    private void action(String icon,String name,String sub,Runnable run){LinearLayout r=row();r.setPadding(dp(16),dp(16),dp(16),dp(16));r.setMinimumHeight(dp(80));r.addView(new Glyph(icon,ACCENT),new LinearLayout.LayoutParams(dp(26),dp(26)));LinearLayout words=column();LinearLayout.LayoutParams wp=new LinearLayout.LayoutParams(0,-2,1);wp.setMargins(dp(16),0,dp(8),0);r.addView(words,wp);words.addView(label(name,16,TEXT,true));gap(words,4);words.addView(label(sub,13,MUTED,false));r.addView(new Glyph("arrow",MUTED),new LinearLayout.LayoutParams(dp(18),dp(18)));clickable(r,run);r.setContentDescription(name+". "+sub);page.addView(r,new LinearLayout.LayoutParams(-1,-2));gap(page,10);}
    private void gesture(LinearLayout parent,String icon,String name,String sub){LinearLayout r=row();r.addView(new Glyph(icon,CYAN),new LinearLayout.LayoutParams(dp(28),dp(28)));LinearLayout t=column();LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.leftMargin=dp(18);r.addView(t,p);t.addView(label(name,16,TEXT,true));gap(t,5);t.addView(label(sub,14,MUTED,false));parent.addView(r);}
    private void divider(LinearLayout parent){gap(parent,18);View v=new View(this);v.setBackgroundColor(LINE);parent.addView(v,new LinearLayout.LayoutParams(-1,dp(1)));gap(parent,18);}
    private void open(Intent intent){try{startActivity(intent);}catch(Exception e){new AlertDialog.Builder(this).setTitle("Open settings manually").setMessage("This shortcut is unavailable. Open your phone's Settings and search for System navigation or Default launcher.").setPositiveButton("OK",null).show();}}
    private void link(String url){try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}catch(Exception e){new AlertDialog.Builder(this).setTitle("Link").setMessage(url).setPositiveButton("Copy",(d,w)->{android.content.ClipboardManager c=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);c.setPrimaryClip(ClipData.newPlainText("HyperGesture",url));Toast.makeText(this,"Link copied",Toast.LENGTH_SHORT).show();}).setNegativeButton("Close",null).show();}}
    private long version(String name){try{return getPackageManager().getPackageInfo(name,0).getLongVersionCode();}catch(Exception e){return -1;}}
    private String homeName(){try{ResolveInfo r=getPackageManager().resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),65536);if(r!=null&&r.activityInfo!=null&&!"android".equals(r.activityInfo.packageName))return r.loadLabel(getPackageManager()).toString();}catch(Exception ignored){}return "Not selected";}
    private void info(){new AlertDialog.Builder(this).setTitle("Compatibility & diagnostics").setMessage("Android API: "+Build.VERSION.SDK_INT+" (requires 35)\nSystem launcher: "+version("com.miui.home")+" (requires 601052343)\nSystem UI: "+version("com.android.systemui")+" (requires 202408080)\n\nVersion detection does not confirm module activation. In LSPosed logs, look for READY for both MiuiHome and SystemUI under MN-HyperGestures.\n\nDirect gestures with built-in haptic feedback. Haptic requests are logged under MN-HyperGestures. No custom visual effects or native animation experiment.").setPositiveButton("Done",null).show();}
    private void licenses(){String text="HyperGesture\n© 2026 Mohammad Alnashawati\n\n";for(String file:new String[]{"NOTICE.txt","LICENSE.txt"}){try(InputStream in=getAssets().open(file);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)out.write(b,0,n);text+=out.toString("UTF-8")+"\n\n";}catch(IOException ignored){}}TextView t=label(text,13,TEXT,false);t.setTextIsSelectable(true);t.setPadding(dp(24),dp(16),dp(24),dp(16));ScrollView s=new ScrollView(this);s.addView(t);new AlertDialog.Builder(this).setTitle("Legal & acknowledgements").setView(s).setPositiveButton("Done",null).show();}
    private void showScope(){if(scopeImage==null)return;Dialog d=new Dialog(this,android.R.style.Theme_Material_NoActionBar);LinearLayout body=column();body.setBackgroundColor(BG);body.setPadding(dp(16),dp(32),dp(16),dp(24));Button close=new Button(this);close.setText("Close guide image");close.setOnClickListener(v->d.dismiss());body.addView(close);ScrollView sv=new ScrollView(this);ImageView image=new ImageView(this);image.setImageBitmap(scopeImage);image.setAdjustViewBounds(true);image.setContentDescription("Module scope setup reference");sv.addView(image,new ScrollView.LayoutParams(-1,-2));body.addView(sv,new LinearLayout.LayoutParams(-1,0,1));d.setContentView(body);body.setOnApplyWindowInsetsListener((v,i)->{Insets b=i.getInsets(WindowInsets.Type.systemBars());v.setPadding(dp(16)+b.left,b.top,dp(16)+b.right,b.bottom);return i;});d.show();body.requestApplyInsets();}
    private final class Glyph extends View {
        final String kind;final Paint p=new Paint(3);final int color;
        Glyph(String kind,int color){super(MainActivity.this);this.kind=kind;this.color=color;setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);}
        private void path(Canvas c,float... points){Path q=new Path();q.moveTo(points[0],points[1]);for(int i=2;i<points.length;i+=2)q.lineTo(points[i],points[i+1]);c.drawPath(q,p);}
        @Override protected void onDraw(Canvas c){super.onDraw(c);c.save();c.scale(getWidth()/24f,getHeight()/24f);p.setColor(color);p.setStrokeWidth(1.8f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);p.setStyle(Paint.Style.STROKE);
            switch(kind){
                case "home":path(c,3,10,12,3,21,10);path(c,5,9,5,21,10,21,10,14,14,14,14,21,19,21,19,9);break;
                case "book":c.drawRoundRect(4,3,20,21,2,2,p);path(c,8,3,8,21);path(c,12,8,16,8);path(c,12,12,16,12);break;
                case "info":c.drawCircle(12,12,9,p);path(c,12,11,12,17);c.drawPoint(12,7,p);break;
                case "arrow":path(c,8,5,15,12,8,19);break;
                case "back":path(c,20,12,4,12);path(c,10,6,4,12,10,18);break;
                case "coffee":path(c,4,8,17,8,17,15);c.drawArc(4,9,17,21,0,180,false,p);path(c,4,15,4,8);c.drawArc(14,8,22,15,270,180,false,p);path(c,3,22,19,22);path(c,7,2,7,5);path(c,12,2,12,5);break;
                case "download":path(c,12,3,12,16);path(c,6,10,12,16,18,10);path(c,4,18,4,21,20,21,20,18);break;
                case "up":path(c,12,20,12,4);path(c,6,10,12,4,18,10);break;
                case "switch":path(c,3,8,21,8,17,4);path(c,21,16,3,16,7,20);break;
                case "recent":c.drawRoundRect(7,3,17,21,2,2,p);path(c,3,6,3,18);path(c,21,6,21,18);break;
                case "press":c.drawRoundRect(5,19,19,21,1,1,p);c.drawCircle(12,10,4,p);c.drawCircle(12,10,8,p);break;
                case "settings":c.drawCircle(12,12,7,p);c.drawCircle(12,12,2.5f,p);for(int i=0;i<8;i++){c.save();c.rotate(i*45,12,12);path(c,12,2,12,5);c.restore();}break;
                case "telegram":path(c,2,10,22,3,18,21,11,15,7,19,7,12,22,3);path(c,7,12,17,7,11,15);break;
                case "reddit":c.drawOval(3,9,21,21,p);c.drawCircle(2.5f,10,2,p);c.drawCircle(21.5f,10,2,p);path(c,12,9,14,3,19,4);c.drawCircle(20,4,2,p);p.setStyle(Paint.Style.FILL);c.drawCircle(8,14,1.4f,p);c.drawCircle(16,14,1.4f,p);p.setStyle(Paint.Style.STROKE);c.drawArc(8,14,16,18,15,150,false,p);break;
            }c.restore();
        }
    }
}

