package com.example.caden.quitclock;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;


import com.google.gson.Gson;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private long startTime;
    private SharedPreferences startTimePref;

    private ArrayList<String> locationList = new ArrayList<>();
    private SharedPreferences locationListPrefs;
    private String selectedLocation;
    private SharedPreferences selectedLocationPref;

    private SharedPreferences locationSettingPrefs;

    private Boolean pickersEnabled = true;

    long timerSeconds = 0;

    private boolean runThread;

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

        selectedLocationPref = getSharedPreferences("selectedLocation", MODE_PRIVATE);
        selectedLocation = selectedLocationPref.getString("selectedLocation", "Home");

        locationListPrefs = getSharedPreferences("locationList", MODE_PRIVATE);

        HashSet<String> defaultSet = new HashSet<>();
        locationList = new ArrayList<>(locationListPrefs.getStringSet("locationList", defaultSet));
        if (locationList.size() < 3) {
            locationList.add("Home");
            locationList.add("Work");
            locationList.add("Vacation");
        }
        locationList.add(0, "Add Location");

        final ArrayAdapter<String> locationsArrayAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, locationList);
        locationsArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        locationSpinner.setAdapter(locationsArrayAdapter);
        locationSpinner.setSelection(locationsArrayAdapter.getPosition(selectedLocation));

        setUpPickers();

        updateLocationSettings();

        locationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                final Spinner locationsSpinner = findViewById(R.id.spn_locations);

                if(position == 0) {
                    locationSpinner.setSelection(locationsArrayAdapter.getPosition(selectedLocation));

                    AlertDialog.Builder locationBuilder = new AlertDialog.Builder(MainActivity.this);
                    locationBuilder.setTitle("Name your new location:");

                    final EditText locationInput = new EditText(MainActivity.this);
                    locationInput.setFilters(new InputFilter[] {new InputFilter.LengthFilter(12)});

                    locationInput.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
                    locationBuilder.setView(locationInput);

                    locationBuilder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String locationName = locationInput.getText().toString();
                            if (!locationName.equals("")) {
                                locationList.remove(0);
                                locationList.add(locationName);
                            }

                            HashSet<String> locationListSet = new HashSet<>(locationList);

                            SharedPreferences.Editor listPrefsEditor = locationListPrefs.edit();
                            listPrefsEditor.putStringSet("locationList", locationListSet);
                            listPrefsEditor.apply();

                            locationList.add(0, "Add Location");

                            Spinner locationSpinner = findViewById(R.id.spn_locations);
                            ArrayAdapter<String> locationsArrayAdapter = new ArrayAdapter<String>(
                                    MainActivity.this, android.R.layout.simple_spinner_item, locationList);
                            locationsArrayAdapter.setDropDownViewResource(
                                    android.R.layout.simple_spinner_dropdown_item);
                            locationSpinner.setAdapter(locationsArrayAdapter);
                            locationSpinner.setSelection(locationsArrayAdapter.getPosition(locationName));

                            selectedLocation = locationName;
                            SharedPreferences.Editor selectedLocationPrefsEditor = selectedLocationPref.edit();
                            selectedLocationPrefsEditor.putString("selectedProjectName", selectedLocation);
                            selectedLocationPrefsEditor.apply();
                        }
                    });
                    locationBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    locationBuilder.show();
                }

                selectedLocation = locationsSpinner.getSelectedItem().toString();
                SharedPreferences.Editor selectedLocationPrefsEditor = selectedLocationPref.edit();
                selectedLocationPrefsEditor.putString("selectedLocation", selectedLocation);
                selectedLocationPrefsEditor.apply();
                updateLocationSettings();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        lightUpButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                if (!pickersEnabled) {
                    if (startTime == 0) {
                        runThread = true;
                        new Thread(countdownRunnable).start();
                    }

                    startTime = System.nanoTime();
                    SharedPreferences.Editor startTimePrefsEditor = startTimePref.edit();
                    startTimePrefsEditor.putLong("startTime", startTime);
                    startTimePrefsEditor.apply();
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


                    JSONObject timerJSONObject = new JSONObject();
                    try {
                        timerJSONObject.put("hours", hourPicker.getValue());
                        timerJSONObject.put("minutes", minutePicker.getValue());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    SharedPreferences.Editor locationSettingsPrefsEditor = locationSettingPrefs.edit();
                    Gson locationSettingsGson = new Gson();
                    String timerJson = locationSettingsGson.toJson(timerJSONObject);
                    locationSettingsPrefsEditor.putString(selectedLocation, timerJson);
                    locationSettingsPrefsEditor.commit();


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

    public void updateLocationSettings() {
        final Button lockButton = findViewById(R.id.btn_lock_number_pickers);
        final NumberPicker hourPicker = findViewById(R.id.nbp_hour);
        final NumberPicker minutePicker = findViewById(R.id.nbp_minute);

        locationSettingPrefs = getSharedPreferences(selectedLocation, MODE_PRIVATE);
        if (locationSettingPrefs.contains(selectedLocation)) {
            Gson locationSettingsGson = new Gson();
            String locationSettingsJson = locationSettingPrefs.getString(selectedLocation, "");
            JSONObject timerValuesJson = locationSettingsGson.fromJson(locationSettingsJson, JSONObject.class);

            try {
                String hourString = String.format(timerValuesJson.get("hours").toString(), "%.0f");
                String minuteString = String.format(timerValuesJson.get("minutes").toString(), "%.0f");

                int hours = (int) Double.parseDouble(hourString);
                int minutes = (int) Double.parseDouble(minuteString);

                hourPicker.setEnabled(false);
                hourPicker.setValue(hours);
                minutePicker.setEnabled(false);
                minutePicker.setValue(minutes);
                timerSeconds = (hourPicker.getValue() * 3600) + (minutePicker.getValue() * 60);
                lockButton.setText("L"); //icon change
                pickersEnabled = false;

            } catch (JSONException e) {
                System.out.println("json exception on updateLocationSettings catch");
            }
        } else {
            hourPicker.setEnabled(true);
            hourPicker.setValue(0);
            minutePicker.setEnabled(true);
            minutePicker.setValue(0);
            lockButton.setText("U"); //icon change
            pickersEnabled = true;
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
        if (startTime != 0) {
            runThread = true;
            new Thread(countdownRunnable).start();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        runThread = false;
    }
}
