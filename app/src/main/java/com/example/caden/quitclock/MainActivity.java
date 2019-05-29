package com.example.caden.quitclock;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    Statistics statistics = new Statistics(this);
    ManageLacations manageLacations = new ManageLacations(this);

    private long startTime;
    private SharedPreferences startTimePref;

    private Boolean pickersEnabled = true;

    long timerSeconds = 0;
    long tempTimerSeconds = 0;

    private boolean runThread;

    private Handler countdownHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            final TextView timer = findViewById(R.id.timer);

                Bundle timerBundle = msg.getData();

                timer.setText(timerBundle.getString("Time"));
        }
    };

    //Initialises shared preferences and listeners.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Spinner locationSpinner = findViewById(R.id.spn_locations);
        final Button lightUpButton = findViewById(R.id.btn_light_up);
        final Button lockButton = findViewById(R.id.btn_lock_number_pickers);
        final Button statisticsButton = findViewById(R.id.btn_info);

        startTimePref = getSharedPreferences("startTime", MODE_PRIVATE);
        startTime = startTimePref.getLong("startTime", 0);

        manageLacations.loadLocationPrefs();
        manageLacations.updateLocationSettings();

        setUpPickers();

        //Handles spinner item changes.
        locationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                manageLacations.spinnerSelection(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        //Determines the conditions cigarettes were lit under, and reports to statistics.
        lightUpButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                if (!pickersEnabled) {
                    long elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000;
                    if (startTime == 0) {
                        saveStartTime(System.currentTimeMillis());
                        statistics.addCigaretteSmoked(startTime);
                        statistics.addLocationTime(manageLacations.getSelectedLocation(), startTime);
                        runThread = true;
                        new Thread(countdownRunnable).start();
                    } else if (timerSeconds > elapsedSeconds) {
                        AlertDialog.Builder warningBuilder = new AlertDialog.Builder(MainActivity.this);
                        warningBuilder.setTitle(
                                "Your time isn't up yet. Are you sure you want to light up?");
                        warningBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                saveStartTime(System.currentTimeMillis());
                                statistics.addPrematureSmoke(startTime);
                                statistics.addCigaretteSmoked(startTime);
                                statistics.addLocationTime(manageLacations.getSelectedLocation(), startTime);
                                statistics.addExtraMinutes(0, startTime);
                            }
                        });
                        warningBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                        warningBuilder.show();
                    } else {
                        saveStartTime(System.currentTimeMillis());
                        statistics.addCigaretteSmoked(startTime);
                        statistics.addLocationTime(manageLacations.getSelectedLocation(), startTime);
                        long extraSeconds = elapsedSeconds - timerSeconds;
                        int extraMinutes = (int) TimeUnit.SECONDS.toMinutes(extraSeconds);
                        statistics.addExtraMinutes(extraMinutes, startTime);
                    }
                }
            }
        });

        //Locks or unlocks number pickers to change timer intervals.
        lockButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                final Button lockButton  = findViewById(R.id.btn_lock_number_pickers);
                final NumberPicker hourPicker = findViewById(R.id.nbp_hour);
                final NumberPicker minutePicker = findViewById(R.id.nbp_minute);
                if (pickersEnabled) {
                    long newTimerSeconds = (hourPicker.getValue() * 3600) + (minutePicker.getValue() * 60);
                    if (newTimerSeconds < tempTimerSeconds) {
                        AlertDialog.Builder warningBuilder = new AlertDialog.Builder(MainActivity.this);
                        warningBuilder.setTitle(
                                "Are you sure you want to reduce the timer for this location?");
                        warningBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                manageLacations.setLocationTime();
                                statistics.addTimerTurnDown(System.currentTimeMillis());
                            }
                        });
                        warningBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                        warningBuilder.show();
                    } else {
                        manageLacations.setLocationTime();
                    }
                } else {
                    tempTimerSeconds = (hourPicker.getValue() * 3600) + (minutePicker.getValue() * 60);
                    pickersEnabled = true;
                    hourPicker.setEnabled(true);
                    minutePicker.setEnabled(true);
                    lockButton.setBackgroundResource(R.drawable.round_lock_open_black_36);
                }
            }
        });

        //Launches statistics screen.
        statisticsButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, StatisticsActivity.class);
                startActivity(i);
            }
        });
    }

    //Changes start time and saves corresponding shared preference.
    public void saveStartTime(long time){
        startTime = time;
        SharedPreferences.Editor startTimePrefsEditor = startTimePref.edit();
        startTimePrefsEditor.putLong("startTime", startTime);
        startTimePrefsEditor.apply();
    }

    //Initializes number picker settings.
    public void setUpPickers() {
        final NumberPicker hourPicker = findViewById(R.id.nbp_hour);
        hourPicker.setMaxValue(23);
        hourPicker.setMinValue(0);

        final NumberPicker minutePicker = findViewById(R.id.nbp_minute);
        minutePicker.setMaxValue(59);
        minutePicker.setMinValue(0);
        minutePicker.setFormatter(new NumberPicker.Formatter() {
            @Override
            public String format(int i) {
                return String.format(Locale.US, "%02d", i);
            }
        });

        if (!pickersEnabled) {
            hourPicker.setEnabled(false);
            minutePicker.setEnabled(false);
        }
    }

    //Sets instructions for background thread to update timer.
    private Runnable countdownRunnable = new Runnable() {
        @Override
        public void run() {
            while (runThread){
                Message timerMessage = Message.obtain();
                Bundle timerBundle = new Bundle();
                if (findViewById(R.id.timer) != null) {
                    timerBundle.putString("Time", getTimeDisplayValue());
                    timerMessage.setData(timerBundle);

                    countdownHandler.sendMessage(timerMessage);
                }
            }
        }
    };

    //Returns a formatted string to display as the timer.
    public String getTimeDisplayValue(){
         final TextView timerDisplay = findViewById(R.id.timer);

        long elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000;

        long hour;
        long minute;
        long second;

        if (timerSeconds > elapsedSeconds){
            timerDisplay.setTextColor(getResources().getColor(R.color.colorRed));
            long remainingSeconds = timerSeconds - elapsedSeconds;
            hour = TimeUnit.SECONDS.toHours(remainingSeconds);
            minute = TimeUnit.SECONDS.toMinutes(remainingSeconds) % 60;
            second = remainingSeconds % 60;
        } else {
            timerDisplay.setTextColor(getResources().getColor(R.color.colorGreen));
            long extraSeconds = elapsedSeconds - timerSeconds;
            hour = TimeUnit.SECONDS.toHours(extraSeconds);
            minute = TimeUnit.SECONDS.toMinutes(extraSeconds) % 60;
            second = extraSeconds % 60;
        }

        return (String.valueOf(String.format(
                Locale.US, "%01d:%02d:%02d", hour, minute, second)));
    }

    public void setPickersEnabled(Boolean value){
        pickersEnabled = value;
    }

    public void setTimerSeconds(long seconds){
        timerSeconds = seconds;
    }

    public void setTempTimerSeconds(long seconds){
        tempTimerSeconds = seconds;
    }

    //Starts timer thread and initializes statistics.
    @Override
    public void onResume() {
        super.onResume();
        if (startTime != 0) {
            runThread = true;
            new Thread(countdownRunnable).start();
            statistics.loadStats();
        }
    }

    //Ends thread and saves statistics.
    @Override
    public void onPause() {
        super.onPause();
        runThread = false;
        statistics.saveStats();
    }
}
