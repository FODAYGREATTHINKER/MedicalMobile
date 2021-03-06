package com.se3a04.medicalmobile;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Vector;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

public class CalendarView extends ListActivity {
	
	final ArrayList<String> currentAppointmentsList = new ArrayList<String>();
	// List view Adapter
	private ArrayAdapter<String> arrAdapter;
	private ListView lv;
	private String[] currentAppointments;
	
	private String myCurrentDate = ""; 
	private String currentUser;
	private String currentMode;
	private String targetUser;
	
	public GregorianCalendar month, itemmonth;// calendar instances.

	
	public CalendarAdapter adapter;// adapter instance
	public Handler handler;// for grabbing some event values for showing the dot
							// marker.
	public ArrayList<String> items; // container to store calendar items which
									// needs showing the event marker

	public ArrayList<AppointmentMiniInfo> userAppointments;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.calendar);
		 Locale.setDefault( Locale.US );
		month = (GregorianCalendar) GregorianCalendar.getInstance();
		itemmonth = (GregorianCalendar) month.clone();

		GregorianCalendar gregorianCalendar1=new GregorianCalendar();            
		String tempMonth=String.valueOf(1+gregorianCalendar1.get(GregorianCalendar.MONTH));            
		String tempDay=String.valueOf(gregorianCalendar1.get(GregorianCalendar.DAY_OF_MONTH));
		if(tempDay.length()==1){
			tempDay = "0" + tempDay;
		}
		if(tempMonth.length()==1){
			tempMonth = "0" + tempMonth;
		}
		String tempYear=String.valueOf(gregorianCalendar1.get(GregorianCalendar.YEAR));
		myCurrentDate = tempYear +"-"+tempMonth+"-"+tempDay;
		
		items = new ArrayList<String>();
		
		//**user setup customization
		userSetup();

		
		adapter = new CalendarAdapter(this, month);

		GridView gridview = (GridView) findViewById(R.id.gridview);
		gridview.setAdapter(adapter);

		handler = new Handler();
		handler.post(calendarUpdater);

		TextView title = (TextView) findViewById(R.id.title);
		title.setText(android.text.format.DateFormat.format("MMMM yyyy", month));

		RelativeLayout previous = (RelativeLayout) findViewById(R.id.previous);

		previous.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				setPreviousMonth();
				refreshCalendar();
			}
		});

		RelativeLayout next = (RelativeLayout) findViewById(R.id.next);
		next.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				setNextMonth();
				refreshCalendar();

			}
		});
		
		final CalendarView self = this;
		
		Button requestButton = (Button) findViewById(R.id.requestButton);
		requestButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if(!(myCurrentDate.equals(""))){
					Intent toAppointmentRequest = new Intent(getApplicationContext(),
							AppointmentRequest.class);
					toAppointmentRequest.putExtra("date", myCurrentDate);
					toAppointmentRequest.putExtra("currentUser", currentUser);
					toAppointmentRequest.putExtra("currentMode", currentMode);
					startActivityForResult(toAppointmentRequest,1);	
				}
			}	
		});
		
		gridview.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View v,
					int position, long id) {

				((CalendarAdapter) parent.getAdapter()).setSelected(v);
				String selectedGridDate = CalendarAdapter.dayString
						.get(position);
				String[] separatedTime = selectedGridDate.split("-");
				String gridvalueString = separatedTime[2].replaceFirst("^0*",
						"");// taking last part of date. ie; 2 from 2012-12-02.
				int gridvalue = Integer.parseInt(gridvalueString);
				// navigate to next or previous month on clicking offdays.
				if ((gridvalue > 10) && (position < 8)) {
					setPreviousMonth();
					refreshCalendar();
				} else if ((gridvalue < 7) && (position > 28)) {
					setNextMonth();
					refreshCalendar();
				}
				((CalendarAdapter) parent.getAdapter()).setSelected(v);
				
				myCurrentDate = selectedGridDate;
				
				currentAppointmentsList.clear();
				ArrayList<String> appts = new ArrayList<String>();
				for (int i = 0; i < userAppointments.size(); i++) {
					if(userAppointments.get(i).date.equals(selectedGridDate)){
						String temp = userAppointments.get(i).time + ": "+ userAppointments.get(i).info;
						appts.add(temp);
//						showToast(userAppointments.get(i).info);
					}
				}
				currentAppointments= appts.toArray(new String[appts.size()]);
//				showToast(currentAppointments.toString());
				lv = getListView(); // get the type of view
				currentAppointmentsList.addAll(Arrays.asList(currentAppointments));
				arrAdapter = new ArrayAdapter<String>(self, R.layout.list_item,
						R.id.product_name, currentAppointmentsList);
				setListAdapter(arrAdapter);
			}
		});
	}
	
	private void userSetup(){
		Bundle extras = getIntent().getExtras();
		if (extras != null){
			//profile accessed from a list
			targetUser = extras.getString("targetUser");
		}else{
			targetUser = "";
		}
		Button requestButton = (Button) findViewById(R.id.requestButton);
		
		if(MainMenu.sentType.equals("Secretary")){
			requestButton.setText("Create Appointment");
			currentUser = targetUser;
			currentMode = "Secretary";
		}else{
			requestButton.setText("Request Appointment");
			currentUser = MainMenu.display_name;
			currentMode = "Normal";
		}
		userAppointments = getAppointments();
	}
	
	private ArrayList<AppointmentMiniInfo> getAppointments(){
		LogIn.database.open();
		Vector<String> usernames = LogIn.database.readApptColumn("appt_username");
		Vector<String> times = LogIn.database.readApptColumn("appt_time");
		Vector<String> dates = LogIn.database.readApptColumn("appt_date");
		Vector<String> infos = LogIn.database.readApptColumn("appt_info");
		LogIn.database.close();
		ArrayList<AppointmentMiniInfo> userAppointmentsTest = new ArrayList<AppointmentMiniInfo>();
		for(int i=0; i<usernames.size();i++){
			if(usernames.get(i).equals(currentUser)){
				userAppointmentsTest.add(new AppointmentMiniInfo(dates.get(i),times.get(i),infos.get(i)));
			}
		}
		return userAppointmentsTest;
	}

	protected void setNextMonth() {
		if (month.get(GregorianCalendar.MONTH) == month
				.getActualMaximum(GregorianCalendar.MONTH)) {
			month.set((month.get(GregorianCalendar.YEAR) + 1),
					month.getActualMinimum(GregorianCalendar.MONTH), 1);
		} else {
			month.set(GregorianCalendar.MONTH,
					month.get(GregorianCalendar.MONTH) + 1);
		}

	}

	protected void setPreviousMonth() {
		if (month.get(GregorianCalendar.MONTH) == month
				.getActualMinimum(GregorianCalendar.MONTH)) {
			month.set((month.get(GregorianCalendar.YEAR) - 1),
					month.getActualMaximum(GregorianCalendar.MONTH), 1);
		} else {
			month.set(GregorianCalendar.MONTH,
					month.get(GregorianCalendar.MONTH) - 1);
		}

	}

	protected void showToast(String string) {
		Toast.makeText(this, string, Toast.LENGTH_SHORT).show();

	}

	public void refreshCalendar() {
		TextView title = (TextView) findViewById(R.id.title);

		adapter.refreshDays();
		adapter.notifyDataSetChanged();
		handler.post(calendarUpdater); // generate some calendar items

		title.setText(android.text.format.DateFormat.format("MMMM yyyy", month));
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		  if (requestCode == 1) {
			     if(resultCode == RESULT_OK){      
			 		LogIn.database.open();
					Vector<String> usernames = LogIn.database.readApptColumn("appt_username");
					Vector<String> times = LogIn.database.readApptColumn("appt_time");
					Vector<String> dates = LogIn.database.readApptColumn("appt_date");
					Vector<String> infos = LogIn.database.readApptColumn("appt_info");
					LogIn.database.close();
					ArrayList<AppointmentMiniInfo> userAppointmentsTest = new ArrayList<AppointmentMiniInfo>();
					for(int i=0; i<usernames.size();i++){
						if(usernames.get(i).equals(currentUser)){
							userAppointmentsTest.add(new AppointmentMiniInfo(dates.get(i),times.get(i),infos.get(i)));
						}
					}
					userAppointments = userAppointmentsTest ;
					refreshCalendar();       
			     }
			     if (resultCode == RESULT_CANCELED) {    
			         //Do nothing?
			     }
		  }
	}//onActivityResult

	public Runnable calendarUpdater = new Runnable() {

		@Override
		public void run() {
			items.clear();
			
			for (int i = 0; i < userAppointments.size(); i++) {
				items.add(userAppointments.get(i).date);
				items.add(userAppointments.get(i).date);
				items.add(userAppointments.get(i).date);
				items.add(userAppointments.get(i).date);
				items.add(userAppointments.get(i).date);
				items.add(userAppointments.get(i).date);
			}

			adapter.setItems(items);
			adapter.notifyDataSetChanged();
		}
	};
}
