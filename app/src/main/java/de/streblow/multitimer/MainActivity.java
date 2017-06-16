package de.streblow.multitimer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ImageButton;
import android.widget.ListView;

public class MainActivity extends Activity {

	public static Activity mainActivity = null;
	public static Ringtone playingRingtone = null;

	private static String NORMAL = "Normal";
	private static String WAKELOCK = "Wakelock";
	private static String ALARM = "Alarm";
	private static String NOTIFY = "Notify";
	private String behaviour;

	private final String savedDataFilename = "timeevents";
	private final String tempAudioFilename = "tempaudio.3gp";

	private ArrayList<TimerListItem> timerListItems;
	private ArrayList<TimeEvent> timeEvents;
	private ArrayList<RunningTimer> runningTimerList;
	private ArrayList<Integer> firedTimerList;

	private int savedCheckedItemId;

	private long nextfiringdatemilliseconds;

	private Boolean editTimeEventPending;
	private Boolean addTimeEventPending;
	private TimeEvent pendingTimeEvent;
	private int pendingTimeEventId;

	private Handler timerHandler;
	private Runnable timerRunnable;

	private Handler mediaHandler;
	private Runnable mediaRunnable;
	private boolean mediaRunnableIsIdle;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		if (mainActivity == null)
			mainActivity = this;
		playingRingtone = null;

		//create a list of firing Timers
		firedTimerList = new ArrayList<Integer>();

