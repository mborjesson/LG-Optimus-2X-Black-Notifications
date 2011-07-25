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

import com.martinborjesson.o2xtouchlednotifications.services.*;
import com.martinborjesson.o2xtouchlednotifications.utils.*;

public class SMSMMSReceiver extends BroadcastReceiver {

	public static final String ID = SMSMMSReceiver.class.getName() + ".SMS_MMS";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		Logger.logDebug("Received SMS/MMS");
		MainService.newNotification(context, ID);
	}

}
