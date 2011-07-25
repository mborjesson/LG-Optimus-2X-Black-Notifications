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

package com.martinborjesson.o2xtouchlednotifications.notifications;

import android.content.*;
import android.telephony.*;

import com.martinborjesson.o2xtouchlednotifications.services.*;
import com.martinborjesson.o2xtouchlednotifications.utils.*;

public class CallListener extends PhoneStateListener {
	public static final String ID = CallListener.class.getName() + ".MISSED_CALL";

	private int previousState = TelephonyManager.CALL_STATE_IDLE; // TODO: set this to the current when it is created and not assume idle
	private Context context= null;
	
	public CallListener(Context context) {
		this.context = context;
	}

	@Override
	public void onCallStateChanged(int state, String incomingNumber) {
		Logger.logDebug("Call state updated: " + state + " (previous: " + previousState + ")");
		if (previousState == TelephonyManager.CALL_STATE_RINGING && state == TelephonyManager.CALL_STATE_IDLE) {
			Logger.logDebug("Detected missed call");
			MainService.newNotification(context, ID);
		}
		previousState = state;
	}
}
