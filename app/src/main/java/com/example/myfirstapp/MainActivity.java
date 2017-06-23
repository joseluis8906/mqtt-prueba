package com.example.myfirstapp;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity {

    Intent intent;
    String data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("Mqtt");
        actionBar.setHomeAsUpIndicator(R.mipmap.ic_mqtt_logo);

        TextView textView = (TextView) findViewById(R.id.textView);
        textView.setText("Inicio");


        if (!isMyServiceRunning()){
            intent = new Intent(this, MyMqttService.class);
            intent.putExtra("data", data);
            startService(intent);
            Log.d("App", "Service started");
        } else {
            Log.d("App", "Service already running");
        }

    }

    private boolean isMyServiceRunning() {
        ActivityManager activityManager = (ActivityManager)getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo serviceInfo: activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (MyMqttService.class.getName().equals(serviceInfo.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
    // handler for received Intents for the "my-event" event
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String topic = intent.getStringExtra("topic");
            String message = intent.getStringExtra("message");
            //Log.i("mqtt", "Got message: " + message);
            handleMessage(topic, message);
        }
    };

    @Override
    public void onResume() {
        super.onResume();

        // Register mMessageReceiver to receive messages.
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("my-event"));
    }

    @Override
    protected void onPause() {
        // Unregister since the activity is not visible
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onPause();
    }

    /** Called when the user taps the Send button */
    public void sendMessage(View view) {
        EditText editText = (EditText) findViewById(R.id.editText);
        String message = editText.getText().toString();
        Intent intent2 = new Intent ("my-event2");
        intent2.putExtra("message", message);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent2);
        editText.setText("");
    }

    public void handleMessage (String topic, String Message) {
        TextView textView = (TextView) findViewById(R.id.textView);
        textView.setText(textView.getText() + "\n" + Message);
    }

    @Override
    protected void onDestroy() {
        // Unregister since the activity is not visible
        stopService(intent);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onDestroy();
    }
}
