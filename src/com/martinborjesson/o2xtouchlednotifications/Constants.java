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

package com.martinborjesson.o2xtouchlednotifications;

public class Constants {
	public static final boolean DEFAULT_SERVICE_ENABLED = true;
	public static final boolean DEFAULT_SET_TOUCH_LED_STRENGTH_ON_BOOT = true;
	public static final boolean DEFAULT_AUTO_BRIGHTNESS = false;
	public static final boolean DEFAULT_AUTOSTART_SERVICE = true;
	
	public static final int DEFAULT_PULSE_FADE_IN = 500;

	public static final int DEFAULT_PULSE_ACTIVE = 3000;

	public static final int DEFAULT_PULSE_FADE_OUT = 500;

	public static final int DEFAULT_PULSE_INACTIVE = 2000;
	
	public static final int DEFAULT_PULSE_MAX_LED_STRENGTH = 20;
	
	public static final boolean DEFAULT_PREFERENCE_KEY_ONLY_LIGHT_NOTIFICATION = false;
	
	public static final float DEFAULT_ACCELEROMETER_TOLERANCE = 2.5f;
	
	public static final int DEFAULT_DISABLE_NOTIFICATIONS_FROM_HOURS = 23;
	public static final int DEFAULT_DISABLE_NOTIFICATIONS_FROM_MINUTES = 0;
	public static final int DEFAULT_DISABLE_NOTIFICATIONS_TO_HOURS = 8;
	public static final int DEFAULT_DISABLE_NOTIFICATIONS_TO_MINUTES = 0;
	
	public static final boolean DEFAULT_DISABLE_NOTIFICATIONS = false;
	
	public static final boolean DEFAULT_CHECK_GMAIL = true;
	public static final boolean DEFAULT_CHECK_SMS_MMS = true;
	public static final boolean DEFAULT_CHECK_MMS = true;
	public static final boolean DEFAULT_CHECK_MISSED_CALLS = true;
	
	public static final String DEFAULT_OFF_TIME_SLEEP = "22:00";
	public static final String DEFAULT_ON_TIME_SLEEP = "08:00";
	
	public static final float DEFAULT_PULSE_TIMEOUT = 1;

	public static final int DEFAULT_TOUCH_LED_STRENGTH = 20;
	
	public static final boolean DEFAULT_USE_ACCELEROMETER_IN_KEYGUARD = false;
	
	public static final boolean DEFAULT_LOGGING = false;
	public static final boolean DEFAULT_LOGGING_FILE = false;

	public static final boolean DEFAULT_LOG_NOTIFICATIONS_FROM_ACTIVITIES = false;
	public static final boolean DEFAULT_REACT_ON_LIGHT_NOTIFICATIONS = false;
	
	public static final boolean DEFAULT_DELAY_UNTIL_SCREEN_OFF = true;
	
	public static final boolean DEFAULT_DISPLAY_NOTIFICATION = false;
	public static final boolean DEFAULT_DISPLAY_NOTIFICATION_ALWAYS = false;
	public static final boolean DEFAULT_DISPLAY_NOTIFICATION_ACTIVITY = false;
	
	public static final boolean DEFAULT_INDICIATE_CHARGE = false;
	public static final boolean DEFAULT_DISABLE_PULSE_WHILE_CHARGING = false;
	
	public static final boolean DEFAULT_DISABLE_PULSE_ON_LOW_BATTERY = true;
	public static final int DEFAULT_LOW_BATTERY_DISABLE_PERCENT = 15;
	
	public static final int DEFAULT_LED_BRIGHTNESS_WHILE_CHARGING = 3;
	public static final int DEFAULT_PULSE_DELAY = 4;
	
	public static final boolean DEFAULT_VIBRATE_ENABLED = false;
	public static final float DEFAULT_VIBRATE_SLOWER_OVER_TIME = 1.1f;	
	
	public static final int DEFAULT_PULSE_MODE = 0;

	public static final int DEFAULT_FEEDBACK_PRIORITY = 0;

	public static final boolean DEFAULT_CUSTOMIZED_PULSE = false;
	
	public static final String PREFERENCE_KEY_DEFAULT_PULSE = Constants.class.getName() + ".DEFAULT_PULSE";
	public static final String PREFERENCE_KEY_CUSTOMIZED_PULSE = "customizedPulseEnabled";
	public static final String PREFERENCE_KEY_TOUCH_LED_MODE = "touchLEDMode";
	public static final String PREFERENCE_KEY_TOUCH_LED_BRIGHTNESS = "touchLEDBrightness";
	public static final String PREFERENCE_KEY_TOUCH_LED_FADE_IN_TIME = "touchLEDFadeInTime";
	public static final String PREFERENCE_KEY_TOUCH_LED_FADE_OUT_TIME = "touchLEDFadeOutTime";
	public static final String PREFERENCE_KEY_TOUCH_LED_FULLY_LIT_TIME = "touchLEDFullyLitTime";
	public static final String PREFERENCE_KEY_TOUCH_LED_INACTIVE_TIME = "touchLEDInactiveTime";
	public static final String PREFERENCE_KEY_VIBRATION_MODE = "vibrationMode";
	public static final int DEFAULT_VIBRATE_TYPE = -1;
	public static final String PREFERENCE_KEY_VIBRATION_DURATION = "vibrationDuration";
	public static final int DEFAULT_VIBRATE_DURATION = 1000;
	public static final String PREFERENCE_KEY_VIBRATION_DELAY = "vibrationDelay";
	public static final int DEFAULT_VIBRATE_DELAY = 3000;
	public static final String PREFERENCE_KEY_NOTIFICATION_RINGTONE = "notificationRingtone";
	public static final String DEFAULT_NOTIFICATION_RINGTONE = null;
	public static final String PREFERENCE_KEY_NOTIFICATION_RINGTONE_MODE = "audioMode";
	public static final int DEFAULT_NOTIFICATION_RINGTONE_TYPE = -1;
	public static final String PREFERENCE_KEY_NOTIFICATION_RINGTONE_DELAY = "audioDelay";
	public static final int DEFAULT_NOTIFICATION_RINGTONE_DELAY = 3000;	
	public static final String PREFERENCE_KEY_PRIORITY = "priority";
	public static final String PREFERENCE_KEY_ONLY_LIGHT_NOTIFICATION = "onlyLightNotification";
	
	
}
