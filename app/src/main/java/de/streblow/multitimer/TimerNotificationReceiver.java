package de.streblow.multitimer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class TimerNotificationReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Intent activityintent;
		if (MainActivity.mainActivity == null)
			activityintent = new Intent(context, MainActivity.class);
		else
			activityintent = MainActivity.mainActivity.getIntent();
		activityintent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent pendingintent = PendingIntent.getActivity(context, 0, activityintent, 0);
		Notification notification;
		if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
			notification = new Notification.Builder(context)
				.setContentTitle(context.getResources().getString(R.string.app_name))
				.setContentText(context.getResources().getString(R.string.notification))
				.setSmallIcon(R.drawable.notify)
				.setContentIntent(pendingintent)
				.setAutoCancel(true)
				.build();
		else
			notification = new Notification.Builder(context)
					.setContentTitle(context.getResources().getString(R.string.app_name))
					.setContentText(context.getResources().getString(R.string.notification))
					.setSmallIcon(R.drawable.notify)
					.setContentIntent(pendingintent)
					.setAutoCancel(true)
					.getNotification();
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		NotificationManager manager = 
			(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
		manager.notify(0, notification); 
	}

}
