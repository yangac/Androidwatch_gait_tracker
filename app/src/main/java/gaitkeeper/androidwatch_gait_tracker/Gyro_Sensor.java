package gaitkeeper.androidwatch_gait_tracker;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

/**
 * Created by Andrew on 02/20/16.
 */
public class Gyro_Sensor implements SensorEventListener
{
    private float axisX = 0;
    private float axisY = 0;
    private float axisZ = 0;
    private float omegaMagnitude = 0;
    private static final float ns_2_s = 1.0f/1000000000.f;

    private float timestamp = 0;
    private SensorEventListener gyro_listener;

    public Gyro_Sensor(SensorEventListener listener)
    {
            gyro_listener = listener;
    }

    @Override
    public void onSensorChanged(SensorEvent event)
    {
        if (timestamp != 0)
        {
            final float dT = (event.timestamp - timestamp) * ns_2_s;

            axisX = event.values[0];
            axisY = event.values[1];
            axisZ = event.values[2];

            //possible data loss:
            omegaMagnitude = (float) Math.sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ);

            //if(omegaMagnitude > EPSILON);
            //{
            axisX /= omegaMagnitude;
            axisY /= omegaMagnitude;
            axisZ /= omegaMagnitude;
            //}
            timestamp = event.timestamp;
        }
    }

    public float getAxisX()
    {
        return axisX;
    }

    public float getAxisY()
    {
        return axisY;
    }

    public float getAxisZ()
    {
        return axisZ;
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
