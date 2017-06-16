package de.streblow.multitimer;

public class TimerListItem {

	private int icon1;
	private int icon2;
	private String title;
	private String time;
	
	public TimerListItem(int icon1, int icon2, String title, String time) {
		super();
		this.icon1 = icon1;
		this.icon2 = icon2;
		this.title = title;
		this.time = time;
	}

	public int getIcon1() {
		return icon1;
	}

	public void setIcon1(int icon) {
		this.icon1 = icon;
	}

	public int getIcon2() {
		return icon2;
	}

	public void setIcon2(int icon) {
		this.icon2 = icon;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}

}
