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

import com.adobe.fre.FREContext;
import com.adobe.fre.FREFunction;
import com.adobe.fre.FREObject;

public class SetIntTagFunction implements FREFunction {
	
	private static String TAG = "setIntTag";
	
	public FREObject call(FREContext context, FREObject[] args)
	{
		if (args == null || args.length != 2)
		{
			Log.e(TAG, "Wrong arguments.");
			return null;
		}
		
		String tagName;
		try {
			tagName = args[0].getAsString();
		} catch (Exception e) {
			Log.e(TAG, "Wrong object passed for tag Name. Object expected: String. Cannot set tag.");
			return null;
		}
		
		if (tagName == null)
		{
			Log.e(TAG, "tagName is null. Cannot set tag.");
			return null;
		}

		Integer tagValue;
		try {
			tagValue = args[1].getAsInt();
		} catch (Exception e) {
			Log.e(TAG, "Wrong object passed for tag Value. Object expected: Integer. Cannot set tag.");
			return null;
		}
		
		if (tagValue == null)
		{
			Log.e(TAG, "tagValue is null. Cannot set tag.");
			return null;
		}
		
		PushWoosh.getInstance().PushWooshNotificationSetIntTag(tagName, tagValue);
		return null;
	}
}
