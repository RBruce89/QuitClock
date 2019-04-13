package com.example.caden.quitclock;

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

    private ArrayList<String> locationList = new ArrayList<>();
    private String selectedLocation;

    private Boolean pickersEnabled = false;

    private Boolean runThread = true;

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
                startTime = System.nanoTime();
                new Thread(countdownRunnable).start();
            }
        });

        lockButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                NumberPicker hourPicker = findViewById(R.id.nbp_hour);
                NumberPicker minutePicker = findViewById(R.id.nbp_minute);
                if (pickersEnabled) {
                    pickersEnabled = false;
                    hourPicker.setEnabled(false);
                    minutePicker.setEnabled(false);
                    //change icon
                } else {
                    pickersEnabled = true;
                    hourPicker.setEnabled(true);
                    minutePicker.setEnabled(true);
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
        final NumberPicker selectedHours = findViewById(R.id.nbp_hour);
        final NumberPicker selectedMinutes = findViewById(R.id.nbp_minute);
        long timerSeconds = (selectedHours.getValue() * 3600) + (selectedMinutes.getValue() * 60);

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
}
