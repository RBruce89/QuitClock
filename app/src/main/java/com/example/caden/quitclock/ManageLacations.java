package com.example.caden.quitclock;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.support.v7.app.AlertDialog;
import android.text.InputFilter;
import android.text.InputType;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Spinner;
import com.google.gson.Gson;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashSet;

import static android.content.Context.MODE_PRIVATE;

public class ManageLacations {

    private ArrayList<String> locationList = new ArrayList<>();
    private SharedPreferences locationListPrefs;
    private String selectedLocation;
    private SharedPreferences selectedLocationPref;
    private SharedPreferences locationSettingPrefs;

    private MainActivity mainActivity;

    public ManageLacations(MainActivity mainActivity)
    {
        this.mainActivity = mainActivity;
    }

    //changes spinners selected location
    public void spinnerSelection(int position){
        final Spinner locationSpinner = mainActivity.findViewById(R.id.spn_locations);

        //Displays and handles dialog to create a new location.
        if(position == 0) {
            final ArrayAdapter<String> locationsArrayAdapter = new ArrayAdapter<>(mainActivity,
                    android.R.layout.simple_spinner_item, locationList);

            locationSpinner.setSelection(locationsArrayAdapter.getPosition(selectedLocation));

            AlertDialog.Builder locationBuilder = new AlertDialog.Builder(mainActivity);
            locationBuilder.setTitle(R.string.add_location_title);

            final EditText locationInput = new EditText(mainActivity);
            locationInput.setFilters(new InputFilter[] {new InputFilter.LengthFilter(10)});
            locationInput.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
            locationBuilder.setView(locationInput);

            locationBuilder.setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String locationName = locationInput.getText().toString();
                    if (!locationName.equals("")) {
                        mainActivity.setTempTimerSeconds(0);

                        locationList.remove(0);
                        locationList.add(locationName);

                        HashSet<String> locationListSet = new HashSet<>(locationList);

                        SharedPreferences.Editor listPrefsEditor = locationListPrefs.edit();
                        listPrefsEditor.putStringSet("locationList", locationListSet);
                        listPrefsEditor.apply();

                        locationList.add(0, "Add Location");

                        ArrayAdapter<String> locationsArrayAdapter = new ArrayAdapter<>(
                                mainActivity, android.R.layout.simple_spinner_item, locationList);
                        locationsArrayAdapter.setDropDownViewResource(
                                android.R.layout.simple_spinner_dropdown_item);
                        locationSpinner.setAdapter(locationsArrayAdapter);
                        locationSpinner.setSelection(locationsArrayAdapter.getPosition(locationName));

                        saveSelectedLocation(locationName);
                    }
                }
            });
            locationBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            locationBuilder.show();
        }

        saveSelectedLocation(locationSpinner.getSelectedItem().toString());
        updateLocationSettings();
    }

    //Changes selected location and saves to shared preferences.
    public void saveSelectedLocation(String location){
        selectedLocation = location;
        SharedPreferences.Editor selectedLocationPrefsEditor = selectedLocationPref.edit();
        selectedLocationPrefsEditor.putString("selectedLocation", selectedLocation);
        selectedLocationPrefsEditor.apply();
    }

    //Initializes spinner values from shared preferences.
    public void loadLocationPrefs() {
        selectedLocationPref = mainActivity.getSharedPreferences("selectedLocation", MODE_PRIVATE);
        selectedLocation = selectedLocationPref.getString("selectedLocation", "Home");

        locationListPrefs = mainActivity.getSharedPreferences("locationList", MODE_PRIVATE);

        HashSet<String> defaultSet = new HashSet<>();
        locationList = new ArrayList<>(locationListPrefs.getStringSet("locationList", defaultSet));

        if (locationList.size() < 3) {
            locationList.add("Home");
            locationList.add("Work");
            locationList.add("Vacation");
        }
        locationList.add(0, "Add Location");

        final Spinner locationSpinner = mainActivity.findViewById(R.id.spn_locations);

        final ArrayAdapter<String> locationsArrayAdapter = new ArrayAdapter<>(mainActivity,
                android.R.layout.simple_spinner_item, locationList);
        locationsArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        locationSpinner.setAdapter(locationsArrayAdapter);
        locationSpinner.setSelection(locationsArrayAdapter.getPosition(selectedLocation));
    }

    //Locks pickers and saves settings to shared preferences.
    public void setLocationTime() {
        Button lockButton  = mainActivity.findViewById(R.id.btn_lock_number_pickers);
        NumberPicker hourPicker = mainActivity.findViewById(R.id.nbp_hour);
        NumberPicker minutePicker = mainActivity.findViewById(R.id.nbp_minute);

        mainActivity.setPickersEnabled(false);
        hourPicker.setEnabled(false);
        minutePicker.setEnabled(false);
        lockButton.setBackgroundResource(R.drawable.round_lock_black_36);
        mainActivity.setTimerSeconds((hourPicker.getValue() * 3600) + (minutePicker.getValue() * 60));

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
        locationSettingsPrefsEditor.apply();
    }

    //Changes number picker settings for associated locations.
    public void updateLocationSettings() {
        final Button lockButton = mainActivity.findViewById(R.id.btn_lock_number_pickers);
        final NumberPicker hourPicker = mainActivity.findViewById(R.id.nbp_hour);
        final NumberPicker minutePicker = mainActivity.findViewById(R.id.nbp_minute);

        locationSettingPrefs = mainActivity.getSharedPreferences(selectedLocation, MODE_PRIVATE);
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
                mainActivity.setTimerSeconds((hourPicker.getValue() * 3600) + (minutePicker.getValue() * 60));
                lockButton.setBackgroundResource(R.drawable.round_lock_black_36);
                mainActivity.setPickersEnabled(false);

            } catch (JSONException e) {
                System.out.println("json exception on updateLocationSettings catch");
            }
        } else {
            hourPicker.setEnabled(true);
            hourPicker.setValue(0);
            minutePicker.setEnabled(true);
            minutePicker.setValue(0);
            lockButton.setBackgroundResource(R.drawable.round_lock_open_black_36);
            mainActivity.setPickersEnabled(true);
        }
    }

    public String getSelectedLocation(){
        return selectedLocation;
    }

}
