package com.example.myfirstapp;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.List;

public class MyMqttService extends Service implements MqttCallback {

    private String TAG = "My Mqtt Service";

    public String data;

    private boolean isRunning  = false;

    MqttAsyncClient sampleClient;
    String broker;
    String clientId;
    int QoS;
    String topic;
    MqttConnectOptions connOpts;
    IMqttToken token;
    MemoryPersistence persistense;


    @Override
    public void onCreate() {

        Log.d(TAG, "onCreate called");

        isRunning = true;

        broker = "tcp://149.56.109.238:1883";
        clientId = MqttAsyncClient.generateClientId();
        QoS = 0;
        topic = "prueba/prueba";
        persistense = new MemoryPersistence();

        try {

            sampleClient = new MqttAsyncClient(broker, clientId, persistense);
            connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            connOpts.setUserName("android");
            String password = "12345678";
            connOpts.setPassword(password.toCharArray());

            token = sampleClient.connect(connOpts);
            token.waitForCompletion(3500);
            sampleClient.setCallback(this);
            token = sampleClient.subscribe(topic, QoS);
            token.waitForCompletion(5000);
        }
        catch (MqttException e) {

            Log.i("mqtt", "reason " + e.getReasonCode());
            Log.i("mqtt", "msg " + e.getMessage());
            Log.i("mqtt", "loc " + e.getLocalizedMessage());
            Log.i("mqtt", "cause " + e.getCause());
            Log.i("mqtt", "excep " + e);
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


            //data = (String) intent.getExtras().get("data");
            LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("my-event2"));

        sendMessage("servicio a toda");
        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {

        Log.d(TAG, "onBind done");
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {

        return false;
    }

    @Override
    public void connectionLost(Throwable throwable) {

    }

    @Override
    @SuppressLint("NewApi")
    public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {

        this.handleMessage (s, new String(mqttMessage.getPayload()));
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

    }

    // handler for received Intents for the "my-event" event
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract data included in the Intent
            String message = intent.getStringExtra("message");
            sendMessage(message);
        }
    };

    // Methods used by the binding client components
    public void sendMessage(String msj) {

        MqttMessage message = new MqttMessage(msj.getBytes());
        message.setQos(QoS);

        try {

            token = sampleClient.publish(topic, message);
            token.waitForCompletion(1000);
        }
        catch (MqttException e) {

            Log.i("mqtt", "reason " + e.getReasonCode());
            Log.i("mqtt", "msg " + e.getMessage());
            Log.i("mqtt", "loc " + e.getLocalizedMessage());
            Log.i("mqtt", "cause " + e.getCause());
            Log.i("mqtt", "excep " + e);
            e.printStackTrace();
        }
    }

    public void handleMessage(String topic, String message) {

        Intent intent = new Intent("my-event");
        intent.putExtra("topic", topic);
        intent.putExtra("message", message);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        if(isAppIsInBackground(getApplicationContext())){
            NotificationCompat.Builder mBuilder;
            NotificationManager mNotifyMgr =(NotificationManager) getApplicationContext().getSystemService(NOTIFICATION_SERVICE);

            int icono = R.mipmap.ic_launcher;

            mBuilder =new NotificationCompat.Builder(getApplicationContext())
                    .setSmallIcon(icono)
                    .setContentTitle(topic)
                    .setContentText(message)
                    .setVibrate(new long[] {100, 250, 100, 500})
                    .setAutoCancel(true);
            mBuilder.setDefaults(Notification.DEFAULT_ALL);
            mNotifyMgr.notify(1, mBuilder.build());
        }
    }

    public static boolean isAppIsInBackground(Context context) {
        boolean isInBackground = true;
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT_WATCH) {
            List<ActivityManager.RunningAppProcessInfo> runningProcesses = am.getRunningAppProcesses();
            for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
                if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    for (String activeProcess : processInfo.pkgList) {
                        if (activeProcess.equals(context.getPackageName())) {
                            isInBackground = false;
                        }
                    }
                }
            }
        } else {
            List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
            ComponentName componentInfo = taskInfo.get(0).topActivity;
            if (componentInfo.getPackageName().equals(context.getPackageName())) {
                isInBackground = false;
            }
        }

        return isInBackground;
    }

    @Override
    public void onDestroy() {



        Intent intent = new Intent(this, MyMqttService.class);
        intent.putExtra("data", "as");
        startService(intent);

        /*isRunning = false;
        try {
            sampleClient.disconnect();
        }
        catch (MqttException e) {
            e.printStackTrace();
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onDestroy();
        Log.i ("mqtt", "MyMqtt Service destroyed");*/
    }

}
