package de.streblow.multitimer;

import java.util.ArrayList;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class TimerListAdapter extends ArrayAdapter<TimerListItem> {

	private final Context context;
	private final ArrayList<TimerListItem> timerlistitemsArrayList;

	public TimerListAdapter(Context context, ArrayList<TimerListItem> timerlistitemsArrayList) {
		super(context, R.layout.timerlistitem, timerlistitemsArrayList);
		this.context = context;
		this.timerlistitemsArrayList = timerlistitemsArrayList;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// 1. Create inflater 
		LayoutInflater inflater = (LayoutInflater) context
		.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		// 2. Get rowView from inflater
		View rowView = null;
		rowView = inflater.inflate(R.layout.timerlistitem, parent, false);
		// 3. Get icon1, icon2, title & time views from the rowView
		ImageView imgView1 = (ImageView) rowView.findViewById(R.id.item_icon1); 
		ImageView imgView2 = (ImageView) rowView.findViewById(R.id.item_icon2); 
		TextView titleView = (TextView) rowView.findViewById(R.id.item_title);
		TextView counterView = (TextView) rowView.findViewById(R.id.item_time);
		// 4. Set the text for textView 
		imgView1.setImageResource(timerlistitemsArrayList.get(position).getIcon1());
		imgView2.setImageResource(timerlistitemsArrayList.get(position).getIcon2());
		titleView.setText(timerlistitemsArrayList.get(position).getTitle());
		counterView.setText(timerlistitemsArrayList.get(position).getTime());
		// 5. return rowView
		return rowView;
	}

}
