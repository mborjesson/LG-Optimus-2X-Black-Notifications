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

import android.content.*;
import android.preference.*;

import com.martinborjesson.o2xtouchlednotifications.*;
import com.martinborjesson.o2xtouchlednotifications.services.*;
import com.martinborjesson.o2xtouchlednotifications.touchled.*;
import com.martinborjesson.o2xtouchlednotifications.utils.*;

public class TouchLEDStaticPulseReceiver extends BroadcastReceiver {
	
	public final static String START_STATIC_PULSE = "START_STATIC_PULSE";
	public final static String STOP_STATIC_PULSE = "STOP_STATIC_PULSE";

	static private int maxLEDStrength = TouchLED.getTouchLED().getMax();
	static private boolean disabled = false;

    static private SharedPreferences prefs = null;
    static private TouchLED touchLED = null;

    static public void reset() {
    	prefs = null;
    	disabled = false;
    }
    
    static public void stopPulse() {
    	disabled = true;
    }

	@Override
	public void onReceive(Context context, Intent intent) {
		if (disabled) {
			return;
		}
		// this is fast enough so we can do everything without waking the device
    	if (prefs == null) {
    		Logger.logDebug("Loading properties...");
    		prefs = PreferenceManager.getDefaultSharedPreferences(context);
    		
    		AppProperties props = new AppProperties(context, MainService.TOUCH_LED_STATUS_FILE);
    		props.load();
    		String id = props.get(MainService.PROPERTY_ACTIVE_NOTIFICATION, Constants.PREFERENCE_KEY_DEFAULT_PULSE);

    		maxLEDStrength = prefs.getInt(id + "." + Constants.PREFERENCE_KEY_TOUCH_LED_BRIGHTNESS, Constants.DEFAULT_PULSE_MAX_LED_STRENGTH);
    		
    		Logger.logDebug("Pulse max LED strength: " + maxLEDStrength);
    	}
    	if (touchLED == null) {
    		touchLED = TouchLED.getTouchLED();
    	}
		if (intent != null && intent.getAction() != null) {
			
			if (intent.getAction().equals(START_STATIC_PULSE)) {
				Logger.logDebug("Start pulse");
				touchLED.set(TouchLED.SEARCH, maxLEDStrength);
			} else if (intent.getAction().equals(STOP_STATIC_PULSE)) {
				Logger.logDebug("Stop pulse");
				touchLED.set(TouchLED.SEARCH, touchLED.getMin());
			}
		}
	}
}
