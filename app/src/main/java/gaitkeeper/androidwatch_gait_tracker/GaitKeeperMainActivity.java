package gaitkeeper.androidwatch_gait_tracker;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.util.FloatMath;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;


public class GaitKeeperMainActivity extends WearableActivity implements SensorEventListener {

    private static final SimpleDateFormat AMBIENT_DATE_FORMAT =
            new SimpleDateFormat("HH:mm", Locale.US);

    private BoxInsetLayout mContainerView;
    private TextView mTextView;
    private TextView mClockView;

    private SensorManager gyro_manager;
    private Sensor gyro;
    private SensorEventListener gyro_listener;

    private float axisX = 1;
    private float axisY = 2;
    private float axisZ = 3;
    private float omegaMagnitude = 4;

    private long time_start = 0;
    private long time_stop = 0;

    private static final float ns_2_s = 1.0f/1000000000.f;

    protected int count = 0;
    protected boolean write = false;
    protected boolean write_once = false;
    protected boolean isFile_closed = false;
    private String write_status = "Not Writing";

    String filename = "Gyro_data";
    String data;
    FileOutputStream outputfile_stream;

    private Context gate_context;
    private FileWriter fw;
    private File output_file;
    private BufferedWriter bw;

    private int DataCount = 0;
    private int DataCount_MAX = 500;
    String suffix = "";

    private double [] x_axis_data = new double[DataCount_MAX];
    private double [] y_axis_data = new double[DataCount_MAX];
    private double [] z_axis_data = new double[DataCount_MAX];

    private Vibrator vibrator;
    long[] vibrationPattern = {0, 500, 50, 300};
    final int indexInPatternToRepeat = -1;

