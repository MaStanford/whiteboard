package com.coventsystems.whiteboard;

import android.util.Log;

public class Consts {

	protected static final String SAVE_SUCCESS = "SAVE_SUCCESS";
	protected static final String SAVE_FAIL_BITNULL = "SAVE_FAIL_BITNULL";
	protected static final String SAVE_FAIL_PERMISSIONS = "SAVE_FAIL_PERMISSIONS";

	protected static final String SHARED_KEY = "SHARED_KEY";
	protected static final String KEY_SAVED_NUM = "KEY_SAVED_NUM";
	protected static final String KEY_SAVED_TIME = "KEY_SAVED_TIME";
	
	protected static final boolean DEBUG = false;
	protected static final boolean DEBUG_VERBOSE = false;
	
	public static void DEBUG_LOG(String tag,String msg){
		if(DEBUG)
			Log.d(tag, msg);
	}
}
