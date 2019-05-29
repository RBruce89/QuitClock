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
    private ArrayList<Long[]> mExtraMinutes = new ArrayList<>();
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

    public void addExtraMinutes(long extraMinutes, long timeStamp)
    {
        Long[] entry = {extraMinutes, timeStamp};
        mExtraMinutes.add(entry);
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
            for (Long timerDown : mTimerTurnDowns) {
                long timerDownHours = TimeUnit.MILLISECONDS.toHours(timerDown);
                if (timerDownHours < (currentHours - 720)) {
                    mTimerTurnDowns.remove(timerDown);
                }
            }
        }
        if (mPrematureSmokes != null) {
            for (Long prematureSmoke : mPrematureSmokes) {
                long prematureSmokeHours = TimeUnit.MILLISECONDS.toHours(prematureSmoke);
                if (prematureSmokeHours < (currentHours - 720)) {
                    mPrematureSmokes.remove(prematureSmoke);
                }
            }
        }
        if (mExtraMinutes != null) {
            for (Long[] extraMinutesTime : mExtraMinutes) {
                long extraMinutesTimeHours = TimeUnit.MILLISECONDS.toHours(extraMinutesTime[1]);
                if (extraMinutesTimeHours < (currentHours - 720)) {
                    mPrematureSmokes.remove(extraMinutesTime);
                }
            }
        }
        if (mLocationTimes != null) {
            for (HashMap.Entry<String, ArrayList<Long>> locationEntry : mLocationTimes.entrySet()) {
                ArrayList<Long> times = locationEntry.getValue();
                for (Long time : times) {
                    long timeHours = TimeUnit.MILLISECONDS.toHours(time);
                    if (timeHours < (currentHours - 720)) {
                        times.remove(time);
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

        if (statsPrefs.contains("extraMinutes")) {
            String json = statsPrefs.getString("extraMinutes", "");
            Type type = new TypeToken<ArrayList<Long[]>>(){}.getType();
            mExtraMinutes = statsGson.fromJson(json, type);
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
        if (mExtraMinutes != null) {
            String extraMinutes = statsGson.toJson(mExtraMinutes);
            statsPrefsEditor.putString("extraMinutes", extraMinutes);
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
        double intervalsDay = (double) 24 / smokedDay;
        int smokedMonth = getCigsFromLast30Days();
        double intervalsMonth;
        int yearlySmokedMonth;
        int monthDays;
        if (getDaysSinceFirstCig() > 29) {
            intervalsMonth = (double) 720 / smokedMonth;
            yearlySmokedMonth = (int) (smokedMonth * 12.16);
            monthDays = 30;
        } else {
            intervalsMonth = (double) (getDaysSinceFirstCig() * 24) / smokedMonth;
            yearlySmokedMonth = (int) (smokedMonth * (365 / getDaysSinceFirstCig()));
            monthDays = (int) getDaysSinceFirstCig();
        }
        String locationsMonth = getMostFrequentLocation();
        int timerDropsMonth = (mTimerTurnDowns == null) ? 0 : mTimerTurnDowns.size();  //not reporting
        int earlySmokesMonth = (mPrematureSmokes == null) ? 0 : mPrematureSmokes.size();
        int extraMinutesMonth = getAverageExtraMinutes();
        int smokedTotal = (mCigarettesSmoked == null) ? 0 : mCigarettesSmoked.size();
        int smokedDailyTotal = smokedTotal / (int) getDaysSinceFirstCig();

        Resources res = mainActivity.getResources();

        String pluralItem = res.getQuantityString(R.plurals.cigarette, smokedDay);
        String statsText = res.getString(R.string.day_smoked, smokedDay, pluralItem);

        pluralItem = res.getQuantityString(R.plurals.hour, (int) intervalsDay);
        statsText += res.getString(R.string.day_intervals, intervalsDay, pluralItem);

        pluralItem = res.getQuantityString(R.plurals.cigarette, smokedMonth);
        String dayPlural = res.getQuantityString(R.plurals.day, monthDays);
        statsText += res.getString(R.string.month_smoked, monthDays, dayPlural, smokedMonth, pluralItem);

        pluralItem = res.getQuantityString(R.plurals.hour, (int) intervalsMonth);
        statsText += res.getString(R.string.month_intervals, intervalsMonth, pluralItem);

        pluralItem = res.getQuantityString(R.plurals.cigarette, yearlySmokedMonth);
        statsText += res.getString(R.string.month_yearly_smoked, yearlySmokedMonth, pluralItem);

        statsText += res.getString(R.string.month_location, locationsMonth);

        pluralItem = res.getQuantityString(R.plurals.time, timerDropsMonth);
        statsText += res.getString(R.string.month_timer_drops, timerDropsMonth, pluralItem);

        pluralItem = res.getQuantityString(R.plurals.cigarette, earlySmokesMonth);
        statsText += res.getString(R.string.month_early_smokes, earlySmokesMonth, pluralItem);

        pluralItem = res.getQuantityString(R.plurals.minute, extraMinutesMonth);
        statsText += res.getString(R.string.month_extra_minutes, extraMinutesMonth, pluralItem);

        pluralItem = res.getQuantityString(R.plurals.cigarette, smokedTotal);
        statsText += res.getString(R.string.total_smoked, smokedTotal, pluralItem);

        pluralItem = res.getQuantityString(R.plurals.cigarette, smokedDailyTotal);
        statsText += res.getString(R.string.total_smoked_daily, smokedDailyTotal, pluralItem);

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

    //Returns the average amount of extra minutes between smokes.
    private int getAverageExtraMinutes()
    {
        int totalExtraMinutes = 0;

        for (Long[] extraMinutesEntry : mExtraMinutes){
            totalExtraMinutes += extraMinutesEntry[0];
        }

        int averageExtraMinutes = 0;
        if (mExtraMinutes.size() > 0) {
            averageExtraMinutes = (int) (totalExtraMinutes / mExtraMinutes.size());
        }

        return averageExtraMinutes;
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

    //Returns the location where the most cigarettes were smoked.
    private String getMostFrequentLocation()
    {
        String mostFrequentLocation = "";
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
