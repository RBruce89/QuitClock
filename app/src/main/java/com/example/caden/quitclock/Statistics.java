package com.example.caden.quitclock;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.widget.TextView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class Statistics{

    private ArrayList<Long> mCigarettesSmoked = new ArrayList<>();
    private ArrayList<Long> mTimerTurnDowns = new ArrayList<>();
    private ArrayList<Long> mPrematureSmokes = new ArrayList<>();
    private HashMap<String, ArrayList<Long>> mLocationTimes = new HashMap<>();

    private MainActivity mainActivity;


    public Statistics(MainActivity mainActivity)
    {
        this.mainActivity = mainActivity;
    }

    public void addCigaretteSmoked(long timeStamp)
    {
        mCigarettesSmoked.add(timeStamp);
    }

    public void addTimerTurnDown(long timeStamp)
    {
        mTimerTurnDowns.add(timeStamp);
    }

    public void addPrematureSmoke(long timeStamp)
    {
        mPrematureSmokes.add(timeStamp);
    }

    public void addLocationTime(String location, long timeStamp)
    {
        if(mLocationTimes.get(location) == null){
            mLocationTimes.put(location, new ArrayList<Long>());
        }
        mLocationTimes.get(location).add(timeStamp);
    }

    //Loops through saved statistics and removes those older than 30 days (excluding smokes).
    public void removeOutdated()
    {
        long currentHours = TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis());

        if (mTimerTurnDowns != null) {
            for (int i = 0; i < mTimerTurnDowns.size(); i++) {
                long timerDownHours = TimeUnit.MILLISECONDS.toHours(mTimerTurnDowns.get(i));
                if (timerDownHours < (currentHours - 720)) {
                    mTimerTurnDowns.remove(i);
                    i--;
                }
            }
        }
        if (mPrematureSmokes != null) {
            for (int i = 0; i < mPrematureSmokes.size(); i++) {
                long prematureSmokeHours = TimeUnit.MILLISECONDS.toHours(mPrematureSmokes.get(i));
                if (prematureSmokeHours < (currentHours - 720)) {
                    mPrematureSmokes.remove(i);
                    i--;
                }
            }
        }
        if (mLocationTimes != null) {
            for (HashMap.Entry<String, ArrayList<Long>> locationEntry : mLocationTimes.entrySet()) {
                ArrayList<Long> times = locationEntry.getValue();
                for (int i = 0; i < times.size(); i++) {
                    long timeHours = TimeUnit.MILLISECONDS.toHours(times.get(i));
                    if (timeHours < (currentHours - 720)) {
                        times.remove(i);
                    }
                }
            }
        }
    }

    //Initializes statistics from shared preferences.
    public void loadStats()
    {
        SharedPreferences statsPrefs = PreferenceManager.getDefaultSharedPreferences(
                mainActivity.getApplicationContext());

        Gson statsGson = new Gson();

        if (statsPrefs.contains("cigarettesSmoked")) {
            String json = statsPrefs.getString("cigarettesSmoked", "");
            Type type = new TypeToken<ArrayList<Long>>(){}.getType();
            mCigarettesSmoked = statsGson.fromJson(json, type);
        }

        if (statsPrefs.contains("timerTurnDowns")) {
            String json = statsPrefs.getString("timerTurnDowns", "");
            Type type = new TypeToken<ArrayList<Long>>(){}.getType();
            mTimerTurnDowns = statsGson.fromJson(json, type);
        }

        if (statsPrefs.contains("prematureSmokes")) {
            String json = statsPrefs.getString("prematureSmokes", "");
            Type type = new TypeToken<ArrayList<Long>>(){}.getType();
            mPrematureSmokes = statsGson.fromJson(json, type);
        }

        if (statsPrefs.contains("locationTimes")) {
            String json = statsPrefs.getString("locationTimes", "");
            Type type = new TypeToken<HashMap<String, ArrayList<Long>>>(){}.getType();
            mLocationTimes = statsGson.fromJson(json, type);
        }
    }

    //Saves current statistics to shared preferences.
    public void saveStats()
    {
        SharedPreferences statsPrefs = PreferenceManager.getDefaultSharedPreferences(
                mainActivity.getApplicationContext());
        SharedPreferences.Editor statsPrefsEditor = statsPrefs.edit();
        Gson statsGson = new Gson();
        if (mCigarettesSmoked != null) {
            String cigarettesSmoked = statsGson.toJson(mCigarettesSmoked);
            statsPrefsEditor.putString("cigarettesSmoked", cigarettesSmoked);
        }
        if (mTimerTurnDowns != null) {
            String timerTurnDowns = statsGson.toJson(mTimerTurnDowns);
            statsPrefsEditor.putString("timerTurnDowns", timerTurnDowns);
        }
        if (mPrematureSmokes != null) {
            String prematureSmokes = statsGson.toJson(mPrematureSmokes);
            statsPrefsEditor.putString("prematureSmokes", prematureSmokes);
        }
        if (mLocationTimes != null) {
            String locationTimes = statsGson.toJson(mLocationTimes);
            statsPrefsEditor.putString("locationTimes", locationTimes);
        }
        statsPrefsEditor.commit();
    }

    //Finds and displays Statistic values.
    public void display(TextView statsTextView)
    {
        int smokedDay = getCigsFromLast24Hours();
        int prematureSmokesDay = getPrematureSmokesDay();
        String topLocationDay = getMostFrequentLocationDay();

        int smokedMonth = getCigsFromLast30Days();
        int monthDays;
        double dailySmokedMonth;
        double yearlySmokedMonth;
        if (getDaysSinceFirstCig() > 29) {
            monthDays = 30;
            dailySmokedMonth = smokedMonth / 30;
            yearlySmokedMonth = dailySmokedMonth * 365;
        } else {
            monthDays = (int) getDaysSinceFirstCig();
            dailySmokedMonth = (smokedMonth / getDaysSinceFirstCig());
            yearlySmokedMonth = dailySmokedMonth * 365;
        }
        int timerDownsMonth = (mTimerTurnDowns == null) ? 0 : mTimerTurnDowns.size();
        int earlyLightsMonth = (mPrematureSmokes == null) ? 0 : mPrematureSmokes.size();
        String topLocationMonth = getMostFrequentLocationMonth();

        int smokedTotal = (mCigarettesSmoked == null) ? 0 : mCigarettesSmoked.size();
        double averageDailyTotal = smokedTotal / (int) getDaysSinceFirstCig();
        double averageYearlyTotal = averageDailyTotal * 365;

        Resources res = mainActivity.getResources();

        String statsText = res.getString(R.string.last_day);
        statsText += res.getString(R.string.cigs, smokedDay);
        statsText += res.getString(R.string.early_lights, prematureSmokesDay);
        statsText += res.getString(R.string.top_location, topLocationDay);
        statsText += "\n";

        String dayPlural = res.getQuantityString(R.plurals.day, monthDays);
        statsText += res.getString(R.string.last_month, monthDays, dayPlural);
        statsText += res.getString(R.string.cigs, smokedMonth);
        statsText += res.getString(R.string.daily_average, dailySmokedMonth);
        statsText += res.getString(R.string.yearly_average, yearlySmokedMonth);
        statsText += res.getString(R.string.timer_downs, timerDownsMonth);
        statsText += res.getString(R.string.early_lights, earlyLightsMonth);
        statsText += res.getString(R.string.top_location, topLocationMonth);
        statsText += "\n";

        statsText += res.getString(R.string.lifetime);
        statsText += res.getString(R.string.cigs, smokedTotal);
        statsText += res.getString(R.string.daily_average, averageDailyTotal);
        statsText += res.getString(R.string.yearly_average, averageYearlyTotal);

        statsTextView.setText(statsText);
    }

    //Returns number of cigarettes smoked in the last 24 hours.
    private int getCigsFromLast24Hours()
    {
        int cigsFromLast24Hours = 0;
        long currentHours = TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis());

        for (long timeStamp : mCigarettesSmoked) {
            long timeStampHours = TimeUnit.MILLISECONDS.toHours(timeStamp);
            if (timeStampHours > (currentHours - 24)){
                cigsFromLast24Hours += 1;
            }
        }

        return cigsFromLast24Hours;
    }

    //Returns number of cigarettes smoked in the last 30 days.
    private int getCigsFromLast30Days()
    {
        int cigsFromLast30Days = 0;
        long currentHours = TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis());

        for (long timeStamp : mCigarettesSmoked) {
            long timeStampHours = TimeUnit.MILLISECONDS.toHours(timeStamp);
            if (timeStampHours > (currentHours - 720)){
                cigsFromLast30Days += 1;
            }
        }

        return cigsFromLast30Days;
    }

    //Returns days passed since first cigarette smoked.
    private long getDaysSinceFirstCig()
    {
        long firstCigTime = System.currentTimeMillis();
        long currentDays = (int) TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis());

        for (long timeStamp : mCigarettesSmoked) {
            if (timeStamp < firstCigTime){
                firstCigTime = timeStamp;
            }
        }

        return (currentDays - TimeUnit.MILLISECONDS.toDays(firstCigTime)) + 1;
    }

    //Returns number of premature smokes in the last 24 hours.
    private int getPrematureSmokesDay()
    {
        int prematureSmokesFromLast24Hours = 0;
        long currentHours = TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis());

        for (long timeStamp : mPrematureSmokes) {
            long timeStampHours = TimeUnit.MILLISECONDS.toHours(timeStamp);
            if (timeStampHours > (currentHours - 24)){
                prematureSmokesFromLast24Hours += 1;
            }
        }

        return prematureSmokesFromLast24Hours;
    }

    //Returns the location where the most cigarettes were smoked in the last day.
    private String getMostFrequentLocationDay()
    {
        long currentHours = TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis());
        String mostFrequentLocation = "None";
        int locationFrequency = 0;

        for (HashMap.Entry<String, ArrayList<Long>> locationEntry : mLocationTimes.entrySet()){
            ArrayList<Long> times = locationEntry.getValue();

            int locationCigsDay = 0;
            for (long timeStamp : times) {
                long timeStampHours = TimeUnit.MILLISECONDS.toHours(timeStamp);
                if (timeStampHours > (currentHours - 24)){
                    locationCigsDay += 1;
                }
            }

            if (locationCigsDay > locationFrequency){
                locationFrequency = locationCigsDay;
                mostFrequentLocation = locationEntry.getKey();
            }
        }

        return mostFrequentLocation;
    }

    //Returns the location where the most cigarettes were smoked in the last month.
    private String getMostFrequentLocationMonth()
    {
        String mostFrequentLocation = "None";
        int locationFrequency = 0;

        for (HashMap.Entry<String, ArrayList<Long>> locationEntry : mLocationTimes.entrySet()){
            ArrayList<Long> times = locationEntry.getValue();
            if (times.size() > locationFrequency){
                locationFrequency = times.size();
                mostFrequentLocation = locationEntry.getKey();
            }
        }

        return mostFrequentLocation;
    }
}
