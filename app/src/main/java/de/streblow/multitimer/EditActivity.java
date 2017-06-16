package de.streblow.multitimer;

import java.io.File;
import java.text.DecimalFormat;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaRecorder;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

public class EditActivity extends Activity {

	public Boolean add;
	public int id;
	public int type;
	public String description;
	public String mediapath;
	public Boolean hasmedia;
	public String ringtonepath;
	public int time1;
	public int time2;

	private MediaRecorder mediaRecorder;
	private MediaPlayer mediaPlayer;
	private Boolean playing;

	private String tempaudiofilename;
	private Boolean mediachanged;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_edit);

		add = getIntent().getExtras().getBoolean("add");
		tempaudiofilename = getIntent().getExtras().getString("tempaudiofilename");
		mediachanged = false;

		if (add) {
			setTitle(getResources().getString(R.string.add_activity_title));
			//initialize all parameters
			type = TimeEvent.TIME_EVENT_COUNTDOWN;
			description = "";
			String fileName = "snd" + String.valueOf(System.currentTimeMillis()) + ".3gp";
			mediapath = getFilesDir() + "/" + fileName;
			hasmedia = false;
			ringtonepath = null;
			time1 = 0;
			time2 = 0;
		} else {
			setTitle(getResources().getString(R.string.edit_activity_title));
			//get all parameters from intent
			id = getIntent().getIntExtra("id", -1);
			type = getIntent().getIntExtra("type", TimeEvent.TIME_EVENT_COUNTDOWN);
			description = getIntent().getStringExtra("description");
			mediapath = getIntent().getStringExtra("mediapath");
			hasmedia = getIntent().getBooleanExtra("hasmedia", false);
			ringtonepath = getIntent().getStringExtra("ringtonepath");
			time1 = getIntent().getIntExtra("time1", 0);
			time2 = getIntent().getIntExtra("time2", 0);
		}
		mediaRecorder = null;
		mediaPlayer = null;
		playing = false;

		final EditText et = (EditText) findViewById(R.id.editDescription);
		et.setText(description);

		final RadioButton rbCountdown = (RadioButton) findViewById(R.id.radioCountdown);
		final RadioButton rbReminder = (RadioButton) findViewById(R.id.radioReminder);
		final NumberPicker np1 = (NumberPicker) findViewById(R.id.numberPicker1);
		final NumberPicker np2 = (NumberPicker) findViewById(R.id.numberPicker2);
		final TextView tv = (TextView) findViewById(R.id.textTime);
		if (type == TimeEvent.TIME_EVENT_COUNTDOWN) {
			rbCountdown.setChecked(true);
			rbReminder.setChecked(false);
			tv.setText(getResources().getString(R.string.edit_time_countdown));
		}
		if (type == TimeEvent.TIME_EVENT_REMINDER) {
			rbCountdown.setChecked(false);
			rbReminder.setChecked(true);
			tv.setText(getResources().getString(R.string.edit_time_reminder));
		}
		rbCountdown.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (type == TimeEvent.TIME_EVENT_REMINDER) {
					type = TimeEvent.TIME_EVENT_COUNTDOWN;
					String[] nums = new String[60];
					for(int i=0; i<nums.length; i++)
						nums[i] = new DecimalFormat("00").format(i);
                    np1.setDisplayedValues(null);
					np1.setMinValue(0);
					np1.setMaxValue(59);
					np1.setDisplayedValues(nums);
					rbReminder.setChecked(false);
					tv.setText(getResources().getString(R.string.edit_time_countdown));
				}
			}
		});
		rbReminder.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (type == TimeEvent.TIME_EVENT_COUNTDOWN) {
					type = TimeEvent.TIME_EVENT_REMINDER;
					int n = np1.getValue();
					String[] nums = new String[24];
					for(int i=0; i<nums.length; i++)
						nums[i] = new DecimalFormat("00").format(i);
                    np1.setDisplayedValues(null);
					np1.setMinValue(0);
					np1.setMaxValue(23);
					np1.setDisplayedValues(nums);
					np1.setValue(Math.min(23, n));
					rbCountdown.setChecked(false);
					tv.setText(getResources().getString(R.string.edit_time_reminder));
				}
			}
		});
		if (type == TimeEvent.TIME_EVENT_COUNTDOWN) {
			String[] nums = new String[60];
			for(int i=0; i<nums.length; i++)
				nums[i] = new DecimalFormat("00").format(i);
			np1.setMinValue(0);
			np1.setMaxValue(59);
			np1.setDisplayedValues(nums);
			np1.setValue(time1);
		}
		if(type == TimeEvent.TIME_EVENT_REMINDER) {
			String[] nums = new String[24];
			for(int i=0; i<nums.length; i++)
				nums[i] = new DecimalFormat("00").format(i);
			np1.setMinValue(0);
			np1.setMaxValue(23);
			np1.setDisplayedValues(nums);
			np1.setValue(time1);
		}
		String[] nums = new String[24];
		nums = new String[60];
		for(int i=0; i<nums.length; i++)
			nums[i] = new DecimalFormat("00").format(i);
		np2.setMinValue(0);
		np2.setMaxValue(59);
		np2.setDisplayedValues(nums);
		np2.setValue(time2);
		np1.setWrapSelectorWheel(true);
		np2.setWrapSelectorWheel(true);

		ImageButton imageButton = (ImageButton) findViewById(R.id.buttonRecord);
		imageButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (playing)
					return;
				//start recording
				mediaRecorder = new MediaRecorder();
				try {
					mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
					mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
					mediaRecorder.setOutputFile(getFilesDir() + "/" + tempaudiofilename);
					mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
					mediaRecorder.prepare();
				} catch (Exception e) {
					e.printStackTrace();
					mediaRecorder = null;
				}
				if (mediaRecorder != null) {
					mediaRecorder.start();
					Toast.makeText(getApplicationContext(), getString(R.string.edit_recordingstarted), Toast.LENGTH_LONG).show();
				}
				//recording is running...
				ImageButton btn = (ImageButton) findViewById(R.id.buttonRecord);
				btn.setEnabled(false);
				btn = (ImageButton) findViewById(R.id.buttonStop);
				btn.setEnabled(true);
				btn = (ImageButton) findViewById(R.id.buttonPlay);
				btn.setEnabled(false);
				btn = (ImageButton) findViewById(R.id.buttonDelete);
				btn.setEnabled(false);
			}
		});
		imageButton.setEnabled(true);
		imageButton = (ImageButton) findViewById(R.id.buttonStop);
		imageButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				//stop recording
				try {
					if (mediaRecorder != null) {
						mediaRecorder.stop();
						mediaRecorder.reset();
						mediaRecorder.release();
						mediaRecorder = null;
						hasmedia = true;
						mediachanged = true;
						//file must be readable by other processes (external mediaplayer)
						File file = new File(getFilesDir(), tempaudiofilename);
						file.setReadable(true, false);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				//...
				ImageButton btn = (ImageButton) findViewById(R.id.buttonRecord);
				btn.setEnabled(true);
				btn = (ImageButton) findViewById(R.id.buttonStop);
				btn.setEnabled(false);
				btn = (ImageButton) findViewById(R.id.buttonPlay);
				btn.setEnabled(true);
				btn = (ImageButton) findViewById(R.id.buttonDelete);
				btn.setEnabled(true);
			}
		});
		imageButton.setEnabled(false);
		imageButton = (ImageButton) findViewById(R.id.buttonPlay);
		imageButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (playing)
					return;
				//play saved file
				mediaPlayer = new MediaPlayer();
				try {
					mediaPlayer.setDataSource(getFilesDir() + "/" + tempaudiofilename);
					mediaPlayer.setOnCompletionListener(new OnCompletionListener() {
						public void onCompletion(MediaPlayer mp) {
							try {
								mp.stop();
								mp.reset();
								mp.release();
							} catch (Exception e) {
								e.printStackTrace();
							}
							playing = false;
							ImageButton btn = (ImageButton) findViewById(R.id.buttonRecord);
							btn.setEnabled(true);
							btn = (ImageButton) findViewById(R.id.buttonPlay);
							btn.setEnabled(true);
							btn = (ImageButton) findViewById(R.id.buttonDelete);
							btn.setEnabled(true);
						}
					});
					playing = true;
					mediaPlayer.setVolume(1.0f, 1.0f);
					mediaPlayer.prepare();
					mediaPlayer.start();
				} catch (Exception e) {
					e.printStackTrace();
					mediaPlayer = null;
					playing = false;
				}
				//...
				ImageButton btn = (ImageButton) findViewById(R.id.buttonRecord);
				btn.setEnabled(false);
				btn = (ImageButton) findViewById(R.id.buttonStop);
				btn.setEnabled(false);
				btn = (ImageButton) findViewById(R.id.buttonPlay);
				btn.setEnabled(false);
				btn = (ImageButton) findViewById(R.id.buttonDelete);
				btn.setEnabled(false);
			}
		});
		if (hasmedia)
			imageButton.setEnabled(true);
		else
			imageButton.setEnabled(false);
		imageButton = (ImageButton) findViewById(R.id.buttonDelete);
		imageButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (hasmedia) {
					DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							switch (which){
							case DialogInterface.BUTTON_POSITIVE:
								//Yes button clicked
								File file = new File(getFilesDir(), tempaudiofilename);
								file.delete();
								hasmedia = false;
								mediachanged = true;
								break;
							case DialogInterface.BUTTON_NEGATIVE:
								//No button clicked
								break;
							}
						}
					};
					AlertDialog.Builder builder = new AlertDialog.Builder(EditActivity.this);
					builder.setTitle(getResources().getString(R.string.edit_delete_title));
					builder.setMessage(getResources().getString(R.string.edit_delete_message));
					builder.setPositiveButton(getResources().getString(R.string.yes), dialogClickListener);
					builder.setNegativeButton(getResources().getString(R.string.no), dialogClickListener);
					builder.show();
				}
				//...
				ImageButton btn = (ImageButton) findViewById(R.id.buttonRecord);
				btn.setEnabled(true);
				btn = (ImageButton) findViewById(R.id.buttonStop);
				btn.setEnabled(false);
				btn = (ImageButton) findViewById(R.id.buttonPlay);
				btn.setEnabled(false);
				btn = (ImageButton) findViewById(R.id.buttonDelete);
				btn.setEnabled(false);
			}
		});
		if (hasmedia)
			imageButton.setEnabled(true);
		else
			imageButton.setEnabled(false);
		imageButton = (ImageButton) findViewById(R.id.buttonOpen);
		imageButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
				intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select ringtone for notifications:");
				intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
				intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
				intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL);
				startActivityForResult(intent, 0);
				//...
			}
		});
		imageButton.setEnabled(true);

		Button clickButton = (Button) findViewById(R.id.buttonSave);
		clickButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				//return values via intent
				Intent intent = new Intent();
				intent.putExtra("id", id);
				if (rbCountdown.isChecked())
					type = TimeEvent.TIME_EVENT_COUNTDOWN;
				if (rbReminder.isChecked())
					type = TimeEvent.TIME_EVENT_REMINDER;
				intent.putExtra("type", type);
				if (et.getText().toString() == null || et.getText().toString() == "")
					description = "???";
				else
					description = et.getText().toString();
				intent.putExtra("description", description);
				intent.putExtra("mediapath", mediapath);
				intent.putExtra("hasmedia", hasmedia);
				intent.putExtra("mediachanged", mediachanged);
				if (ringtonepath != null)
					intent.putExtra("ringtonepath", ringtonepath);
				time1 = np1.getValue();
				time2 = np2.getValue();
				intent.putExtra("time1", time1);
				intent.putExtra("time2", time2);
				setResult(RESULT_OK, intent);
				finish();
			}
		});
		clickButton.setEnabled(true);
		clickButton = (Button) findViewById(R.id.buttonCancel);
		clickButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				setResult(RESULT_CANCELED, intent);
				finish();
			}
		});
		clickButton.setEnabled(true);
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mediaRecorder != null) {
			mediaRecorder.release();
			mediaRecorder = null;
		}
		if (mediaPlayer != null) {
			mediaPlayer.release();
			mediaPlayer = null;
		}
		playing = false;
	}

	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (requestCode == 0) { //RingtoneManger
			if (resultCode == RESULT_OK) {
				Uri uri = intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
				if (uri != null) {
					ringtonepath = uri.toString();
				} else
					ringtonepath = null;
			} else if (resultCode == RESULT_CANCELED) {
				// Handle cancel
			}
		}
	}

}
