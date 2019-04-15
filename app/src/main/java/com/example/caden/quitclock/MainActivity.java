package com.example.caden.quitclock;

import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.DragEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private long startTime;
    private SharedPreferences startTimePref;

    private ArrayList<String> locationList = new ArrayList<>();
    private String selectedLocation;

    private Boolean pickersEnabled = true;

    long timerSeconds = 0;

    private Boolean runThread = true;
    private Boolean firstLoad = true;

    private Handler countdownHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            final TextView timer = findViewById(R.id.timer);

            Bundle timerBundle = msg.getData();

            timer.setText(timerBundle.getString("Time"));
        }
    };

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

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.locations, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        locationSpinner.setAdapter(adapter);

        setUpPickers();

        locationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        lightUpButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                if (!pickersEnabled) {
                    startTime = System.nanoTime();
                    SharedPreferences.Editor startTimePrefsEditor = startTimePref.edit();
                    startTimePrefsEditor.putLong("startTime", startTime);
                    startTimePrefsEditor.apply();

                    if (firstLoad) {
                        new Thread(countdownRunnable).start();
                    }
                }
            }
        });

        lockButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                Button lockButton  = findViewById(R.id.btn_lock_number_pickers);
                NumberPicker hourPicker = findViewById(R.id.nbp_hour);
                NumberPicker minutePicker = findViewById(R.id.nbp_minute);
                if (pickersEnabled) {
                    pickersEnabled = false;
                    hourPicker.setEnabled(false);
                    minutePicker.setEnabled(false);
                    lockButton.setText("L");
                    timerSeconds = (hourPicker.getValue() * 3600) + (minutePicker.getValue() * 60);
                    //change icon
                } else {
                    pickersEnabled = true;
                    hourPicker.setEnabled(true);
                    minutePicker.setEnabled(true);
                    lockButton.setText("U");
                    //change icon
                }
            }
        });

        statisticsButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {

            }
        });
    }

    public void setUpPickers() {
        NumberPicker hourPicker = findViewById(R.id.nbp_hour);
        hourPicker.setMaxValue(23);
        hourPicker.setMinValue(0);

        NumberPicker minutePicker = findViewById(R.id.nbp_minute);
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

    private Runnable countdownRunnable = new Runnable() {
        @Override
        public void run() {
            while (runThread){
                Message timerMessage = Message.obtain();
                Bundle timerBundle = new Bundle();
                timerBundle.putString("Time", getTimeDisplayValue());
                timerMessage.setData(timerBundle);

                countdownHandler.sendMessage(timerMessage);
            }

        }
    };

    public String getTimeDisplayValue(){

        long elapsedSeconds = (System.nanoTime() - startTime) / 1000000000;

        long hour;
        long minute;
        long second;

        if (timerSeconds > elapsedSeconds){
            //turn view red
            long remainingSeconds = timerSeconds - elapsedSeconds;
            hour = TimeUnit.SECONDS.toHours(remainingSeconds);
            minute = TimeUnit.SECONDS.toMinutes(remainingSeconds) % 60;
            second = remainingSeconds % 60;
        } else {
            //turn view green
            long extraSeconds = elapsedSeconds - timerSeconds;
            hour = TimeUnit.SECONDS.toHours(extraSeconds);
            minute = TimeUnit.SECONDS.toMinutes(extraSeconds) % 60;
            second = extraSeconds % 60;
        }

        return (String.valueOf(String.format(
                Locale.US, "%01d:%02d:%02d", hour, minute, second)));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!pickersEnabled) {
            new Thread(countdownRunnable).start();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        runThread = false;
    }
}
