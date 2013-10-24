package com.coventsystems.whiteboard;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class AboutScreen extends Activity {

	RelativeLayout mAboutClick;
	TextView tvVersion;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_about);
		
		tvVersion = (TextView) findViewById(R.id.about_version);
		try {
		PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
		tvVersion.setText("Version " + pInfo.versionCode/100.0);
		} catch (NameNotFoundException e) {}
		
		mAboutClick = (RelativeLayout) findViewById(R.id.about_view);
		mAboutClick.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				finish();
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}