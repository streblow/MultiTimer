package de.streblow.multitimer;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public class Messagebox {

	public static void showMessagebox(Context context, String title, String message) {
		new AlertDialog.Builder(context)
			.setMessage(message)
			.setTitle(title)
			.setCancelable(true)
			.setNeutralButton(android.R.string.ok,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {}
				})
			.show();
	}

}
