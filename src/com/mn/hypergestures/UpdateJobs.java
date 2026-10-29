package com.mn.hypergestures;

import android.app.*;
import android.app.job.*;
import android.content.*;
import android.content.pm.PackageManager;
import java.util.concurrent.*;

public final class UpdateJobs extends JobService {
    static final int JOB=43012, NOTICE=43013;
    private Future<?> work;
    private final ExecutorService executor=Executors.newSingleThreadExecutor();
    static void schedule(Context c) {
        JobScheduler scheduler=c.getSystemService(JobScheduler.class);
        if(scheduler.getPendingJob(JOB)==null) scheduler.schedule(new JobInfo.Builder(JOB,new ComponentName(c,UpdateJobs.class))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPersisted(true)
            .setPeriodic(12*60*60*1000L,60*60*1000L).build());
    }
    static void notifyUpdate(Context c,UpdateClient.Release r) throws Exception {
        if(!UpdatePolicy.newer(UpdateClient.installed(c),r.code))return;
        if(c.checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=PackageManager.PERMISSION_GRANTED)return;
        NotificationManager nm=c.getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel("updates","App updates",NotificationManager.IMPORTANCE_DEFAULT));
        if(!nm.areNotificationsEnabled()||nm.getNotificationChannel("updates").getImportance()==NotificationManager.IMPORTANCE_NONE)return;
        if(UpdateClient.prefs(c).getLong("notified",0)>=r.code)return;
        PendingIntent open=PendingIntent.getActivity(c,0,new Intent(c,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        nm.notify(NOTICE,new Notification.Builder(c,"updates").setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("HyperGesture update available").setContentText("Version "+r.name+" is ready to download.")
            .setContentIntent(open).setAutoCancel(true).build());
        UpdateClient.prefs(c).edit().putLong("notified",r.code).apply();
    }
    @Override public boolean onStartJob(JobParameters p) {
        work=executor.submit(()->{try{UpdateClient.Release r=UpdateClient.check(this);if(!Thread.currentThread().isInterrupted())notifyUpdate(this,r);}catch(Exception ignored){}
            if(!Thread.currentThread().isInterrupted())new android.os.Handler(getMainLooper()).post(()->jobFinished(p,false));});return true;
    }
    @Override public boolean onStopJob(JobParameters p){if(work!=null)work.cancel(true);return true;}
    @Override public void onDestroy(){executor.shutdownNow();super.onDestroy();}
}

