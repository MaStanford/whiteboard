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
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.OutputStream;

import com.coventsystems.whiteboard.FileService.LocalBinder;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.*;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_main);

		MyView mv = (MyView)findViewById(R.id.view_whiteboard);

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
	}

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
			Log.d("SERVICE IS", "" +mService);
		}
		
		public void onServiceDisconnected(ComponentName className) {
			mBound = false;
		}
	};
	
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
		Log.d("ONSTART SERVICE IS", "" +mService);
	}

	protected void onStop(){
		super.onStop();
		//Unbind the Services
		if (mBound) {
			unbindService(mConnection);
			mBound = false;
		}
	}
	
	/***************************************************
	 * Handler
	 *************************************************/
	public Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.arg1){
			case Consts.SAVE_SUCCESS:
				Toast.makeText(mContext, "Save Successful", Toast.LENGTH_SHORT).show();
				break;
			case Consts.SAVE_FAIL_BITNULL:
				Toast.makeText(mContext, "Save failed, Bitmap is blank", Toast.LENGTH_SHORT).show();
				break;
			case Consts.SAVE_FAIL_PERMISSIONS:
				Toast.makeText(mContext, "Save Fail, Invalid Permissions", Toast.LENGTH_SHORT).show();
				break;
			}
		}
	};

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
			mBitmap = mService.load(mBitmap);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/*
	 * Save feature 
	 */
	public void save(View v){
		if (mService != null){
			mService.save(mBitmap);
		} else {
			Log.d("mService is null","mService is null");
		}
	}


	public void load(View v){
		mBitmap = mService.load(mBitmap);
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


		private static final float MINP = 0.25f;
		private static final float MAXP = 0.75f;

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
		}

		private void touch_move(float x, float y) {
			float dx = Math.abs(x - mX);
			float dy = Math.abs(y - mY);
			if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
				mPath.quadTo(mX, mY, (x + mX)/2, (y + mY)/2);
				mX = x;
				mY = y;
				if(mEraseMode){
					// commit the path to our offscreen
					mCanvas.drawPath(mPath, mPaint);
					touch_start(mX,mY);
				}
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
