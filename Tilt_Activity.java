package com.example.vaggelis.assignment_1;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
//Accessed by swiping to the left from the 1st activity. The user has access to a spirit level for checking whether a
//surface is horizontal or not. Moreover, the roll and pitch angles are displayed in degrees. In case the phone is perfectly
//horizontal the background becomes green. The user can return to the main activity by swiping to the right.
public class Tilt_Activity extends AppCompatActivity implements SensorEventListener {

    public SensorManager sm; //sm is an object of class SensorManager. We create an instance of SensorManager class
    public Sensor aSensor; //Represents the accelerometer sensor
    public Sensor mSensor; //Represents the magnetic field sensor
    float [] accelerometerValues = new float[3]; //for storing the data of the accelerometer sensor
    float [] magneticFieldValues=new float[3]; //for storing the data of the magnetic field sensor
    public float roll; //Holds the roll angle in degrees
    private float pitch; //Holds the pitch angle in degrees
    public float[] smoothed=new float[3];  //Smoother values after low-pass filtering the data
    //values of the magnetic field and accelerometer sensors
    Tilt_Animation_Layout tilt_anim; //Tilt_Animation_Layout is a custom View
    public static int roll_angle_canvas;
    public static int pitch_angle_canvas;
    public static int roll_initial;
    public static int pitch_initial;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setBackgroundColor(Color.BLACK);
        tilt_anim= new Tilt_Animation_Layout(this);
        setContentView(tilt_anim);
        initialization();
        calculateOrientation();
    }

    private void initialization() {
        // Now we obtain (via an intent) the roll and pitch angles that we have at the moment we transitioned from the MainActivity to the TiltActivity
        Intent angles=getIntent();
        roll_initial=angles.getIntExtra("roll_angle",0);
        pitch_initial=angles.getIntExtra("pitch_angle",0);


        //Get an instance of SensorManger for accessing sensors. We basically initialize the sm variable
        sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        //Determine a default sensor type, in this case is magnetometer (MAGNETIC FIELD SENSOR). We basically initialize the mSensor variabl
        mSensor = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        //Determine a default sensor type, in this case is accelerometer. We basically initialize the aSensor variable
        aSensor=sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    // Method which calculates the azimuth (not used here), roll and pitch angles
    private void calculateOrientation() {
        float[] orientation = new float[3]; //values[0] stores the azimuth angle (in radians) (0 when phone faces north)
        //values[1] stores the pitch angle (in radians)
        //values[2] stores the roll angle (in radians)

        float[] R = new float[9]; //Holds the rotation matrix

        //Compute the rotation matrix R which is used to compute the devices's orientation
        SensorManager.getRotationMatrix(R, null, accelerometerValues, magneticFieldValues);

        //Now, we compute the device's orientation (based on the rotation matrix).
        //This method returns the azimuth, pitch and roll angles of the phone (in radians) and stores
        //them inside the float variable "values".
        SensorManager.getOrientation(R, orientation);

        //Convert the stored data of the "values" array from radians to degrees
        pitch=(float) Math.toDegrees(orientation [1]); //pitch_angle
        roll = (float) Math.toDegrees(orientation[2]); //roll angle
        if (roll==0.0 && pitch==0.0) { //We do this to make the initial position of the dot (when Tilt_Activity starts) to be the (roll,pitch) position
                                    //when we left the MainActivity. Note that whenever Tilt_Activity starts the very initial pitch and roll angels are 0
                                    //and that we why we use this if statement
            roll_angle_canvas=roll_initial;
            pitch_angle_canvas=pitch_initial;
        }
        else{
            roll_angle_canvas = (int) roll;
            pitch_angle_canvas = (int) pitch;
        }
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

    //Called when sensor values have changed. This method belongs to the SensorEventListener Interface
    @Override
    public void onSensorChanged(SensorEvent event) {
        //Read the MAGNETIC FIELD sensor values from SensorEvent
        if (event.sensor.getType()==Sensor.TYPE_MAGNETIC_FIELD){
            smoothed=LowPassFilter.filter(event.values,magneticFieldValues );
            magneticFieldValues[0]=smoothed[0];
            magneticFieldValues[1]=smoothed[1];
            magneticFieldValues[2]=smoothed[2];
        }
        //Read the ACCELEROMETER sensor values from SensorEvent
        if (event.sensor.getType()==Sensor.TYPE_ACCELEROMETER){
            smoothed=LowPassFilter.filter(event.values,accelerometerValues );
            accelerometerValues[0]=smoothed[0];
            accelerometerValues[1]=smoothed[1];
            accelerometerValues[2]=smoothed[2];
        }
        calculateOrientation();

    }

    //Called when the accuracy of a sensor has changed
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) { }


    //Custom View Class
    public class Tilt_Animation_Layout extends View {

        Bitmap dot_bm;
        Paint paint=new Paint();
        Paint green_paintbrush_fill=new Paint();

        public Tilt_Animation_Layout(Context context) {   super(context);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            //PAINTBRUSHES used for filling the background
            green_paintbrush_fill.setColor(Color.GREEN);
            green_paintbrush_fill.setStyle(Paint.Style.FILL);


            //Make the background of the Tilt_Activity GREEN in case both the Roll and Pitch angles are 0, otherwise the background is BLACK (as set inside the OnCreate() method)
            if (roll_angle_canvas==0 && pitch_angle_canvas==0) {
                Rect background = new Rect();
                background.set(0, 0, canvas.getWidth(), canvas.getHeight());
                canvas.drawRect(background, green_paintbrush_fill);
            }

            dot_bm= BitmapFactory.decodeResource(getResources(),R.drawable.dot);
            //We will make the dot moving from -80 to 80 roll angle and -50 to 50 pitch angle
            int cw=canvas.getWidth(); //gets the width of the screen
            int ch=canvas.getHeight(); //gets the height of the screen
            int dotW=dot_bm.getWidth(); //gets the width of the dot picture
            int dotH=dot_bm.getHeight(); //gets the height of the dot picture
            int left_right_margin=(cw*400)/1080; //makes sure that the photo stays within the right and left edges of the screen (used Samsung Galaxy s5 screen width as the reference)
            int to_left_right=(5*cw)/1080; //this is by how many pixels the picture will be moved when the roll angle changes (used Samsung Galaxy s5 screen width as the reference)
            int to_top_down=(16*ch)/1848; //this is by how many pixels the picture will be moved when the pitch angle changes (used Samsung Galaxy s5 screen width as the reference)
            int top_down_margin=(800*ch)/1848; //makes sure that the photo stays within the bottom and up edges of the screen (used Samsung Galaxy s5 screen width as the reference)
            if (roll_angle_canvas>=-80 && roll_angle_canvas<=80 && pitch_angle_canvas>=-50 && pitch_angle_canvas<=50 ) {
                canvas.drawBitmap(dot_bm, cw / 2 - dotW / 2 + roll_angle_canvas * to_left_right, ch / 2 - dotH / 2 - pitch_angle_canvas * to_top_down, null);
            }
            else if (roll_angle_canvas<-80 && pitch_angle_canvas>=-50 && pitch_angle_canvas<=50) {
                canvas.drawBitmap(dot_bm, cw / 2 - dotW / 2 - left_right_margin, ch / 2 - dotH / 2 - pitch_angle_canvas * to_top_down, null);
            }
            else if (roll_angle_canvas>80 && pitch_angle_canvas>=-50 && pitch_angle_canvas<=50){
                canvas.drawBitmap(dot_bm, cw / 2 - dotW / 2 + left_right_margin, ch / 2 - dotH / 2 - pitch_angle_canvas * to_top_down, null);
            }
            else if (pitch_angle_canvas<-50 && roll_angle_canvas>=-80 && roll_angle_canvas<=80 ){
                canvas.drawBitmap(dot_bm, cw / 2 - dotW / 2 + roll_angle_canvas *to_left_right, ch / 2 - dotH / 2 +top_down_margin, null);
            }
            else if (pitch_angle_canvas>50 && roll_angle_canvas>=-80 && roll_angle_canvas<=80 ){
                canvas.drawBitmap(dot_bm, cw / 2 - dotW / 2 + roll_angle_canvas * to_left_right, ch / 2 - dotH / 2 -top_down_margin, null);
            }
            else if (pitch_angle_canvas>50 && roll_angle_canvas>80 ){ //top-right edge
                canvas.drawBitmap(dot_bm, cw / 2 - dotW / 2 + left_right_margin , ch / 2 - dotH / 2 -top_down_margin, null);
            }
            else if (pitch_angle_canvas<-50 && roll_angle_canvas>80 ){ //bottom-right edge
                canvas.drawBitmap(dot_bm, cw / 2 - dotW / 2 + left_right_margin , ch / 2 - dotH / 2 +top_down_margin, null);
            }
            else if (pitch_angle_canvas>50 && roll_angle_canvas<-80 ){ //top-left edge
                canvas.drawBitmap(dot_bm, cw / 2 - dotW / 2 - left_right_margin , ch / 2 - dotH / 2 -top_down_margin, null);
            }
            else if (pitch_angle_canvas<-50 && roll_angle_canvas<-80 ){ //bottom-left edge
                canvas.drawBitmap(dot_bm, cw / 2 - dotW / 2 - left_right_margin , ch / 2 - dotH/ 2 +top_down_margin, null);
            }

            //We will also display the pitch and roll angles on the top middle of the screen as well as a text to inform the user to swipe right if he wants to go to the MainActivity
            String roll_an=String.valueOf(roll_angle_canvas);
            String pitch_an=String.valueOf(pitch_angle_canvas);
            paint.setColor(Color.WHITE);
            paint.setTextSize(40);
            canvas.drawText("Roll: "+roll_an+"°",canvas.getWidth()/2-70,100,paint);
            canvas.drawText("Pitch: "+pitch_an+"°",canvas.getWidth()/2-70,200,paint);
            canvas.drawText("Swipe Right",canvas.getWidth()/2-70,canvas.getHeight()-15,paint);
            //Draw a cross which will indicate the center of the screen (roll and pitch angles are 0)
            canvas.drawLine(canvas.getWidth()/2-200,canvas.getHeight()/2,canvas.getWidth()/2+200,canvas.getHeight()/2,paint);
            canvas.drawLine(canvas.getWidth()/2,canvas.getHeight()/2-200,canvas.getWidth()/2,canvas.getHeight()/2+200,paint);
            invalidate();
        }
    }

    //Method which is used to swipe from left to righ to go from TiltActivity to MainActivity
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
                if (x2>x1){
                    Intent tilt=new Intent(Tilt_Activity.this,MainActivity.class);
                    finish();
                    startActivity(tilt);
                    overridePendingTransition(R.anim.slide_in_left,R.anim.slide_out_right);
                }
                break;
        }
        return false;
    }



    //Make the slide animation whenever the back phone key is pressed
    @Override
    public boolean onKeyUp( int keyCode, KeyEvent event ) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Intent tilt = new Intent(Tilt_Activity.this, MainActivity.class);
            finish();
            startActivity(tilt);
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        }
        return super.onKeyUp(keyCode, event);
    }

}