//////////////////////////////////////////////////////////////////////////////////////
//
//  Copyright 2013 Pushwoosh (http://pushwoosh.com)
//
//  Huge thanks goes to Freshplanet (http://freshplanet.com | opensource@freshplanet.com)
//  
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//  
//  http://www.apache.org/licenses/LICENSE-2.0
//  
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
//  
//////////////////////////////////////////////////////////////////////////////////////

package com.pushwoosh.nativeExtensions;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.util.HashMap;
import java.util.Map;

public class PushActivity extends Activity
{
	/**
	 * Called when the activity is first created.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		finish();

		Intent notifyIntent = new Intent();
		notifyIntent.setAction(Intent.ACTION_MAIN);
		notifyIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        PackageManager manager = this.getPackageManager();
        Intent launchIntent = manager.getLaunchIntentForPackage(this.getPackageName());

		notifyIntent.setComponent(launchIntent.getComponent());
		
		notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
		notifyIntent.putExtras(getIntent().getExtras());
        
    	if(PushWoosh.INSTANCE != null) {
    	 	PushWoosh.INSTANCE.checkMessage(notifyIntent);
    	}

   		this.startActivity(notifyIntent);
	}
}
