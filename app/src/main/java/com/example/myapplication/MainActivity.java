package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.Arrays;
import java.util.Queue;

public class MainActivity extends AppCompatActivity {


    int blockSize = 16384;
    double[] x = new double[blockSize];
    double[] y = new double[blockSize];
    double[] ampl = new double[blockSize];
    int samplingFrequency =  16384;

    //ImageView imageView;
    //int width = 1000;
    //int height = 600;

    int channelConfiguration = AudioFormat.CHANNEL_IN_MONO;
    int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
    private final int MY_PERMISSIONS_RECORD_AUDIO = 1234;

   // Bitmap bitmap ;
    //Canvas canvas ;
    //Paint paint ;

    boolean flag;

    Handler handler;
    Runnable runnableCode;


    LineGraphSeries<DataPoint> series;
    GraphView graph;


    FloatingActionButton fab;

    TextView temperature;

    TextView frequency;
    TextView hertz;
    TextView celcius;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //imageView = (ImageView) findViewById(R.id.imageView);

        graph = (GraphView) findViewById(R.id.graph);
        graph.getGridLabelRenderer().setGridColor(Color.WHITE);
        graph.setBackgroundColor(getResources().getColor(android.R.color.black));
        graph.getGridLabelRenderer().setVerticalLabelsColor(Color.WHITE);
        graph.getGridLabelRenderer().setHorizontalLabelsColor(Color.WHITE);

        temperature = (TextView) findViewById(R.id.temperature);
        temperature.setText("  Temperature [°C]:");
        frequency = (TextView) findViewById(R.id.frequency);
        frequency.setText("  Frequency [Hz]:");

        celcius = (TextView) findViewById(R.id.celcius);
        celcius.setText(0+ "");
        hertz = (TextView) findViewById(R.id.hertz);
        hertz.setText(0+ "");

        flag = false;


         handler = new Handler();
         runnableCode = new Runnable() {
            @Override
            public void run() {

                new AsyncCaller().execute();

                //Log.i("tak","handler");

                handler.postDelayed(this, 600);
            }
        };
        fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> {
            if(flag){
                handler.removeCallbacksAndMessages(null);
                fab.setImageResource(R.drawable.ic_baseline_play_arrow_24);
                flag=false;


            }
            else {
                handler.post(runnableCode);
                fab.setImageResource(R.drawable.ic_baseline_pause_24);
                flag=true;
            }
        });


    }


    private void Odczyt() {

        short[] audioBuffer = new short[blockSize];

        int bufferSize = AudioRecord.getMinBufferSize(samplingFrequency, channelConfiguration, audioEncoding);

        if (ContextCompat.checkSelfPermission( this,android.Manifest.permission.RECORD_AUDIO ) != PackageManager.PERMISSION_GRANTED )
        {
            ActivityCompat.requestPermissions(
                    this,
                    new String [] { android.Manifest.permission.RECORD_AUDIO },
                    MY_PERMISSIONS_RECORD_AUDIO
            );
        }
        AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, samplingFrequency, channelConfiguration, audioEncoding, bufferSize);

        audioRecord.startRecording();

        int bufferReadResult = audioRecord.read(audioBuffer,0,blockSize);

        for(int i = 0; i < blockSize && i < bufferReadResult; i++){

            x[i] = (double) audioBuffer[i] / 32768.0;

        }

        audioRecord.stop();

    }

    private void FFTcalc(){

        for(int i = 0; i<blockSize; i++) y[i] = 0.0;
        FFT fft = new FFT(blockSize);
        fft.fft(x,y);
        for(int i = 0; i<blockSize/2; i++) ampl[i] = x[i] * x[i] + y[i] + y[i];
    }


private void draw(){

    graph.removeAllSeries();
    series = new LineGraphSeries<DataPoint>();
    series.setColor(Color.RED);

    double max = Arrays.stream(ampl).max().getAsDouble();

    for (int i = 0; i < ampl.length/2; i++) ampl[i] /= max;

    for(int i =0; i<5500; i++) {

        series.appendData(new DataPoint(i, ampl[i]), true, ampl.length/2);
        if(ampl[i]==1){
            frequency.setText("  Frequency [Hz]:");
            temperature.setText("  Temperature [°C]:");
            celcius.setText(Math.round((i-1431.78)/60.74)+"");
            hertz.setText(i+"");
        }

    }

    graph.addSeries(series);
    graph.getViewport().setMinX(0);
   graph.getViewport().setMaxX(5000);
   graph.getViewport().setMinY(0);
   graph.getViewport().setMaxY(1);
    graph.getViewport().setYAxisBoundsManual(true);
    graph.getViewport().setXAxisBoundsManual(true);





}

//    private void draw(){
//
//    bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
//    canvas = new Canvas(bitmap);
//    paint = new Paint();
//    canvas.drawColor(Color.BLACK);
//    paint.setColor(Color.YELLOW);
//
//    double max = Arrays.stream(ampl).max().getAsDouble();
//
//    for (int i = 0; i < ampl.length; i++) ampl[i] /= max;
//
//    for (int i = 0; i < ampl.length; i++) {
//
//        int xx = i;
//
//        int upy = (int) (400 - (ampl[i] * 400.0));
//
//        int downy = 400;
//        canvas.drawLine(xx, upy, xx + 1, downy, paint);
//    }
//
//    // Display the newly created bitmap on app interface
//    imageView.setImageBitmap(bitmap);
//
//}

    private class AsyncCaller extends AsyncTask<Void, Void, Void>
    {


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            draw();
        }
        @Override
        protected Void doInBackground(Void... params) {

            //this method will be running on background thread so don't update UI frome here
            //do your long running http tasks here,you dont want to pass argument and u can access the parent class' variable url over here
              Odczyt();
                FFTcalc();

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
        }
    }
}

