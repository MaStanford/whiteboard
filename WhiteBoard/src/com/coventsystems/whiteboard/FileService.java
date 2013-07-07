package com.coventsystems.whiteboard;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.OutputStream;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Service;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;

public class FileService extends Service {
	
	
	private static final String mDir = "/Whiteboard/";

	private String[] mFileList;
	private File mPath = new File(Environment.getExternalStorageDirectory() + "/whiteboard/");
	private String mChosenFile;
	private static final String FTYPE = ".jpg";    
	private static final int DIALOG_LOAD_FILE = 1000;
	private static final String TAG = null;

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

	protected void save(Bitmap mBitmap){
		if (mBitmap != null) {
			try {
				String path = Environment.getExternalStorageDirectory().toString() + mDir;
				OutputStream fOut = null;
				File file = new File(path, "screentest.jpg");
				fOut = new FileOutputStream(file);
				mBitmap.compress(Bitmap.CompressFormat.JPEG, 85, fOut);
				fOut.flush();
				fOut.close();
				Log.e("ImagePath", "Image Path : " + 
						MediaStore.Images.Media.insertImage( getContentResolver(), 
								file.getAbsolutePath(), file.getName(), file.getName()));
			}
			catch (Exception e) {
				Log.e("Save: ", "Not Saved - Exception");
				e.printStackTrace();
			}
		}else{
			Log.e("Save: ", "Not Saved - mBitmap NULL");
		}
	}

	protected Bitmap load(Bitmap bitMap){ 
		if (bitMap != null) {
			try {
				File root = Environment.getExternalStorageDirectory();
				mBitmap = BitmapFactory.decodeFile(root + mDir + "screentest.jpg");
				//mv.draw(mCanvas);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}else{
		}
		return mBitmap;
	}

	private void loadFileList() {
		try {
			mPath.mkdirs();
		}
		catch(SecurityException e) {
			Log.e(TAG, "unable to write on the sd card " + e.toString());
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

	protected Dialog onCreateDialog(int id) {
		Dialog dialog = null;
		AlertDialog.Builder builder = new Builder(this);

		switch(id) {
		case DIALOG_LOAD_FILE:
			builder.setTitle("Choose your file");
			if(mFileList == null) {
				Log.e(TAG, "Showing file picker before loading the file list");
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
		}
		dialog = builder.show();
		return dialog;
	}

}
