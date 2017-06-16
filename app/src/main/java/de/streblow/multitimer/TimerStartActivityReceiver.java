package de.streblow.multitimer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class TimerStartActivityReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Intent activityintent = new Intent(context, SplashActivity.class);
		activityintent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(activityintent);
	}

}
