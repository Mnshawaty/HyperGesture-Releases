package com.mn.hypergestures;

import android.app.Application;
import android.app.KeyguardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.provider.Settings;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import java.util.WeakHashMap;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/** Specific to the two supplied APK versions. Never impersonates the default HOME globally. */
public final class MainHook implements IXposedHookLoadPackage {
    private static final String HOME = "com.miui.home";
    private static final String SYS = "com.android.systemui";
    private static final String PREFIX = "[MN-HyperGestures] ";
    private boolean installed;

    private static void log(String s) { XposedBridge.log(PREFIX + s); }
    private static void error(String s, Throwable t) { log(s + ": " + t); }

    private static void pulse(Object nav,Context context,boolean recent){
        try{
            if(!(nav instanceof android.view.View))return;
            boolean delivered=((android.view.View)nav).performHapticFeedback(recent?android.view.HapticFeedbackConstants.CONFIRM:android.view.HapticFeedbackConstants.GESTURE_END);
            log("Haptic requested: "+(recent?"RECENTS":"NAVIGATION")+" accepted="+delivered);
        }catch(Throwable t){error("Haptic unavailable; navigation unaffected",t);}
    }

    @Override public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam p) {
        if ((!HOME.equals(p.packageName) && !SYS.equals(p.packageName))
                || !p.packageName.equals(p.processName)) return;
        if (Build.VERSION.SDK_INT != 35) { log("SKIP unsupported Android SDK " + Build.VERSION.SDK_INT); return; }
        XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                if (installed) return;
                installed = true;
                Context c = (Context) param.args[0];
                try {
                    long version = c.getPackageManager().getPackageInfo(p.packageName, 0).getLongVersionCode();
                    long expected = HOME.equals(p.packageName) ? 601052343L : 202408080L;
                    if (version != expected) { log("SKIP version mismatch " + p.packageName + " " + version); return; }
                    if (HOME.equals(p.packageName)) installHome(p.classLoader);
                    else installSystemUi(p.classLoader);
                } catch (Throwable t) { error("INSTALL FAILED " + p.packageName, t); }
            }
        });
    }

    private void installSystemUi(ClassLoader cl) throws Throwable {
        Class<?> owner = XposedHelpers.findClass("com.android.systemui.assist.PhoneStateMonitorController", cl);
        // Verified against the uploaded APK: this method only resets force_fsg_nav_bar for a non-stock HOME.
        XposedHelpers.findAndHookMethod(owner, "onDefaultHomeChanged", ComponentName.class, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                try {
                    ComponentName home = (ComponentName) p.args[0];
                    List<?> stock = (List<?>) XposedHelpers.getStaticObjectField(owner, "MIUI_HOME_PKG_NAME_LIST");
                    if (home != null && !stock.contains(home.getPackageName())
                            && XposedHelpers.getBooleanField(p.thisObject, "mFullscreenGesture")) {
                        p.setResult(null);
                        log("SystemUI: preserved gesture mode for " + home.getPackageName());
                    }
                } catch (Throwable t) { error("SystemUI guard; using original", t); }
            }
        });
        log("READY SystemUI onDefaultHomeChanged guard");
    }

    private void installHome(ClassLoader cl) throws Throwable {
        Class<?> recents = XposedHelpers.findClass("com.miui.home.recents.BaseRecentsImpl", cl);
        Class<?> nav = XposedHelpers.findClass("com.miui.home.recents.NavStubView", cl);
        Class<?> selector = XposedHelpers.findClass("com.miui.home.recents.settings.NavBarTypeContainerPreference", cl);
        Class<?> cts = XposedHelpers.findClass("com.miui.home.recents.cts.CircleToSearchHelper", cl);
        Class<?> observer = XposedHelpers.findClass("com.miui.home.recents.OverviewComponentObserver", cl);
        Class<?> command = XposedHelpers.findClass("com.miui.home.recents.OverviewCommandHelper", cl);
        // Preflight the entire structural contract before changing any behavior.
        require(recents, "setIsUseMiuiHomeAsDefaultHome", boolean.class);
        require(recents, "updateFsgWindowState");
        require(recents, "addFsgGestureWindow");
        require(recents, "updateUseLauncherRecentsAndFsGesture");
        recents.getDeclaredField("mIsUseMiuiHomeAsDefaultHome");
        require(selector, "showDialogToSetMiuiHomeAsDefault");
        require(selector, "setForceFSGNavBar", boolean.class);
        require(cts, "isThirdHome", Context.class);
        require(nav, "performAppToHome");
        require(nav, "performAppToRecents", boolean.class);
        require(nav, "finishDirectly", boolean.class);
        require(nav, "finishRecentsActivityDirectly");
        require(observer, "getInstance", Context.class);
        require(observer, "updateOverviewTargets");
        require(observer, "getHomeIntent");
        command.getDeclaredConstructor(Context.class, observer);
        require(command, "onOverviewToggle");
        require(nav, "onTouchEvent", MotionEvent.class);
        require(nav, "shouldBlockGesture", MotionEvent.class, int.class);
        require(nav, "isRecentsDisabled");
        nav.getDeclaredField("mMiuiGestureDetector");
        List<XC_MethodHook.Unhook> hooks = new ArrayList<>();
        try {
            hooks.add(XposedHelpers.findAndHookMethod(recents, "setIsUseMiuiHomeAsDefaultHome", boolean.class, new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam p) { p.args[0] = true; }
            }));
            hooks.add(XposedHelpers.findAndHookMethod(recents, "updateFsgWindowState", new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam p) {
                    XposedHelpers.setBooleanField(p.thisObject, "mIsUseMiuiHomeAsDefaultHome", true);
                }
            }));
            hooks.add(XposedHelpers.findAndHookMethod(recents, "addFsgGestureWindow", new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam p) {
                    if (p.hasThrowable()) return;
                    try {
                        XposedHelpers.setBooleanField(p.thisObject, "mIsUseMiuiHomeAsDefaultHome", true);
                        XposedHelpers.callMethod(p.thisObject, "updateUseLauncherRecentsAndFsGesture");
                        XposedHelpers.callMethod(p.thisObject, "updateFsgWindowState");
                        log("Launcher gesture eligibility refreshed");
                    } catch (Throwable t) { error("Window refresh", t); }
                }
            }));
            hooks.add(XposedHelpers.findAndHookMethod(selector, "showDialogToSetMiuiHomeAsDefault", new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam p) {
                    try {
                        // Runs only after the user explicitly selects Gestures in Xiaomi's settings UI.
                        XposedHelpers.callMethod(p.thisObject, "setForceFSGNavBar", true);
                        XposedHelpers.callMethod(p.thisObject, "setSelectItem", XposedHelpers.getObjectField(p.thisObject, "mFullScreenGestureView"));
                        p.setResult(null);
                        log("User selected Gestures without replacing default HOME");
                    } catch (Throwable t) { error("Gesture selection; using original", t); }
                }
            }));
            hooks.add(XposedHelpers.findAndHookMethod(cts, "isThirdHome", Context.class, new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam p) { p.setResult(false); }
            }));
            hooks.add(XposedHelpers.findAndHookMethod(nav, "performAppToHome", new RouteHook(observer, command, false)));
            hooks.add(XposedHelpers.findAndHookMethod(nav, "performAppToRecents", boolean.class, new RouteHook(observer, command, true)));
            hooks.add(XposedHelpers.findAndHookMethod(nav, "onTouchEvent", MotionEvent.class, new BottomTouchHook(observer, command)));
            log("READY 0.4.3 MiuiHome: eligibility, selection, HOME/Recents routing, CTS third-HOME gate");
        } catch (Throwable t) {
            for (XC_MethodHook.Unhook h : hooks) h.unhook();
            throw t;
        }
        // Diagnostic only: don't consume or replay touch input, so the native long-press timer remains intact.
        try {
            Class<?> events = XposedHelpers.findClass("com.miui.home.recents.gesture.NavStubGestureEventManager", cl);
            XposedHelpers.findAndHookMethod(events, "onLongPress", new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam p) { log("Native indicator long-press received"); }
            });
            XposedHelpers.findAndHookMethod(cts, "invokeCircleToSearch", Context.class, long.class, int.class, new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam p) { log("CTS invocation returned " + p.getResult()); }
            });
        } catch (Throwable t) { error("Optional diagnostics", t); }
    }

    private static Method require(Class<?> c, String name, Class<?>... args) throws Exception {
        Method m = c.getDeclaredMethod(name, args); m.setAccessible(true); return m;
    }

    private static final class TouchState {
        final SwipeDecision decision;
        final MotionEvent down;
        final float density;
        boolean cancelledDetector, aborted, dispatched;
        float lastX,lastY;
        Runnable hold;
        TouchState(MotionEvent e,float d) { density=d; down=MotionEvent.obtain(e); decision=new SwipeDecision(e.getRawX()/d,e.getRawY()/d); }
        void recycle(){down.recycle();}
    }

    /** Third-party HOME only: don't depend on Xiaomi's launcher animation state reaching its terminal actions. */
    private static final class BottomTouchHook extends XC_MethodHook {
        final Class<?> observer,command;
        final WeakHashMap<Object,TouchState> states=new WeakHashMap<>();
        BottomTouchHook(Class<?> o,Class<?> c){observer=o;command=c;}
        private void detector(Object nav, MotionEvent e) {
            XposedHelpers.callMethod(XposedHelpers.getObjectField(nav,"mMiuiGestureDetector"),"onTouchEvent",e);
        }
        private void cancelDetector(Object nav,TouchState s,MotionEvent e) {
            if(s.cancelledDetector)return;
            MotionEvent cancel=MotionEvent.obtain(e);cancel.setAction(MotionEvent.ACTION_CANCEL);
            try{detector(nav,cancel);}finally{cancel.recycle();s.cancelledDetector=true;}
        }
        private final Handler handler=new Handler(Looper.getMainLooper());
        private void remove(Object nav) {TouchState old=states.remove(nav);if(old!=null){if(old.hold!=null)handler.removeCallbacks(old.hold);old.recycle();}}
        private void dispatch(Object nav,Context ctx,TouchState state,int decision) {
            if(state.dispatched)return;
            state.dispatched=true;
            try {
                KeyguardManager k=(KeyguardManager)ctx.getSystemService(Context.KEYGUARD_SERVICE);
                if(k==null||k.isKeyguardLocked()||Settings.Global.getInt(ctx.getContentResolver(),"force_fsg_nav_bar",0)!=1
                    ||XposedHelpers.getBooleanField(nav,"mDisableHomeRecents")||XposedHelpers.getBooleanField(nav,"mDisableTouch"))return;
                ResolveInfo resolved=ctx.getPackageManager().resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),65536);
                if(resolved==null||resolved.activityInfo==null||HOME.equals(resolved.activityInfo.packageName)||"android".equals(resolved.activityInfo.packageName))return;
                if(decision==SwipeDecision.HOME){
                    Intent home=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                        .setComponent(new ComponentName(resolved.activityInfo.packageName,resolved.activityInfo.name))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                    ctx.startActivity(home);
                    pulse(nav,ctx,false);
                    log("Direct swipe dispatch: HOME");
                }else if(decision==SwipeDecision.RECENTS){
                    Object obs=XposedHelpers.callStaticMethod(observer,"getInstance",ctx);
                    XposedHelpers.callMethod(obs,"updateOverviewTargets");
                    XposedHelpers.callMethod(XposedHelpers.newInstance(command,ctx,obs),"onOverviewToggle");
                    pulse(nav,ctx,true);
                    log("Direct swipe dispatch: RECENTS");
                }else if(decision==SwipeDecision.HORIZONTAL){
                    android.app.ActivityManager am=(android.app.ActivityManager)ctx.getSystemService(Context.ACTIVITY_SERVICE);
                    // Host is MiuiHome, whose manifest requests REAL_GET_TASKS and REORDER_TASKS.
                    java.util.List<android.app.ActivityManager.RunningTaskInfo> tasks=am.getRunningTasks(24);
                    if(tasks==null||tasks.isEmpty()){log("SWITCH: no tasks available");return;}
                    int current=tasks.get(0).taskId;
                    for(android.app.ActivityManager.RunningTaskInfo task:tasks){
                        if(task.taskId==current||task.topActivity==null||task.numActivities<=0)continue;
                        String pkg=task.topActivity.getPackageName();
                        if(pkg.equals(HOME)||pkg.equals(SYS)||pkg.equals("android")||pkg.equals(resolved.activityInfo.packageName))continue;
                        am.moveTaskToFront(task.taskId,android.app.ActivityManager.MOVE_TASK_WITH_HOME);
                        pulse(nav,ctx,false);
                        log("Direct swipe dispatch: SWITCH task="+task.taskId);return;
                    }
                    log("SWITCH: no previous app task");
                }
            }catch(Throwable t){error("Direct swipe dispatch decision="+decision,t);}
        }
        @Override protected void beforeHookedMethod(MethodHookParam p) {
            Object nav=p.thisObject;MotionEvent e=(MotionEvent)p.args[0];int action=e.getActionMasked();
            TouchState s=states.get(nav);
            try {
                if(action!=MotionEvent.ACTION_DOWN && s!=null && s.aborted){if(action==1||action==3)remove(nav);p.setResult(true);return;}
                if(s!=null && s.dispatched){if(action==1||action==3)remove(nav);p.setResult(true);return;}
                Context c=(Context)XposedHelpers.getObjectField(nav,"mContext");
                if(action==MotionEvent.ACTION_DOWN){
                    remove(nav);
                    if(Settings.Global.getInt(c.getContentResolver(),"force_fsg_nav_bar",0)!=1)return;
                    ResolveInfo home=c.getPackageManager().resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),65536);
                    if(home==null||home.activityInfo==null||HOME.equals(home.activityInfo.packageName)||"android".equals(home.activityInfo.packageName))return;
                    KeyguardManager kg=(KeyguardManager)c.getSystemService(Context.KEYGUARD_SERVICE);
                    if(kg==null||kg.isKeyguardLocked()||XposedHelpers.getBooleanField(nav,"mDisableHomeRecents")||XposedHelpers.getBooleanField(nav,"mDisableTouch")
                       || Boolean.TRUE.equals(XposedHelpers.callMethod(nav,"isRecentsDisabled"))
                       || Boolean.TRUE.equals(XposedHelpers.callMethod(nav,"shouldBlockGesture",e,1)))return;
                    s=new TouchState(e,c.getResources().getDisplayMetrics().density);states.put(nav,s);
                    detector(nav,e);p.setResult(true);log("External bottom gesture DOWN");return;
                }
                if(s==null)return;
                KeyguardManager kg=(KeyguardManager)c.getSystemService(Context.KEYGUARD_SERVICE);
                if(action==MotionEvent.ACTION_CANCEL||e.getPointerCount()!=1||kg==null||kg.isKeyguardLocked()
                   ||XposedHelpers.getBooleanField(nav,"mDisableHomeRecents")||XposedHelpers.getBooleanField(nav,"mDisableTouch")){
                    cancelDetector(nav,s,e);s.aborted=true;if(action==1||action==3)remove(nav);p.setResult(true);return;
                }
                float x=e.getRawX()/s.density,y=e.getRawY()/s.density;
                s.decision.move(x,y,e.getEventTime());
                s.lastX=x;s.lastY=y;
                if(s.hold!=null)handler.removeCallbacks(s.hold);
                if(s.decision.isVertical()||s.decision.isHorizontal())cancelDetector(nav,s,e);
                else detector(nav,e);
                p.setResult(true);
                if(action==MotionEvent.ACTION_MOVE && s.decision.isVertical()){
                    long delay=s.decision.recentsDelay(e.getEventTime());
                    if(delay>=0){
                        final TouchState active=s;final Context ctx=c;
                        s.hold=()->{
                            if(states.get(nav)!=active||active.aborted||active.dispatched)return;
                            if(active.decision.finish(active.lastX,active.lastY,android.os.SystemClock.uptimeMillis())==SwipeDecision.RECENTS)
                                dispatch(nav,ctx,active,SwipeDecision.RECENTS);
                        };
                        handler.postDelayed(s.hold,delay);
                    }
                }
                if(action==MotionEvent.ACTION_UP){
                    int decision=s.decision.finish(x,y,e.getEventTime());
                    if(decision!=SwipeDecision.NONE)dispatch(nav,c,s,decision);
                    remove(nav);
                }
            }catch(Throwable t){
                error("Bottom touch",t);
                // Never pass an orphan MOVE/UP to the native state machine after consuming DOWN.
                if(s!=null){try{cancelDetector(nav,s,e);}catch(Throwable ignored){}s.aborted=true;if(action==1||action==3)remove(nav);p.setResult(true);}
            }
        }
    }

    private static final class RouteHook extends XC_MethodHook {
        final Class<?> observer, command;
        final boolean overview;
        RouteHook(Class<?> observer, Class<?> command, boolean overview) {
            this.observer = observer; this.command = command; this.overview = overview;
        }
        @Override protected void beforeHookedMethod(MethodHookParam p) {
            try {
                Context c = (Context) XposedHelpers.getObjectField(p.thisObject, "mContext");
                if (Settings.Global.getInt(c.getContentResolver(), "force_fsg_nav_bar", 0) != 1) return;
                ResolveInfo resolved = c.getPackageManager().resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 65536);
                if (resolved == null || resolved.activityInfo == null || HOME.equals(resolved.activityInfo.packageName)
                        || "android".equals(resolved.activityInfo.packageName)) return;
                Object obs = XposedHelpers.callStaticMethod(observer, "getInstance", c);
                XposedHelpers.callMethod(obs, "updateOverviewTargets");
                Intent observed = (Intent) XposedHelpers.callMethod(obs, "getHomeIntent");
                if (observed == null || observed.getComponent() == null
                        || !resolved.activityInfo.packageName.equals(observed.getComponent().getPackageName())) return;
                Object cmd = overview ? XposedHelpers.newInstance(command, c, obs) : null;
                XposedHelpers.callMethod(p.thisObject, "finishDirectly", false);
                XposedHelpers.callMethod(p.thisObject, "finishRecentsActivityDirectly");
                if (overview) XposedHelpers.callMethod(cmd, "onOverviewToggle");
                else c.startActivity(new Intent(observed).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED));
                p.setResult(null);
                pulse(p.thisObject,c,overview);
                log(overview ? "Routed to Xiaomi fallback Recents" : "Routed to selected external HOME");
            } catch (Throwable t) { error("Routing failed; using original", t); }
        }
    }
}

