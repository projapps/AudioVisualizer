package com.projapps.audiovisualizer

import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import ca.uol.aig.fftpack.RealDoubleFFT
import com.jjoe64.graphview.series.BarGraphSeries
import com.jjoe64.graphview.series.BaseSeries
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import kotlinx.android.synthetic.main.activity_main.*



const val SAMPLE_RATE = 44100
const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

class MainActivity : AppCompatActivity() {

    private var mAudioRecord: AudioRecord? = null
    private var mRecordThread: Thread? = null
    private var mBaseSeries: BaseSeries<DataPoint>? = null

    private var mMinBufferSize = 0
    private var isRecording = false
    private var isBarGraph = true

    private var transformer: RealDoubleFFT? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mMinBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
    }

    override fun onStart() {
        super.onStart()
        initGraphView()
    }

    override fun onPause() {
        super.onPause()
        stopRecording()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        // Handle item selection
        when (item?.itemId) {
            R.id.device_access_mic -> {
                startRecording()
                invalidateOptionsMenu()
                return true
            }
            R.id.device_access_mic_muted -> {
                stopRecording()
                invalidateOptionsMenu()
                return true
            }
            R.id.action_settings -> {
                initGraphView()
                return true
            }
            R.id.privacy_policy -> {
                startActivity(Intent(this, PolicyActivity::class.java))
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val item1: MenuItem?
        val item2: MenuItem?

        if (mAudioRecord == null) {
            item1 = menu?.findItem(R.id.device_access_mic_muted)
            item2 = menu?.findItem(R.id.device_access_mic)
        } else {
            item1 = menu?.findItem(R.id.device_access_mic)
            item2 = menu?.findItem(R.id.device_access_mic_muted)
        }

        if (item1 != null) {
            item1.isEnabled = false
            item1.isVisible = false
            if (item2 != null) {
                item2.isEnabled = true
                item2.isVisible = true
            }
        }

        return super.onPrepareOptionsMenu(menu)
    }

    private fun startRecording() {
        mAudioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, mMinBufferSize)
        mAudioRecord!!.startRecording()
        isRecording = true

        mRecordThread = Thread(Runnable { updateGraphView() })
        mRecordThread!!.start()
    }

    private fun stopRecording() {
        if (mAudioRecord != null) {
            if (isRecording) {
                mAudioRecord?.stop()
                isRecording = false
                mRecordThread = null
            }
            mAudioRecord?.release()
            mAudioRecord = null
            mBaseSeries?.resetData(arrayOf<DataPoint>())
        }
    }

    private fun initGraphView() {
        if (isBarGraph) {
            mBaseSeries = LineGraphSeries<DataPoint>(arrayOf<DataPoint>())
            graph.title = "Time Domain"
        } else {
            mBaseSeries = BarGraphSeries<DataPoint>(arrayOf<DataPoint>())
            graph.title = "Frequency Domain"
        }

        isBarGraph = !isBarGraph

        if (graph.series.count() > 0) {
            graph.removeAllSeries()
        }
        graph.addSeries(mBaseSeries)
    }

    private fun updateGraphView() {
        val audioData = ShortArray(mMinBufferSize)
        while (isRecording) {
            val read = mAudioRecord!!.read(audioData, 0, mMinBufferSize)
            if (read != AudioRecord.ERROR_INVALID_OPERATION && read != AudioRecord.ERROR_BAD_VALUE) {
                val num = audioData.size
                val data = arrayOfNulls<DataPoint>(num)
                if (isBarGraph) {
                    // apply Fast Fourier Transform here
                    transformer = RealDoubleFFT(num)
                    val toTransform = DoubleArray(num)
                    for (i in 0 until num) {
                        toTransform[i] = audioData[i].toDouble() / Short.MAX_VALUE
                    }
                    transformer!!.ft(toTransform)
                    for (i in 0 until num) {
                        data[i] = DataPoint(i.toDouble(), toTransform[i])
                    }
                } else {
                    for (i in 0 until num) {
                        data[i] = DataPoint(i.toDouble(), audioData[i].toDouble())
                    }
                }
                this@MainActivity.runOnUiThread { mBaseSeries!!.resetData(data) }
            }
        }
    }
}
