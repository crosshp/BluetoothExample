package com.example.kav.bluetoothexample.Activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.example.kav.bluetoothexample.R;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by kav on 16/06/08.
 */
public class GraphActivity extends AppCompatActivity {
    static final String RSSI_INTENT = "RSSI";
    static final String INTENT_FILTER_RSSI = "INTENT_FILTER_RSSI";
    private BroadcastReceiver broadcastReceiverForGetRSSI = null;


    private long startOfScanTime = 0;
    private GraphView graph = null;
    private DataPoint[] generateData = {};
    private Map<String, LineGraphSeries<DataPoint>> mapSeries = new HashMap<>();
    private Map<String, Integer> colorMap = new HashMap<>();
    private List<Integer> colors = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.graph_layout);


        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        colors.add(Color.GREEN);
        colors.add(Color.CYAN);
        colors.add(Color.BLACK);
        colors.add(Color.RED);
        colors.add(Color.BLUE);
        colors.add(Color.YELLOW);
        colors.add(Color.GRAY);
        colors.add(Color.DKGRAY);
        graph = (GraphView) findViewById(R.id.graphViewBig);
        graph.getViewport().setMinY(-100);
        graph.getViewport().setMaxY(-45);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setScrollable(true);
        graph.getViewport().setScalable(true);
        graph.getLegendRenderer().setVisible(true);
        graph.getLegendRenderer().setFixedPosition(30, 30);
        startOfScanTime = Calendar.getInstance().getTimeInMillis();
        registerBroadcastReceiver();
    }

    private void registerBroadcastReceiver() {
        broadcastReceiverForGetRSSI = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int rssi = intent.getIntExtra(RSSI_INTENT, 0);
                Calendar calendar = Calendar.getInstance();
                String address = intent.getStringExtra(MainActivity.ADDRESS_INTENT);
                String name = intent.getStringExtra(MainActivity.NAME_INTENT);
                reDrawGraph(address, name, calendar.getTimeInMillis() - startOfScanTime, rssi);
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(INTENT_FILTER_RSSI);
        registerReceiver(broadcastReceiverForGetRSSI, filter);
    }

    public void reDrawGraph(String address, String name, long time, int rssi) {
        LineGraphSeries<DataPoint> series = mapSeries.get(address);
        if (series != null) {
            series.setColor(colorMap.get(address));
            series.appendData(new DataPoint(time, rssi), false, 50000);
        } else {
            series = new LineGraphSeries<DataPoint>(generateData);
            series.resetData(generateData);
            colorMap.put(address, colors.get(0));
            series.setColor(colors.get(0));
            colors.remove((int) 0);
            series.appendData(new DataPoint(time, rssi), false, 50000);
            series.setTitle(name);
            mapSeries.put(address, series);
            graph.addSeries(series);
        }
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(broadcastReceiverForGetRSSI);
        super.onDestroy();
    }
}
