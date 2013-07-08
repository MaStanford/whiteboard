/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.coventsystems.whiteboard;

import com.coventsystems.whiteboard.FileService.LocalBinder;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.*;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

public class FingerPaint extends Activity implements ColorPickerDialog.OnColorChangedListener {

	MyView mv;

	Context mContext;

	private static Paint mPaint;
	private MaskFilter  mEmboss;
	private MaskFilter  mBlur;
	private static Bitmap  mBitmap;
	private static Canvas  mCanvas;
	private static boolean mEraseMode = false;
	private static final int PAINT_WIDTH = 5;
	private static final int ERASE_WIDTH = 50;

	//Service
	FileService mService;
	boolean mBound = false;
	boolean active = true;


	/***************************************************
	 * Services
	 *************************************************/

	//	/**
	//	 * Service Connection
	//	 * When you are bound to the service you will get an asynchronous 
	//	 * callback to onServiceConnected() in your SeviceConnection
	//	 * @Author Mstanford
	//	 */
	private ServiceConnection mConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder service) {
			LocalBinder binder = (LocalBinder) service;
			mService = binder.getService();
			mBound = true;
			Consts.DEBUG_LOG("SERVICE IS", "" +mService);
		}

		public void onServiceDisconnected(ComponentName className) {
			mBound = false;
		}
	};

	/***************************************************
	 * BROADCAST RECEIVER
	 ***************************************************/

	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			receivedBroadcast(intent);
		}
	};

	private void receivedBroadcast(Intent intent) {
		if (intent.getAction().equals(Consts.SAVE_SUCCESS)) {
			Toast.makeText(mContext, "Save Successful, Saved at " + 
					mService.getSaveFileDir() + mService.getFileName(), Toast.LENGTH_LONG).show();
		}
		if(intent.getAction().equals(Consts.SAVE_FAIL_PERMISSIONS)){
			Toast.makeText(mContext, "Save failed, Bitmap is blank", Toast.LENGTH_LONG).show();
		}
		if(intent.getAction().equals(Consts.SAVE_FAIL_BITNULL)){
			Toast.makeText(mContext, "Save Fail, Invalid Permissions", Toast.LENGTH_LONG).show();
		}
	}
	
	/********************************************
	 * SHAKE TO ERASE
	 ********************************************/
	private SensorManager mSensorManager;
	private float mAccel; // acceleration apart from gravity
	private float mAccelCurrent; // current acceleration including gravity
	private float mAccelLast; // last acceleration including gravity

	private final SensorEventListener mSensorListener = new SensorEventListener() {

		public void onSensorChanged(SensorEvent se) {
			if(Consts.DEBUG_VERBOSE)
				Consts.DEBUG_LOG("SHAKE","SHAKE");
			float x = se.values[0];
			float y = se.values[1];
			float z = se.values[2];
			mAccelLast = mAccelCurrent;
			mAccelCurrent = (float) Math.sqrt((double) (x*x + y*y + z*z));
			float delta = mAccelCurrent - mAccelLast;
			mAccel = mAccel * 0.9f + delta; // perform low-cut filter
			if(mAccel > 6){
				Toast.makeText(mContext, "Whiteboard Erased", Toast.LENGTH_SHORT).show();
				mv.clearDrawing();
			}
		}
		@Override
		public void onAccuracyChanged(Sensor arg0, int arg1) {
			
		}
	};

	/****************************
	 * Paint Methods
	 * **************************
	 ****************************/
	public void colorChanged(int color) {
		mPaint.setColor(color);
	}

	public void setErase(View v){
		//mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
		mPaint.setStrokeWidth(ERASE_WIDTH);
		mPaint.setColor(Color.WHITE);
		mEraseMode = true;
	}

	public void setErase(){
		//mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
		mPaint.setStrokeWidth(ERASE_WIDTH);
		mPaint.setColor(Color.WHITE);
		mEraseMode = true;
	}

	public void setBlue(View v){
		mPaint.setStrokeWidth(PAINT_WIDTH);
		mPaint.setColor(Color.BLUE);
		mEraseMode = false;
	}

	public void setRed(View v){
		mPaint.setStrokeWidth(PAINT_WIDTH);
		mPaint.setColor(Color.RED);
		mEraseMode = false;
	}

	public void setBlack(View v){
		mPaint.setStrokeWidth(PAINT_WIDTH);
		mPaint.setColor(Color.BLACK);
		mEraseMode = false;
	}


	/********************
	 * BUTTON METHODS
	 * Decided for quick mock up not use listeners
	 ******************/
	
	/**
	 * Save feature 
	 */
	public void save(View v){
		if (mService != null){
			mService.save(mBitmap);
		} else {
			Consts.DEBUG_LOG("mService is null","mService is null");
		}
	}

	public void load(View v){
		Toast.makeText(mContext, "Feature not availible in DEMO", Toast.LENGTH_SHORT).show();
		//mBitmap = mService.load(mBitmap);
	}

	public void sendEmail(View v){
		Toast.makeText(mContext, "Feature not availible in DEMO", Toast.LENGTH_SHORT).show();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_main);
		
		mv = (MyView)findViewById(R.id.view_whiteboard);

		mPaint = new Paint();
		mPaint.setAntiAlias(true);
		mPaint.setDither(true);
		mPaint.setColor(0xFFFF0000);
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setStrokeJoin(Paint.Join.ROUND);
		mPaint.setStrokeCap(Paint.Cap.ROUND);
		mPaint.setStrokeWidth(PAINT_WIDTH);

		mEmboss = new EmbossMaskFilter(new float[] { 1, 1, 1 },0.4f, 6, 3.5f);

		mBlur = new BlurMaskFilter(8, BlurMaskFilter.Blur.NORMAL);

		mContext = getApplicationContext();
		
		/*
		 * Sensor stuff for shaker
		 */
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
	    mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
	    mAccel = 0.00f;
	    mAccelCurrent = SensorManager.GRAVITY_EARTH;
	    mAccelLast = SensorManager.GRAVITY_EARTH;
	}
	@Override
	protected void onResume() {
		super.onResume();

		//Register Shake listener
		mSensorManager.registerListener(mSensorListener, 
				mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), 
				SensorManager.SENSOR_DELAY_NORMAL);
 
		// Register broadcast recievers
		IntentFilter fileReceive = new IntentFilter();
		fileReceive.addAction(Consts.SAVE_FAIL_BITNULL);
		fileReceive.addAction(Consts.SAVE_FAIL_PERMISSIONS);
		fileReceive.addAction(Consts.SAVE_SUCCESS);
		registerReceiver(mBroadcastReceiver, fileReceive);
	}

	@Override
	protected void onStart(){
		super.onStart();
		/****************************************
		 * SERVICES
		 ***************************************/
		// Bind to LocalService
		Intent intent = new Intent(mContext, FileService.class);
		startService(intent);
		//http://developer.android.com/guide/components/bound-services.html
		bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
		Consts.DEBUG_LOG("ONSTART SERVICE IS", "" +mService);
	}

	protected void onStop(){
		super.onStop();
		//Unbind the Services
		if (mBound) {
			unbindService(mConnection);
			mBound = false;
		}
		// Unregister the Broadcast reciever
		unregisterReceiver(mBroadcastReceiver);
		//Unregister shaker
		mSensorManager.unregisterListener(mSensorListener);
	}

	private static final int COLOR_MENU_ID = Menu.FIRST;
	private static final int EMBOSS_MENU_ID = Menu.FIRST + 1;
	private static final int BLUR_MENU_ID = Menu.FIRST + 2;
	private static final int ERASE_MENU_ID = Menu.FIRST + 3;
	private static final int SRCATOP_MENU_ID = Menu.FIRST + 4;
	private static final int SAVE_MENU_ID = Menu.FIRST + 5;
	private static final int LOAD_MENU_ID = Menu.FIRST + 6;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		menu.add(0, COLOR_MENU_ID, 0, "Color").setShortcut('3', 'c');
		menu.add(0, EMBOSS_MENU_ID, 0, "Emboss").setShortcut('4', 's');
		menu.add(0, BLUR_MENU_ID, 0, "Blur").setShortcut('5', 'z');
		menu.add(0, ERASE_MENU_ID, 0, "Erase").setShortcut('5', 'z');
		menu.add(0, SRCATOP_MENU_ID, 0, "SrcATop").setShortcut('5', 'z');
		menu.add(0, SAVE_MENU_ID, 0, "Save").setShortcut('5', 'z');
		menu.add(0, LOAD_MENU_ID, 0, "Load").setShortcut('5', 'z');
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		mPaint.setXfermode(null);
		mPaint.setAlpha(0xFF);
		mPaint.setStrokeWidth(PAINT_WIDTH);
		mEraseMode  = false;

		switch (item.getItemId()) {
		case COLOR_MENU_ID:
			new ColorPickerDialog(this, this, mPaint.getColor()).show();
			return true;
		case EMBOSS_MENU_ID:
			if (mPaint.getMaskFilter() != mEmboss) {
				mPaint.setMaskFilter(mEmboss);
			} else {
				mPaint.setMaskFilter(null);
			}
			return true;
		case BLUR_MENU_ID:
			if (mPaint.getMaskFilter() != mBlur) {
				mPaint.setMaskFilter(mBlur);
			} else {
				mPaint.setMaskFilter(null);
			}
			return true;
		case ERASE_MENU_ID:
			setErase();
			return true;
		case SRCATOP_MENU_ID:
			mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
			mPaint.setAlpha(0x80);
			return true;
		case SAVE_MENU_ID:
			if (mService != null){
				mService.save(mBitmap);
			} else {
				Log.d("mService is null","mService is null");
			}
			return true;
		case LOAD_MENU_ID:
			Toast.makeText(mContext, "Feature not availible in DEMO", Toast.LENGTH_SHORT).show();
			//mBitmap = mService.load(mBitmap);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}


	/**
	 * View that we are listening to onTouch and drawing 
	 * the onTouch to a bitmap.  
	 * 
	 * We are using calling the custom view and inflating it 
	 * from the XML.  It needs to be static to be called from XML
	 * 
	 * @author mStanford
	 *
	 */
	public static class MyView extends View {

		private Path    mPath;
		private Paint   mBitmapPaint;


		public MyView(Context c) {
			super(c);

			mPath = new Path();
			mBitmapPaint = new Paint(Paint.DITHER_FLAG);
		}

		public MyView(Context c, AttributeSet a) {
			super(c,a);

			mPath = new Path();
			mBitmapPaint = new Paint(Paint.DITHER_FLAG);
		}
		public MyView(Context c, AttributeSet a, int d) {
			super(c,a,d);

			mPath = new Path();
			mBitmapPaint = new Paint(Paint.DITHER_FLAG);
		}


		@Override
		protected void onSizeChanged(int w, int h, int oldw, int oldh) {
			super.onSizeChanged(w, h, oldw, oldh);
			mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
			mCanvas = new Canvas(mBitmap);
			//http://stackoverflow.com/questions/9901024/android-bitmap-how-to-save-canvas-with-green-background-in-android
			mCanvas.drawColor(Color.WHITE);
			super.onSizeChanged(w, h, oldw, oldh);
		}


		@Override
		protected void onDraw(Canvas canvas) {
			//canvas.drawColor(0xFFAAAAAA);
			canvas.drawColor(0x00000000);
			canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
			canvas.drawPath(mPath, mPaint);
		}

		private float mX, mY;
		private static final float TOUCH_TOLERANCE = 4;

		private void touch_start(float x, float y) {
			mPath.reset();
			mPath.moveTo(x, y);
			mX = x;
			mY = y;
			mCanvas.drawPoint(x, y, mPaint);
		}
		
		@SuppressLint("WrongCall")
		public void clearDrawing(){
			mBitmap.eraseColor(Color.WHITE);
			mPath = null;
			mPath = new Path();
			this.invalidate();
		}

		private void touch_move(float x, float y) {
			float dx = Math.abs(x - mX);
			float dy = Math.abs(y - mY);
			if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
				mPath.quadTo(mX, mY, (x + mX)/2, (y + mY)/2);
				mX = x;
				mY = y;
				//if(mEraseMode){
					//commit the path to our offscreen
					//mCanvas.drawPath(mPath, mPaint);
					//touch_start(mX,mY);
				//}
			}
		}

		private void touch_up() {
			mPath.lineTo(mX, mY);
			// commit the path to our off screen
			mCanvas.drawPath(mPath, mPaint);
			// kill this so we don't double draw
			mPath.reset();
		}

		@Override
		public boolean onTouchEvent(MotionEvent event) {
			float x = event.getX();
			float y = event.getY();

			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				touch_start(x, y);
				invalidate();
				break;
			case MotionEvent.ACTION_MOVE:
				touch_move(x, y);
				invalidate();
				break;
			case MotionEvent.ACTION_UP:
				touch_up();
				invalidate();
				break;
			}
			return true;
		}
	}
}
