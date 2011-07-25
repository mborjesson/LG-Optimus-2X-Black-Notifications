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

package com.martinborjesson.o2xtouchlednotifications.receivers;

import android.content.*;
import android.preference.*;

import com.martinborjesson.o2xtouchlednotifications.*;
import com.martinborjesson.o2xtouchlednotifications.touchled.*;
import com.martinborjesson.o2xtouchlednotifications.utils.*;

/**
 * This receiver handles device booting
 * @author Martin Borjesson
 *
 */
public class BootReceiver extends BroadcastReceiver {

	public void onReceive(Context context, Intent intent) {
		FeedbackService.performFixes(context); // perform special fixes if needed
		if (TouchLED.getTouchLED().hasProperPermissions()) {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			if (TouchLED.getTouchLED().canChangeLEDBrightness() && prefs.getBoolean("checkBoxTouchLEDStrengthSetOnBootPref", Constants.DEFAULT_SET_TOUCH_LED_STRENGTH_ON_BOOT)) {
	    		int value = prefs.getInt("seekBarTouchLEDStrengthPref", Constants.DEFAULT_TOUCH_LED_STRENGTH);
				Logger.logDebug("Loading stored value for touch LED: " + value);
				TouchLED.getTouchLED().setAll(value);
			}
			if (prefs.getBoolean("checkboxAutostartService", Constants.DEFAULT_AUTOSTART_SERVICE)) {
				FeedbackService.startService(context, null);
			}
		}
	}
}