    private static final String TAG = "GaitClassifier";
    private Instances m_Data;
    private Classifier m_Classifier;
    private String [] m_Classes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gait_keeper_main);
        setAmbientEnabled();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        RelativeLayout rlayout = (RelativeLayout) findViewById(R.id.mainlayout);
        gate_context = getApplicationContext();
        Random r = new Random();
        String a = Character.toString((char) (r.nextInt(26) + 'a'));
        String b = Character.toString((char) (r.nextInt(26) + 'a'));
        String c = Character.toString((char) (r.nextInt(26) + 'a'));
        String d = Character.toString((char) (r.nextInt(26) + 'a'));

        suffix = a + b +c + d;
        filename = filename + "_" + suffix + ".txt";
        isFile_closed = false;
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        //-1 - don't repeat

        rlayout.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                int DataCount = 0;
                write = !write;
                if(write  )
                {
                    try
                    {

                        //output_file = new File(gate_context.getFilesDir(), filename);
                        output_file = new File("/sdcard/", filename);
                        fw = new FileWriter(output_file.getAbsoluteFile());
                        bw = new BufferedWriter(fw);

                        time_start = System.currentTimeMillis()/1000;
                        bw.write("Time Start = " + time_start + "\n");
                        //outputfile_stream = openFileOutput(filename, Context.MODE_PRIVATE);
                    }
                    catch (Exception e)
                    {
                        mTextView.setText("AXIS X = " + axisX + "\n" +
                                "AXIS Y = " + axisY + "\n" +
                                "AXIS Z = " + axisZ + "\n" +
                                "Write = " + write_status + "\n" +
                                "FILE OPEN EXCEPTION");
                    }

                }
                else
                {
                    try
                    {
                        if(!isFile_closed)
                        {
                            bw.close();
                        }
                        else
                        {
                            isFile_closed = true;
                        }
                    }
                    catch (Exception e)
                    {
                        mTextView.setText("AXIS X = " + axisX + "\n" +
                                "AXIS Y = " + axisY + "\n" +
                                "AXIS Z = " + axisZ + "\n" +
                                "Write = " + write_status + "\n" +
                                "FILE CLOSE EXCEPTION");
                    }
                }
            }
        });


        mContainerView = (BoxInsetLayout) findViewById(R.id.container);
        mTextView = (TextView) findViewById(R.id.text);
        mClockView = (TextView) findViewById(R.id.clock);

        gyro_manager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        gyro = gyro_manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyro_manager.registerListener(this, gyro_manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        updateDisplay();
    }

    @Override
    public void onSensorChanged(SensorEvent event)
    {
        //final float dT = (event.timestamp - timestamp) * ns_2_s;
        axisX = event.values[0];
        axisY = event.values[1];
        axisZ = event.values[2];

        //possible data loss:
        omegaMagnitude = (float) Math.sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ);

        //if(omegaMagnitude > EPSILON);
        //{
        //axisX /= omegaMagnitude;
        //axisY /= omegaMagnitude;
        //axisZ /= omegaMagnitude;
        //}
        //timestamp = event.timestamp;
        updateDisplay();
    }

    @Override
    public void onAccuracyChanged(Sensor sens, int arg1)
    {
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        updateDisplay();
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        updateDisplay();
    }

    @Override
    public void onExitAmbient() {
        updateDisplay();
        super.onExitAmbient();
    }

    @Override
    public void onResume() {
        super.onResume();
        gyro_manager.registerListener(this, gyro_manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        gyro_manager.unregisterListener(this);
    }

    @Override
    public void onDestroy()
    {
        if(!isFile_closed)
        {
            try {
                bw.close();
            } catch (Exception e) {

            }
        }
        gyro_manager.unregisterListener(this, gyro_manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
        super.onDestroy();
    }

    private void updateDisplay()
    {
        time_stop = System.currentTimeMillis()/1000;
        long diff = time_stop - time_start;
        if(write)
        {
            write_status = "Writing";
        }
        else
        {
            write_status = "Not Writing";
        }

        if (isAmbient()) {
            mContainerView.setBackgroundColor(getResources().getColor(android.R.color.black));
            mTextView.setTextColor(getResources().getColor(android.R.color.white));
            mClockView.setVisibility(View.VISIBLE);


            mTextView.setText("AXIS X = " + axisX + "\n" +
                               "AXIS Y = " + axisY + "\n" +
                               "AXIS Z = " + axisZ + "\n" +
                               "Write = " + write_status + "\n" +
                               "Count = " + DataCount + "\n" +
                               "Suffix = " + suffix + "\n");

        } else {
            mContainerView.setBackground(null);
            mTextView.setTextColor(getResources().getColor(android.R.color.black));
            mClockView.setVisibility(View.VISIBLE);

            mTextView.setText("AXIS X = " + axisX + "\n" +
                              "AXIS Y = " + axisY + "\n" +
                              "AXIS Z = " + axisZ + "\n" +
                              "Write = " + write_status + "\n" +
                              "Count = " + DataCount + "\n" +
                             "Suffix = " + suffix + "\n");
        }
        String output = axisX + " " +
                        axisY + " " +
                        axisZ + "\n";

        if(write && (DataCount < DataCount_MAX) )
        {
            try
            {
                bw.write(output);
                x_axis_data[DataCount] = axisX;
                y_axis_data[DataCount] = axisY;
                z_axis_data[DataCount] = axisZ;
                DataCount++;
            }
            catch (Exception e)
            {
                mTextView.setText("AXIS X = " + axisX + "\n" +
                        "AXIS Y = " + axisY + "\n" +
                        "AXIS Z = " + axisZ + "\n" +
                        "Write = " + write_status + "\n" +
                        "WRITING EXCEPTION");
            }
        }
        else if(DataCount >= DataCount_MAX)
        {
            if(write_once)
            {
                vibrator.vibrate(vibrationPattern, indexInPatternToRepeat);
                //Pass the data to the classifier.


            }
            else
            {
                //Pass the data to the classifier.
                try
                {
                    bw.write("Time Stop = " + time_stop + "\n");
                    write_once = true;
                    vibrator.vibrate(vibrationPattern, indexInPatternToRepeat);
                }
                catch (Exception e)
                {
                }
            }

        }
    }

    private Instances initializeRawDataFormat() {
        // Raw Data Points to extract features from
        Attribute attribute1 = new Attribute("x");
        Attribute attribute2 = new Attribute("y");
        Attribute attribute3 = new Attribute("z");
        // Declare the feature vector
        FastVector fvWekaAttributes = new FastVector(3);
        fvWekaAttributes.addElement(attribute1);
        fvWekaAttributes.addElement(attribute2);
        fvWekaAttributes.addElement(attribute3);
        // Create an empty raw data set
        Instances rawData = new Instances("raw_accel" /*relation name*/, fvWekaAttributes /*attribute vector*/, 250 /*initial capacity*/);
        return rawData;
    }

    //private void readData(BufferedReader reader, String userid) throws Exception {
    private void readData(float[] x_axis, float[] y_axis, float[] z_axis, String userid) throws Exception {
        /*
        Instances rawData = initializeRawDataFormat();
        if (reader == null)
            return;
        String mLine;
        while ((mLine = reader.readLine()) != null) {
            //process line
            Instance iDataPoint = null;
            if (mLine.contains(":") || mLine.contains("="))
                continue; // skip line
            else if (mLine.contentEquals("")) {
                // End of sample window. Extract transformed features now
                if (rawData.numInstances() != 0) { // skip any blank line if we haven't read in any data yet
                    iDataPoint = transformData2(rawData, userid);
                    m_Data.add(iDataPoint);
                    rawData.clear(); // reset to read in new window
                }
            } else {
                String[] data = mLine.split("\\s+");
                // build and add instance
                iDataPoint = new DenseInstance(4);
                iDataPoint.setValue(rawData.attribute("x"), Double.parseDouble(data[0]));
                iDataPoint.setValue(rawData.attribute("y"), Double.parseDouble(data[1]));
                iDataPoint.setValue(rawData.attribute("z"), Double.parseDouble(data[2]));
                rawData.add(iDataPoint);
            }
        }
        if (rawData.numInstances() != 0) { // handle potential window near end of file
            Instance iDataPoint = transformData2(rawData, userid);
            m_Data.add(iDataPoint);
        } */


        Instances rawData = initializeRawDataFormat();
        Instance iDataPoint = null;
         // build and add instance
        for (int i = 0; i < DataCount_MAX; i++)
        {
                iDataPoint = new DenseInstance(4);
                iDataPoint.setValue(rawData.attribute("x"), x_axis[i]);
                iDataPoint.setValue(rawData.attribute("y"), y_axis[i]);
                iDataPoint.setValue(rawData.attribute("z"), z_axis[i]);
                rawData.add(iDataPoint);
        }
    }


    public Instance transformData2(Instances rawData, String userid) {
        String [] axis = new String[] {"x", "y", "z"};
        Instance windowDataPoint = new DenseInstance(19);
        // Mean, Variance, Standard Deviation, Minimum
        for (int i = 0; i < axis.length; i++) {
            double mean = rawData.meanOrMode(i);
            double var = rawData.variance(i);
            double sd = Math.sqrt(var);
            double minimum = rawData.attributeStats(i).numericStats.min;
            windowDataPoint.setValue(m_Data.attribute("Mean " + axis[i]), mean);
            windowDataPoint.setValue(m_Data.attribute("Variance " + axis[i]), var);
            windowDataPoint.setValue(m_Data.attribute("Standard Deviation " + axis[i]), sd);
            windowDataPoint.setValue(m_Data.attribute("Minimum " + axis[i]), minimum);
        }
        // Average Absolute Sample Difference
        double [] sumDeltas = new double[] {0,0,0};
        Instance prevInst = rawData.instance(0);
        for (int instIdx = 1; instIdx < rawData.numInstances(); instIdx++) {
            Instance currInst = rawData.instance(instIdx);
            for (int i = 0; i < axis.length; i++) {
                sumDeltas[i] += currInst.value(rawData.attribute(axis[i])) - prevInst.value(rawData.attribute(axis[i]));
            }
            prevInst = currInst;
        }
        for (int i = 0; i < axis.length; i++) {
            windowDataPoint.setValue(m_Data.attribute("Average Absolute Sample Difference " + axis[i]), sumDeltas[i]/rawData.numInstances());
        }
        for (int i = 0; i < axis.length-1; i++) {
            for (int j = i + 1; j < axis.length; j++) {
                double [] arrI = rawData.attributeToDoubleArray(i);
                double [] arrJ = rawData.attributeToDoubleArray(j);
                windowDataPoint.setValue(m_Data.attribute("Correlation " + axis[i] + axis[j]), Utils.correlation(arrI, arrJ, arrI.length));
            }
        }
        // Class attribute
        windowDataPoint.setValue(m_Data.attribute("userid"), userid);

        return windowDataPoint;
    }
}
