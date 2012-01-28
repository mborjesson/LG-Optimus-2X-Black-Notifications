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

import com.commonsware.cwac.wakeful.*;
import com.martinborjesson.o2xtouchlednotifications.*;
import com.martinborjesson.o2xtouchlednotifications.services.*;
import com.martinborjesson.o2xtouchlednotifications.touchled.*;
import com.martinborjesson.o2xtouchlednotifications.touchled.devices.TouchLEDP350;
import com.martinborjesson.o2xtouchlednotifications.touchled.devices.TouchLEDP970;
import com.martinborjesson.o2xtouchlednotifications.utils.*;

public class TouchLEDService extends WakefulIntentService {
	static private int fadeInTime = 1000;
	static private int activeTime = 500;
	static private int fadeOutTime = 2500;
	static private int maxLEDStrength = TouchLED.getTouchLED().getMax();
    
	static private final int STATE_FADE_IN = 1;
	static private final int STATE_FADE_OUT = 2;
    
    static private boolean active = false;
    static private boolean running = false;
    static private boolean disabled = false;
    
    static private SharedPreferences prefs = null;
    static private TouchLED touchLED = null;
    
    public TouchLEDService() {
		super("TouchLEDService");
	}

	public static void waitUntilDone(long timeout) {
		long start = System.currentTimeMillis();
		if (running) {
			Logger.logDebug("Has to wait for pulse to complete...");
		}
		while (running && System.currentTimeMillis()-start < timeout) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
    private void doPulse(final int state) {
		running = true;
		try {
	    	active = true;
	    	int previousValue = 0;
        	long fadeStart = System.currentTimeMillis();
	        while (active) {
	        	long currentTime = System.currentTimeMillis()-fadeStart;
	        	if (state == STATE_FADE_IN || state == STATE_FADE_OUT) {
		        	int value = 0;
		        	if (state == STATE_FADE_IN) {
						value = (int)MathUtils.lerp(touchLED.getMin(), maxLEDStrength, (float)currentTime/fadeInTime);
		        	} else if (state == STATE_FADE_OUT) {
						value = (int)MathUtils.lerp(maxLEDStrength, touchLED.getMin(), (float)currentTime/fadeOutTime);
		        	}
	        		if (previousValue != value) {
	        			touchLED.set(TouchLED.SEARCH, value);
	        			previousValue = value;
	        		}
	        	}
	        	// change states
	        	if (state == STATE_FADE_IN && currentTime >= fadeInTime) {
	        		if (previousValue != maxLEDStrength) {
	        			touchLED.set(TouchLED.SEARCH, maxLEDStrength);
	        		}
	        		active = false;
	        	} else if (state == STATE_FADE_OUT && currentTime >= fadeOutTime) {
	        		if (previousValue != touchLED.getMin()) {
	        			touchLED.set(TouchLED.SEARCH, touchLED.getMin());
	        		}
	        		active = false;
	        	}
	        	
        		Thread.sleep(10); // some sleeping
	        }
		} catch (Exception e) {
			Logger.logDebug("TouchLEDPulse exception: " + e.getMessage());
		} finally {
		}
		running = false;
    }
    
    static public void stopPulse() {
    	disabled = true;
    	active = false;
    }
    
    static public void reset() {
    	prefs = null;
    	disabled = false;
    }

	@Override
	public void doWakefulWork(Intent intent) {
		if (disabled) {
			return;
		}
    	if (prefs == null) {
    		Logger.logDebug("Loading properties...");
    		prefs = PreferenceManager.getDefaultSharedPreferences(this);
    		
    		AppProperties props = new AppProperties(this, MainService.TOUCH_LED_STATUS_FILE);
    		props.load();
    		String id = props.get(MainService.PROPERTY_ACTIVE_NOTIFICATION, Constants.PREFERENCE_KEY_DEFAULT_PULSE);
    		fadeInTime = MainService.toInt(prefs.getString(id + "." + Constants.PREFERENCE_KEY_TOUCH_LED_FADE_IN_TIME, String.valueOf(Constants.DEFAULT_PULSE_FADE_IN)), Constants.DEFAULT_PULSE_FADE_IN);
    		fadeOutTime = MainService.toInt(prefs.getString(id + "." + Constants.PREFERENCE_KEY_TOUCH_LED_FADE_OUT_TIME, String.valueOf(Constants.DEFAULT_PULSE_FADE_OUT)), Constants.DEFAULT_PULSE_FADE_OUT);
    		activeTime = MainService.toInt(prefs.getString(id + "." + Constants.PREFERENCE_KEY_TOUCH_LED_FULLY_LIT_TIME, String.valueOf(Constants.DEFAULT_PULSE_ACTIVE)), Constants.DEFAULT_PULSE_ACTIVE);
    		//For some reasons on P350 im getting here brightes equal to 20 not 255
    		//Also I think pulsing is not supported on P350 - full light or none
    		if(!(TouchLED.getTouchLED() instanceof TouchLEDP350))
    			maxLEDStrength = prefs.getInt(id + "." + Constants.PREFERENCE_KEY_TOUCH_LED_BRIGHTNESS, Constants.DEFAULT_PULSE_MAX_LED_STRENGTH);
    		Logger.logDebug("Pulse fade in time: " + fadeInTime);
    		Logger.logDebug("Pulse active time: " + activeTime);
    		Logger.logDebug("Pulse fade out time: " + fadeOutTime);
    		Logger.logDebug("Pulse max LED strength: " + maxLEDStrength);
    	}
    	if (touchLED == null) {
    		touchLED = TouchLED.getTouchLED();
    	}
    	if (intent != null && intent.getAction() != null) {
    		if (intent.getAction().equals(TouchLEDReceiver.START_PULSE)) {
    			Logger.logDebug("Start pulse");
    			doPulse(STATE_FADE_IN);
    		} else if (intent.getAction().equals(TouchLEDReceiver.STOP_PULSE)) {
    			Logger.logDebug("Stop pulse");
    			doPulse(STATE_FADE_OUT);
    		}
    	}
	}
}
