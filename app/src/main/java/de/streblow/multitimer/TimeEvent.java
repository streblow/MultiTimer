package de.streblow.multitimer;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.ToneGenerator;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;

public class TimeEvent {

	public static final int TIME_EVENT_COUNTDOWN = 0;
	public static final int TIME_EVENT_REMINDER = 1;

	public int eventType;
	public String description;
	public String mediapath;
	public Boolean hasmedia;
	public String ringtonepath;
	public Boolean running;
	public Boolean paused;
	public Boolean fired;
	public long milliseconds;
	public long millisecondsleft;
	public Date date;
	public long firingdatemilliseconds;

	public MediaPlayer mediaPlayer;
	public Boolean playing;

	public TimeEvent(int eventType, String description, String mediapath,
			Boolean hasmedia, String ringtonepath, long seconds, Date date) {
		this.eventType = eventType;
		this.description = description;
		if (description == null || description == "")
			this.description = "";
		this.mediapath = mediapath;
		if (mediapath == null)
			this.mediapath = "";
		this.hasmedia = hasmedia;
		this.ringtonepath = ringtonepath;
		this.running = false;
		this.paused = false;
		this.fired = false;
		if (eventType == TIME_EVENT_COUNTDOWN) {
			this.milliseconds = seconds * 1000;
			this.millisecondsleft = seconds * 1000;
			this.date = null;
		}
		if (eventType == TIME_EVENT_REMINDER) {
			this.milliseconds = 0;
			this.millisecondsleft = 0;
			this.date = date;
		}
		mediaPlayer = null;
		playing = false;
		firingdatemilliseconds = 0;
	}

	public void startTimer() {
		running = true;
		paused = false;
		fired = false;
		Calendar tzcal = Calendar.getInstance();
		TimeZone tz = tzcal.getTimeZone();
		int gmtOffset = tz.getRawOffset();
		Calendar now = Calendar.getInstance();
		if (eventType == TIME_EVENT_REMINDER) {
			Calendar today = Calendar.getInstance();
			today.clear();
			today.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH),now.get(Calendar.DAY_OF_MONTH));
			Calendar time = Calendar.getInstance();
			time.setTime(date);
			milliseconds = today.getTimeInMillis() + time.getTimeInMillis() - (now.getTimeInMillis() - (long)gmtOffset);
			if (milliseconds <= 0)
				milliseconds += 24 * 60 * 60 * 1000;
		}
		millisecondsleft = milliseconds;
		firingdatemilliseconds = now.getTimeInMillis() - (long)gmtOffset + milliseconds;
	}

	public void stopTimer() {
		running = false;
		paused = false;
		fired = false;
		if (eventType == TIME_EVENT_COUNTDOWN)
			millisecondsleft = milliseconds;
		if (eventType == TIME_EVENT_REMINDER) {
			milliseconds = 0;
			millisecondsleft = 0;
		}
		firingdatemilliseconds = 0;
	}

	public void pauseTimer() {
		if (eventType == TIME_EVENT_COUNTDOWN) {
			paused = true;
			firingdatemilliseconds = 0;
		}
	}

	public void resumeTimer() {
		if (eventType == TIME_EVENT_COUNTDOWN) {
			paused = false;
			Calendar tzcal = Calendar.getInstance();
			TimeZone tz = tzcal.getTimeZone();
			int gmtOffset = tz.getRawOffset();
			Calendar now = Calendar.getInstance();
			firingdatemilliseconds = now.getTimeInMillis() - (long)gmtOffset + millisecondsleft;
		}
	}

	public void fireTimer() {
		running = false;
		paused = false;
		fired = true;
		if (eventType == TIME_EVENT_COUNTDOWN)
			millisecondsleft = 0;
		if (eventType == TIME_EVENT_REMINDER) {
			milliseconds = 0;
			millisecondsleft = 0;
		}
		firingdatemilliseconds = 0;
	}

	public void playMedia() {
		if (ringtonepath == null) {
			if (!hasmedia || mediapath == "") {
				if (playing)
					return;
				playing = true;
				ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
				//TONE_CDMA_ALERT_CALL_GUARD: 3 x 1319 Hz, 125 ms on, 125 ms off (750 ms)
				toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 750);
				synchronized(toneG) {
					try {
						toneG.wait(750);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				playing = false;
			} else {
				if (playing)
					return;
				mediaPlayer = new MediaPlayer();
				try {
					mediaPlayer.setDataSource(mediapath);
					mediaPlayer.prepare();
					mediaPlayer.setOnCompletionListener(new OnCompletionListener() {
						public void onCompletion(MediaPlayer mp) {
							try {
								mp.stop();
								mp.release();
								mp = null;
							} catch (Exception e) {
								e.printStackTrace();
							}
							playing = false;
						}
					});
					playing = true;
					mediaPlayer.setVolume(1.0f, 1.0f);
					mediaPlayer.start();
				} catch (Exception e) {
					e.printStackTrace();
					mediaPlayer = null;
					playing = false;
				}
			}
		} else {
			Uri uri = Uri.parse(ringtonepath);
			Ringtone ringtone = RingtoneManager.getRingtone(MainActivity.mainActivity.getBaseContext(), uri);
			if (ringtone != null) {
				playing = true;
				MainActivity.playingRingtone = ringtone;
				ringtone.play();
			}
		}
	}

}
