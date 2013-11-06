package com.aquasoup.aquasparrow;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsMessage;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOService;

public class SparrowService extends IOIOService {

    private static final int IOIO_BOARD_PIN_NUMBER = 13;
    private static final long TIME_TO_FINISH = 1000*480;
    private static final long TIME_TICK = 1000;
    private static final String INTENT_SMS_FILTER = "android.provider.Telephony.SMS_RECEIVED";

    private Context ctx;
    private Resources res;
    private DigitalOutput valvePin;
    private SparrowSmsParser sparrowSmsParser;
    private BroadcastReceiver SmsReceiver;
    private NotificationManager nm;

    {
        SmsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String messageBody="";
                SmsMessage[] smsMessage;
                Bundle bundle= intent.getExtras();

                sendLogToUI(res.getString(R.string.command_obtained));

                if (bundle != null) {
                    try {
                        Object[] pdus = (Object[]) bundle.get("pdus");
                        smsMessage = new SmsMessage[pdus.length];
                        for (int i = 0; i < smsMessage.length; i++) {
                            smsMessage[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                            messageBody = smsMessage[i].getMessageBody();
                        }
                    } catch (Exception e) {
                        sendLogToUI(res.getString(R.string.receiver_exception));
                    }

                    sparrowSmsParser.checkSmsCode(messageBody);
                }
            }
        };
    }

    private CountDownTimer TimerTickToCloseValve = new CountDownTimer(TIME_TO_FINISH, TIME_TICK) {

        @Override
        public void onTick(long millisUntilFinished) {
        }

        @Override
        public void onFinish() {
            closeValve();
        }
    };

    private SparrowSmsParser.SmsListener SmsListener=new SparrowSmsParser.SmsListener() {
        @Override
        public void openValve() {
            SparrowService.this.openValve();
            TimerTickToCloseValve.start();
        }

        @Override
        public void badSmsCode() {
            sendLogToUI(res.getString(R.string.bad_command));
        }
    };

    public SparrowService() {
        ctx = SparrowService.this;
    }

    private void openValve(){
        sendLogToUI(res.getString(R.string.open_valve));
        try {
            if(valvePin != null){
                valvePin.write(false);
            }
        } catch (ConnectionLostException e) {
            e.printStackTrace();
        }
    }

    private void closeValve(){
        sendLogToUI(res.getString(R.string.close_valve));
        try {
            if(valvePin != null){
                valvePin.write(true);
            }
        } catch (ConnectionLostException e) {
            e.printStackTrace();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        res = getResources();
        sendLogToUI(res.getString(R.string.service_started));
        RegisterLogsReceiver();
        startNotification();
        createAndAddListenerToParser();
    }

    private void startNotification(){
        Notification notice=createNotification();
        nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(SparrowConstants.NOTIFICATION_ID, notice);
    }

    private void stopNotification(){
        nm.cancel(SparrowConstants.NOTIFICATION_ID);
    }

    private void createAndAddListenerToParser(){

        sparrowSmsParser=new SparrowSmsParser();
        sparrowSmsParser.addListener(SmsListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(SmsReceiver);
        stopNotification();
        sendLogToUI(res.getString(R.string.service_destroyed));
    }

    @Override
    protected IOIOLooper createIOIOLooper() {

        return new BaseIOIOLooper() {

            @Override
            protected void setup(){
                try {
                    valvePin = ioio_.openDigitalOutput(IOIO_BOARD_PIN_NUMBER,true);
                } catch (ConnectionLostException e) {
                    sendLogToUI(res.getString(R.string.connection_lost));
                }
            }

            @Override
            public void loop(){
            }
        };
    }

    private Notification createNotification() {
        String notificationTitle = res.getString(R.string.notification_title);
        String notificationText = res.getString(R.string.notification_text);
        //compat
        Intent resultIntent = new Intent(ctx, SparrowActivity.class);
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(ctx, 0, resultIntent, 0);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(ctx);
        notificationBuilder.setWhen(System.currentTimeMillis());
        notificationBuilder.setContentIntent(contentIntent);
        notificationBuilder.setContentTitle(notificationTitle);
        notificationBuilder.setContentText(notificationText);
        notificationBuilder.setOngoing(true);
        notificationBuilder.setSmallIcon(R.drawable.sparrow_notif);
        Bitmap largeIcon = BitmapFactory.decodeResource(res, R.drawable.sparrow_notif);
        notificationBuilder.setLargeIcon(largeIcon);

        return notificationBuilder.getNotification();
    }

    private void sendLogToUI(String log) {
        Intent intent = new Intent();
        intent.setAction(SparrowConstants.ACTION_LOG);
        intent.putExtra(SparrowConstants.STRING_ID, log);
        sendBroadcast(intent);
    }

    private void RegisterLogsReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(INTENT_SMS_FILTER);
        registerReceiver(SmsReceiver, filter);
    }

}
