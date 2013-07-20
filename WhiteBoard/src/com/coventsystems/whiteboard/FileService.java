package com.coventsystems.whiteboard;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Service;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;

public class FileService extends Service {

	private String[] mFileList;
	private File mPath = new File(Environment.getExternalStorageDirectory() + "/whiteboard/");
	private String mChosenFile;
	private static final String FTYPE = ".jpg";    
	private static final int DIALOG_LOAD_FILE = 1000;
	//private static final int DIALOG_SAVE_FILE = 2000;
	private static final String TAG = "FileService";
	
	boolean mExternalStorageAvailable = false;
	boolean mExternalStorageWriteable = false;
	String state = Environment.getExternalStorageState();
	
	private String fileName = "tmp";
	SharedPreferences mSharedPrefs;
	
	Bitmap mBitmap;

	public class LocalBinder extends Binder {
		public FileService getService() {
			return FileService.this;
		}
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return mBinder;
	}

	// Binder given to clients
	private final IBinder mBinder = new LocalBinder();

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return super.onStartCommand(intent, flags, startId);
	}
	

	
	@Override
	public void onCreate() {
		super.onCreate();
	    mSharedPrefs = getSharedPreferences(Consts.SHARED_KEY, 0);
	}



	@Override
	public void onDestroy() {
		super.onDestroy();
	}


	protected boolean isExternalAvailible(){
		if (Environment.MEDIA_MOUNTED.equals(state)) {
		    // We can read and write the media
		    mExternalStorageAvailable = mExternalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
		    // We can only read the media
		    mExternalStorageAvailable = true;
		    mExternalStorageWriteable = false;
		} else {
		    // Something else is wrong. It may be one of many other states, but all we need
		    //  to know is we can neither read nor write
		    mExternalStorageAvailable = mExternalStorageWriteable = false;
		}
		
		return mExternalStorageWriteable;
	}

	protected String getSaveFileDir(){
		if(isExternalAvailible()){
			Log.d("GetSaveFileDir",mPath.toString());
			if(!mPath.exists())
				mPath.mkdirs();
			return Environment.getExternalStorageDirectory().toString() + "/";
		}
		Log.d("GetSaveFileDir",getFilesDir().getAbsolutePath());
		return getFilesDir().getPath()+ "/";
	}
	
	protected String getFileName(){
		
		String mTAG = TAG +  "getFileName";
		
		SharedPreferences.Editor editor = mSharedPrefs.edit();
		String prevTime = mSharedPrefs.getString(Consts.KEY_SAVED_TIME, "1-1-3001");
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
		String currTime =  sdf.format(new Date());
		int numSuffix = mSharedPrefs.getInt(Consts.KEY_SAVED_NUM, 0);
		
		if(prevTime.contentEquals(currTime)){
		    editor.putString(Consts.KEY_SAVED_TIME, currTime);
			editor.putInt(Consts.KEY_SAVED_NUM,++numSuffix);
		    editor.commit();
		    Consts.DEBUG_LOG(mTAG + "getFileName", currTime + "-" + numSuffix);
			return currTime + "-" + numSuffix;
		}
	    editor.putString(Consts.KEY_SAVED_TIME, currTime);
		editor.putInt(Consts.KEY_SAVED_NUM,0);
		editor.commit();
		Log.d("FileName", currTime + "-" + numSuffix);
		return currTime;
	}
	
	protected void save(final Bitmap mBitmap){
		new Thread(){
			@Override
			public void run() {
				Looper.prepare();
					if (mBitmap != null) {
						try {
							//showDialog(2000);
							String path = getSaveFileDir();
							OutputStream fOut = null;
							File file = new File(path, getFileName() + ".jpg");
							fOut = new FileOutputStream(file);
							mBitmap.compress(Bitmap.CompressFormat.JPEG, 85, fOut);
							fOut.flush();
							fOut.close();
							Consts.DEBUG_LOG("File Saved", "Location = " + path + getFileName() + ".jpg");
							Consts.DEBUG_LOG("ImagePath", "Image Path : " + 
									MediaStore.Images.Media.insertImage( getContentResolver(), 
											file.getAbsolutePath(), file.getName(), file.getName()));
							/*
							 * Broadcast Save is successful to Activity
							 */
							Intent saveSuccess = new Intent(Consts.SAVE_SUCCESS);
							getApplicationContext().sendBroadcast(saveSuccess);
						}
						catch (Exception e) {
							Log.e("Save: ", "Not Saved - Exception");
							Intent saveSuccess = new Intent(Consts.SAVE_FAIL_PERMISSIONS);
							getApplicationContext().sendBroadcast(saveSuccess);
							e.printStackTrace();
						}
					}else{
						Log.e("Save: ", "Not Saved - mBitmap NULL");
						Intent saveSuccess = new Intent(Consts.SAVE_FAIL_BITNULL);
						getApplicationContext().sendBroadcast(saveSuccess);
					}
			}
		}.start();
	}
	protected void saveNaming(final Bitmap mBitmap, final String saveName){
		new Thread(){
			@Override
			public void run() {
				Looper.prepare();
					if (mBitmap != null) {
						try {
							//showDialog(2000);
							String path = saveName;
							//String path = getSaveFileDir();
							OutputStream fOut = null;
							File file = new File(path, saveName + ".jpg");
							fOut = new FileOutputStream(file);
							mBitmap.compress(Bitmap.CompressFormat.JPEG, 85, fOut);
							fOut.flush();
							fOut.close();
							Consts.DEBUG_LOG("File Saved", "Location = " + path + getFileName() + ".jpg");
							Consts.DEBUG_LOG("ImagePath", "Image Path : " + 
									MediaStore.Images.Media.insertImage( getContentResolver(), 
											file.getAbsolutePath(), file.getName(), file.getName()));
							/*
							 * Broadcast Save is successful to Activity
							 */
							Intent saveSuccess = new Intent(Consts.SAVE_SUCCESS);
							getApplicationContext().sendBroadcast(saveSuccess);
						}
						catch (Exception e) {
							Log.e("Save: ", "Not Saved - Exception");
							Intent saveSuccess = new Intent(Consts.SAVE_FAIL_PERMISSIONS);
							getApplicationContext().sendBroadcast(saveSuccess);
							e.printStackTrace();
						}
					}else{
						Log.e("Save: ", "Not Saved - mBitmap NULL");
						Intent saveSuccess = new Intent(Consts.SAVE_FAIL_BITNULL);
						getApplicationContext().sendBroadcast(saveSuccess);
					}
			}
		}.start();
	}

	/*
	 * Loads a bmp from a path and returns it
	 */
	protected Bitmap load(Bitmap bitMap){ 
		if (bitMap != null) {
			try {
				File root = Environment.getExternalStorageDirectory();
				mBitmap = BitmapFactory.decodeFile(mPath + "screentest.jpg");
				//mv.draw(mCanvas);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}else{
		}
		return mBitmap;
	}

	
	protected void sendEmail(){
		
	}
	
	/*
	 * Loads a list of files at a specified location and of a specified type
	 */
	private void loadFileList() {
		try {
			mPath.mkdirs();
		}
		catch(SecurityException e) {
			Consts.DEBUG_LOG(TAG, "unable to write on the sd card " + e.toString());
		}
		if(mPath.exists()) {
			FilenameFilter filter = new FilenameFilter() {
				public boolean accept(File dir, String filename) {
					File sel = new File(dir, filename);
					return filename.contains(FTYPE) || sel.isDirectory();
				}
			};
			mFileList = mPath.list(filter);
		}
		else {
			mFileList= new String[0];
		}
	}

	/*
	 * Reads a list of files and lets the user choose 1
	 */
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = null;
		AlertDialog.Builder builder = new Builder(this);

		switch(id) {
		case DIALOG_LOAD_FILE:
			builder.setTitle("Choose your file");
			if(mFileList == null) {
				Consts.DEBUG_LOG(TAG, "Showing file picker before loading the file list");
				dialog = builder.create();
				return dialog;
			}
			builder.setItems(mFileList, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					mChosenFile = mFileList[which];
					//you can do stuff with the file here too
				}
			});
			break;
			/*case DIALOG_SAVE_FILE:
				builder.setTitle("Choose your file");
				if(mFileList == null) {
					Consts.DEBUG_LOG(TAG, "Showing file picker before loading the file list");
					dialog = builder.create();
					return dialog;
				}
				builder.setItems(mFileList, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						mChosenFile = mFileList[which];
						//you can do stuff with the file here too
					}
				});
				break;*/
		}
		dialog = builder.show();
		return dialog;
	}
}
