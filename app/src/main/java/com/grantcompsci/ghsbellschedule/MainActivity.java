package com.grantcompsci.ghsbellschedule;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.design.widget.AppBarLayout;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.github.sundeepk.compactcalendarview.CompactCalendarView;
import com.github.sundeepk.compactcalendarview.domain.Event;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import hirondelle.date4j.DateTime;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = MainActivity.class.getSimpleName();

        /*

       *
       * Current Date = todayDate
       *        Anything that depends on today's date and time gets stored here
       *        (int, String, DateTime, Date, Calendar (?))
       *
       * Date user selects = mSelectedDate
       *        Any time user goes forward one day/backward one day or selects a date from drop-down calendar it should set mSelectedDate
       *        (int, String, DateTime, Date, Calendar (?))
       *

    */

    private static final String PREFS_FILE = "com.grantcompsci.ghsbellschedule";
    private static final String KEY_JSONSCHEDULEDATA = "key_jsonscheduldata";
    private static final String KEY_JSONPERIODSCHEDULEDATA = "key_jsonperiodscheduldata";
    private static final String KEY_DATEVERSIONCHECK = "key_dateversioncheck";
    private static final String KEY_DATEVERSION = "key_dateversion";
    private static final String KEY_VERSION = "key_version";
    private SharedPreferences.Editor mEditor;
    private SharedPreferences mSharedPreferences;
    private ScheduleType mScheduleType;
    private TextView mSummaryLabel;
    private Period [] mPeriods;
    private DividerItemDecoration mDividerItemDecoration;
    private String mJsonScheduleData;
    private String mJsonPeriodScheduleData;
    //Calendar mCalendar = Calendar.getInstance();
    private Date mTodayDate;
    //private DateTime mTodayDateTime;
    //private String mTodayDateString;
    private int mTodayDateInt;
    private CompactCalendarView mCompactCalendarView;
    private boolean isExpanded = false;
    private AppBarLayout mAppBarLayout;
    private String mOldDateStringPreScroll = "";
    private boolean mScrolled;
    private Date mSelectedDate;
    private String mSelectedDateString;
    private int mSelectedDateInt;
    private DateTime mSelectedDateTime;
    private SimpleDateFormat mYearMonthDayFormatter =  new SimpleDateFormat("yyyyMMdd");


    RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //!! Log.i(TAG,"APP STARTED ===========> ");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        final ImageView arrow = (ImageView) findViewById(R.id.date_picker_arrow);
        setTitle("Grant Bell Schedule");
        mAppBarLayout = (AppBarLayout) findViewById(R.id.app_bar_layout);
        // Set up the CompactCalendarView
        mCompactCalendarView = (CompactCalendarView) findViewById(R.id.compactcalendar_view);
        // Force English and set first day of week to Sunday
        mCompactCalendarView.setLocale(TimeZone.getDefault(), Locale.US);
        mCompactCalendarView.setFirstDayOfWeek(Calendar.SUNDAY);
       // mCompactCalendarView.setShouldDrawDaysHeader(true);

        mCompactCalendarView.setListener(new CompactCalendarView.CompactCalendarViewListener() {
            @Override
            public void onDayClick(Date dateClicked) {
                //!! Log.i(TAG,"CLICKED A DATE ===========> dateClicked = " + dateClicked);
                setSelectedDate(dateClicked);
                updateDisplay();
                ViewCompat.animate(arrow).rotation(0).start();
                mAppBarLayout.setExpanded(false, true);
                isExpanded = false;
                //mCompactCalendarView.addEvent(new Event(Color.GREEN,1488186000000L));
            }

            @Override
            public void onMonthScroll(Date firstDayOfNewMonth) {
                /* -When the month scrolls we want to select the same day in the next/past month
                   -e.g. February 12th scrolls to March 12th, January 12th, etc.
                   -If we're scrolling from a month that has 31 days to a month that has 30 days we automatically select the last day of the month
                   -if we're scrolling to February when the date is the 29th or 30th we'll automatically select the 28th (or 29th on a leap year)
                */
                DateTime dateTime = new DateTime(new SimpleDateFormat("yyyy-MM-dd").format(firstDayOfNewMonth));
                int dateOffset = ((int) mSelectedDateTime.getDay())-1;
                //Log.i(TAG,"SCROLLED ===========> dateTime = " + dateTime.getMonth());

                if (mSelectedDateTime.getDay() == 31){
                    dateTime = dateTime.getEndOfMonth();
                }
                else if (dateTime.getMonth()==2){
                    //Log.i(TAG,"SCROLLED ===========> dateTime = " + dateTime.getDay());
                    if (mSelectedDateTime.getDay() > 28){
                        dateTime = dateTime.getEndOfMonth();
                    }
                    else{
                        dateTime = dateTime.plusDays(dateOffset);
                        //dateTime = dateTime.getEndOfMonth();
                    }
                }
                else{
                    dateTime = dateTime.plusDays(dateOffset);
                }
                //!! Log.i(TAG,"SCROLLED AND SET DATE ===========>  new date: "+ dateTime.format("D MMMM YYYY", Locale.US));
                setSubtitle(dateTime.format("D MMMM YYYY", Locale.US));
                mScrolled = true;
            }
        });


        // Set current date/time to today/now
        setSelectedDate(new Date());



        RelativeLayout datePickerButton = (RelativeLayout) findViewById(R.id.date_picker_button);

        datePickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView datePickerTextViewGetter = (TextView) findViewById(R.id.date_picker_text_view);
                if (isExpanded) {
                    ViewCompat.animate(arrow).rotation(0).start();
                    if (mScrolled==true){
                        if (mSelectedDate != null){
                            setSelectedDate(mSelectedDate);
                            setSubtitle(mOldDateStringPreScroll);
                        }
                        else{
                            setSelectedDate(new Date());
                        }
                        mScrolled = false;
                    }
                    mAppBarLayout.setExpanded(false, true);
                    isExpanded = false;
                } else {
                    mOldDateStringPreScroll = (String) datePickerTextViewGetter.getText();
                    ViewCompat.animate(arrow).rotation(180).start();
                    mAppBarLayout.setExpanded(true, true);
                    isExpanded = true;
                }
            }
        });

        ImageView leftArrow = (ImageView) findViewById(R.id.leftArrow);

        leftArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String yesterdayDateString = ((mSelectedDateInt -1) + "");
                Date yesterdayDate = mSelectedDate;
                try {
                    yesterdayDate = mYearMonthDayFormatter.parse(yesterdayDateString);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                setSelectedDate(yesterdayDate);
                mOldDateStringPreScroll = mSelectedDateTime.format("D MMMM YYYY", Locale.US);
                //!! Log.i(TAG,"CLICKED LEFT ARROW ===========> ");
                updateDisplay();
            }
        });

        ImageView rightArrow = (ImageView) findViewById(R.id.rightArrow);

        rightArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String tomorrowDateString = ((mSelectedDateInt + 1) + "");
                Date tomorrowDate = mSelectedDate;
                try {
                    tomorrowDate = mYearMonthDayFormatter.parse(tomorrowDateString);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                setSelectedDate(tomorrowDate);
                mOldDateStringPreScroll = mSelectedDateTime.format("D MMMM YYYY", Locale.US);
                //!! Log.i(TAG,"CLICKED RIGHT ARROW ===========> ");
                updateDisplay();
            }
        });

        // the TextViews (and other visible stuff) aren't created until setContentView is called.  If you you try to do stuff like below before setContentView you end up with a null object reference
        mSummaryLabel = (TextView)findViewById(R.id.scheduleTypeLabel);
        mSummaryLabel.setText(" LOADING ");


    }


    /* I put all of this code in onResume because there's no guarnatee that the app will get destroyed.
    If app stays in memory overnight I want it to check for a new version file the next day, which means all of this needs to be in onResume (right?)

    onResume we now:
        1) Get current date and time (should be ints?)
        2) Check to see if we've ever done a version check, our version check was before today, or it's befor 8:15am
            I assume snow days/late start announced before 8:15, so we want to check for new versions whenever app is loaded before that time
        3) Check for network availability
        4) If network available, download version JSON
        5) If version JSON newer than one stored in SharedPrefs

     */
    @Override
    protected void onResume() {

        super.onResume();
        setTodayDate(new Date());
        setSelectedDate(new Date());
        checkCalendarUpdates();
        updateDisplay();

    }


    public void setTodayDate(Date date) {
        mTodayDate = date;
        mTodayDateInt = Integer.parseInt(new SimpleDateFormat("yyyyMMdd").format(mTodayDate));
    }

    public void setSelectedDate(Date date) {
        setSubtitle(new SimpleDateFormat("d MMMM yyyy", /*Locale.getDefault()*/Locale.ENGLISH).format(date));
        if (mCompactCalendarView != null) {
            mCompactCalendarView.setCurrentDate(date);

            mSelectedDate = date;
            mSelectedDateTime = new DateTime (new SimpleDateFormat("yyyy-MM-dd").format(mSelectedDate));
            mSelectedDateString = new SimpleDateFormat("yyyyMMdd").format(mSelectedDate);
            mSelectedDateInt = Integer.parseInt(new SimpleDateFormat("yyyyMMdd").format(mSelectedDate));
        }

    }


    @Override
    public void setTitle(CharSequence title) {
        TextView tvTitle = (TextView) findViewById(R.id.title);

        if (tvTitle != null) {
            tvTitle.setText(title);
        }
    }

    public void setSubtitle(String subtitle) {
        TextView datePickerTextView = (TextView) findViewById(R.id.date_picker_text_view);

        if (datePickerTextView != null) {
            datePickerTextView.setText(subtitle);
        }
    }

    private void checkCalendarUpdates(){
        /*  Schedule information exists in three files:
            - scheduleFile - An imported Google Calendar ics file converted to json.  Contains a list of dates and a SUMMARY with schedule type (A, B, A-FLEX, EARLY RELEASE, etc)
            - periodScheduleFile - Manually created json file that maps the schedule type to a list of period NAMEs with their corresponding START/END times
            - versionFile holds DATE of the most recent version and the VERSION of the update (so that multiple revisions can be published in a single day)

            This method checks to see if we have an up-to-date version of the schedule.. It does so as follows:

            1) Checks the version file on the server against the version we have in SharedPrefs if we've never done a download, haven't done a download since yesterday, or it's before 8:15am
            2) If the DATE/VERSION on the server are greater than the one we've got in SharedPrefs we download the SchedulePeriod data and store it in shared Prefs

             AB Calendar grabbed from here: https://calendar.google.com/calendar/embed?src=u2r5154prrjr5uhukp71857g70%40group.calendar.google.com&ctz=America/Los_Angeles%22


         */
        String siteName = "http://www.grantcompsci.com/bellapp/";
        String scheduleFile = "schoolYearSchedule.json";
        String periodScheduleFile = "periodSchedule.json";
        String versionFile = "versionNumber.json";
        final String scheduleUrl= siteName + scheduleFile;
        final String periodUrl = siteName+periodScheduleFile;
        final String versionUrl = siteName+versionFile;
        mSharedPreferences = getSharedPreferences(PREFS_FILE,Context.MODE_PRIVATE);
        mEditor = mSharedPreferences.edit();
        Calendar calendar = Calendar.getInstance();
        int currentTime = Integer.parseInt(new SimpleDateFormat("Hmm").format(calendar.getTime()));


        //!! Log.i(TAG,"Our time right now is ===========> " + currentTime);
        //!! Log.i(TAG,"The last time we checked the version was ===========> " + mSharedPreferences.getInt(KEY_DATEVERSIONCHECK,0) + " AND today's date = " + mTodayDateInt);


        // mTodayDateInt (int) used to check to see if we've checked for schedule version updates today.
        // currentTime (int) used to see if it's before 8:15am, if it is we check for version updates even if we've checked already (weather-related late start change)


        if (mSharedPreferences.getInt(KEY_DATEVERSIONCHECK,0)==0 || mTodayDateInt > mSharedPreferences.getInt(KEY_DATEVERSIONCHECK,0)  || currentTime < 815 )  {
            //!! Log.i(TAG,"Looks like we either haven't checked the version at all or haven't checked it since yesterday ===========> ");
            // if we have a working network connection we run our code, if not we pop up a toast message saying we don't have one right now.
            // TODO: What to do if no network? Add a refresh button? Automatically attempt every 5 minutes until we get a network connection (battery issues).
            if (isNetworkAvailable()) {


                //Create OkHttp client and build the request
                final  OkHttpClient client = new OkHttpClient();


                Request versionRequest = new Request.Builder()
                        .url(versionUrl) //the location for the file that holds most recent version date/number
                        .build();


                Call versionCall = client.newCall(versionRequest);
                versionCall.enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        // I imagine we want to be doing something here.  What are best practices for failure?
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        String versionJsonData = response.body().string();
                        if (response.isSuccessful()){
                            boolean latestVersion = false;
                            try {
                                // Check to see if we have an older version than the most recent published version
                                latestVersion = checkVersionUpdate(versionJsonData);
                                if (!latestVersion) {
                                    Request request = new Request.Builder()
                                            .url(scheduleUrl)
                                            .build();

                                    // Creates a call based on the built request
                                    Call schedCall = client.newCall(request);
                                    // Execute asynchronous call in order received in queue (don't interfere with main (UI) thread
                                    // Callback pings us when async task is running in background to display stuff
                                    schedCall.enqueue(new Callback() {
                                        @Override
                                        public void onFailure(Call call, IOException e) {

                                        }
                                        // when we get a response save it to a String called jsonData,
                                        // if response is successful run the getCurrentDetails method and save the result to mScheduleType

                                        // TODO: I'm not doing anything with mScheduleType right now.  Need a refresher on how to deal with it.


                                        @Override
                                        public void onResponse(Call call, Response response) throws IOException {
                                            try {
                                                String jsonData = response.body().string();
                                                mJsonScheduleData = jsonData; // Store in shared prefs, then pull it back out of shared prefs and use it in getCurrentDetails
                                                mEditor.putString(KEY_JSONSCHEDULEDATA, mJsonScheduleData);
                                                mEditor.apply();

                                                if (response.isSuccessful()) {
                                                    mScheduleType = getCurrentDetails(jsonData);
                                                    Request periodRequest = new Request.Builder()
                                                            .url(periodUrl)
                                                            .build();
                                                    Call periodCall = client.newCall(periodRequest);
                                                    periodCall.enqueue(new Callback() {

                                                        @Override
                                                        public void onFailure(Call call, IOException e) {


                                                        }

                                                        @Override
                                                        public void onResponse(Call call, Response response) throws IOException {
                                                            String periodScheduleData = response.body().string();
                                                            mJsonPeriodScheduleData = periodScheduleData;
                                                            mEditor.putString(KEY_JSONPERIODSCHEDULEDATA, mJsonPeriodScheduleData);
                                                            mEditor.apply();
                                                            if (response.isSuccessful()){
                                                                try {
                                                                     /*mPeriod = getDailySchedule(mScheduleType, periodScheduleData);*/
                                                                    mPeriods = getPeriodSchedule(periodScheduleData);


                                                                    runOnUiThread(new Runnable() {
                                                                        @Override
                                                                        public void run() {
                                                                            PeriodAdapter adapter = new PeriodAdapter(mPeriods);
                                                                            adapter.setDateNumber(mSelectedDateInt);
                                                                            adapter.setTodayDateNumber(mTodayDateInt);
                                                                            //!! Log.i(TAG,"UPDATED DISPLAY AFTER UPDATING SCHEDULE DATA (DISPLAY SCHEDULE METHOD) using this date: ----------->" + mTodayDateInt );
                                                                            RecyclerView mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);
                                                                            mDividerItemDecoration = new DividerItemDecoration(mRecyclerView.getContext(),1);
                                                                            mRecyclerView.addItemDecoration(mDividerItemDecoration);
                                                                            mRecyclerView.setAdapter(adapter);
                                                                            mRecyclerView.setLayoutManager(layoutManager);

                                                                        }
                                                                    });


                                                                } catch (JSONException e) {
                                                                    //!! Log.e(TAG, "Exception caught: ", e);
                                                                }
                                                            }
                                                        }
                                                    });


                                                    // You have to update the UI on the UI thread, otherwise you get a "CalledFromWrongThreadException"
                                                    runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            mSummaryLabel.setText(mScheduleType.getScheduleType());

                                                        }
                                                    });

                                                } else {
                                                    // Log.i(TAG,"FAILURE 999999999999999999999999");

                                                }
                                            }
                                            catch (IOException e) {
                                                //!! Log.e(TAG, "Exception caught: ", e);
                                            }
                                            catch (JSONException e){
                                                //!! Log.e(TAG, "Exception caught: ", e);
                                            }
                                        }
                                    });


                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });


            }

            else {
                //potentially can make the toast persist for more than 3 seconds by using "The best solution" on http://stackoverflow.com/questions/2220560/can-an-android-toast-be-longer-than-toast-length-long
                //TODO: Figure out how long to make this message.  Don't know that students even need to see it.
                Toast.makeText(this, R.string.network_unavailable_message,
                        Toast.LENGTH_LONG).show();

            }


            // We execute the call, and log to Logcat (presumably?) if successful, catch if exception and print to console

        }

        else{
            //Log.i(TAG,"Looks like we've already checked today and it's after 8:15a ===========> ");
        }
        
        //!! Log.i(TAG,"WE'RE RUNNING AFTER SHAREDPREFS GET LOADED ===========> ");
        //updateDisplay();


    }

    private void updateDisplay(){
        //!! Log.i(TAG,"BEGINNING TO UPDATE DISPLAY ===========> " + mSelectedDate + " " + mTodayDateInt);
        if (mSharedPreferences != null){
            mSharedPreferences = getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
            mEditor = mSharedPreferences.edit();
            if (mSharedPreferences.getString(KEY_JSONSCHEDULEDATA,"")!="" ){
                mCompactCalendarView.removeAllEvents();
                String jsonData = mSharedPreferences.getString(KEY_JSONSCHEDULEDATA, "");
                try {
                    mScheduleType = getCurrentDetails(jsonData);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            if (mSharedPreferences.getString(KEY_JSONPERIODSCHEDULEDATA,"")!="" ){
                String periodScheduleData = mSharedPreferences.getString(KEY_JSONPERIODSCHEDULEDATA, "");
                try {
                    mPeriods = getPeriodSchedule(periodScheduleData);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

        }
        if (mPeriods!=null){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mSummaryLabel.setText(mScheduleType.getScheduleType());
                    PeriodAdapter adapter = new PeriodAdapter(mPeriods);;
                    adapter.setDateNumber(mSelectedDateInt);
                    adapter.setTodayDateNumber(mTodayDateInt);
                    //!! Log.i(TAG,"UPDATED DISPLAY IN UPDATEDISPLAY METHOD ----------->" );
                    RecyclerView mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);
                    // Weird error (probably my fault) that seems to cause relativelayout in recyclerview to increase in size by 1 pixel every time we refresh.
                    //mDividerItemDecoration = new DividerItemDecoration(mRecyclerView.getContext(),1);
                    //mRecyclerView.addItemDecoration(mDividerItemDecoration);
                    mRecyclerView.addItemDecoration(new CustomDividerItemDecoration(mRecyclerView.getContext()));
                    mRecyclerView.setAdapter(adapter);
                    mRecyclerView.setLayoutManager(layoutManager);


                }
            });
        }
    }



    private ScheduleType getCurrentDetails(String jsonData) throws JSONException {
        JSONObject calendarSchedule = new JSONObject(jsonData);
        ScheduleType scheduleType = new ScheduleType();
        boolean matchFound = false;
/*        if (mSharedPreferences!=null && !mCompactCalendarView.equals(null)) {

        }*/
        /*
        First we get the JSONArray using schedule
        Then we iterate through the array using a for loop looking for today's date
                if we match date matchFound = true and setScheduleType to SUMMARY value for match
                if matchFound is false (after loop's end) setScheduleType to "NO SCHOOL"
        When we find it we use scheduleType to set scheduleType to our schedule type ("B-FLEX", etc).

        TODO:  I had to manually edit data, but we might need to replace calendar later.  Better to use default iCal download, right?
         */
        //String scheduleSummary;
        JSONArray calendarScheduleArray = calendarSchedule.getJSONArray("VEVENT");
        for (int i = 0; i < calendarScheduleArray.length(); i++) {
            JSONObject calendarScheduleDayObject = calendarScheduleArray.getJSONObject(i);
            if (calendarScheduleDayObject.getString("DTSTART;VALUE=DATE").equals(mSelectedDateString)){
              //scheduleSummary = calendarScheduleDayObject.getString("SUMMARY");
                //!! Log.i(TAG,"SCHEDULE TYPE FOR THIS DATE ===========> " + scheduleSummary);
                scheduleType.setScheduleType(calendarScheduleDayObject.getString("SUMMARY"));
                matchFound = true;
            }
            if (calendarScheduleDayObject.getString("SUMMARY").equals("NO SCHOOL") || calendarScheduleDayObject.getString("SUMMARY").equals("CONFERENCES")){
                Date eventDate = new Date();
                String eventDateString = calendarScheduleDayObject.getString("DTSTART;VALUE=DATE");
                try {
                    eventDate = new SimpleDateFormat("yyyyMMdd").parse(eventDateString);
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                mCompactCalendarView.addEvent(new Event(Color.WHITE,eventDate.getTime()));


            }

        }

        if(!matchFound){
            scheduleType.setScheduleType(getString(R.string.no_school));
        }



        return scheduleType;
    }
    /*
        We load the A/B schedule and the period schedule into shared prefs. We want to be able to change those schedules without an app update,
        so we check a version file to see if we need to update.

        The version file contains two fields, one for date and one for version. It looks something like this:

        DATE: 20161117
        VERSION: 01

        We do an update (return false) under the following conditions:

        1) The versionDate  in SharedPrefs is less than the date in the version file
        2) The versionVersion in SharedPrefs is less than the version in the version file

        If either one of the above conditions is true we update sharedPrefs with the latest versionDate and versionVersion before downloading the new A/B and period schedules.
        Then we return false.

        If neither are true we return true and no update is performed.
     */





    private boolean checkVersionUpdate(String jsonData) throws JSONException{
        boolean isMostRecent;
        JSONObject versionObject = new JSONObject(jsonData);
        int versionDateObject = Integer.parseInt(versionObject.getString("DATE"));  // date of the most recent version update
        int versionVersionObject = Integer.parseInt(versionObject.getString("VERSION")); // version number of most recent update
        //!! Log.i(TAG,"WHAT ARE WE PUTTING IN DATEVERSIONCHECK?  SHOULD BE TODAY'S DATE ===========> " + mTodayDateInt);
        mEditor.putInt(KEY_DATEVERSIONCHECK, mTodayDateInt);
        mEditor.apply();

        if (mSharedPreferences.getInt(KEY_DATEVERSION,0)==0 || versionDateObject > mSharedPreferences.getInt(KEY_DATEVERSION,0)){

            mEditor.putInt(KEY_DATEVERSION, versionDateObject);
            mEditor.putInt(KEY_VERSION, versionVersionObject);
            mEditor.apply();
            isMostRecent = false;
        }

        else if (versionDateObject == mSharedPreferences.getInt(KEY_DATEVERSION,0)){
            if (mSharedPreferences.getInt(KEY_VERSION,0)==0 || versionVersionObject > mSharedPreferences.getInt(KEY_VERSION,0)){
                mEditor.putInt(KEY_DATEVERSION, versionDateObject);
                mEditor.putInt(KEY_VERSION, versionVersionObject);
                mEditor.apply();
                //!! Log.i(TAG,"SERVER VERSION DATE SAME AS LOCAL, VERSION NUMBER GREATER ===========> "+ mSharedPreferences.getInt(KEY_DATEVERSION,0));
                isMostRecent = false;
            }
            //Server version date and version number both match
            else{
                isMostRecent=true;
            }
        }
        //server version date is less than the date in SharedPrefs (this shouldn't happen)
        else{
            isMostRecent=true;
        }
        return isMostRecent;

    }





    private Period[] getPeriodSchedule(String jsonData) throws JSONException {
        JSONObject periodBells = new JSONObject(jsonData);
        Period[] periods;
        // We whitelist ScheduleTypes because the data is messy and hunting down all the outliers is more of a pain than it's worth.  I may regret this later.
        if (mScheduleType.getScheduleType().equals("A") ||
                mScheduleType.getScheduleType().equals("B") ||
                mScheduleType.getScheduleType().equals("A-FLEX") ||
                mScheduleType.getScheduleType().equals("B-FLEX") ||
                mScheduleType.getScheduleType().equals("B-RACEFORWARD") ||
                mScheduleType.getScheduleType().equals("A-RACEFORWARD") ||
                mScheduleType.getScheduleType().equals("FINALS-1") ||
                mScheduleType.getScheduleType().equals("FINALS-2") ||
                mScheduleType.getScheduleType().equals("FINALS-3") ||
                mScheduleType.getScheduleType().equals("SPECIAL") || // Putting this here so that I can be flexible when there's some sort of one-off schedule
                mScheduleType.getScheduleType().equals("A-ACT") ||
                mScheduleType.getScheduleType().equals("B-PSAT") ||
                mScheduleType.getScheduleType().equals("A-LATE START") ||
                mScheduleType.getScheduleType().equals("B-LATE START") ||
                mScheduleType.getScheduleType().equals("A-EARLY DISMISSAL") ||
                mScheduleType.getScheduleType().equals("B-EARLY DISMISSAL")){
                    JSONArray periodScheduleArray = periodBells.getJSONArray(mScheduleType.getScheduleType());
                    periods = new Period[periodScheduleArray.length()];


                    for (int i = 0; i < periodScheduleArray.length(); i++) {
                        JSONObject jsonPeriod = periodScheduleArray.getJSONObject((i));
                        Period period = new Period();
                        period.setPeriodName(jsonPeriod.getString("NAME"));
                        period.setPeriodStart(jsonPeriod.getString("START"));
                        period.setPeriodEnd(jsonPeriod.getString("END"));

                        periods[i]=period;

            }
        }

        else{
            JSONArray periodScheduleArray = periodBells.getJSONArray("NO-SCHOOL");
            periods = new Period[periodScheduleArray.length()];

            for (int i = 0; i < periodScheduleArray.length(); i++) {
                JSONObject jsonPeriod = periodScheduleArray.getJSONObject((i));
                Period period = new Period();
                period.setPeriodName(jsonPeriod.getString("NAME"));
                period.setPeriodStart(jsonPeriod.getString("START"));
                period.setPeriodEnd(jsonPeriod.getString("END"));

                periods[i] = period;
            }

        }


        return periods;

    }






    // Check for internet connection before we try to get today's schedule
    private boolean isNetworkAvailable() {
        ConnectivityManager manager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        boolean isAvailable=false;
        if (networkInfo !=null && networkInfo.isConnected()){
            isAvailable = true;
        }

        return isAvailable;

    }

}
