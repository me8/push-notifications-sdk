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

import java.util.HashMap;
import java.util.Map;

import android.util.Log;

import com.adobe.fre.FREContext;
import com.adobe.fre.FREFunction;

public class GCMExtensionContext extends FREContext {

	private static String TAG = "gcmContext";
	
	public GCMExtensionContext() {
		Log.d(TAG, "GCMExtensionContext.GCMExtensionContext");
	}
	
	@Override
	public void dispose() {
		Log.d(TAG, "GCMExtensionContext.dispose");
		GCMExtension.context = null;
	}

	/**
	 * Registers AS function name to Java Function Class
	 */
	@Override
	public Map<String, FREFunction> getFunctions() {
		Log.d(TAG, "GCMExtensionContext.getFunctions");
		
		Map<String, FREFunction> functionMap = new HashMap<String, FREFunction>();
		functionMap.put("registerPush", new GCMRegisterFunction());
		functionMap.put("setBadgeNumber", new SetBadgeValueFunction());
		functionMap.put("setIntTag", new SetIntTagFunction());
		functionMap.put("setStringTag", new SetStringTagFunction());
		functionMap.put("pause", new PauseFunction());
		functionMap.put("resume", new ResumeFunction());
		
		return functionMap;	
	}

}
