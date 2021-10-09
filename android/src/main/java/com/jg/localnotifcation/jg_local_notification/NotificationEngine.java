package com.jg.localnotifcation.jg_local_notification;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;

import androidx.core.app.NotificationCompat;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static com.jg.localnotifcation.jg_local_notification.Constants.SCHEDULE_NOTIFICATION_PAYLOAD;

public class NotificationEngine {

    private static final String INVALID_ICON_ERROR_CODE = "INVALID_ICON";
    private static final String DRAWABLE = "drawable";

    /*Notification Create*/
    NotificationManager myNotificationManager;
    NotificationCompat.Builder myNotificationBuilder;
    Intent myNotificationIntent;

    public HashMap<String,Object> show(NotificationDataModel myNotificationDataModel, Context context){
        HashMap<String,Object> status= new  HashMap<String,Object>();
        if(myNotificationDataModel!=null && context!=null){
            myNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            myNotificationIntent = getLaunchIntent(context);
            /*Set Intent Data*/
            myNotificationIntent.putExtra("notificationId", myNotificationDataModel.getNotificationId());
            myNotificationIntent.putExtra("source", "localNotif");
            myNotificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            myNotificationIntent.putExtra("LocalNotifClick", true);
            if(myNotificationDataModel.getPayload()!=null){
                myNotificationIntent.putExtra("payload", myNotificationDataModel.getPayload());
            }
            myNotificationIntent.setAction(Long.toString(System.currentTimeMillis()));

            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0 /* Request code */, myNotificationIntent,
                    PendingIntent.FLAG_ONE_SHOT);
            /*Set Notification Channel*/
            String channelId = "notifications";
            String channelName = "Push Notification Service";
            final int importance = NotificationManager.IMPORTANCE_HIGH;
            boolean enableVibration=true;

            Uri soundURI = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            if (myNotificationDataModel.getSoundFileName()!=null&&!myNotificationDataModel.getSoundFileName().isEmpty()) {
                String soundfile = "";
                if (myNotificationDataModel.getSoundFileName().contains(".mp3") || myNotificationDataModel.getSoundFileName().contains(".ogg") || myNotificationDataModel.getSoundFileName().contains(".wav")) {
                    soundfile = myNotificationDataModel.getSoundFileName().substring(0, (myNotificationDataModel.getSoundFileName().length() - 4));
                }
                if (!soundfile.isEmpty()) {
                    soundURI = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context
                            .getPackageName() + "/raw/" + soundfile);
                }
            }

            if(myNotificationDataModel.getChannelId()!=null && myNotificationDataModel.getChannelName()!=null){
                channelId=myNotificationDataModel.getChannelId();
                channelName=myNotificationDataModel.getChannelName();
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                NotificationChannel mChannel = new NotificationChannel(
                        channelId,channelName , importance);
                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build();
                mChannel.setVibrationPattern(new long[]{1000, 1000});
                mChannel.enableVibration(enableVibration);
                mChannel.setSound(soundURI, audioAttributes);
                myNotificationManager.createNotificationChannel(mChannel);
            }

            /*Set Notification builder*/
            myNotificationBuilder = new NotificationCompat.Builder(context, channelId);

