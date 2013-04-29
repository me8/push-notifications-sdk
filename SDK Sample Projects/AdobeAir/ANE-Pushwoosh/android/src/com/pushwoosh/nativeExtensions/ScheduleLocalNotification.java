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
import android.util.Log;
import org.json.JSONObject;
import org.json.JSONException;

import com.adobe.fre.FREContext;
import com.adobe.fre.FREFunction;
import com.adobe.fre.FREObject;

public class ScheduleLocalNotification implements FREFunction {

	private static String TAG = "scheduleLocalNotification";
	
	public FREObject call(FREContext context, FREObject[] args)
	{
		//timeInSeconds, json string: {alertBody: text, alertAction:text, soundName:text, badge: int, custom: {json}}
		if (args == null)
		{
			Log.e(TAG, "Wrong arguments.");
			return null;
		}
		
		
		Integer seconds;
		try {
			seconds = args[0].getAsInt();
		} catch (Exception e) {
			Log.e(TAG, "Wrong object passed for Seconds Value. Object expected: Integer.");
			return null;
		}
		
		String messageJson;
		try {
			messageJson = args[1].getAsString();
		} catch (Exception e) {
			Log.e(TAG, "Wrong object passed for local notification. Object expected: JSON String.");
			return null;
		}
		
		try
		{
				JSONObject notification = new JSONObject(messageJson);
				
				String message = notification.getString("alertBody");
				String userData = null;
				if(notification.has("custom"))
					userData = notification.getJSONObject("custom").toString();
				
				PushWoosh.getInstance().PushWooshScheduleLocalNotification(message, seconds, userData);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
			Log.e(TAG, "Local notification: Cannot parse JSON String.");
			return null;
		}

		return null;
	}
}
