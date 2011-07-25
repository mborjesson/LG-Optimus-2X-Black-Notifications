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

package com.martinborjesson.o2xtouchlednotifications.feedbacks;

import com.martinborjesson.o2xtouchlednotifications.*;
import com.martinborjesson.o2xtouchlednotifications.services.*;
import com.martinborjesson.o2xtouchlednotifications.utils.*;

import android.content.*;
import android.os.*;
import android.preference.*;

public class VibratorReceiver extends BroadcastReceiver {
	static final public String ACTION_START_VIBRATOR = VibratorReceiver.class.getName() + ".ACTION_START_VIBRATOR";
	
    static private SharedPreferences prefs = null;
    
    static private Vibrator vibrator = null;
    static private int vibrateTime = 0;
    static private int vibrateDelay = 0;
    static private float vibrateSlowerOverTime = Constants.DEFAULT_VIBRATE_SLOWER_OVER_TIME;
    
    static private final int TYPE_ONCE = 0;
    static private final int TYPE_CONSTANT = 1;
    static private final int TYPE_SLOWER_OVER_TIME = 2;
    
    static private long numVibrations = 0;
    static private int vibrateType = TYPE_SLOWER_OVER_TIME;

    static public void stop() {
    	if (vibrator != null) {
    		vibrator.cancel();
    	}
    	reset();
    }
    
    static public void reset() {
    	prefs = null;
    	vibrator = null;
    	numVibrations = 0;
    }

    static public void init(Context context, String pulseKey) {
    	if (prefs == null) {
    		Logger.logDebug("Loading vibrator properties...");
    		prefs = PreferenceManager.getDefaultSharedPreferences(context);

			vibrateTime = MainService.toInt(prefs.getString(pulseKey + "." + Constants.PREFERENCE_KEY_VIBRATION_DURATION, String.valueOf(Constants.DEFAULT_VIBRATE_DURATION)), Constants.DEFAULT_VIBRATE_DURATION);
			vibrateDelay = MainService.toInt(prefs.getString(pulseKey + "." + Constants.PREFERENCE_KEY_VIBRATION_DELAY, String.valueOf(Constants.DEFAULT_VIBRATE_DELAY)), Constants.DEFAULT_VIBRATE_DELAY);
			vibrateType = MainService.toInt(prefs.getString(pulseKey + "." + Constants.PREFERENCE_KEY_VIBRATION_MODE, String.valueOf(Constants.DEFAULT_VIBRATE_TYPE)), Constants.DEFAULT_VIBRATE_TYPE);
			Logger.logDebug("Pulse vibrate duration: " + vibrateTime);
			vibrator = (Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE);
    	}
    }

	@Override
	public void onReceive(Context context, Intent intent) {
		if (vibrator == null) {
			return;
		}
		vibrator.vibrate(vibrateTime);
		
		// register new alarm
		int delay = 0;
		if (vibrateType == TYPE_CONSTANT) {
    		delay = vibrateDelay;
    	} else if (vibrateType == TYPE_SLOWER_OVER_TIME) {
    		delay = (int)Math.round(Math.max(vibrateDelay, Math.pow(numVibrations, vibrateSlowerOverTime)*1000));
    	}
		Logger.logDebug("Vibrate delay: " + delay);
    	if (delay > 0) {
    		MainService.startAlarm(context, VibratorReceiver.ACTION_START_VIBRATOR, VibratorReceiver.class, vibrateTime+delay, 0, MainService.ALARM_TYPE_BROADCAST, false);
    	}
		numVibrations++;
	}

}