		ListView listView = (ListView) findViewById(R.id.listview);
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				/*
					parent The AdapterView where the click happened.
					view The view within the AdapterView that was clicked (this will be a view provided by the adapter)
					position The position of the view in the adapter.
					id The row id of the item that was clicked.
				*/
				ListView lv = (ListView) findViewById(R.id.listview);
				lv.setSelection(position);
				lv.setItemChecked(position, true);
				updateButtonStates(position);
			}
		});
		listView.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				/*
					parent The AdapterView where the click happened.
					view The view within the AdapterView that was clicked (this will be a view provided by the adapter)
					position The position of the view in the adapter.
					id The row id of the item that was clicked.
				*/
				ListView lv = (ListView) findViewById(R.id.listview);
				lv.setSelection(position);
				lv.setItemChecked(position, true);
				updateButtonStates(position);
				if (timeEvents.get(position).playing)
					if (playingRingtone != null) {
						playingRingtone.stop();
						playingRingtone = null;
						timeEvents.get(position).playing = false;
					} else if (timeEvents.get(position).mediaPlayer != null) {
						timeEvents.get(position).mediaPlayer.stop();
						timeEvents.get(position).mediaPlayer.release();
						timeEvents.get(position).mediaPlayer = null;
						timeEvents.get(position).playing = false;
					}
				return true;
			}
		});

		ImageButton clickButton = (ImageButton) findViewById(R.id.ImageButtonPlay);
		clickButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				ListView lv = (ListView) findViewById(R.id.listview);
				int id = lv.getCheckedItemPosition();
				if (id != ListView.INVALID_POSITION) {
					timeEvents.get(id).startTimer();
					timerListItems.get(id).setIcon2(R.drawable.dot_green);
					addToRunningTimerList(id);
					updateButtonStates(id);
					TimerListAdapter tla = (TimerListAdapter) lv.getAdapter();
					tla.notifyDataSetChanged();
					if (timeEvents.get(id).playing)
						if (playingRingtone != null) {
							playingRingtone.stop();
							playingRingtone = null;
							timeEvents.get(id).playing = false;
						} else if (timeEvents.get(id).mediaPlayer != null) {
							timeEvents.get(id).mediaPlayer.stop();
							timeEvents.get(id).mediaPlayer.release();
							timeEvents.get(id).mediaPlayer = null;
							timeEvents.get(id).playing = false;
						}
				}
			}
		});
		clickButton.setEnabled(false);

		clickButton = (ImageButton) findViewById(R.id.ImageButtonPause);
		clickButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				ListView lv = (ListView) findViewById(R.id.listview);
				int id = lv.getCheckedItemPosition();
				if (id != ListView.INVALID_POSITION) {
					if (!timeEvents.get(id).paused) {
						timeEvents.get(id).pauseTimer();
						timerListItems.get(id).setIcon2(R.drawable.dot_red);
						removeFromRunningTimerList(id);
					} else {
						timeEvents.get(id).resumeTimer();
						timerListItems.get(id).setIcon2(R.drawable.dot_green);
						addToRunningTimerList(id);
					}
					updateButtonStates(id);
					TimerListAdapter tla = (TimerListAdapter) lv.getAdapter();
					tla.notifyDataSetChanged();
				}
			}
		});
		clickButton.setEnabled(false);

		clickButton = (ImageButton) findViewById(R.id.ImageButtonStop);
		clickButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				ListView lv = (ListView) findViewById(R.id.listview);
				int id = lv.getCheckedItemPosition();
				if (id != ListView.INVALID_POSITION) {
					if (!timeEvents.get(id).paused)
						removeFromRunningTimerList(id);
					timeEvents.get(id).stopTimer();
					timerListItems.get(id).setIcon2(R.drawable.dot_blue);
					if (timeEvents.get(id).eventType == TimeEvent.TIME_EVENT_COUNTDOWN) {
						timerListItems.get(id).setTime(new SimpleDateFormat("mm:ss", Locale.getDefault()).format(new Date(timeEvents.get(id).milliseconds)));
					}
					updateButtonStates(id);
					TimerListAdapter tla = (TimerListAdapter) lv.getAdapter();
					tla.notifyDataSetChanged();
					if (timeEvents.get(id).playing)
						if (playingRingtone != null) {
							playingRingtone.stop();
							playingRingtone = null;
							timeEvents.get(id).playing = false;
						} else if (timeEvents.get(id).mediaPlayer != null) {
							timeEvents.get(id).mediaPlayer.stop();
							timeEvents.get(id).mediaPlayer.release();
							timeEvents.get(id).mediaPlayer = null;
							timeEvents.get(id).playing = false;
						}
				}
			}
		});
		clickButton.setEnabled(false);

		clickButton = (ImageButton) findViewById(R.id.ImageButtonEdit);
		clickButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				ListView lv = (ListView) findViewById(R.id.listview);
				int id = lv.getCheckedItemPosition();
				Intent intent = new Intent(MainActivity.this, EditActivity.class);
				intent.putExtra("add", false);
				intent.putExtra("tempaudiofilename", tempAudioFilename);
				intent.putExtra("id", id);
				intent.putExtra("type", timeEvents.get(id).eventType);
				intent.putExtra("description", timeEvents.get(id).description);
				intent.putExtra("mediapath", timeEvents.get(id).mediapath);
				intent.putExtra("hasmedia", timeEvents.get(id).hasmedia);
				if (timeEvents.get(id).ringtonepath != null)
					intent.putExtra("ringtonepath", timeEvents.get(id).ringtonepath);
				int time1 = 0;
				int time2 = 0;
				if (timeEvents.get(id).eventType == TimeEvent.TIME_EVENT_COUNTDOWN) {
					time1 = (int) ((timeEvents.get(id).milliseconds / 1000) / 60);
					time2 = (int) ((timeEvents.get(id).milliseconds / 1000) % 60);
				}
				if (timeEvents.get(id).eventType == TimeEvent.TIME_EVENT_REMINDER) {
					Calendar cal = Calendar.getInstance();
					cal.setTime(timeEvents.get(id).date);
					time1 = cal.get(Calendar.HOUR_OF_DAY);
					time2 = cal.get(Calendar.MINUTE);
				}
				intent.putExtra("time1", time1);
				intent.putExtra("time2", time2);
				try {
					//file must be readable by other processes (external mediaplayer)
					File outfile = new File(getFilesDir(), tempAudioFilename);
					copy(new File(timeEvents.get(id).mediapath), outfile);
					outfile.setReadable(true, false);
				} catch (IOException e) {
					e.printStackTrace();
				}
				startActivityForResult(intent, 0);
			}
		});
		clickButton.setEnabled(false);

		clickButton = (ImageButton) findViewById(R.id.ImageButtonAdd);
		clickButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this, EditActivity.class);
				intent.putExtra("add", true);
				intent.putExtra("tempaudiofilename", tempAudioFilename);
				startActivityForResult(intent, 1);
			}
		});
		clickButton.setEnabled(true);

		clickButton = (ImageButton) findViewById(R.id.ImageButtonRemove);
		clickButton.setOnClickListener(new OnClickListener() {
			ListView lv;
			int id;
			@Override
			public void onClick(View v) {
				//remove currently selected item and select next if available,
				//previous if next is not available or nothing if there is no
				//more item
				lv = (ListView) findViewById(R.id.listview);
				id = lv.getCheckedItemPosition();
				if (id != ListView.INVALID_POSITION) {
					DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							switch (which){
							case DialogInterface.BUTTON_POSITIVE:
								//Yes button clicked
								int count = lv.getCount();
								//delete media file
								if (timeEvents.get(id).hasmedia) {
									File file = new File(timeEvents.get(id).mediapath);
									file.delete();
								}
								timeEvents.remove(id);
								timerListItems.remove(id);
								if (id == count - 1)
									id -= 1;
								TimerListAdapter tla = (TimerListAdapter) lv.getAdapter();
								tla.notifyDataSetChanged();
								if (id != -1) {
									lv.setSelection(id);
									lv.setItemChecked(id, true);
								}
								updateButtonStates(id);
								break;
							case DialogInterface.BUTTON_NEGATIVE:
								//No button clicked
								break;
							}
						}
					};
					AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
					builder.setTitle(getResources().getString(R.string.delete_title));
					builder.setMessage(getResources().getString(R.string.delete_message));
					builder.setPositiveButton(getResources().getString(R.string.yes), dialogClickListener);
					builder.setNegativeButton(getResources().getString(R.string.no), dialogClickListener);
					builder.show();
				}
			}
		});
		clickButton.setEnabled(false);

		savedCheckedItemId = -1;

		nextfiringdatemilliseconds = 0;

		editTimeEventPending = false;
		addTimeEventPending = false;
		pendingTimeEvent = null;
		pendingTimeEventId = -1;

		timerHandler = new Handler();
		timerRunnable = new Runnable() {
			@Override
			public void run() {
				if (runningTimerList.size() > 0) {
					updateRunningTimerList();
					ListView lv = (ListView) findViewById(R.id.listview);
					TimerListAdapter tla = (TimerListAdapter) lv.getAdapter();
					tla.notifyDataSetChanged();
				}
				timerHandler.postDelayed(this, 1000);
			}
		};

		mediaHandler = new Handler();
		mediaRunnable = new Runnable() {
			@Override
			public void run() {
				if (firedTimerList.size() > 0) {
					if (timeEvents.get(firedTimerList.get(0)).playing)
						if (playingRingtone != null) {
							if (playingRingtone.isPlaying())
								mediaHandler.postDelayed(this, 250);
							else {
								playingRingtone = null;
								timeEvents.get(firedTimerList.get(0)).playing = false;
								mediaHandler.postDelayed(this, 0);
							}
						} else
							mediaHandler.postDelayed(this, 250);
					else {
						if (mediaRunnableIsIdle) {
							timeEvents.get(firedTimerList.get(0)).playMedia();
							mediaRunnableIsIdle = false;
							mediaHandler.postDelayed(this, 500);
						} else {
							firedTimerList.remove(0);
							mediaRunnableIsIdle = true;
							mediaHandler.postDelayed(this, 0);
						}
					}
				} else {
					mediaRunnableIsIdle = true;
					mediaHandler.postDelayed(this, 1000);
				}
			}
		};
	}

	@Override
	public void onPause() {
		super.onPause();
		savedCheckedItemId = -1;
		editTimeEventPending = false;
		addTimeEventPending = false;
		pendingTimeEvent = null;
		pendingTimeEventId = -1;
		timerHandler.removeCallbacks(timerRunnable);
		mediaHandler.removeCallbacks(mediaRunnable);
		if (playingRingtone != null)
			playingRingtone.stop();
		playingRingtone = null;
		writeTimeEvents();
		timeEvents.clear();
		timeEvents = null;
		timerListItems.clear();
		timerListItems = null;
		if (behaviour.equals(NORMAL)) {
			//nothing to do
		} else if (behaviour.equals(WAKELOCK)) {
			//nothing to do
		} else if (behaviour.equals(ALARM)) {
			if (nextfiringdatemilliseconds > 0) {
				Calendar tzcal = Calendar.getInstance();
				TimeZone tz = tzcal.getTimeZone();
				int gmtOffset = tz.getRawOffset();
				AlarmManager manager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
				Intent intent = new Intent(this, TimerStartActivityReceiver.class);
				PendingIntent pending = PendingIntent.getBroadcast(this, 0, intent, 0);
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
					manager.setExact(AlarmManager.RTC_WAKEUP, nextfiringdatemilliseconds + (long) gmtOffset, pending);
				} else {
					manager.set(AlarmManager.RTC_WAKEUP, nextfiringdatemilliseconds + (long) gmtOffset, pending);
				}
			}
		} else if (behaviour.equals(NOTIFY)) {
			if (nextfiringdatemilliseconds > 0) {
				Calendar tzcal = Calendar.getInstance();
				TimeZone tz = tzcal.getTimeZone();
				int gmtOffset = tz.getRawOffset();
				AlarmManager manager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
				Intent intent = new Intent(this, TimerNotificationReceiver.class);
				PendingIntent pending = PendingIntent.getBroadcast(this, 0, intent, 0);
				manager.set(AlarmManager.RTC_WAKEUP, nextfiringdatemilliseconds + (long)gmtOffset, pending);
			}
		}

	}

	@Override
	public void onResume() {
		super.onResume();

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		behaviour = prefs.getString("prefBehaviour", NORMAL);

		if (behaviour.equals(NORMAL)) {
			//
		} else if (behaviour.equals(WAKELOCK)) {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		} else if (behaviour.equals(ALARM)) {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
					WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
					WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
			Intent intent = new Intent(getBaseContext(), TimerStartActivityReceiver.class);
			PendingIntent pendingIntent = PendingIntent.getBroadcast(getBaseContext(), 0, intent, 0);
			AlarmManager manager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
			manager.cancel(pendingIntent);
		} else if (behaviour.equals(NOTIFY)) {
			Intent intent = new Intent(getBaseContext(), TimerNotificationReceiver.class);
			PendingIntent pendingIntent = PendingIntent.getBroadcast(getBaseContext(), 0, intent, 0);
			AlarmManager manager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
			manager.cancel(pendingIntent);
		}

		//create a list of TimeEvents
		timeEvents = new ArrayList<TimeEvent>();
		readTimeEvents();

		//create a list of running TimeEvents
		runningTimerList = new ArrayList<RunningTimer>();
		createRunningTimerList();

		//create a list of TimerListItems from the list of TimeEvents
		timerListItems = new ArrayList<TimerListItem>();
		createTimerListItemsFromTimeEvents();

		//update pending TimeEvent changes
		if (editTimeEventPending) {
			timeEvents.get(pendingTimeEventId).eventType = pendingTimeEvent.eventType;
			timeEvents.get(pendingTimeEventId).description = pendingTimeEvent.description;
			timeEvents.get(pendingTimeEventId).mediapath = pendingTimeEvent.mediapath;
			timeEvents.get(pendingTimeEventId).hasmedia = pendingTimeEvent.hasmedia;
			timeEvents.get(pendingTimeEventId).ringtonepath = pendingTimeEvent.ringtonepath;
			if (pendingTimeEvent.eventType == TimeEvent.TIME_EVENT_COUNTDOWN) {
				timeEvents.get(pendingTimeEventId).running = false;
				timeEvents.get(pendingTimeEventId).paused = false;
				timeEvents.get(pendingTimeEventId).fired = false;
				timeEvents.get(pendingTimeEventId).milliseconds = pendingTimeEvent.milliseconds;
				timeEvents.get(pendingTimeEventId).millisecondsleft = pendingTimeEvent.millisecondsleft;
				timeEvents.get(pendingTimeEventId).date = null;
			}
			if (pendingTimeEvent.eventType == TimeEvent.TIME_EVENT_REMINDER) {
				timeEvents.get(pendingTimeEventId).running = false;
				timeEvents.get(pendingTimeEventId).paused = false;
				timeEvents.get(pendingTimeEventId).fired = false;
				timeEvents.get(pendingTimeEventId).milliseconds = 0;
				timeEvents.get(pendingTimeEventId).millisecondsleft = 0;
				timeEvents.get(pendingTimeEventId).date = pendingTimeEvent.date;
			}
			TimerListItem timerlistitem = createTimerListItemFromTimeEvent(timeEvents.get(pendingTimeEventId));
			timerListItems.get(pendingTimeEventId).setTitle(timerlistitem.getTitle());
			timerListItems.get(pendingTimeEventId).setIcon1(timerlistitem.getIcon1());
			timerListItems.get(pendingTimeEventId).setIcon2(timerlistitem.getIcon2());
			timerListItems.get(pendingTimeEventId).setTime(timerlistitem.getTime());
			
		}
		if (addTimeEventPending) {
			timeEvents.add(pendingTimeEvent);
			TimerListItem timerlistitem = createTimerListItemFromTimeEvent(pendingTimeEvent);
			timerListItems.add(timerlistitem);
		}

		//setup listView
		ListView listView = (ListView) findViewById(R.id.listview);
		TimerListAdapter adapter = new TimerListAdapter(this, timerListItems);
		listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		listView.setAdapter(adapter);
		listView.setSelector(R.drawable.list_selector);

		if (editTimeEventPending) {
			listView.setSelection(pendingTimeEventId);
			listView.setItemChecked(pendingTimeEventId, true);
			updateButtonStates(pendingTimeEventId);
			editTimeEventPending = false;
		} else if (addTimeEventPending) {
			listView.setSelection(listView.getCount() - 1);
			listView.setItemChecked(listView.getCount() - 1, true);
			updateButtonStates(listView.getCount() - 1);
			addTimeEventPending = false;
		} else if (timerListItems.size() > 0) {
			listView.setSelection(savedCheckedItemId);
			listView.setItemChecked(savedCheckedItemId, true);
			updateButtonStates(savedCheckedItemId);
		}

		playingRingtone = null;
		timerHandler.postDelayed(timerRunnable, 0);
		mediaRunnableIsIdle = true;
		mediaHandler.postDelayed(mediaRunnable, 200);
	}

	@Override
	public void onStop() {
		super.onStop();
		//
	}

	@Override
	public void onStart() {
		super.onStart();
		//
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		//inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		case R.id.action_settings:
			Intent prefsActivity = new Intent(MainActivity.this, Prefs.class);
			startActivity(prefsActivity);
			return true;
		case R.id.action_help:
			HelpDialog help = new HelpDialog(this);
			help.setTitle(R.string.help_title);
			help.show();
			return true;
		case R.id.action_about:
			AboutDialog about = new AboutDialog(this);
			about.setTitle(R.string.about_title);
			about.show();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	protected void updateButtonStates(int position) {
		ImageButton clickButton;
		clickButton = (ImageButton) findViewById(R.id.ImageButtonPlay);
		if (position == -1 || position == ListView.INVALID_POSITION) {
			clickButton.setEnabled(false);
			clickButton = (ImageButton) findViewById(R.id.ImageButtonPause);
			clickButton.setEnabled(false);
			clickButton = (ImageButton) findViewById(R.id.ImageButtonStop);
			clickButton.setEnabled(false);
			clickButton = (ImageButton) findViewById(R.id.ImageButtonEdit);
			clickButton.setEnabled(false);
			clickButton = (ImageButton) findViewById(R.id.ImageButtonAdd);
			clickButton.setEnabled(true);
			clickButton = (ImageButton) findViewById(R.id.ImageButtonRemove);
			clickButton.setEnabled(false);
			return;
		}
		TimeEvent timeevent = timeEvents.get(position);
		if (timeevent.running) {
			clickButton.setEnabled(false);
			clickButton = (ImageButton) findViewById(R.id.ImageButtonPause);
			if (timeevent.eventType == TimeEvent.TIME_EVENT_COUNTDOWN)
				clickButton.setEnabled(true);
			if (timeevent.eventType == TimeEvent.TIME_EVENT_REMINDER)
				clickButton.setEnabled(false);
			clickButton = (ImageButton) findViewById(R.id.ImageButtonStop);
			clickButton.setEnabled(true);
			clickButton = (ImageButton) findViewById(R.id.ImageButtonEdit);
			clickButton.setEnabled(false);
			clickButton = (ImageButton) findViewById(R.id.ImageButtonAdd);
			clickButton.setEnabled(true);
			clickButton = (ImageButton) findViewById(R.id.ImageButtonRemove);
			clickButton.setEnabled(false);
		} else {
			clickButton.setEnabled(true);
			clickButton = (ImageButton) findViewById(R.id.ImageButtonPause);
			clickButton.setEnabled(false);
			clickButton = (ImageButton) findViewById(R.id.ImageButtonStop);
			if (timeevent.fired)
				clickButton.setEnabled(true);
			else
				clickButton.setEnabled(false);
			clickButton = (ImageButton) findViewById(R.id.ImageButtonEdit);
			clickButton.setEnabled(true);
			clickButton = (ImageButton) findViewById(R.id.ImageButtonAdd);
			clickButton.setEnabled(true);
			clickButton = (ImageButton) findViewById(R.id.ImageButtonRemove);
			clickButton.setEnabled(true);
		}
	}

	protected TimerListItem createTimerListItemFromTimeEvent(TimeEvent timeevent) {
		int icon1 = R.drawable.bell;
		int icon2 = R.drawable.dot_blue;
		String description = "";
		String time = "00:00";
		if (timeevent.eventType == TimeEvent.TIME_EVENT_COUNTDOWN) {
			icon1 = R.drawable.bell;
		}
		if (timeevent.eventType == TimeEvent.TIME_EVENT_REMINDER) {
			icon1 = R.drawable.clock;
			time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(timeevent.date);
		}
		if (!timeevent.running && !timeevent.fired && !timeevent.paused) {
			icon2 = R.drawable.dot_blue;
			if (timeevent.eventType == TimeEvent.TIME_EVENT_COUNTDOWN) {
				time = new SimpleDateFormat("mm:ss", Locale.getDefault()).format(new Date(timeevent.milliseconds));
			}
		}
		if (timeevent.running && !timeevent.paused) {
			icon2 = R.drawable.dot_green;
			if (timeevent.eventType == TimeEvent.TIME_EVENT_COUNTDOWN) {
				time = new SimpleDateFormat("mm:ss", Locale.getDefault()).format(new Date(timeevent.millisecondsleft));
			}
		}
		if (timeevent.paused) {
			icon2 = R.drawable.dot_red;
			if (timeevent.eventType == TimeEvent.TIME_EVENT_COUNTDOWN) {
				time = new SimpleDateFormat("mm:ss", Locale.getDefault()).format(new Date(timeevent.millisecondsleft));
			}
		}
		if (timeevent.fired) {
			icon2 = R.drawable.dot_yellow;
			if (timeevent.eventType == TimeEvent.TIME_EVENT_COUNTDOWN) {
				time = "00:00";
			}
		}
		description = timeevent.description;
		TimerListItem timerlistitem = new TimerListItem(icon1, icon2, description, time);
		return timerlistitem;
	}

	protected void createTimerListItemsFromTimeEvents() {
		timerListItems.clear();
		TimerListItem timerlistitem;
		for (int i = 0; i < timeEvents.size(); i++) {
			TimeEvent timeevent = timeEvents.get(i);
			timerlistitem = createTimerListItemFromTimeEvent(timeevent);
			timerListItems.add(timerlistitem);
		}
	}

	protected void createRunningTimerList() {
		if (timeEvents.size() > 0) {
			Calendar tzcal = Calendar.getInstance();
			TimeZone tz = tzcal.getTimeZone();
			int gmtOffset = tz.getRawOffset();
			Calendar cal = Calendar.getInstance();
			long currentTime = cal.getTimeInMillis() - (long)gmtOffset;
			for (int i = 0; i < timeEvents.size(); i++) {
				TimeEvent timeevent = timeEvents.get(i);
				if (timeevent.running && !timeevent.paused) {
					long millisecondsleft = timeevent.firingdatemilliseconds - currentTime;
					if (millisecondsleft > 0) {
						timeevent.millisecondsleft = millisecondsleft;
						runningTimerList.add(new RunningTimer(i));
					} else {
						timeevent.fireTimer();
						firedTimerList.add(i);
					}
					
				}
			}
		}
	}

	private void addToRunningTimerList(int position) {
		runningTimerList.add(new RunningTimer(position));
	}

	private void removeFromRunningTimerList(int position) {
		for (int i = 0; i < runningTimerList.size(); i++)
			if (runningTimerList.get(i).position == position) {
				runningTimerList.remove(i);
				return;
			}
	}

	private void updateRunningTimerList() {
		ListView lv = (ListView) findViewById(R.id.listview);
		int id = lv.getCheckedItemPosition();
		Calendar tzcal = Calendar.getInstance();
		TimeZone tz = tzcal.getTimeZone();
		int gmtOffset = tz.getRawOffset();
		Calendar cal = Calendar.getInstance();
		long currentTime = cal.getTimeInMillis() - (long)gmtOffset;
		for (int i = 0; i < runningTimerList.size(); i++) {
			RunningTimer runningtimer = runningTimerList.get(i);
			TimeEvent timeevent = timeEvents.get(runningtimer.position);
			TimerListItem timerlistitem = timerListItems.get(runningtimer.position);
			long millisecondsleft = timeevent.millisecondsleft;
			millisecondsleft = timeevent.firingdatemilliseconds - currentTime;
			if (millisecondsleft > 0) {
				timeevent.millisecondsleft = millisecondsleft;
				if (timeevent.eventType == TimeEvent.TIME_EVENT_COUNTDOWN) {
					timerlistitem.setTime(new SimpleDateFormat("mm:ss", Locale.getDefault()).format(new Date(millisecondsleft)));
				}
			} else {
				timeevent.fireTimer();
				if (timeevent.eventType == TimeEvent.TIME_EVENT_COUNTDOWN)
					timerlistitem.setTime("00:00");
				timerlistitem.setIcon2(R.drawable.dot_yellow);
				//updateButtonStates if item is selected
				if (id == runningtimer.position)
					updateButtonStates(id);
				firedTimerList.add(runningtimer.position);
				removeFromRunningTimerList(runningtimer.position);
			}
		}
	}

	protected void writeTimeEvents() {
		DataOutputStream dos = null;
		ListView lv;
		int id;
		try {
			File file = new File(getFilesDir(), savedDataFilename);
			dos = new DataOutputStream(new FileOutputStream(file));
			//write number of items
			dos.writeInt(timeEvents.size());
			//write id of currently selected item
			if (timeEvents.size() > 0) {
				lv = (ListView) findViewById(R.id.listview);
				id = lv.getCheckedItemPosition();
				dos.writeInt(id);
			} else
				dos.writeInt(-1);
			//write each item of timeEvents
			nextfiringdatemilliseconds = 0;
			for (int i = 0; i < timeEvents.size(); i++) {
				dos.writeInt(timeEvents.get(i).eventType);
				dos.writeUTF(timeEvents.get(i).description);
				dos.writeUTF(timeEvents.get(i).mediapath);
				dos.writeBoolean(timeEvents.get(i).hasmedia);
				if (timeEvents.get(i).ringtonepath != null) {
					dos.writeBoolean(true);
					dos.writeUTF(timeEvents.get(i).ringtonepath);
				} else
					dos.writeBoolean(false);
				dos.writeBoolean(timeEvents.get(i).running);
				dos.writeBoolean(timeEvents.get(i).paused);
				dos.writeBoolean(timeEvents.get(i).fired);
				if (timeEvents.get(i).eventType == TimeEvent.TIME_EVENT_COUNTDOWN) {
					dos.writeLong(timeEvents.get(i).milliseconds);
					dos.writeLong(timeEvents.get(i).millisecondsleft);
				}
				if (timeEvents.get(i).eventType == TimeEvent.TIME_EVENT_REMINDER) {
					dos.writeLong(timeEvents.get(i).milliseconds);
					dos.writeLong(timeEvents.get(i).millisecondsleft);
					dos.writeLong(timeEvents.get(i).date.getTime());
				}
				dos.writeLong(timeEvents.get(i).firingdatemilliseconds);
				if (timeEvents.get(i).firingdatemilliseconds != 0)
					if (nextfiringdatemilliseconds == 0)
						nextfiringdatemilliseconds = timeEvents.get(i).firingdatemilliseconds;
					else if (timeEvents.get(i).firingdatemilliseconds < nextfiringdatemilliseconds)
						nextfiringdatemilliseconds = timeEvents.get(i).firingdatemilliseconds;
			}
			//close file
			dos.close();
			dos = null;
		} catch (Exception e) {
			e.printStackTrace();
			if (dos != null)
				try {
					dos.close();
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
		}
	}

	protected void readTimeEvents() {
		DataInputStream dis = null;
		timeEvents.clear();
		TimeEvent timeevent;
		try {
			File file = new File(getFilesDir(), savedDataFilename);
			dis = new DataInputStream(new FileInputStream(file));
			//read number of items
			int count = dis.readInt();
			//read id of former selected item (only if count > 0)
			if (count > 0)
				savedCheckedItemId = dis.readInt();
			else
				savedCheckedItemId = -1;
			//read each item of timeEvents
			int eventType;
			String description;
			String mediapath;
			Boolean hasmedia;
			String ringtonepath;
			Boolean running;
			Boolean paused;
			Boolean fired;
			Long milliseconds;
			Long millisecondsleft;
			Long datemilliseconds;
			Date date;
			Long firingdatemilliseconds;
			for (int i = 0; i < count; i++) {
				eventType = dis.readInt();
				description = dis.readUTF();
				mediapath = dis.readUTF();
				hasmedia = dis.readBoolean();
				if (dis.readBoolean())
					ringtonepath = dis.readUTF();
				else
					ringtonepath = null;
				running = dis.readBoolean();
				paused = dis.readBoolean();
				fired = dis.readBoolean();
				if (eventType == TimeEvent.TIME_EVENT_COUNTDOWN) {
					milliseconds = dis.readLong();
					millisecondsleft = dis.readLong();
					firingdatemilliseconds = dis.readLong();
					timeevent = new TimeEvent(eventType, description, mediapath, hasmedia, ringtonepath, milliseconds / 1000, null);
					timeevent.running = running;
					timeevent.paused = paused;
					timeevent.fired = fired;
					timeevent.millisecondsleft = millisecondsleft;
					timeevent.firingdatemilliseconds = firingdatemilliseconds;
					timeEvents.add(timeevent);
				}
				if (eventType == TimeEvent.TIME_EVENT_REMINDER) {
					milliseconds = dis.readLong();
					millisecondsleft = dis.readLong();
					datemilliseconds = dis.readLong();
					firingdatemilliseconds = dis.readLong();
					date = new Date(datemilliseconds);
					timeevent = new TimeEvent(eventType, description, mediapath, hasmedia, ringtonepath, 0, date);
					timeevent.running = running;
					timeevent.paused = paused;
					timeevent.fired = fired;
					timeevent.milliseconds = milliseconds;
					timeevent.millisecondsleft = millisecondsleft;
					timeevent.firingdatemilliseconds = firingdatemilliseconds;
					timeEvents.add(timeevent);
				}
			}
			//close file
			dis.close();
			dis = null;
		} catch (Exception e) {
			e.printStackTrace();
			if (dis != null)
				try {
					dis.close();
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
		}
	}

	public void copy(File src, File dst) throws IOException {
		InputStream in = new FileInputStream(src);
		OutputStream out = new FileOutputStream(dst);
		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		in.close();
		out.close();
	}

	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		int type;
		String description;
		String mediapath;
		Boolean hasmedia;
		Boolean mediachanged;
		String ringtonepath;
		int time1;
		int time2;
		if (requestCode == 0) { //edit
			if (resultCode == RESULT_OK) {
				addTimeEventPending = false;
				editTimeEventPending = true;
				pendingTimeEvent = null;
				pendingTimeEventId = intent.getIntExtra("id", -1);
				type = intent.getIntExtra("type", TimeEvent.TIME_EVENT_COUNTDOWN);
				description = intent.getStringExtra("description");
				mediapath = intent.getStringExtra("mediapath");
				hasmedia = intent.getBooleanExtra("hasmedia", false);
				mediachanged = intent.getBooleanExtra("mediachanged", false);
				ringtonepath = intent.getStringExtra("ringtonepath");
				time1 = intent.getIntExtra("time1", 0);
				time2 = intent.getIntExtra("time2", 0);
				pendingTimeEvent = null;
				if (type == TimeEvent.TIME_EVENT_COUNTDOWN)
					pendingTimeEvent = new TimeEvent(type, description, mediapath, hasmedia, ringtonepath, (long)time1 * 60 + (long)time2, null);
				if (type == TimeEvent.TIME_EVENT_REMINDER) {
						Calendar cal = Calendar.getInstance();
						TimeZone tz = cal.getTimeZone();
						int gmtOffset = tz.getRawOffset();
						pendingTimeEvent = new TimeEvent(type, description, mediapath, hasmedia, ringtonepath, 0, new Date((long)time1 * 60 * 60 * 1000 + (long)time2 * 60 *1000 - (long)gmtOffset));
					}
				if (mediachanged) {
					if (hasmedia) {
						try {
							//file must be readable by other processes (external mediaplayer)
							File infile = new File(getFilesDir() + "/" + tempAudioFilename);
							File outfile = new File(mediapath);
							copy(infile, outfile);
							infile.delete();
							outfile.setReadable(true, false);
						} catch (IOException e) {
							e.printStackTrace();
						}
					} else {
						File file = new File(mediapath);
						file.delete();
					}
				}
			} else if (resultCode == RESULT_CANCELED) {
				// Handle cancel
				File file = new File(getFilesDir() + "/" + tempAudioFilename);
				file.delete();
			}
		}
		if (requestCode == 1) { //add
			if (resultCode == RESULT_OK) {
				addTimeEventPending = true;
				editTimeEventPending = false;
				pendingTimeEvent = null;
				pendingTimeEventId = -1;
				type = intent.getIntExtra("type", TimeEvent.TIME_EVENT_COUNTDOWN);
				description = intent.getStringExtra("description");
				mediapath = intent.getStringExtra("mediapath");
				hasmedia = intent.getBooleanExtra("hasmedia", false);
				mediachanged = intent.getBooleanExtra("mediachanged", false);
				ringtonepath = intent.getStringExtra("ringtonepath");
				time1 = intent.getIntExtra("time1", 0);
				time2 = intent.getIntExtra("time2", 0);
				pendingTimeEvent = null;
				if (type == TimeEvent.TIME_EVENT_COUNTDOWN)
					pendingTimeEvent = new TimeEvent(type, description, mediapath, hasmedia, ringtonepath, (long)time1 * 60 + (long)time2, null);
				if (type == TimeEvent.TIME_EVENT_REMINDER) {
					Calendar cal = Calendar.getInstance();
					TimeZone tz = cal.getTimeZone();
					int gmtOffset = tz.getRawOffset();
					pendingTimeEvent = new TimeEvent(type, description, mediapath, hasmedia, ringtonepath, 0, new Date((long)time1 * 60 * 60 * 1000 + (long)time2 * 60 * 1000 - (long)gmtOffset));
				}
				if (mediachanged) {
					if (hasmedia) {
						try {
							//file must be readable by other processes (external mediaplayer)
							File infile = new File(getFilesDir() + "/" + tempAudioFilename);
							File outfile = new File(mediapath);
							copy(infile, outfile);
							infile.delete();
							outfile.setReadable(true, false);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			} else if (resultCode == RESULT_CANCELED) {
				// Handle cancel
				File file = new File(getFilesDir() + "/" + tempAudioFilename);
				file.delete();
			}
		}
	}

}
