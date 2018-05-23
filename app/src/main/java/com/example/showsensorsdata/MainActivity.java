package com.example.showsensorsdata;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements SensorEventListener, LocationListener ,SurfaceHolder.Callback{


    private static final String TAG = MainActivity.class.getSimpleName();
    @BindView(R.id.latitude)
    TextView latitude;
    @BindView(R.id.longitude)
    TextView longitude;
    @BindView(R.id.x_compass)
    TextView xCompass;
    @BindView(R.id.y_compass)
    TextView yCompass;
    @BindView(R.id.z_compass)
    TextView zCompass;
    @BindView(R.id.x_gyroscope)
    TextView xGyroscope;
    @BindView(R.id.y_gyroscope)
    TextView yGyroscope;
    @BindView(R.id.z_gyroscope)
    TextView zGyroscope;
    @BindView(R.id.surface_view)
    SurfaceView surfaceView;
    @BindView(R.id.image_view)
    ImageView image_view;

    private float[] compass = {0,0,0};
    private float[] gyroscope = {0,0,0};
    private Location location ;
    private static final int LOCATION_MIN_TIME = 10 * 1000;
    static final float ALPHA = 0.25f;
    private float[] rotation = new float[9];
    private float[] orientation = new float[3];
    private float[] smoothhed = new float[3];
    private SensorManager sensorManager;
    private Sensor compassSensor;
    private Sensor gyroscopeSensor;
    private LocationManager locationManager;
    private GeomagneticField geomagneticField;
    private double bearing = 0;
    private static final int COMPASS = Sensor.TYPE_MAGNETIC_FIELD;
    private static final int GYROSCOPE = Sensor.TYPE_GRAVITY;



    Camera camera;
    SurfaceHolder surfaceHolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);

        Log.e(TAG, "before init camera");
        try{
            camera = Camera.open();
        }catch(RuntimeException e){
            Log.e(TAG, "init_camera: " + e);
        }
        Camera.Parameters param;
        param = camera.getParameters();
        //modify parameter
        param.setPreviewFrameRate(30);
        param.setPreviewSize(1280, 720);
        camera.setParameters(param);

    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onStart() {
        super.onStart();
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        compassSensor = sensorManager.getDefaultSensor(COMPASS);
        gyroscopeSensor = sensorManager.getDefaultSensor(GYROSCOPE);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        sensorManager.registerListener(this, compassSensor, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_FASTEST);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_MIN_TIME,0,this);
        Location gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if(gpsLocation != null)
        {
            location = gpsLocation;
            onLocationChanged(location);
        }
        else
        {
            Location networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if(networkLocation != null)
            {
                location = networkLocation;
            }
            else {
                location = new Location("FIXED");
                location.setAltitude(1);
                location.setLatitude(43.296482);
                location.setLongitude(5.36978);
            }
            onLocationChanged(location);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_MIN_TIME,10,this);
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        sensorManager.unregisterListener(this, compassSensor);
        sensorManager.unregisterListener(this, gyroscopeSensor);
        locationManager.removeUpdates(this);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent)
    {
        boolean compassOrGyr = false;
        if(sensorEvent.sensor.getType() == COMPASS)
        {
            smoothhed = lowPass(sensorEvent.values, compass);
            compass[0] = smoothhed[0];
            compass[1] = smoothhed[1];
            compass[2] = smoothhed[2];
            compassOrGyr = true;
        }
        else if(sensorEvent.sensor.getType() == GYROSCOPE)
        {
            smoothhed = lowPass(sensorEvent.values, gyroscope);
            gyroscope[0] = smoothhed[0];
            gyroscope[1] = smoothhed[1];
            gyroscope[2] = smoothhed[2];
            compassOrGyr = true;
        }
        SensorManager.getRotationMatrix(rotation, null, compass, gyroscope);
        SensorManager.getOrientation(rotation, orientation);
        bearing = orientation[0];
        bearing = Math.toDegrees(bearing);
        if(geomagneticField != null)
        {
            bearing += geomagneticField.getDeclination();
        }
        update();
    }

    protected float[] lowPass( float[] input, float[] output ) {
        if ( output == null ) return input;
        for ( int i=0; i<input.length; i++ ) {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }
        return output;
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        if(sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD &&
                i == SensorManager.SENSOR_STATUS_UNRELIABLE)
        {
            Log.e(TAG, "Can not read gyroscope !!!");
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        this.location = location;
        update();
        geomagneticField = new GeomagneticField((float)location.getLatitude(), (float)location.getLongitude(),                  (float)location.getAltitude(),System.currentTimeMillis());

    }
    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    void update()
    {
        Log.d(TAG, "on Run...");
        if (location != null) {
            latitude.setText(String.valueOf(location.getLatitude()));
            longitude.setText(String.valueOf(location.getLongitude()));
        }

        xCompass.setText(String.valueOf(compass[0]));
        yCompass.setText(String.valueOf(compass[1]));
        zCompass.setText(String.valueOf(compass[2]));

        xGyroscope.setText(String.valueOf(gyroscope[0]));
        yGyroscope.setText(String.valueOf(gyroscope[1]));
        zGyroscope.setText(String.valueOf(gyroscope[2]));

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }
    public float[] getCompass()
    {
        return compass;
    }
    public float[] getGyroscope()
    {
        return gyroscope;
    }

    public Location getLocation() {
        return location;
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        try {
            camera.setPreviewDisplay(surfaceHolder);
            camera.setDisplayOrientation(90);
            camera.startPreview();
            //camera.takePicture(shutter, raw, jpeg)
        } catch (Exception e) {
            Log.e(TAG, "init_camera: " + e);
            return;
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }
}
