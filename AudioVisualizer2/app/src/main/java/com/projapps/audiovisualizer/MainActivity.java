package com.projapps.audiovisualizer;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;

import com.jjoe64.graphview.BarGraphView;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;

import ca.uol.aig.fftpack.RealDoubleFFT;

public class MainActivity extends AppCompatActivity {
    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private AudioRecord mAudioRecord = null;
    private FrameLayout mLayout = null;
    private GraphViewSeries mGraphSeries = null;
    private Thread mRecordThread = null;

    private int mMinBufferSize = 0;
    private boolean isRecording = false;
    private boolean isBarGraph = true;

    private RealDoubleFFT transformer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mLayout = (FrameLayout) findViewById(R.id.layout);
        mMinBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
    }

    @Override
    protected void onStart() {
        super.onStart();
        initGraphView();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopRecording();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.device_access_mic:
                startRecording();
                invalidateOptionsMenu();
                return true;
            case R.id.device_access_mic_muted:
                stopRecording();
                invalidateOptionsMenu();
                return true;
            case R.id.action_settings:
                initGraphView();
                return true;
            case R.id.privacy_policy:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item1, item2;
        if (mAudioRecord == null) {
            item1 = menu.findItem(R.id.device_access_mic_muted);
            item2 = menu.findItem(R.id.device_access_mic);
        } else {
            item1 = menu.findItem(R.id.device_access_mic);
            item2 = menu.findItem(R.id.device_access_mic_muted);
        }
        if (item1 != null) {
            item1.setEnabled(false);
            item1.setVisible(false);
            if (item2 != null) {
                item2.setEnabled(true);
                item2.setVisible(true);
            }
        }
        return true;
    }

    public void startRecording() {
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, mMinBufferSize);
        mAudioRecord.startRecording();
        isRecording = true;

        mRecordThread = new Thread(new Runnable() {
            @Override
            public void run() {
                updateGraphView();
            }
        });
        mRecordThread.start();
    }

    public void stopRecording() {
        if (mAudioRecord != null) {
            if (isRecording) {
                mAudioRecord.stop();
                isRecording = false;
                mRecordThread = null;
            }
            mAudioRecord.release();
            mAudioRecord = null;
            mGraphSeries.resetData(new GraphView.GraphViewData[]{});
        }
    }

    public void initGraphView() {
        GraphView graphView;

        if (isBarGraph) {
            graphView = new LineGraphView(this, "Time Domain");
        } else {
            graphView = new BarGraphView(this, "Frequency Domain");
        }
        isBarGraph = !isBarGraph;

        mGraphSeries = new GraphViewSeries(new GraphView.GraphViewData[]{});
        graphView.addSeries(mGraphSeries);

        if (mLayout.getChildCount() > 0) {
            mLayout.removeAllViews();
        }
        mLayout.addView(graphView);
    }

    public void updateGraphView() {
        short[] audioData = new short[mMinBufferSize];
        while (isRecording) {
            int read = mAudioRecord.read(audioData, 0, mMinBufferSize);
            if (read != AudioRecord.ERROR_INVALID_OPERATION && read != AudioRecord.ERROR_BAD_VALUE) {
                int num = audioData.length;
                final GraphView.GraphViewData[] data = new GraphView.GraphViewData[num];
                if (isBarGraph) {
                    // apply Fast Fourier Transform here
                    transformer = new RealDoubleFFT(num);
                    double[] toTransform = new double[num];
                    for (int i = 0; i < num; i++) {
                        toTransform[i] = (double) audioData[i] / Short.MAX_VALUE;
                    }
                    transformer.ft(toTransform);
                    for (int i = 0; i < num; i++) {
                        data[i] = new GraphView.GraphViewData(i, toTransform[i]);
                    }
                } else {
                    for (int i = 0; i < num; i++) {
                        data[i] = new GraphView.GraphViewData(i, audioData[i]);
                    }
                }
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mGraphSeries.resetData(data);
                    }
                });
            }
        }
    }
}