            if(myNotificationDataModel.getBody()!=null && myNotificationDataModel.getTitle()!=null){
                myNotificationBuilder.setContentTitle(myNotificationDataModel.getTitle());
                myNotificationBuilder.setContentText(myNotificationDataModel.getBody());
                myNotificationBuilder.setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(myNotificationDataModel.getBody()));
            }else{
                return null;
            }
            myNotificationBuilder.setAutoCancel(true);
            myNotificationBuilder.setVibrate(new long[]{1000, 1000});
            myNotificationBuilder.setSound(soundURI);
            myNotificationBuilder.setDeleteIntent(createOnDismissedIntent(context.getApplicationContext(),myNotificationDataModel.getPayload()));
            myNotificationBuilder.setContentIntent(pendingIntent);
            boolean hasAInvalidIcon=hasAInvalidIcon(context,myNotificationDataModel.getIcon());
            if(hasAInvalidIcon){
                return null;
            }else{
                myNotificationBuilder.setSmallIcon(getDrawableResourceId(context, myNotificationDataModel.getIcon()));
                myNotificationBuilder.setColor(Color.parseColor("#d32518"));
            }
            showNotification(myNotificationDataModel);
            return status;
        }else{
            return  null;
        }
    }

    public void showNotification(final NotificationDataModel myNotificationDataModel){
        if( myNotificationManager!=null && myNotificationBuilder!=null && myNotificationDataModel!=null) {
            if(myNotificationDataModel.getLargeImage()!=null&&!myNotificationDataModel.getLargeImage().isEmpty()){
                DownloadImageAsync asyncTask = (DownloadImageAsync) new DownloadImageAsync(myNotificationDataModel.getLargeImage(),new DownloadImageAsync.DownloadImageAsyncResponse(){
                    @Override
                    public void onImageDownloaded(Bitmap output){
                        if(output!=null){
                            myNotificationBuilder.setStyle(new NotificationCompat.BigPictureStyle().bigPicture(output).setBigContentTitle(myNotificationDataModel.getTitle()).setSummaryText(myNotificationDataModel.getBody()));
                            myNotificationManager.notify(myNotificationDataModel.getNotificationId(), myNotificationBuilder.build());
                        }
                    }
                }).execute();
            }
            else if(myNotificationDataModel.getSmallImage()!=null&& !myNotificationDataModel.getSmallImage().isEmpty()){
                DownloadImageAsync asyncTask = (DownloadImageAsync) new DownloadImageAsync(myNotificationDataModel.getSmallImage(),new DownloadImageAsync.DownloadImageAsyncResponse(){
                    @Override
                    public void onImageDownloaded(Bitmap output){
                        if(output!=null){
                            myNotificationBuilder.setLargeIcon(output);
                            myNotificationManager.notify(myNotificationDataModel.getNotificationId(), myNotificationBuilder.build());
                        }
                    }
                }).execute();
            }else{
                myNotificationManager.notify(myNotificationDataModel.getNotificationId(), myNotificationBuilder.build());
            }
        }
    }


    private PendingIntent createOnDismissedIntent(Context context,String payload) {
        Intent intent = new Intent(context, MyNotificationDismissedReceiver.class);
        intent.putExtra("payload", payload);
        PendingIntent pendingIntent =
                PendingIntent.getBroadcast(context,
                        0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return pendingIntent;
    }

    private boolean hasAInvalidIcon(Context applicationContext,String icon) {
        return !StringUtils.isNullOrEmpty(icon) && !isValidDrawableResource(applicationContext, icon);
    }


    private static boolean isValidDrawableResource(Context context, String name) {
        int resourceId = context.getResources().getIdentifier(name, DRAWABLE, context.getPackageName());
        if (resourceId == 0) {
            return false;
        }
        return true;
    }

    private static Intent getLaunchIntent(Context context) {
        String packageName = context.getPackageName();
        PackageManager packageManager = context.getPackageManager();
        return packageManager.getLaunchIntentForPackage(packageName);
    }

    private static int getDrawableResourceId(Context context, String name) {
        return context.getResources().getIdentifier(name, DRAWABLE, context.getPackageName());
    }

    public  void scheduleNotification (NotificationDataModel dataModel,Context context,int delay) {
        try {
            Intent notificationIntent = new Intent(context, MyNotificationPublisher.class);
            notificationIntent.putExtra(Constants.SCHEDULE_NOTIFICATION_PAYLOAD,  dataModel);
            notificationIntent.putExtra("test",  "hi");
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 459, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            long futureInMillis = SystemClock.elapsedRealtime() + delay;
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            assert alarmManager != null;
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, futureInMillis, pendingIntent);
        } catch (Exception e) {
System.out.println(e);
        }
    }

}