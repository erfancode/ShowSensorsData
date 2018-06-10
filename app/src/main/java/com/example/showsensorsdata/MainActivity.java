package com.example.showsensorsdata;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnSuccessListener;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements SensorEventListener, LocationListener, SurfaceHolder.Callback {


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
    @BindView(R.id.distance)
    TextView distance;
    @BindView(R.id.counter)
    TextView counter;
    @BindView(R.id.angle)
    TextView angle;
    @BindView(R.id.surface_view)
    SurfaceView surfaceView;
    @BindView(R.id.image_view)
    ImageView imageView;
    @BindView(R.id.graph)
    GraphView graphView;

    String out = null;
    int locationCounter = 0;
    private float[] compass = {0, 0, 0};
    private float[] gyroscope = {0, 0, 0};
    private float[] azimuth = new float[3];
    private Location location = new Location("");
    private static final int LOCATION_MIN_TIME = 1 * 1000;
    static final float ALPHA = 0.25f;
    private float[] rotation = new float[9];
    private float[] orientation = new float[3];
    private float[] smoothhed = new float[3];
    float normalVector[] = new float[3];
    private SensorManager sensorManager;
    private Sensor compassSensor;
    private Sensor gyroscopeSensor;
    private LocationManager locationManager;
    private GeomagneticField geomagneticField;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;
    private LatLng baseLocation = new LatLng(35.719001, 51.387735);
    private double bearing = 0;
    private static final int COMPASS = Sensor.TYPE_ACCELEROMETER;
    private static final int GYROSCOPE = Sensor.TYPE_MAGNETIC_FIELD;



    File root = android.os.Environment.getExternalStorageDirectory();
    File dir = new File (root.getAbsolutePath() + "/download");
    File file ;
    FileOutputStream f = null;
    PrintWriter pw = null;

    Camera camera;
    SurfaceHolder surfaceHolder;

    int graphConter = 0;
    LineGraphSeries<DataPoint> series = new LineGraphSeries();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        location.setLatitude(35.719506);
        location.setLongitude(51.386812);
        dir.mkdirs();
        file = new File(dir, "myData.txt");

        try {
            f = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        pw = new PrintWriter(f);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null)
                        {

                            MainActivity.this.location = location;
                        }
                    }
                });

        LocationCallback mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                List<Location> locationList = locationResult.getLocations();
                if (locationList.size() > 0) {
                    //The last location in the list is the newest
                    Location location = locationList.get(locationList.size() - 1);
                    locationCounter++;
                    counter.setText(String.valueOf(locationCounter));
                    Log.i("MapsActivity", "Location: " + location.getLatitude() + " " + location.getLongitude());
                    MainActivity.this.location = location;

                }
            }
        };
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(100); // two minute interval
        mLocationRequest.setFastestInterval(100);
        mLocationRequest.setMaxWaitTime(500);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());


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

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @SuppressLint("MissingPermission")
    @Override
    protected void onStart() {
        super.onStart();
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        compassSensor = sensorManager.getDefaultSensor(COMPASS);
        gyroscopeSensor = sensorManager.getDefaultSensor(GYROSCOPE);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        sensorManager.registerListener(this, compassSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_NORMAL);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, LOCATION_MIN_TIME, 3, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

            }
        });
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
            //smoothhed = lowPass(sensorEvent.values, compass);
            compass[0] = sensorEvent.values[0];
            compass[1] = sensorEvent.values[1];
            compass[2] = sensorEvent.values[2];

            //series.appendData(new DataPoint(graphConter++, compass[0]), false,200000);
            //graphView.addSeries(series);
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
        out = String.valueOf(compass[0]) + "," + String.valueOf(compass[1]) + "," +String.valueOf(compass[2]) + "," ;
        out += String.valueOf(gyroscope[0]) + "," + String.valueOf(gyroscope[1]) + "," +String.valueOf(gyroscope[2]) + "," ;

        float I[] = new float[9];
        SensorManager.getRotationMatrix(rotation, I,compass, gyroscope);
        //SensorManager.remapCoordinateSystem(rotation, SensorManager.AXIS_X, SensorManager.AXIS_MINUS_Z, rotation);
        //SensorManager.getOrientation(rotation, orientation);
        float[] results = new float[3];
        SensorManager.getOrientation(rotation, results);
        /*for(int i = 0; i < 3;i++)
        {
            compass[i] = results[i] * 100;
        }*/
        azimuth[0] = results[0];
        azimuth[1] = results[1];
        azimuth[2] = results[2];
        double z[] = new double[3];
        z[0] = (int)(Math.toDegrees(results[0]));

        z[1] = (int)Math.toDegrees(results[1]) ;

        z[2] = (int)Math.toDegrees(results[2]);
        /*if (results[0] < 0) {
            z[0] += 360;
            results[0] += 2 * Math.PI;
        }
        if (results[1] < 0) {
            z[1] += 180;
            results[1] += Math.PI;
        }*/
        //if (results[2] < 0)
        //    results[2] += 2 * Math.PI;


        normalVector[0] = (float) (Math.sin(results[1]) * Math.cos(results[0])) * -1;
        normalVector[1] = (float) (Math.sin(results[2]) * Math.cos(results[0])) ;
        normalVector[2] = (float) (Math.cos(results[1]) * Math.cos(results[2]));
        out += String.valueOf(normalVector[0]) + "," + String.valueOf(normalVector[1]) + "," +String.valueOf(normalVector[2]) + "\n" ;
        Log.d(TAG,z[0] + " " + z[1] + " " + z[2] + "\n\t\t\t\t" + normalVector[0] + " " + normalVector[1] + " " + normalVector[2]);
        Location base = new Location("");
        base.setLatitude(MainActivity.this.baseLocation.latitude);
        base.setLongitude(MainActivity.this.baseLocation.longitude);
        double dis = getDistance(base, location) * 1000;
        distance.setText(String.valueOf(dis));


        //pw.println(out);
        //pw.flush();

        float locationVector[] =  getVectorTwoPoint(location, base);
        float x = dotProduct(normalVector, locationVector);

        /*if(dis > 50)
        {
            imageView.setVisibility(View.GONE);
        }
        else
        {
            ViewGroup.LayoutParams params = (ViewGroup.LayoutParams) imageView.getLayoutParams();
            params.width = ((dis < 5) ? 200 : (int)(200 * 5 / dis));
            params.height = ((dis < 5) ? 200 : (int)(200 * 5 / dis));
            imageView.setLayoutParams(params);
            imageView.setVisibility(View.VISIBLE);
        }*/
        float anglex = (float) Math.toDegrees(Math.acos(x));
        angle.setText(String.valueOf(anglex));
        if(anglex > 20 | anglex < -20)
        {
            imageView.setVisibility(View.GONE);
        }
        else
        {
            imageView.setVisibility(View.VISIBLE);
        }

        /*imageView.setRotationX(9 * gyroscope[1]);
        imageView.setRotationY(9 * gyroscope[0]);
        imageView.setRotation(9 * gyroscope[2]);*/
        bearing = orientation[0];
        bearing = Math.toDegrees(bearing);
        if(geomagneticField != null)
        {
            bearing += geomagneticField.getDeclination();
        }
        update();
    }

    private double getDistance(Location p1, Location p2)
    {
        double dLat1InRad = p1.getLatitude() * (Math.PI / 180);
        double dLong1InRad = p1.getLongitude() * (Math.PI / 180);
        double dLat2InRad = p2.getLatitude() * (Math.PI / 180);
        double dLong2InRad = p2.getLongitude() * (Math.PI / 180);
        double dLongitude = dLong2InRad - dLong1InRad;
        double dLatitude = dLat2InRad - dLat1InRad;
        double a = Math.pow(Math.sin(dLatitude / 2), 2)
                + Math.cos(dLat1InRad) * Math.cos(dLat2InRad) * Math.pow(Math.sin(dLongitude / 2), 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double dDistance = 6378.137 * c;
        return dDistance;
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
        locationCounter++;
        counter.setText(String.valueOf(locationCounter));
        update();
        geomagneticField = new GeomagneticField((float)location.getLatitude(), (float)location.getLongitude(),                  (float)location.getAltitude(),System.currentTimeMillis());

    }
    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    void update()
    {
        //Log.d(TAG, "on Run...");
        if (location != null) {
            latitude.setText(String.valueOf(location.getLatitude()));
            longitude.setText(String.valueOf(location.getLongitude()));
        }

        xCompass.setText(String.valueOf(normalVector[0] ));
        yCompass.setText(String.valueOf(normalVector[1] ));
        zCompass.setText(String.valueOf(normalVector[2] ));

        xGyroscope.setText(String.valueOf(gyroscope[0]));
        yGyroscope.setText(String.valueOf(gyroscope[1]));
        zGyroscope.setText(String.valueOf(gyroscope[2]));

        //imageView.setRotation(5 * (gyroscope[0]  + gyroscope[1]));

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

    private float[] getVectorTwoPoint(Location l1, Location l2)
    {
        float resulte[] = new float[3];
        double length = Math.sqrt((l1.getLatitude() - l2.getLatitude()) * (l1.getLatitude() - l2.getLatitude())
                                + (l1.getLongitude() - l2.getLongitude()) * (l1.getLongitude() - l2.getLongitude()));

        resulte[0] = 0;
        resulte[1] = (float) ((l1.getLatitude() - l2.getLatitude()) / length);
        resulte[2] = (float) ((l1.getLongitude() - l2.getLongitude()) / length);
        return resulte;
    }
    float dotProduct(float vect_A[], float vect_B[])
    {

        float product = 0;

        // Loop for calculate cot product
        for (int i = 0; i < 3; i++)
            product = product + vect_A[i] * vect_B[i];
        double length1 = Math.sqrt(Math.pow(vect_A[0] ,2) + Math.pow(vect_A[1] ,2) + Math.pow(vect_A[2] ,2));
        double length2 = Math.sqrt(Math.pow(vect_B[0] ,2) + Math.pow(vect_B[1] ,2) + Math.pow(vect_B[2] ,2));
       // Log.d(TAG , "length is : " + length1 +  " , " + length2);
        return (float) (product /( length1 * length2)) ;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pw.close();
        try {
            f.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
