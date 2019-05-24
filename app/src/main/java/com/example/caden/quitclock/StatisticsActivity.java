package com.example.caden.quitclock;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


public class StatisticsActivity extends MainActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.statistics);

        statistics.loadStats();
        statistics.removeOutdated();

        TextView statsTextView = findViewById(R.id.txt_statistics);

        statistics.display(statsTextView);

        Button backButton = findViewById(R.id.btn_back);
        backButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}
