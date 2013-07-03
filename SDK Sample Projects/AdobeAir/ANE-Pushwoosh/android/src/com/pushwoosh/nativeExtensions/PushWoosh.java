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

import com.arellomobile.android.push.BasePushMessageReceiver;
import com.arellomobile.android.push.utils.RegisterBroadcastReceiver;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;

import com.arellomobile.android.push.PushManager;
import com.arellomobile.android.push.exception.PushWooshException;
import com.arellomobile.android.push.tags.SendPushTagsAbstractAsyncTask;
import com.arellomobile.android.push.tags.SendPushTagsAsyncTask;
import com.arellomobile.android.push.tags.SendPushTagsCallBack;
import com.google.android.gcm.GCMRegistrar;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;
import android.os.Bundle;
import java.util.HashMap;
import java.util.Map;
import android.content.Context;

import com.adobe.fre.FREContext;
import com.adobe.fre.FREFunction;
import com.adobe.fre.FREObject;


class PushWoosh
{
    public static PushWoosh INSTANCE = null;
	
	private Activity mainActivity = null;
	private boolean broadcastPush = false;

    public PushWoosh()
    {
        INSTANCE = this;
    }
	
	public static PushWoosh getInstance()
	{
		if(INSTANCE == null)
			INSTANCE = new PushWoosh();
		
		return INSTANCE;
	}

    public int PushWooshNotificationRegister(Activity activity)
    {
		mainActivity = activity;
		
		registerReceivers();
		
        ApplicationInfo ai = null;
        try {
            ai = activity.getPackageManager().getApplicationInfo(activity.getApplicationContext().getPackageName(), PackageManager.GET_META_DATA);
            String pwAppid = ai.metaData.getString("PW_APPID");
            System.out.println("App ID: " + pwAppid);
            String projectId = ai.metaData.getString("PW_PROJECT_ID").substring(1);
            System.out.println("Project ID: " + projectId);
			
			broadcastPush = ai.metaData.getBoolean("PW_BROADCAST_PUSH");
			System.out.println("Broadcast push: " + broadcastPush);

            PushManager pushManager = new PushManager(activity, pwAppid, projectId);
            pushManager.onStartup(activity);
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        checkMessage(activity.getIntent());

        return 0;
    }
	
	BroadcastReceiver mBroadcastReceiver = new RegisterBroadcastReceiver()
	{
		@Override
		public void onRegisterActionReceive(Context context, Intent intent)
		{
			checkMessage(intent);
		}
	};
	
	public void registerReceivers()
	{
		IntentFilter intentFilter = new IntentFilter(mainActivity.getPackageName() + ".action.PUSH_MESSAGE_RECEIVE");

		if(broadcastPush)
			mainActivity.registerReceiver(mReceiver, intentFilter);
		
		mainActivity.registerReceiver(mBroadcastReceiver, new IntentFilter(mainActivity.getPackageName() + "." + PushManager.REGISTER_BROAD_CAST_ACTION));
	}
	
	public void onResume()
	{
		registerReceivers();
	}
	
	public void onPause()
	{
		try
		{
			mainActivity.unregisterReceiver(mReceiver);
		}
		catch (Exception e)
		{
			// pass.
		}
		
		try
		{
			mainActivity.unregisterReceiver(mBroadcastReceiver);
		}
		catch (Exception e)
		{
			// pass.
		}
	}
	
	private BroadcastReceiver mReceiver = new BasePushMessageReceiver()
	{
		@Override
		protected void onMessageReceive(Intent intent)
		{
			FREContext freContext = GCMExtension.context;
			if (freContext != null) {
				freContext.dispatchStatusEventAsync("PUSH_RECEIVED", intent.getStringExtra(JSON_DATA_KEY));
			}
		}
	};
	
	
    public int PushWooshNotificationSetIntTag(String tagName, int tagValue)
    {
		Map<String, Object> tags = new HashMap<String, Object>();
		tags.put(tagName, new Integer(tagValue));
		
		PushManager.sendTags(mainActivity, tags, null);
		return 0;
    }
	
    public int PushWooshNotificationSetStringTag(String tagName, String tagValue)
    {
		Map<String, Object> tags = new HashMap<String, Object>();
		tags.put(tagName, tagValue);
		
		PushManager.sendTags(mainActivity, tags, null);
		return 0;
    }
	
    public int PushWooshNotificationUnRegister(Activity activity)
    {
		mainActivity = activity;
        GCMRegistrar.unregister(mainActivity);
        return 0;
    }
    
    public int PushWooshClearLocalNotifications()
    {
		PushManager.clearLocalNotifications(mainActivity);
		return 0;
    }
	
    public int PushWooshScheduleLocalNotification(String message, int seconds, String userdata)
    {
		Bundle extras = new Bundle();
		if(userdata != null)
			extras.putString("u", userdata);
		
		PushManager.scheduleLocalNotification(mainActivity, message, extras, seconds);
        return 0;
    }

    public void checkMessage(Intent intent)
    {
        if (null != intent)
        {
            if (intent.hasExtra(PushManager.PUSH_RECEIVE_EVENT))
            {
                //showMessage("push message is " + intent.getExtras().getString(PushManager.PUSH_RECEIVE_EVENT));
				
				FREContext freContext = GCMExtension.context;
				if (freContext != null) {
					freContext.dispatchStatusEventAsync("PUSH_RECEIVED", intent.getExtras().getString(PushManager.PUSH_RECEIVE_EVENT));
				}
            }
            else if (intent.hasExtra(PushManager.REGISTER_EVENT))
            {
                //showMessage("register");
				
				FREContext freContext = GCMExtension.context;
				if (freContext != null) {
					freContext.dispatchStatusEventAsync("TOKEN_SUCCESS", intent.getExtras().getString(PushManager.REGISTER_EVENT));
				}
            }
            else if (intent.hasExtra(PushManager.UNREGISTER_EVENT))
            {
                //showMessage("unregister");
            }
            else if (intent.hasExtra(PushManager.REGISTER_ERROR_EVENT))
            {
                //showMessage("register error");
				
				FREContext freContext = GCMExtension.context;
				if (freContext != null) {
					freContext.dispatchStatusEventAsync("TOKEN_FAIL", intent.getExtras().getString(PushManager.REGISTER_ERROR_EVENT));
				}
            }
            else if (intent.hasExtra(PushManager.UNREGISTER_ERROR_EVENT))
            {
                //showMessage("unregister error");
            }
        }
    }

    private void showMessage(final String message)
    {
        mainActivity.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(mainActivity, message, Toast.LENGTH_LONG).show();
            }
        });
    }
}
