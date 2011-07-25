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

import com.martinborjesson.o2xtouchlednotifications.services.*;
import com.martinborjesson.o2xtouchlednotifications.utils.*;

/**
 * This receiver handles screen on/off
 * @author Martin Borjesson
 *
 */
public class ScreenReceiver extends BroadcastReceiver {
	
	public void onReceive(Context context, Intent intent) {
		boolean screenOn = false;
		if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
			screenOn = true;
		} else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
			screenOn = false;
		}
		
		Logger.logDebug("Screen on: " + screenOn);
		
		Intent i = new Intent(context, MainService.class);
		i.setAction(!screenOn ? MainService.ACTION_SCREEN_OFF : MainService.ACTION_SCREEN_ON);
		context.startService(i);
	}
}
