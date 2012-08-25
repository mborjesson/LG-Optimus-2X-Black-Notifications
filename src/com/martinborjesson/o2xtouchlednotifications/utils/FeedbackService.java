/**
   Copyright 2011 Martin Börjesson

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package com.martinborjesson.o2xtouchlednotifications.utils;

import java.io.*;

import android.content.*;
import android.os.*;
import android.preference.*;

import com.martinborjesson.o2xtouchlednotifications.*;
import com.martinborjesson.o2xtouchlednotifications.services.*;
import com.martinborjesson.o2xtouchlednotifications.touchled.*;

public class FeedbackService {
	/**
	 * Start the service
	 * @param context
	 * @param testId Set to <code>null</code> to start service as usual
	 */
	public static void startService(Context context, String testId) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		if (prefs.getBoolean("checkBoxServiceEnabled", Constants.DEFAULT_SERVICE_ENABLED)) {
			Logger.logDebug("Starting service...");
			
	    	PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
	    	Intent in = new Intent(context, MainService.class);
	    	if (testId != null) {
	    		in.setAction(MainService.ACTION_DO_TEST);
	    		in.putExtra(MainService.EXTRAS_TEST_ID, testId);
	    	} else if (!pm.isScreenOn()) {
	    		in.setAction(MainService.ACTION_SCREEN_OFF);
	    	} 
			context.startService(in);
		} else {
			Logger.logDebug("Service is disabled");
		}
	}
	
	/**
	 * Perform special fixes, for example superuser-commands and other device-specific fixes
	 * @param context
	 */
	public static void performFixes(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		// perform root-fixes
		File[] files = TouchLED.getTouchLED().getFiles();
		if (prefs.getBoolean("rootPermissionFix", false) && files != null) {
			for (File file : files) {
				if (!file.canRead() || !file.canWrite()) {
					Logger.logDebug("Performing root fix...");
					SuperUser.doSuperUserCommand("chmod 666 " + file.toString());
				}
			}
		}
		TouchLED.reset();
	}
	
	/**
	 * Stop the feedback
	 * @param context
	 */
	public static void stopFeedback(Context context) {
    	Intent in = new Intent(context, MainService.class);
		in.setAction(MainService.ACTION_STOP_PULSE);
		context.startService(in);
	}
	
	/**
	 * Stop the service
	 * @param context
	 */
	public static void stopService(Context context) {
		Logger.logDebug("Stopping service...");
		Intent in = new Intent(context, MainService.class);
		context.stopService(in);
	}
	

}
