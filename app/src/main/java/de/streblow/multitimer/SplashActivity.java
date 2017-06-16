package de.streblow.multitimer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class SplashActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent main = new Intent(this, MainActivity.class);
		startActivity(main);
		finish();
	}

}
