package com.aquasoup.aquasparrow;

import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

public class SparrowService extends IOIOService {
    // private static final int PIN = 6;

    private NotificationCompat.Builder notificationBuilder;
    private Context ctx = SparrowService.this;
    private Resources res;

    // private DigitalOutput pin_;

    @Override
    public IBinder onBind(Intent intent) {
	return null;
    }

    @Override
    public void onStart(Intent intent, int startId) {
	super.onStart(intent, startId);
	res = getResources();
	Notification notice = createNotification();
	notice.flags |= Notification.FLAG_NO_CLEAR;
	startForeground(SparrowConstants.NOTIFICATION_ID, notice);
	sendLogToUI(res.getString(R.string.service_started));
    }

    @Override
    protected IOIOLooper createIOIOLooper() {
	return new BaseIOIOLooper() {

	    @Override
	    protected void setup() throws ConnectionLostException, InterruptedException {
		// pin_ = ioio_.openDigitalOutput(PIN, false);
	    }

	    @Override
	    public void loop() throws ConnectionLostException, InterruptedException {

	    }
	};
    }

    private Notification createNotification() {
	String notificationTitle = res.getString(R.string.notification_title);
	String notificationText = res.getString(R.string.notification_text);

	Intent resultIntent = new Intent(ctx, SparrowActivity.class);
	resultIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

	TaskStackBuilder stackBuilder = TaskStackBuilder.create(ctx);
	stackBuilder.addParentStack(SparrowActivity.class);
	stackBuilder.addNextIntent(resultIntent);
	PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

	notificationBuilder = new NotificationCompat.Builder(ctx);
	notificationBuilder.setWhen(System.currentTimeMillis());
	notificationBuilder.setContentIntent(resultPendingIntent);
	notificationBuilder.setContentTitle(notificationTitle);
	notificationBuilder.setContentText(notificationText);
	notificationBuilder.setOngoing(true);
	notificationBuilder.setSmallIcon(R.drawable.ic_launcher);

	return notificationBuilder.build();
    }

    private void sendLogToUI(String log) {
	Intent intent = new Intent();
	intent.setAction(SparrowConstants.ACTION_LOG);
	intent.putExtra(SparrowConstants.STRING_ID, log);
	sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
	super.onDestroy();
	stopForeground(true);
	sendLogToUI(res.getString(R.string.service_destroyed));
    }

}
