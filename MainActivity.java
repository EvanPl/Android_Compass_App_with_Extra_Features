package com.example.vaggelis.assignment_1;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

//Activity which makes a 16-wind compass rose (with 22.5o angles between each compass point).
//Also the cardinal, intercardinal or secondary-intercardinal direction of the phone together with the degrees of rotation are displayed in two TextViews
//Note, that we implement the SensorEventListener class to be able to monitor sensor operations
public class MainActivity extends AppCompatActivity implements SensorEventListener {


    TextView tv,degrees; //different TextView used in the MainActivity
    private SensorManager sm; //sm is an object of class SensorManager. We create an instance of SensorManager class
    private Sensor aSensor; //Represents the accelerometer sensor
    private Sensor mSensor; //Represents the magnetic field sensor
    float [] accelerometerValues = new float[3]; //for storing the data of the accelerometer sensor
    float [] magneticFieldValues=new float[3]; //for storing the data of the magnetic field sensor

    private ImageView compass_image; //used for the compass image
    private ImageView arrow_image; //used for the arrow image which goes on top of the compass image

    private float azimuth; //Holds the azimuth angle in degrees
    private float current_azimuth=0; //used for the proper animation of the compass image
    private static float pitch; //Holds the pitch angle in degrees
    private static float roll; //Holds the roll angle in degrees
    private float[] smoothed=new float[3];  //Smoother values after low-pass filtering the data
                                            //values of the magnetic field and accelerometer sensors


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().getDecorView().setBackgroundColor(Color.BLACK);
        initialization();
        calculateOrientation();
    }

    //Method for connecting all the objects with their IDs
    private void initialization() {
        //Link the variable compass_image which is of type ImageView with the ImageView with id compassImageView. Same for the variable arrow_image
        compass_image=(ImageView) findViewById(R.id.compassImageView);
        arrow_image=(ImageView) findViewById(R.id.arrowImageView);

        // Link the variable tv which is of type TextView to the TextView with id tvDirectionTextView. Similar for the rest of TextView variables
        tv=(TextView) findViewById(R.id.tvDirectionTextView );
        degrees=(TextView) findViewById(R.id.degreesTextView);

        //Get an instance of SensorManger for accessing sensors. We basically initialize the sm variable
        sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        //Determine a default sensor type, in this case is magnetometer (MAGNETIC FIELD SENSOR). We basically initialize the mSensor variable
        mSensor = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        //Determine a default sensor type, in this case is accelerometer. We basically initialize the aSensor variable
        aSensor=sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    //Method in which data from the MAGNETIC FIELD, ACCELEROMETER sensors will be
    //calculated and converted to the degree by which the phone is rotated
    private void calculateOrientation() {
        float[] orientation = new float[3]; //orientation[0] stores the azimuth angle (in radians) (0 when phone faces north)
        //orientation[1] stores the pitch angle (in radians)
        //orientation[2] stores the roll angle (in radians)

        float[] R = new float [9]; //Holds the rotation matrix

        //Compute the rotation matrix R which is used to compute the devices's orientation
        SensorManager.getRotationMatrix(R,null,accelerometerValues,magneticFieldValues);

        //Now, we compute the device's orientation (based on the rotation matrix).
        //This method returns the azimuth, pitch and roll angles of the phone (in radians) and stores
        //them inside the float variable "values".
        SensorManager.getOrientation(R,orientation);

        //Convert the stored data of the "values" array from radians to degrees
        azimuth=(float) Math.toDegrees(orientation [0]); //Azimuth angle in degrees
        azimuth=(azimuth+360)%360;
        pitch=(float) Math.toDegrees(orientation [1]); //pitch angle
        roll=(float) Math.toDegrees(orientation [2]); //roll angle
        //Call the image_animation() method for the rotation of the compass image


        image_animation();


        //16-wind compass regions
        if (azimuth>=354.38 || azimuth <=16.87) {//NORTH
            tv.setText("N");
        }
        else if (azimuth>=16.88 && azimuth <=39.37) {//North-northeast
            tv.setText("NNE");
        }
        else if (azimuth>=39.38 && azimuth <=61.87) {//Northeast
            tv.setText("NE");
        }
        else if (azimuth>=61.88 && azimuth <=84.37) {//East-northeast
            tv.setText("ENE");
        }
        else if (azimuth>=84.38 && azimuth <=106.87) {//East
            tv.setText("E");
        }
        else if (azimuth>=106.88 && azimuth <=129.37) {//East-southeast
            tv.setText("ESE");
        }
        else if (azimuth>=129.38 && azimuth <=151.87) {//Southeast
            tv.setText("SE");
        }
        else if (azimuth>=151.88 && azimuth <=174.37) {//South-southeast
            tv.setText("SSE");
        }
        else if (azimuth>=174.38 && azimuth <=196.87) {//South
            tv.setText("S");
        }
        else if (azimuth>=196.88 && azimuth <=219.37) {//South-southwest
            tv.setText("SSW");
        }
        else if (azimuth>=219.38 && azimuth <=241.87) {//Southwest
            tv.setText("SW");
        }
        else if (azimuth>=241.88 && azimuth <=253.12) {//West-southwest
            tv.setText("WSW");
        }
        else if (azimuth>=264.38 && azimuth <=286.87) {//West
            tv.setText("W");
        }
        else if (azimuth>=286.88 && azimuth <=309.37) {//West-northwest
            tv.setText("WNW");
        }
        else if (azimuth>=309.38 && azimuth <=331.87) {//Northwest
            tv.setText("NW");
        }
        else if (azimuth>=331.88 && azimuth <=354.37) {//North-northwest
            tv.setText("NNW");
        }
        //Display the degrees (the azimuth angle)
        String deg=String.valueOf((int)azimuth); //We disregard all the decimal places
        degrees.setText(deg+"°");
    }

    //Method which rotates a compass image
    private void image_animation() {
        if (roll>=-90 && roll<=90) { //phone screen is facing up
            Animation anim = new RotateAnimation(-current_azimuth, -azimuth, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            current_azimuth = azimuth;
            anim.setDuration(500); // Amount of time (in milliseconds) for the animation to run
            anim.setRepeatCount(0); // We set the animation to be repeated 0 times
            anim.setFillAfter(true); //Animation applies its transformation after it ends
            compass_image.startAnimation(anim);

        }
        else if (roll<-90 || roll>90) {//phone screen is facing down (to the ground) so rotate compass image on the other direction to when phone screen was facing up
            Animation anim = new RotateAnimation(current_azimuth, azimuth, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            current_azimuth = azimuth;
            anim.setDuration(500); // Amount of time (in milliseconds) for the animation to run
            anim.setRepeatCount(0); // We set the animation to be repeated 0 times
            anim.setFillAfter(true); //Animation applies its transformation after it ends
            compass_image.startAnimation(anim);
        }

    }


    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Register the MAGNETIC FIELD and ACCELEROMETER sensors when the user returns to the activity
        sm.registerListener(this,mSensor,SensorManager.SENSOR_DELAY_FASTEST);
        sm.registerListener(this,aSensor,SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onPause() {
        super.onPause();
        //Unregister/Disable all the sensors in order to prevent battery drain
        sm.unregisterListener(this,mSensor);
        sm.unregisterListener(this,aSensor);
    }

    @Override
    protected void onStop() {
        super.onStop();
        //Unregister/Disable all the sensors in order to prevent battery drain
        sm.unregisterListener(this,mSensor);
        sm.unregisterListener(this,aSensor);
    }


    //Called when sensor values have changed. This method belongs to the SensorEventListener Interface
    @Override
    public void onSensorChanged(SensorEvent event) {
        //Read the MAGNETIC FIELD sensor values from SensorEvent and low pass filter the values
        if (event.sensor.getType()==Sensor.TYPE_MAGNETIC_FIELD){
            smoothed=LowPassFilter.filter(event.values,magneticFieldValues );
            magneticFieldValues[0]=smoothed[0];
            magneticFieldValues[1]=smoothed[1];
            magneticFieldValues[2]=smoothed[2];
        }
        //Read the ACCELEROMETER sensor values from SensorEvent and low pass filter the values
        if (event.sensor.getType()==Sensor.TYPE_ACCELEROMETER){
            smoothed=LowPassFilter.filter(event.values,accelerometerValues );
            accelerometerValues[0]=smoothed[0];
            accelerometerValues[1]=smoothed[1];
            accelerometerValues[2]=smoothed[2];
        }
        calculateOrientation();
    }

    //Method which is being called when the accuracy of a sensor has changed [ NOT USED ]
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) { }

    //Method which is used to swipe from left to right to go from MainActivity to TiltActivity
    float x1,x2,y2,y1;
    public boolean onTouchEvent(MotionEvent touchevent){
        switch (touchevent.getAction()){
            case MotionEvent.ACTION_DOWN:
                x1=touchevent.getX();
                y1=touchevent.getY();
                break;
            case MotionEvent.ACTION_UP:
                x2=touchevent.getX();
                y2=touchevent.getY();
                if (x2<x1){ // If we swipe to the left we enter the Tilt_Activity
                    Intent tilt=new Intent(MainActivity.this,Tilt_Activity.class);
                    tilt.putExtra("roll_angle", (int)roll);
                    tilt.putExtra("pitch_angle",(int)pitch);
                    startActivity(tilt);
                    overridePendingTransition(R.anim.slide_in_right,R.anim.slide_out_left);
                }
                else if (x2>x1){ // If we swipe to the right we enter the MapActivity
                    Intent map=new Intent(MainActivity.this,MapsActivity.class);
                    startActivity(map);
                    overridePendingTransition(R.anim.slide_in_left,R.anim.slide_out_right);
                }
                break;


        }
        return false;
    }

    //Method which will be called whenever the Take Photo button is clicked, and it will open another activity named Photo_Act
    public void TakePhoto (View view){
             Intent photo_act=new Intent(MainActivity.this, Photo_Act.class);
        startActivity(photo_act);
    }
}
