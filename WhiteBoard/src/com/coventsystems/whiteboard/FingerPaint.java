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

import java.io.File;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.EmbossMaskFilter;
import android.graphics.MaskFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore;
import android.text.Editable;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.Toast;

import com.coventsystems.whiteboard.FileService.LocalBinder;

public class FingerPaint extends Activity implements ColorPickerDialog.OnColorChangedListener {

	MyView mv;

	Context mContext;
	static Bitmap  mLoadedBitmap;

	private static Paint mPaint;
	private MaskFilter  mEmboss;
	private MaskFilter  mBlur;
	private static Bitmap  mBitmap;
	private static Canvas  mCanvas;
	private static boolean mEraseMode = false;
	private static final int PAINT_WIDTH = 5;
	private static final int ERASE_WIDTH = 50;
	private static int RESULT_LOAD_IMAGE = 1;
	private static boolean mSaveType = true;  //temporary for testing
	private static int mScreenHeight = 0;
	private static int mScreenWidth = 0;
	private static boolean mEmbossState = false;
	private static boolean mFadedState = false;
	private static boolean mBlurState = false;
	private static boolean mGraphPaperState = false;

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
			String tempFileName = mService.getFileName(false, false, null); //calls getFileName for most recent name from save
			Toast.makeText(mContext, "Save Successful, Saved at " + 
					mService.getSaveFileDir() + tempFileName, Toast.LENGTH_LONG).show();
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
			if(mAccel > 10){
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
			if (mSaveType) {
				Dialog saveDialog = onCreateDialog();
				saveDialog.show();
			}
			else {
				String mChosenName = mService.getFileName(true, true, "default");
				mService.save(mBitmap, mChosenName);
			}
		}
		else {
				Consts.DEBUG_LOG("mService is null","mService is null");
		}
	}
	public void setGraph(View v){
		if(mGraphPaperState) {
			mGraphPaperState = false;
			mv.removeGraph();
		}
		else {
			mGraphPaperState = true;
			mv.graphPaper(mGraphPaperState);
		}
	}

	public void sendEmail(View v){
		Intent mEmail = new Intent(Intent.ACTION_SEND);
		mEmail.setType("message/rfc822");
		mEmail.putExtra(Intent.EXTRA_EMAIL  , new String[]{"recipient@example.com"});
		mEmail.putExtra(Intent.EXTRA_SUBJECT, "Subject");
		mEmail.putExtra(Intent.EXTRA_TEXT   , "Body");
		String mFileName = mService.getFileName(true, true, "default");
		String mFilePath = mService.getSaveFileDir();
		mService.save(mBitmap, mFileName);
		Uri mFileUri = Uri.parse("file://" + mFilePath + mFileName + ".jpg");
		mEmail.putExtra(Intent.EXTRA_STREAM, mFileUri);
		try {
		    startActivity(Intent.createChooser(mEmail, "Email Picture"));
		} catch (android.content.ActivityNotFoundException ex) {
		    Toast.makeText(FingerPaint.this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
		}
	}
	
	/**
	 * This is forcing me into SDK 10.
	 * https://groups.google.com/forum/#!topic/android-developers/Kx2PBIbwf0s
	 * If we use no action bar then we are not allowed to have an options menu, 
	 * which would mean I would have to make our own menu, time permitting that
	 * can be done next build -mStanford
	 */
	public void onOpenMenu(View v){
		openOptionsMenu();
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
//	private static final int EMBOSS_MENU_ID = Menu.FIRST + 1;
//	private static final int BLUR_MENU_ID = Menu.FIRST + 2;
	private static final int ERASE_MENU_ID = Menu.FIRST + 3;
//	private static final int SRCATOP_MENU_ID = Menu.FIRST + 4;
	private static final int SAVE_MENU_ID = Menu.FIRST + 5;
	private static final int LOAD_MENU_ID = Menu.FIRST + 6;
	private static final int SETTINGS_MENU_ID = Menu.FIRST + 7;
	private static final int ABOUT_MENU_ID = Menu.FIRST + 8;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		menu.add(0, COLOR_MENU_ID, 0, "Color").setShortcut('3', 'c');
	//	menu.add(0, EMBOSS_MENU_ID, 0, "Emboss").setShortcut('4', 's');
	//	menu.add(0, BLUR_MENU_ID, 0, "Blur").setShortcut('5', 'z');
		menu.add(0, ERASE_MENU_ID, 0, "Erase").setShortcut('5', 'z');
	//	menu.add(0, SRCATOP_MENU_ID, 0, "Faded").setShortcut('5', 'z');
		menu.add(0, SAVE_MENU_ID, 0, "Save").setShortcut('5', 'z');
		menu.add(0, LOAD_MENU_ID, 0, "Load").setShortcut('5', 'z');
		menu.add(0, SETTINGS_MENU_ID, 0, "Settings").setShortcut('5', 'z');
		menu.add(0, ABOUT_MENU_ID, 0, "About").setShortcut('5', 'z');
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
	/*	case EMBOSS_MENU_ID:
			if (mPaint.getMaskFilter() != mEmboss) {
				mPaint.setMaskFilter(mEmboss);
				mEmbossState = true;
			} else {
				mPaint.setMaskFilter(null);
				mEmbossState = false;
			}
			return true;
		case BLUR_MENU_ID:
			if (mPaint.getMaskFilter() != mBlur) {
				mPaint.setMaskFilter(mBlur);
				mBlurState = true;
			} else {
				mPaint.setMaskFilter(null);
				mBlurState = false;
			}
			return true; */
		case ERASE_MENU_ID:
			setErase();
			return true;
	/*	case SRCATOP_MENU_ID:
			mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
			mPaint.setAlpha(0x80);
			mFadedState = true;
			return true; */
		case SAVE_MENU_ID:
			if (mService != null){
				if (mSaveType) {
					Dialog saveDialog = onCreateDialog();
					saveDialog.show();
				}
				else {
					String mChosenName = mService.getFileName(true, true, "default");
					mService.save(mBitmap, mChosenName);
				}
			}
			else {
					Consts.DEBUG_LOG("mService is null","mService is null");
			}
			return true;
		case LOAD_MENU_ID:
			Intent i = new Intent(
                    Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(i, RESULT_LOAD_IMAGE);
			return true;
		case ABOUT_MENU_ID:
			final Intent  intent = new Intent(this, AboutScreen.class);
			startActivity(intent);
			return true;
		case SETTINGS_MENU_ID:
			Dialog settingsDialog = onCreateSettingsDialog();
			settingsDialog.show();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/*
	 * creates dialog pop-up for entering desired save name
	 */
	protected AlertDialog onCreateDialog() {
		AlertDialog.Builder buildDialog = new AlertDialog.Builder(this);
		buildDialog.setTitle(getString(R.string.dialog_save_title));
		final EditText mUserInput = new EditText(this);
		buildDialog.setView(mUserInput);
		buildDialog.setPositiveButton(R.string.dialog_save_button, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				Editable mUserTextInput = (Editable)mUserInput.getText(); 
				String mSaveName = mUserTextInput.toString();
				mSaveName = mSaveName.replaceAll("[^a-zA-Z0-9\\s]", "");
				mService.save(mBitmap, mSaveName);
			}
		});
		buildDialog.setNegativeButton(R.string.dialog_cancel_button, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
			}
		});
		AlertDialog saveDialog = buildDialog.create();
		return saveDialog;
	}
	protected AlertDialog onCreateSettingsDialog() {
		AlertDialog.Builder buildDialog = new AlertDialog.Builder(this);
		buildDialog.setTitle(getString(R.string.dialog_settings_title));
		final String[] mSettingsList = {"Emboss", "Blur", "Faded"};
		final boolean [] mSelectedSettings = {mEmbossState, mBlurState, mFadedState};
		buildDialog.setMultiChoiceItems(mSettingsList, mSelectedSettings, new DialogInterface.OnMultiChoiceClickListener() {
			public void onClick(DialogInterface dialogInterface, int mItem, boolean mCheck) {
				switch(mItem){
				case 0:
					if (mPaint.getMaskFilter() != mEmboss) {
						mPaint.setMaskFilter(mEmboss);
						mEmbossState = true;
					} else {
						mPaint.setMaskFilter(null);
						mEmbossState = false;
					}
					return;
				case 1:
					if (mPaint.getMaskFilter() != mBlur) {
						mPaint.setMaskFilter(mBlur);
						mBlurState = true;
					} else {
						mPaint.setMaskFilter(null);
						mBlurState = false;
					}
					return;
				case 2:
					if(mFadedState) {
						mPaint.setMaskFilter(null);
						mFadedState = false;
					}
					else
					{
						mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
						mPaint.setAlpha(0x80);
						mFadedState = true;
					}
					return;
				}
			}
		});
		buildDialog.setPositiveButton(R.string.dialog_save_button, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
			}
		});
		AlertDialog saveDialog = buildDialog.create();
		return saveDialog;
	}
	
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
         
        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };

            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();
 
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();
            try {
				mLoadedBitmap = BitmapFactory.decodeFile(picturePath);
	            mv.loadCanvas(mLoadedBitmap);
			}
			catch (Exception e) {
				Toast.makeText(mContext, "Incorrect File Choice", Toast.LENGTH_LONG).show();
				e.printStackTrace();
			}
        }
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
		private Paint   mGraphPaint;


		public MyView(Context c) {
			super(c);

			mPath = new Path();
			mBitmapPaint = new Paint(Paint.DITHER_FLAG);
			mGraphPaint = new Paint(Paint.DITHER_FLAG);
		}

		public MyView(Context c, AttributeSet a) {
			super(c,a);

			mPath = new Path();
			mBitmapPaint = new Paint(Paint.DITHER_FLAG);
			mGraphPaint = new Paint(Paint.DITHER_FLAG);
		}
		public MyView(Context c, AttributeSet a, int d) {
			super(c,a,d);

			mPath = new Path();
			mBitmapPaint = new Paint(Paint.DITHER_FLAG);
			mGraphPaint = new Paint(Paint.DITHER_FLAG);
		}


		@Override
		protected void onSizeChanged(int w, int h, int oldw, int oldh) {
			super.onSizeChanged(w, h, oldw, oldh);
			mScreenHeight = h;
			mScreenWidth = w;
			mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
			mCanvas = new Canvas(mBitmap);
			//http://stackoverflow.com/questions/9901024/android-bitmap-how-to-save-canvas-with-green-background-in-android
			mCanvas.drawColor(Color.WHITE);
			graphPaper(mGraphPaperState);
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
			graphPaper(mGraphPaperState);
		}
		
		@SuppressLint("WrongCall")
		public void clearDrawing(){
			mBitmap.eraseColor(Color.WHITE);
			mPath = null;
			mPath = new Path();
			graphPaper(mGraphPaperState);
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
			graphPaper(mGraphPaperState);
		}
		
		public void graphPaper(boolean mStatus){
			if(mStatus) {
				float mHeight = mScreenHeight;
				float mWidth = mScreenWidth;
				float mHeightInc = mScreenHeight / 25;
				float mWidthInc = mScreenWidth / 25;
				mGraphPaint.setColor(Color.GRAY);
				while(mWidth > 0) {
					mCanvas.drawLine(mWidth, 0, mWidth, mScreenHeight, mGraphPaint);
					mWidth = mWidth - mWidthInc;
				}
				while(mHeight > 0) {
					mCanvas.drawLine(0, mHeight, mScreenWidth, mHeight, mGraphPaint);
					mHeight = mHeight - mHeightInc;
				}
			}
			this.invalidate();
		}
		
		public void removeGraph()
		{
			float mHeight = mScreenHeight;
			float mWidth = mScreenWidth;
			float mHeightInc = mScreenHeight / 25;
			float mWidthInc = mScreenWidth / 25;
			mGraphPaint.setColor(Color.WHITE);
			while(mWidth > 0) {
				mCanvas.drawLine(mWidth, 0, mWidth, mScreenHeight, mGraphPaint);
				mWidth = mWidth - mWidthInc;
			}
			while(mHeight > 0) {
				mCanvas.drawLine(0, mHeight, mScreenWidth, mHeight, mGraphPaint);
				mHeight = mHeight - mHeightInc;
			}
			this.invalidate();
		}
		
		public void loadCanvas(Bitmap mNewBitMap)
		{
			mCanvas.drawBitmap(mNewBitMap, 0, 0, null);
			this.invalidate();
		}
		public void activateGraph()
		{
			graphPaper(mGraphPaperState);
		}

		private void touch_up() {
			mPath.lineTo(mX, mY);
			// commit the path to our off screen
			mCanvas.drawPath(mPath, mPaint);
			// kill this so we don't double draw
			mPath.reset();
			graphPaper(mGraphPaperState);
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
