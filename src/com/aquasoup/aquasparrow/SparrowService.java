package com.aquasoup.aquasparrow;

import android.app.Notification;
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
    private static final int IOIO_BOARD_PIN_NUMBER = 6;
    private static final long TIME_TO_FINISH = 5000; // 5s
    private static final long TIME_TICK = 1000;
    private static final String INTENT_SMS_FILTER = "android.provider.Telephony.SMS_RECEIVED";

    private Context ctx = SparrowService.this;
    private Resources res;
    private DigitalOutput valvePin;
    private SparrowSmsParser sparrowSmsParser;

    private BroadcastReceiver SmsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String messageBody="";
            SmsMessage[] smsMessage = null;
            Bundle bundle = intent.getExtras();

            sendLogToUI(res.getString(R.string.command_obtained));

            if (bundle != null){
                try{
                    Object[] pdus = (Object[]) bundle.get("pdus");
                    smsMessage = new SmsMessage[pdus.length];
                    for(int i=0; i<smsMessage.length; i++){
                        smsMessage[i] = SmsMessage.createFromPdu((byte[])pdus[i]);
                        messageBody = smsMessage[i].getMessageBody();
                    }
                }catch(Exception e){
                    sendLogToUI(res.getString(R.string.receiver_exception));
                }

            sparrowSmsParser.checkSmsCode(messageBody);
        }
        }
    };

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
            sendLogToUI(res.getString(R.string.open_valve));
            // valvePin.write(true);
            TimerTickToCloseValve.start();
        }

        @Override
        public void badSmsCode() {
            sendLogToUI(res.getString(R.string.bad_command));
        }
};

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        res = getResources();
        Notification notice = createNotification();
        startForeground(SparrowConstants.NOTIFICATION_ID, notice);
        RegisterLogsReceiver();
        createAndAddListenerToParser();
        sendLogToUI(res.getString(R.string.service_started));
    }

    private void createAndAddListenerToParser(){
        sendLogToUI(res.getString(R.string.register_listener));
        sparrowSmsParser=new SparrowSmsParser();
        sparrowSmsParser.addListener(SmsListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
        unregisterReceiver(SmsReceiver);
        sendLogToUI(res.getString(R.string.service_destroyed));
    }

    @Override
    protected IOIOLooper createIOIOLooper() {
        return new BaseIOIOLooper() {

            @Override
            protected void setup() throws ConnectionLostException, InterruptedException {
                valvePin = ioio_.openDigitalOutput(IOIO_BOARD_PIN_NUMBER, false);
            }

            @Override
            public void loop() throws ConnectionLostException, InterruptedException {

            }
        };
    }

    private void closeValve(){
        sendLogToUI(res.getString(R.string.close_valve));
        // valvePin.write(true);
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
