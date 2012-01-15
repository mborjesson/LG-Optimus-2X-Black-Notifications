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

package com.martinborjesson.o2xtouchlednotifications.services;

import java.io.*;
import java.util.*;

import android.accounts.*;
import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.content.pm.PackageManager.NameNotFoundException;
import android.hardware.*;
import android.os.*;
import android.preference.*;
import android.telephony.*;

import com.martinborjesson.o2xtouchlednotifications.*;
import com.martinborjesson.o2xtouchlednotifications.feedbacks.*;
import com.martinborjesson.o2xtouchlednotifications.notifications.*;
import com.martinborjesson.o2xtouchlednotifications.receivers.*;
import com.martinborjesson.o2xtouchlednotifications.touchled.*;
import com.martinborjesson.o2xtouchlednotifications.touchled.devices.*;
import com.martinborjesson.o2xtouchlednotifications.utils.*;

public class MainService extends Service implements SensorEventListener {
	public static final String ACTION_SCREEN_OFF = MainService.class.getName() + ".ACTION_SCREEN_OFF";
	public static final String ACTION_SCREEN_ON = MainService.class.getName() + ".ACTION_SCREEN_ON";
	public static final String ACTION_DO_TEST = MainService.class.getName() + ".ACTION_DO_TEST";
	public static final String ACTION_START_PULSE = MainService.class.getName() + ".ACTION_START_PULSE";
	public static final String ACTION_CANCEL_PULSE = MainService.class.getName() + ".ACTION_CANCEL_PULSE";
	public static final String ACTION_STOP_PULSE = MainService.class.getName() + ".ACTION_STOP_PULSE";
	public static final String ACTION_READ_SETTINGS = MainService.class.getName() + ".ACTION_READ_SETTINGS";
	public static final String ACTION_USER_INTERACTION = MainService.class.getName() + ".ACTION_USER_INTERACTION";
	public static final String ACTION_NEW_NOTIFICATION = MainService.class.getName() + ".ACTION_NEW_NOTIFICATION";
	public static final String ACTION_REMOVE_NOTIFICATION = MainService.class.getName() + ".ACTION_REMOVE_NOTIFICATION";
	public static final String ACTION_START_ACCELEROMETER = MainService.class.getName() + ".ACTION_START_ACCELEROMETER";
	public static final String ACTION_DEVICE_CHARGING = MainService.class.getName() + ".ACTION_DEVICE_CHARGING";
	public static final String ACTION_PENDING_NOTIFICATION = MainService.class.getName() + ".ACTION_PENDING_NOTIFICATION";
	
	public static final String EXTRAS_NOTIFICATION_ID = MainService.class.getName() + ".EXTRAS_NOTIFICATION_ID";
	public static final String EXTRAS_TEST_ID = MainService.class.getName() + ".EXTRAS_TEST_ID";
	
	public static final String TEST_PULSE_ID = "Test pulse";
	
	public static final int ALARM_TYPE_BROADCAST = 0;
	public static final int ALARM_TYPE_SERVICE = 1;
	
	public static final int STATE_TOUCH_LED_INACTIVE = 0;
	public static final int STATE_TOUCH_LED_PULSE = 1<<1;
	public static final int STATE_TOUCH_LED_CHARGING = 1<<2;
	public static final int STATE_TOUCH_LED_CANCELLED = 1<<3;
	
	public static final int TOUCH_LED_TEST = 1<<1;
	public static final int TOUCH_LED_START_PULSE = 1<<2;
	
	public static final String PROPERTY_TOUCH_LED_STATE = "touchLEDState";
	public static final String PROPERTY_ACTIVE_NOTIFICATION = "activeNotification";

	// notifications
	private CallListener callListener = null;
	private Map<String, BroadcastReceiver> receivers = new HashMap<String, BroadcastReceiver>();
	
	private List<AbstractContentObserver> observers = new ArrayList<AbstractContentObserver>();
	
	private SensorManager sensorManager = null;
	private Sensor sensorAccelerometer = null;
	private Sensor sensorProximity = null;
	
	
	private SerializableArrayList<String> activeNotifications = null;
	
	private float tolerance = 1;
	
	private boolean initSensors = true;
	private float prevX = 0;
	private float prevY = 0;
	private float prevZ = 0;
	
	private boolean useAccelerometerInKeyGuard = Constants.DEFAULT_USE_ACCELEROMETER_IN_KEYGUARD;
	
	private TouchLED touchLED = TouchLED.getTouchLED();
	
//	private boolean enableAutoLEDBrightness = false;
//	private long previousAutoLEDBrightness = touchLED.getMax();
	
	public final static String TOUCH_LED_STATUS_FILE = "touchledstatus.dat";
	private final static String ACTIVE_NOTIFICATIONS_FILE = "active_notifications.dat";
	private boolean notificationDisplayed = false;
	private boolean displayNotification = false;
	private boolean displayNotificationActivity = false;
	private boolean displayNotificationAlways = false;
	private boolean delayUntilScreenOff = true;
	private boolean stopOnScreenOn = false;
	private boolean disableOnCharge = false;
	private boolean deviceCharging = false;
	private boolean ledOnCharge = false;
	static private boolean pendingNewNotification = false;
	private boolean disablePulseOnLowBattery = Constants.DEFAULT_DISABLE_PULSE_ON_LOW_BATTERY;
	private int lowBatteryPulseDisablePercentage = Constants.DEFAULT_LOW_BATTERY_DISABLE_PERCENT;
//	private boolean lastScreenOn = false;
	
	private SharedPreferences preferences = null;
	
	private AppProperties touchLEDProperties = null;
	
	public class MainBinder extends Binder {
        MainService getService() {
            return MainService.this;
        }
    }
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		activeNotifications = new SerializableArrayList<String>(this, ACTIVE_NOTIFICATIONS_FILE);
		
		Logger.logDebug("Create screen service...");
		{
			ScreenReceiver screenReceiver = new ScreenReceiver();
	    	IntentFilter filter = new IntentFilter();
	    	filter.addAction(Intent.ACTION_SCREEN_ON);
	    	filter.addAction(Intent.ACTION_SCREEN_OFF);
	    	registerReceiver(screenReceiver, filter);
	    	receivers.put("ScreenReceiver", screenReceiver);
		}
    	
    	callListener = new CallListener(this); 
    	
		sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
		sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		
		readSettings();
		
		touchLEDProperties = new AppProperties(this, TOUCH_LED_STATUS_FILE);
		touchLEDProperties.load();

		int touchLEDState = touchLEDProperties.getInt(PROPERTY_TOUCH_LED_STATE, STATE_TOUCH_LED_INACTIVE);
		if (touchLEDState == STATE_TOUCH_LED_PULSE) {
			Logger.logDebug("It seems like we crashed for some reason.");
			touchLEDProperties.putInt(PROPERTY_TOUCH_LED_STATE, STATE_TOUCH_LED_INACTIVE);
			touchLEDProperties.save();
			activeNotifications.clear();
			try {
				activeNotifications.unserialize();
			} catch (IOException e) {
				e.printStackTrace();
			}
			Logger.logDebug("Stored activities:");
			if (!activeNotifications.isEmpty()) {
				String id = activeNotifications.get(0);
				int currentPriority = Constants.DEFAULT_FEEDBACK_PRIORITY;
				for (String s : activeNotifications) {
					int nextPriority = toInt(preferences.getString(s + "." + Constants.PREFERENCE_KEY_PRIORITY, String.valueOf(Constants.DEFAULT_FEEDBACK_PRIORITY)), Constants.DEFAULT_FEEDBACK_PRIORITY);
					if (nextPriority > currentPriority) {
						currentPriority = nextPriority;
						id = s;
					}

					Logger.logDebug(s);
				}
				Logger.logDebug("Starting pulse for " + id);
				newNotification(this, id);
			}
		}
		
		{
			UserInteractReceiver userInteractReceiver = new UserInteractReceiver();
			IntentFilter filter = new IntentFilter();
			filter.addAction(Intent.ACTION_USER_PRESENT);
			// WidgetLocker workarounds
			filter.addAction("com.teslacoilsw.widgetlocker.intent.UNLOCKED");
			filter.addAction("com.teslacoilsw.widgetlocker.intent.HIDDEN");
			registerReceiver(userInteractReceiver, filter);
			receivers.put(Intent.ACTION_USER_PRESENT, userInteractReceiver);
		}
		
//		PowerManager pm = (PowerManager)getSystemService(POWER_SERVICE);
//		lastScreenOn = pm.isScreenOn();
		
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		Logger.logDebug("Shutting down service");
		
		Logger.logDebug("Stop screen service...");
		
		stopAll();

		unregisterObservers();
		
		observers.clear();
		
		for (BroadcastReceiver receiver : receivers.values()) {
			unregisterReceiver(receiver);
		}
		receivers.clear();

		unregisterSensors();
		
		Logger.stopLogToFile();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null && intent.getAction() != null) {
			Logger.logDebug("onStartCommand() " + intent.getAction());
			
			if (intent.getAction().equals(ACTION_SCREEN_OFF)) {
				unregisterSensor(sensorAccelerometer);
				TelephonyManager tm = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
				if (tm.getCallState() == TelephonyManager.CALL_STATE_IDLE) {
					if (delayUntilScreenOff && hasNotifications()) {
						startFeedback(TOUCH_LED_START_PULSE, getActiveNotification());
						stopOnScreenOn = true;
					}
					if (ledOnCharge) {
						Intent batteryIntent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
						if (batteryIntent != null) {
							int plugged = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
							int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
							int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
							updateLEDChargeStatus(plugged, level, scale);
						}
					}
				}

			} else if (intent.getAction().equals(ACTION_SCREEN_ON)) {
				if (ledOnCharge) {
					deviceCharging = false;
					// stop
					int touchLEDState = touchLEDProperties.getInt(PROPERTY_TOUCH_LED_STATE, STATE_TOUCH_LED_INACTIVE);
					if (touchLEDState == STATE_TOUCH_LED_CHARGING) {
						touchLEDProperties.putInt(PROPERTY_TOUCH_LED_STATE, STATE_TOUCH_LED_INACTIVE);
						touchLEDProperties.save();
					}
				}
				if (stopOnScreenOn) {
					stopOnScreenOn = false;
					stopFeedback(getActiveNotification());
				} else {
					if (pendingNewNotification) {
						KeyguardManager km = (KeyguardManager)getSystemService(KEYGUARD_SERVICE);
						Logger.logDebug("In keyguard: " + km.inKeyguardRestrictedInputMode());
						if (useAccelerometerInKeyGuard && km.inKeyguardRestrictedInputMode()) {
							Logger.logDebug("In keyguard");
							registerSensor(sensorAccelerometer);
						} else if (!km.inKeyguardRestrictedInputMode()) {
							registerSensor(sensorAccelerometer);
						}
					}
				}
				if (touchLED.isUsable()) {
					// always do this so the led is restored
					int oldValue = preferences.getInt("seekBarTouchLEDStrengthPref", Constants.DEFAULT_TOUCH_LED_STRENGTH);
					touchLED.set(TouchLED.MENU, oldValue);
				}
			} else if (intent.getAction().equals(ACTION_PENDING_NOTIFICATION)) {
				if (pendingNewNotification) {
					KeyguardManager km = (KeyguardManager)getSystemService(KEYGUARD_SERVICE);
					Logger.logDebug("In keyguard: " + km.inKeyguardRestrictedInputMode());
					if (useAccelerometerInKeyGuard && km.inKeyguardRestrictedInputMode()) {
						Logger.logDebug("In keyguard");
						registerSensor(sensorAccelerometer);
					} else if (!km.inKeyguardRestrictedInputMode()) {
						registerSensor(sensorAccelerometer);
					}
				}
			} else if (intent.getAction().equals(ACTION_CANCEL_PULSE)) {
				cancelFeedback(getActiveNotification());
			} else if (intent.getAction().equals(ACTION_USER_INTERACTION)) {
				Logger.logDebug("pnn: " + pendingNewNotification);
				if (pendingNewNotification || !delayUntilScreenOff) {
					stopAll();
				}
			} else if (intent.getAction().equals(ACTION_STOP_PULSE)) {
				KeyguardManager km = (KeyguardManager)getSystemService(KEYGUARD_SERVICE);
				if (useAccelerometerInKeyGuard && km.inKeyguardRestrictedInputMode() || !km.inKeyguardRestrictedInputMode()) {
					stopAll(); // do not stop in lock screen
				}
			} else if (intent.getAction().equals(ACTION_START_PULSE)) {
				startFeedback(TOUCH_LED_START_PULSE, getActiveNotification());
			} else if (intent.getAction().equals(ACTION_NEW_NOTIFICATION)) {
				onNewNotification(intent.getExtras().getString(EXTRAS_NOTIFICATION_ID));
			} else if (intent.getAction().equals(ACTION_REMOVE_NOTIFICATION)) {
				String id = intent.getExtras().getString(EXTRAS_NOTIFICATION_ID);
				Logger.logDebug("Remove notification id: " + id);
				activeNotifications.remove(id);
				if (activeNotifications.isEmpty()) {
					Logger.logDebug("No notifications left, cancel pulse");
					cancelFeedback(getActiveNotification());
				}
				try {
					activeNotifications.serialize();
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else if (intent.getAction().equals(ACTION_READ_SETTINGS)) {
				readSettings();
			} else if (intent.getAction().equals(ACTION_START_ACCELEROMETER)) {
				registerSensor(sensorAccelerometer);
			} else if (intent.getAction().equals(ACTION_DO_TEST)) {
				activeNotifications.add(TEST_PULSE_ID);
				String id = intent.getStringExtra(EXTRAS_TEST_ID);
				if (id == null) {
					id = Constants.PREFERENCE_KEY_DEFAULT_PULSE;
				}
				setActiveNotification(id);
				startFeedback(TOUCH_LED_TEST|TOUCH_LED_START_PULSE, id);
			} else if (intent.getAction().equals(ACTION_DEVICE_CHARGING)) {
				int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
				int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
				int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
				if (ledOnCharge) {
					updateLEDChargeStatus(plugged, level, scale);
				}
				if (disablePulseOnLowBattery) {
					if (plugged == 0 && level <= lowBatteryPulseDisablePercentage) {
						int touchLEDState = touchLEDProperties.getInt(PROPERTY_TOUCH_LED_STATE, STATE_TOUCH_LED_INACTIVE);
						if (touchLEDState == STATE_TOUCH_LED_PULSE) {
							cancelFeedback(getActiveNotification());
						}
					}
				}
			}
		}
		
		return START_STICKY;
	}
	
	private void readSettings() {
		touchLED = TouchLED.getTouchLED();
		
		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		unregisterObservers();

		readNotificationActivitiesSettings(this);
		
		Logger.setEnabled(preferences.getBoolean("checkBoxLog", Constants.DEFAULT_LOGGING));
		
		Logger.logDebug("Device: " + touchLED.getDeviceName());
		
		if (Logger.isEnabled()) {
			if (preferences.getBoolean("checkBoxLogFile", Constants.DEFAULT_LOGGING)) {
				Logger.startLogToFile();
			}
		} else {
			Logger.stopLogToFile();
		}
		
		if (Logger.isEnabled()) {
			try {
				PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
				Logger.logDebug("Version: " + pi.versionName);
			} catch (NameNotFoundException e) {
				e.printStackTrace();
			}
		}
		
		observers.clear();
		if (preferences.getBoolean("checkBoxNotificationsGmailPref", Constants.DEFAULT_CHECK_GMAIL)) {
			Logger.logDebug("Adding Gmail observer");
	    	// add all gmail accounts
			boolean available = false;
			AccountManager am = AccountManager.get(this);
			Account[] accounts = am.getAccountsByType("com.google");
			for (Account account : accounts) {
		    	GmailContentObserver gmailObserver = new GmailContentObserver(getContentResolver(), this, account);
		    	if (gmailObserver.isAvailable(this)) {
		    		available = true;
					observers.add(gmailObserver);
		    	}
			}
			
			if (!available) {
	    		addMonitoredActivity("com.google.android.gm");
			} else {
				addExcludedActivity("com.google.android.gm");
			}
		}
		if (preferences.getBoolean("checkBoxNotificationsSMSMMSPref", Constants.DEFAULT_CHECK_SMS_MMS)) {
			if (!receivers.containsKey("SMSMMSReceiver")) {
				IntentFilter filter = new IntentFilter();
				filter.addAction("android.provider.Telephony.SMS_RECEIVED");
				filter.addAction("android.provider.Telephony.MMS_RECEIVED");
				SMSMMSReceiver receiver = new SMSMMSReceiver();
				registerReceiver(receiver, filter);
				receivers.put("SMSMMSReceiver", receiver);
				Logger.logDebug("Adding SMS/MMS receiver");
			}
		} else {
			if (receivers.containsKey("SMSMMSReceiver")) {
				unregisterReceiver(receivers.remove("SMSMMSReceiver"));
				Logger.logDebug("Removing SMS/MMS receiver");
			}			
		}
		TelephonyManager tm = (TelephonyManager)getSystemService(TELEPHONY_SERVICE);
		if (preferences.getBoolean("checkBoxNotificationsMissedCallsPref", Constants.DEFAULT_CHECK_MISSED_CALLS)) {
			tm.listen(callListener, PhoneStateListener.LISTEN_CALL_STATE);
			Logger.logDebug("Adding Missed Calls listener");
		} else {
			tm.listen(callListener, PhoneStateListener.LISTEN_NONE);
		}
		
		registerObservers();
		
		tolerance = preferences.getFloat("accelerometerTolerancePref", Constants.DEFAULT_ACCELEROMETER_TOLERANCE);
		Logger.logDebug("Accelerometer tolerance: " + tolerance);
		
		useAccelerometerInKeyGuard = preferences.getBoolean("checkBoxAccelerometerKeyGuardEnabled", Constants.DEFAULT_USE_ACCELEROMETER_IN_KEYGUARD);
		Logger.logDebug("Accelerometer enabled in KeyGuard: " + useAccelerometerInKeyGuard);
		
//		enableAutoLEDBrightness = preferences.getBoolean("checkBoxAutoLEDBrightnessPref", Constants.DEFAULT_AUTO_BRIGHTNESS);
//		try {
//			if (Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE) != Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
//				enableAutoLEDBrightness = false;
//			}
//		} catch (SettingNotFoundException e) {
//			enableAutoLEDBrightness = false;
//		}
//		Logger.logDebug("Enable auto LED brightness: " + enableAutoLEDBrightness);
//		if (enableAutoLEDBrightness) {
//			unregisterSensors();
//			registerSensors();
//			previousAutoLEDBrightness = touchLED.getCurrent();
//		}

		displayNotification = preferences.getBoolean("checkBoxDisplayNotification", Constants.DEFAULT_DISPLAY_NOTIFICATION);
		displayNotificationActivity = preferences.getBoolean("checkBoxDisplayNotificationActivity", Constants.DEFAULT_DISPLAY_NOTIFICATION_ACTIVITY);
		displayNotificationAlways = preferences.getBoolean("checkBoxDisplayNotificationAlways", Constants.DEFAULT_DISPLAY_NOTIFICATION_ALWAYS);
		Logger.logDebug("Display notification: " + displayNotification + " (display activity: " + displayNotificationActivity + ", always: " + displayNotificationAlways  + ")");
		if (displayNotification) {
			addExcludedActivity(getPackageName());
			
			if (displayNotificationAlways) {
				createAppNotification();
			} else {
				removeAppNotification();
			}
		} else {
			if (notificationDisplayed) {
				removeAppNotification();
			}
		}
		
		disablePulseOnLowBattery = preferences.getBoolean("checkBoxDisablePulseOnLowBattery", Constants.DEFAULT_DISABLE_PULSE_ON_LOW_BATTERY);
		lowBatteryPulseDisablePercentage = preferences.getInt("seekBarDisablePulseOnLowBatteryPercent", Constants.DEFAULT_LOW_BATTERY_DISABLE_PERCENT);
		
		Logger.logDebug("Disable pulse on low battery: " + disablePulseOnLowBattery + " (" + lowBatteryPulseDisablePercentage + "%)");
		
//		delayUntilScreenOff = preferences.getBoolean("checkBoxPulseDelayUntilScreenOff", Constants.DEFAULT_DELAY_UNTIL_SCREEN_OFF);
//		Logger.logDebug("Delay until screen is off: " + delayUntilScreenOff);
		
		ledOnCharge = preferences.getBoolean("checkBoxEnableLEDWhileCharging", Constants.DEFAULT_INDICIATE_CHARGE);
		Logger.logDebug("LEDs indiciate while charging: " + ledOnCharge);
		
		disableOnCharge = preferences.getBoolean("checkBoxDisablePulseWhileCharging", Constants.DEFAULT_DISABLE_PULSE_WHILE_CHARGING);
		Logger.logDebug("LEDs pulse while charging: " + disableOnCharge);

		if (ledOnCharge || disablePulseOnLowBattery) {
			if (!receivers.containsKey(Intent.ACTION_BATTERY_CHANGED)) {
				BatteryChangedReceiver batteryChangedReceiver = new BatteryChangedReceiver();
				registerReceiver(batteryChangedReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
				receivers.put(Intent.ACTION_BATTERY_CHANGED, batteryChangedReceiver);
			}
		} else {
			BroadcastReceiver receiver = receivers.remove(Intent.ACTION_BATTERY_CHANGED);
			if (receiver != null) {
				unregisterReceiver(receiver);
			}
		}
	}
	
	static public void readNotificationActivitiesSettings(Context context) {
		Intent intent = new Intent(context, AccessibilityService.class);
		intent.setAction(AccessibilityService.ACTION_READ_SETTINGS);
		context.startService(intent);
	}
	
	private void addExcludedActivity(String packageName) {
		Intent intent = new Intent(this, AccessibilityService.class);
		intent.setAction(AccessibilityService.ACTION_ADD_EXCLUDED);
		intent.putExtra("packageName", packageName);
		startService(intent);
	}

	private void addMonitoredActivity(String packageName) {
		Intent intent = new Intent(this, AccessibilityService.class);
		intent.setAction(AccessibilityService.ACTION_ADD);
		intent.putExtra("packageName", packageName);
		startService(intent);
	}

	public boolean registerSensor(Sensor sensor) {
		if (sensor == null) {
			return false;
		}
		initSensors = true;
		Logger.logDebug("Register sensor " + sensor.getName());
		sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
		
		return true;
	}

	public void unregisterSensor(Sensor sensor) {
		Logger.logDebug("Unregister sensor " + sensor.getName());
		sensorManager.unregisterListener(this, sensor);
	}
	
	public void unregisterSensors() {
		Logger.logDebug("Unregister sensors...");
		sensorManager.unregisterListener(this);
	}
	
	public void registerObservers() {
		for (AbstractContentObserver observer : observers) {
			observer.register();
			observer.reset();
		}
	}
	
	public void unregisterObservers() {
		for (AbstractContentObserver observer : observers) {
			observer.unregister();
		}
	}

	public void stopAll() {
		Logger.logDebug("Stop all");
		// stop some alarms
		stopAlarm(this, ACTION_NEW_NOTIFICATION, MainService.class, ALARM_TYPE_SERVICE);
		stopFeedback(getActiveNotification());
	}

	public void startFeedback(int state, String pulseKey) {
		
		boolean test = ((state&TOUCH_LED_TEST) == TOUCH_LED_TEST);
		boolean startPulse = ((state&TOUCH_LED_START_PULSE) == TOUCH_LED_START_PULSE);
    	float timeout = toFloat(preferences.getString("listPulseOptionsTimeoutTimePref", String.valueOf(Constants.DEFAULT_PULSE_TIMEOUT)), Constants.DEFAULT_PULSE_TIMEOUT);
    	long pulseTimeout = (long)(timeout*(60*60*1000));

    	boolean start = false;
//    	long interval = props.getInt(AppProperties.NOTIFICATIONS_CHECK_TIME, Constants.DEFAULT_NOTIFICATION_CHECK)*1000;
    	if (!test && preferences.getBoolean("checkBoxSleepEnabledPref", Constants.DEFAULT_DISABLE_NOTIFICATIONS)) {
    		int fromHrs = 22;
    		int fromMins = 0;
    		int toHrs = 8;
    		int toMins = 0;
    		try {
        		String time = preferences.getString("timeSleepOffTimePref", Constants.DEFAULT_OFF_TIME_SLEEP);
        		String[] s = time.split(":");
        		fromHrs = Integer.valueOf(s[0]);
        		fromMins = Integer.valueOf(s[1]);
    		} catch (NumberFormatException e) {}
    		try {
        		String time = preferences.getString("timeSleepOnTimePref", Constants.DEFAULT_ON_TIME_SLEEP);
        		String[] s = time.split(":");
        		toHrs = Integer.valueOf(s[0]);
        		toMins = Integer.valueOf(s[1]);
    		} catch (NumberFormatException e) {}
    		
    		Logger.logDebug("Disable notifications at " + fromHrs + ":" + (fromMins < 10 ? "0"+fromMins : fromMins) + " and re-enable at " + toHrs + ":" + (toMins < 10 ? "0"+toMins : toMins));
    		Calendar cal = Calendar.getInstance();
    		int curHrs = cal.get(Calendar.HOUR_OF_DAY);
    		int curMins = cal.get(Calendar.MINUTE);
    		Logger.logDebug("Current time: " + curHrs + ":" + (curMins < 10 ? "0"+curMins : curMins));
    		int curTime = curHrs*60+curMins;
    		int fromTime = fromHrs*60+fromMins;
    		int toTime = toHrs*60+toMins;
    		if (curTime > fromTime) {
    			if (toTime <= fromTime) {
    				toTime += 24*60;
    			}
    		} else if (fromTime > toTime) {
    			fromTime -= 24*60;
    		}
			if (curTime >= fromTime && curTime < toTime) {
				// in sleep mode
			} else {
				if (curTime > fromTime) {
					fromTime += 24*60;
				}
    			if (toTime <= fromTime) {
    				toTime += 24*60;
    			}
				long sleepDelay = (fromTime-curTime);
				if (sleepDelay <= 0) {
					sleepDelay = 1;
				}
				sleepDelay = sleepDelay*60*1000;
				Logger.logDebug("Start now, sleep in " + sleepDelay + " ms");
//				startAlarm(this, CANCEL_PULSE, MainService.class, sleepDelay, 0, ALARM_TYPE_SERVICE, false);
				if (sleepDelay < pulseTimeout) {
					pulseTimeout = sleepDelay;
				}
				start = true;
			}
    	} else {
    		start = true;
    	}

    	if (start) {
    		if (startPulse) {
    			pendingNewNotification = false;

    			// leds
    			
    			if (touchLED.isUsable()) {
					Logger.logDebug("Start feedback... (" + pulseKey + ")");
					if (touchLED instanceof TouchLEDP970) {
						((TouchLEDP970)touchLED).setOnOffLED(true);
					}
	
					int touchLEDState = touchLEDProperties.getInt(PROPERTY_TOUCH_LED_STATE, STATE_TOUCH_LED_INACTIVE);
					if (touchLEDState != STATE_TOUCH_LED_PULSE) {
						touchLEDProperties.putInt(PROPERTY_TOUCH_LED_STATE, STATE_TOUCH_LED_PULSE);
						touchLEDProperties.save();
					}
					
					int pulseMode = toInt(preferences.getString(pulseKey + "." + Constants.PREFERENCE_KEY_TOUCH_LED_MODE, String.valueOf(Constants.DEFAULT_PULSE_MODE)), Constants.DEFAULT_PULSE_MODE);
	
					if (pulseMode == 0 || pulseMode == 3) {
						int pulseDelay = 
							toInt(preferences.getString(pulseKey + "." + Constants.PREFERENCE_KEY_TOUCH_LED_FADE_IN_TIME, String.valueOf(Constants.DEFAULT_PULSE_FADE_IN)), Constants.DEFAULT_PULSE_FADE_IN)+
				    		toInt(preferences.getString(pulseKey + "." + Constants.PREFERENCE_KEY_TOUCH_LED_FULLY_LIT_TIME, String.valueOf(Constants.DEFAULT_PULSE_ACTIVE)), Constants.DEFAULT_PULSE_ACTIVE);
						int pulseInterval = 0;
						if (pulseMode == 0) {
				    		pulseInterval =
					    		pulseDelay+
					    		toInt(preferences.getString(pulseKey + "." + Constants.PREFERENCE_KEY_TOUCH_LED_FADE_OUT_TIME, String.valueOf(Constants.DEFAULT_PULSE_FADE_OUT)), Constants.DEFAULT_PULSE_FADE_OUT)+
				    			toInt(preferences.getString(pulseKey + "." + Constants.PREFERENCE_KEY_TOUCH_LED_INACTIVE_TIME, String.valueOf(Constants.DEFAULT_PULSE_INACTIVE)), Constants.DEFAULT_PULSE_INACTIVE);
						} else if (pulseMode == 3) {
							pulseTimeout = pulseDelay+
				    		toInt(preferences.getString(pulseKey + "." + Constants.PREFERENCE_KEY_TOUCH_LED_FADE_OUT_TIME, String.valueOf(Constants.DEFAULT_PULSE_FADE_OUT)), Constants.DEFAULT_PULSE_FADE_OUT)+
				    		1000;
						}
						Logger.logDebug("Start touch led pulse... (interval: " + pulseInterval + ")");
						TouchLEDService.reset();
						startAlarm(this, TouchLEDReceiver.START_PULSE, TouchLEDReceiver.class, 0, pulseInterval, ALARM_TYPE_BROADCAST, false);
						startAlarm(this, TouchLEDReceiver.STOP_PULSE, TouchLEDReceiver.class, pulseDelay, pulseInterval, ALARM_TYPE_BROADCAST, false);
					} else if (pulseMode == 1 || pulseMode == 4) {
				    	int stopStaticPulseDelay = 
				    		toInt(preferences.getString(pulseKey + "." + Constants.PREFERENCE_KEY_TOUCH_LED_FULLY_LIT_TIME, String.valueOf(Constants.DEFAULT_PULSE_ACTIVE)), Constants.DEFAULT_PULSE_ACTIVE);
			    		int pulseInterval = 0;
			    		if (pulseMode == 1) {
			    			pulseInterval = 
					    		stopStaticPulseDelay+
					    		toInt(preferences.getString(pulseKey + "." + Constants.PREFERENCE_KEY_TOUCH_LED_INACTIVE_TIME, String.valueOf(Constants.DEFAULT_PULSE_INACTIVE)), Constants.DEFAULT_PULSE_INACTIVE);
			    		} else if (pulseMode == 4) {
							pulseTimeout = stopStaticPulseDelay+
				    		1000;
	
			    		}
						Logger.logDebug("Start static touch led pulse... (interval: " + pulseInterval + ", delay: " + stopStaticPulseDelay + ", timeout: " + pulseTimeout + ")");
						TouchLEDStaticPulseReceiver.reset();
						startAlarm(this, TouchLEDStaticPulseReceiver.START_STATIC_PULSE, TouchLEDStaticPulseReceiver.class, 0, pulseInterval, ALARM_TYPE_BROADCAST, false);
						startAlarm(this, TouchLEDStaticPulseReceiver.STOP_STATIC_PULSE, TouchLEDStaticPulseReceiver.class, stopStaticPulseDelay, pulseInterval, ALARM_TYPE_BROADCAST, false);
					} else { // constant light
						int maxLEDStrength = preferences.getInt(pulseKey + "." + Constants.PREFERENCE_KEY_TOUCH_LED_BRIGHTNESS, Constants.DEFAULT_PULSE_MAX_LED_STRENGTH);
						touchLED.set(TouchLED.SEARCH, maxLEDStrength);
					}
    			}

				// vibrations
				int vibrateType = MainService.toInt(preferences.getString(pulseKey + "." + Constants.PREFERENCE_KEY_VIBRATION_MODE, String.valueOf(Constants.DEFAULT_VIBRATE_TYPE)), Constants.DEFAULT_VIBRATE_TYPE);
				Logger.logDebug("Vibration mode: " + vibrateType);
				if (vibrateType >= 0) {
					int vibrateDelay = MainService.toInt(preferences.getString(pulseKey + "." + Constants.PREFERENCE_KEY_VIBRATION_DELAY, String.valueOf(Constants.DEFAULT_VIBRATE_DELAY)), Constants.DEFAULT_VIBRATE_DELAY);
					VibratorReceiver.reset();
					VibratorReceiver.init(this, pulseKey);
					startAlarm(this, VibratorReceiver.ACTION_START_VIBRATOR, VibratorReceiver.class, vibrateDelay, 0, ALARM_TYPE_BROADCAST, false);
				}
				
				// notification ringtone
				int notificationRingtoneType = MainService.toInt(preferences.getString(pulseKey + "." + Constants.PREFERENCE_KEY_NOTIFICATION_RINGTONE_MODE, String.valueOf(Constants.DEFAULT_NOTIFICATION_RINGTONE_TYPE)), Constants.DEFAULT_NOTIFICATION_RINGTONE_TYPE);
				String uriString = preferences.getString(pulseKey + "." + Constants.PREFERENCE_KEY_NOTIFICATION_RINGTONE, Constants.DEFAULT_NOTIFICATION_RINGTONE);
				Logger.logDebug("Notification ringtone mode: " + notificationRingtoneType + ", uri: " + uriString);
				if (uriString != null && notificationRingtoneType >= 0) {
					int notificationRingtoneDelay = MainService.toInt(preferences.getString(pulseKey + "." + Constants.PREFERENCE_KEY_NOTIFICATION_RINGTONE_DELAY, String.valueOf(Constants.DEFAULT_NOTIFICATION_RINGTONE_DELAY)), Constants.DEFAULT_NOTIFICATION_RINGTONE_DELAY);
					NotificationRingtoneReceiver.reset();
					NotificationRingtoneReceiver.init(this, pulseKey);
					startAlarm(this, NotificationRingtoneReceiver.ACTION_START_NOTIFICATION_RINGTONE, NotificationRingtoneReceiver.class, notificationRingtoneDelay, 0, ALARM_TYPE_BROADCAST, false);
				}

				if (displayNotification && !displayNotificationAlways) {
					createAppNotification();
				}
    		}
			if (pulseTimeout > 0) {
				Logger.logDebug("Timeout: " + pulseTimeout);
				startAlarm(this, ACTION_CANCEL_PULSE, MainService.class, pulseTimeout, 0, ALARM_TYPE_SERVICE, false);
			}

    	}

	}
	
	public void cancelFeedback(String pulseKey) {
		pendingNewNotification = false;

		Logger.logDebug("Cancel feedback... (" + pulseKey + ")");
		// stop leds
		if (touchLED.isUsable()) {
			int pulseMode = toInt(preferences.getString(pulseKey + "." + Constants.PREFERENCE_KEY_TOUCH_LED_MODE, String.valueOf(Constants.DEFAULT_PULSE_MODE)), Constants.DEFAULT_PULSE_MODE);
			if (pulseMode == 0 || pulseMode == 3) {
				TouchLEDService.stopPulse();
				stopAlarm(this, TouchLEDReceiver.START_PULSE, TouchLEDReceiver.class, ALARM_TYPE_BROADCAST);
				stopAlarm(this, TouchLEDReceiver.STOP_PULSE, TouchLEDReceiver.class, ALARM_TYPE_BROADCAST);
				TouchLEDService.waitUntilDone(10*1000);
			} else if (pulseMode == 1 || pulseMode == 4) {
				TouchLEDStaticPulseReceiver.stopPulse();
				stopAlarm(this, TouchLEDStaticPulseReceiver.START_STATIC_PULSE, TouchLEDStaticPulseReceiver.class, ALARM_TYPE_BROADCAST);
				stopAlarm(this, TouchLEDStaticPulseReceiver.STOP_STATIC_PULSE, TouchLEDStaticPulseReceiver.class, ALARM_TYPE_BROADCAST);
			} else {
				
			}
		}
		
		// stop vibrator
		VibratorReceiver.stop();
		MainService.stopAlarm(this, VibratorReceiver.ACTION_START_VIBRATOR, VibratorReceiver.class, ALARM_TYPE_BROADCAST);
		
		// stop audio
		NotificationRingtoneReceiver.stop();
		MainService.stopAlarm(this, NotificationRingtoneReceiver.ACTION_START_NOTIFICATION_RINGTONE, NotificationRingtoneReceiver.class, ALARM_TYPE_BROADCAST);
		
		if (touchLED.isUsable()) {
			int touchLEDState = touchLEDProperties.getInt(PROPERTY_TOUCH_LED_STATE, STATE_TOUCH_LED_INACTIVE);
			if (touchLEDState == STATE_TOUCH_LED_PULSE) {
				if (touchLED instanceof TouchLEDP970) {
					((TouchLEDP970)touchLED).setOnOffLED(false);
				} else {
					touchLED.set(TouchLED.SEARCH, touchLED.getMin());
				}
				touchLEDProperties.putInt(PROPERTY_TOUCH_LED_STATE, STATE_TOUCH_LED_CANCELLED);
			}
			touchLEDProperties.save();
		}
		deviceCharging = false;
		
	}
	
	public void stopFeedback(String pulseKey) {
		Logger.logDebug("Stop feedback... (" + pulseKey + ")");
		cancelFeedback(pulseKey);
		// stop leds
		if (touchLED.isUsable()) {
			int pulseMode = toInt(preferences.getString(pulseKey + "." + Constants.PREFERENCE_KEY_TOUCH_LED_MODE, String.valueOf(Constants.DEFAULT_PULSE_MODE)), Constants.DEFAULT_PULSE_MODE);
			stopAlarm(this, ACTION_CANCEL_PULSE, MainService.class, ALARM_TYPE_SERVICE);
			if (pulseMode == 0 || pulseMode == 3) {
				stopService(new Intent(this, TouchLEDService.class));
				TouchLEDService.stopPulse();
				TouchLEDService.waitUntilDone(10*1000);
			}
			
			PowerManager pm = (PowerManager)getSystemService(POWER_SERVICE);
	
			int touchLEDState = touchLEDProperties.getInt(PROPERTY_TOUCH_LED_STATE, STATE_TOUCH_LED_INACTIVE);
			if (touchLEDState == STATE_TOUCH_LED_CANCELLED && pm.isScreenOn()) {
				int defaultValue = preferences.getInt("seekBarTouchLEDStrengthPref", Constants.DEFAULT_TOUCH_LED_STRENGTH);
				Logger.logDebug("Resetting default value: " + defaultValue);
				if (touchLED instanceof TouchLEDP970) {
					//((TouchLEDP970)touchLED).setAll(touchLED.getMax());
					// TODO: there seems to be a problem when testing the feedback on P970
				} else {
					touchLED.set(TouchLED.SEARCH, defaultValue);
				}
				touchLEDProperties.putInt(PROPERTY_TOUCH_LED_STATE, STATE_TOUCH_LED_INACTIVE);
				touchLEDProperties.save();
			}
		}
		
		activeNotifications.clear();
		
		try {
			activeNotifications.serialize();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if (displayNotification && !displayNotificationAlways) {
			removeAppNotification();
		}

//		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
//		int defaultValue = prefs.getInt("seekBarTouchLEDStrengthPref", Constants.DEFAULT_TOUCH_LED_STRENGTH);
//		if (pm.isScreenOn()) {
//			Logger.logDebug("Resetting default value: " + defaultValue);
//			try {
//				touchLED.set(defaultValue);
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
	}
	
	static public void startAlarm(Context context, String action, Class<?> cl, long delay, long interval, int type, boolean exact) {
		Intent intent = new Intent(context, cl);
		intent.setAction(action);
		startAlarm(context, intent, delay, interval, type, exact);
	}
	
	static public void startAlarm(Context context, Intent intent, long delay, long interval, int type, boolean exact) {
		AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		
		PendingIntent pendingIntent = null;
		if (type == ALARM_TYPE_BROADCAST) {
			pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
		} else if (type == ALARM_TYPE_SERVICE) {
			pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
		}
		
		if (interval > 0) {
			if (exact) {
				alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime()+delay, interval, pendingIntent);
			} else {
				alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime()+delay, interval, pendingIntent);
			}
		} else {
			alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime()+delay, pendingIntent);
		}
	}

	static public void stopAlarm(Context context, String action, Class<?> cl, int type) {
		AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		
		Intent intent = new Intent(context, cl);
		intent.setAction(action);
		PendingIntent pendingIntent = null;
		if (type == ALARM_TYPE_BROADCAST) {
			pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
		} else if (type == ALARM_TYPE_SERVICE) {
			pendingIntent = PendingIntent.getService(context, 0, intent, 0);
		}
		alarmManager.cancel(pendingIntent);
	}

	/**
	 * Start a new notification
	 * @param context
	 * @param id Id of the notification
	 */
	static public void newNotification(Context context, String id) {
		// we got new notifications
		Logger.logDebug("New notification received, start the pulse if we can");
		pendingNewNotification = true;
		
		int delay = 0;
		PowerManager pm = (PowerManager)context.getSystemService(POWER_SERVICE);
		if (pm.isScreenOn()) {
			// register sensors if possible
			Intent i = new Intent(context, MainService.class);
			i.setAction(ACTION_PENDING_NOTIFICATION);
			context.startService(i);
		}
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		delay = prefs.getInt("seekBarPulseDelayPrefs", Constants.DEFAULT_PULSE_DELAY)*1000;
		Logger.logDebug("Start with a delay of " + delay + " ms");
		
		Intent i = new Intent(context, MainService.class);
		i.setAction(ACTION_NEW_NOTIFICATION);
		i.putExtra(EXTRAS_NOTIFICATION_ID, id);
		if (delay > 0) {
			startAlarm(context, i, delay, 0, ALARM_TYPE_SERVICE, true);
		} else {
			context.startService(i);
		}
	}

	/**
	 * Remove a running notification
	 * @param context
	 * @param id
	 */
	static public void removeNotification(Context context, String id) {
		// a notification was removed, determine if the pulse should stop
		Logger.logDebug("A notification was removed, stop the pulse if we need to");
		
		Intent i = new Intent(context, MainService.class);
		i.setAction(ACTION_REMOVE_NOTIFICATION);
		i.putExtra(EXTRAS_NOTIFICATION_ID, id);
		context.startService(i);
	}
	
	private boolean hasNotifications() {
		return !activeNotifications.isEmpty();
	}
	
	private boolean isPulseActive(String id) {
		return activeNotifications.contains(id);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		
	}
	
	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			float cAx = event.values[0];
			float cAy = event.values[1];
			float cAz = event.values[2];
			if (initSensors) {
				initSensors = false;
				prevX = cAx;
				prevY = cAy;
				prevZ = cAz;
			}
			
			float diffX = cAx-prevX;
			float diffY = cAy-prevY;
			float diffZ = cAz-prevZ;
			
			float m = (float)Math.sqrt(diffX*diffX + diffY*diffY + diffZ*diffZ);
			
			if (m > tolerance && (hasNotifications() || pendingNewNotification) && !isPulseActive(TEST_PULSE_ID)) {
				Logger.logDebug("Device has been moved (" + m + "/" + tolerance + ")");
				unregisterSensor(sensorAccelerometer);
				Intent i = new Intent(this, MainService.class);
				i.setAction(ACTION_STOP_PULSE);
				startService(i);
			}
			
			// set values
			prevX = cAx;
			prevY = cAy;
			prevZ = cAz;
			
//			if (enableAutoLEDBrightness && !hasNotifications()) {
//				boolean alcEnabled = false;
//				try {
//					FileInputStream is = new FileInputStream(new File("/sys/devices/platform/star_aat2870.0/alc"));
//			    	int read = -1;
//			    	byte[] buf = new byte[128];
//			    	int p = 0;
//			    	while ((read = is.read()) != -1) {
//			    		buf[p++] = (byte)read;
//			    	}
//			    	is.close();
//			    	String valueStr = new String(buf, 0, p).trim();
//			    	if (valueStr.equals("1")) {
//			    		alcEnabled = true;
//			    	}
//				} catch (FileNotFoundException e) {
//					e.printStackTrace();
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//				
//				if (alcEnabled) {
//					try {
//						FileInputStream is = new FileInputStream(new File("/sys/devices/platform/star_aat2870.0/alc_level"));
//				    	int read = -1;
//				    	byte[] buf = new byte[128];
//				    	int p = 0;
//				    	while ((read = is.read()) != -1) {
//				    		buf[p++] = (byte)read;
//				    	}
//				    	is.close();
//				    	String valueStr = new String(buf, 0, p).trim();
////			    		int maxAlcValue = 3000; // detected max value (bright room)
//				    	int maxAlcValue = 700;
//			    		int minAlcValue = 0; // detected min value (bright room)
//				    	try {
//				    		int alcValue = Integer.valueOf(valueStr);
//				    		float val = (float)(alcValue-minAlcValue)/(maxAlcValue-minAlcValue);
//				    		val = Math.min(1, Math.max(0, val)); // clamp to 0-1
//				    		float lerp = MathUtils.lerp(touchLED.getMax(), touchLED.getMin(), val);
//				    		long ledVal = Math.round(lerp);
//				    		if (previousAutoLEDBrightness != ledVal) {
//					    		touchLED.setAll(Math.round(lerp));
//				    			previousAutoLEDBrightness = ledVal;
//						    	Logger.logDebug("New LED brightness: " + ledVal);
//				    		}
//				    		
//				    	} catch (NumberFormatException e) {}
//					} catch (FileNotFoundException e) {
//						e.printStackTrace();
//					} catch (IOException e) {
//						e.printStackTrace();
//					}
//				}
//			}
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	static public int toInt(String value, int defaultValue) {
		try {
			return Integer.valueOf(value);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	static public float toFloat(String value, float defaultValue) {
		try {
			return Float.valueOf(value);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	private void createAppNotification() {
		if (!notificationDisplayed) {
			Notification not = new Notification(R.drawable.icon, displayNotificationAlways ? getResources().getString(R.string.app_name) : "New notification", System.currentTimeMillis());
			Intent intent = new Intent(this, MainService.class);
			intent.setAction(ACTION_STOP_PULSE);
			PendingIntent pi = PendingIntent.getService(this, 0, intent, 0);
			String text = null;
			if (displayNotificationActivity && !displayNotificationAlways && activeNotifications.size() > 0) {
				text = "Stop pulse (" + activeNotifications.get(activeNotifications.size()-1) + ")";
			} else {
				text = "Select to stop the pulse";
			}
			not.setLatestEventInfo(this, getResources().getString(R.string.app_name), text, pi);
			if (displayNotificationAlways) {
				not.flags |= Notification.FLAG_NO_CLEAR;
			} else {
				not.deleteIntent = pi;
			}
			
			NotificationManager nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
			nm.notify(R.layout.main, not);
			notificationDisplayed = true;
		}
	}
	
	private void removeAppNotification() {
		NotificationManager nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		nm.cancel(R.layout.main);
		notificationDisplayed = false;
	}
	
	private void updateLEDChargeStatus(int plugged, int level, int scale) {
		if (!touchLED.isUsable()) {
			return;
		}
		PowerManager pm = (PowerManager)getSystemService(POWER_SERVICE);
		if (!pm.isScreenOn()) {
			boolean currentDeviceCharging = deviceCharging;
			if (plugged > 0) {
				if (level < scale) {
					deviceCharging = true;
					// charging
				} else {
					deviceCharging = false;
				}
			} else {
				deviceCharging = false;
			}
			Logger.logDebug("Battery status updated (" + plugged + "/" + level + "/" + scale +") " + currentDeviceCharging + "/" + deviceCharging);
			if (!currentDeviceCharging && deviceCharging) {
				int touchLEDState = touchLEDProperties.getInt(PROPERTY_TOUCH_LED_STATE, STATE_TOUCH_LED_INACTIVE);
				if (touchLEDState == STATE_TOUCH_LED_INACTIVE || touchLEDState == STATE_TOUCH_LED_CANCELLED) {
					Logger.logDebug("LED start");
					
					if (touchLED instanceof TouchLEDP970) {
						((TouchLEDP970)touchLED).setOnOffLED(true);
					}
					
					touchLED.set(TouchLED.MENU, preferences.getInt("seekBarTouchLEDBrightnessWhileChargingPref", Constants.DEFAULT_LED_BRIGHTNESS_WHILE_CHARGING));
					
					touchLEDProperties.putInt(PROPERTY_TOUCH_LED_STATE, STATE_TOUCH_LED_CHARGING);
					touchLEDProperties.save();
				}
			} else {
				if (currentDeviceCharging && !deviceCharging) {
					// fully charged
					int touchLEDState = touchLEDProperties.getInt(PROPERTY_TOUCH_LED_STATE, STATE_TOUCH_LED_INACTIVE);
					if (touchLEDState == STATE_TOUCH_LED_CHARGING) {
						Logger.logDebug("LED stop");
						if (touchLED instanceof TouchLEDP970) {
							((TouchLEDP970)touchLED).setOnOffLED(false);
						} else {
							touchLED.set(TouchLED.MENU, touchLED.getMin());
						}
						touchLEDProperties.putInt(PROPERTY_TOUCH_LED_STATE, STATE_TOUCH_LED_INACTIVE);
						touchLEDProperties.save();
					}
				}
			}
		} else {
			deviceCharging = false;
		}
	}
	
	private void onNewNotification(String id) {
		boolean start = true;
		if (disableOnCharge) {
			Intent batteryIntent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
			if (batteryIntent != null) {
				int status = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
				if (status != 0) {
					Logger.logDebug("Device is charging, do not start");
					start = false;
				}
			}
		}
		if (disablePulseOnLowBattery) {
			Intent batteryIntent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
			if (batteryIntent != null) {
				int plugged = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
				int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
				if (plugged == 0 && level <= lowBatteryPulseDisablePercentage) {
					Logger.logDebug("Device battery is too low, do not start");
					start = false;
				}
			}
		}

		String activeNotification = id;
		if (activeNotification == null) {
			activeNotification = Constants.PREFERENCE_KEY_DEFAULT_PULSE;
		}
		// special case for gmail
		if (activeNotification.startsWith(GmailContentObserver.ID)) {
			activeNotification = GmailContentObserver.ID;
		}
		if (!preferences.getBoolean(activeNotification + "." + Constants.PREFERENCE_KEY_CUSTOMIZED_PULSE, Constants.DEFAULT_CUSTOMIZED_PULSE)) {
			activeNotification = Constants.PREFERENCE_KEY_DEFAULT_PULSE;
		}
		
		if (start && hasNotifications()) {
			// decide if we want to stop this notification
			int currentPriority = toInt(preferences.getString(getActiveNotification() + "." + Constants.PREFERENCE_KEY_PRIORITY, String.valueOf(Constants.DEFAULT_FEEDBACK_PRIORITY)), Constants.DEFAULT_FEEDBACK_PRIORITY);
			int nextPriority = toInt(preferences.getString(activeNotification + "." + Constants.PREFERENCE_KEY_PRIORITY, String.valueOf(Constants.DEFAULT_FEEDBACK_PRIORITY)), Constants.DEFAULT_FEEDBACK_PRIORITY);
			Logger.logDebug("Current priority: " + currentPriority + ", next: " + nextPriority);
			if (currentPriority > nextPriority) {
				start = false;
			} else {
				stopFeedback(getActiveNotification());
			}
		}
		if (start) {
			if (id != null && !activeNotifications.contains(id)) {
				Logger.logDebug("Add new notification id: " + id);
				activeNotifications.add(id);
				try {
					activeNotifications.serialize();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			Logger.logDebug("Using pulse options: " + activeNotification);
			setActiveNotification(activeNotification);
			PowerManager pm = (PowerManager)getSystemService(POWER_SERVICE);
			if (pm.isScreenOn()) {
				KeyguardManager km = (KeyguardManager)getSystemService(KEYGUARD_SERVICE);
				// check if want to use accelerometer in keyguard, and if we're in the keyguard
				if (!km.inKeyguardRestrictedInputMode() || useAccelerometerInKeyGuard && km.inKeyguardRestrictedInputMode()) {
					registerSensor(sensorAccelerometer); // use sensors to determine user activity
				} else {
					Logger.logDebug("In lock screen, do not enable sensors");
				}
			}
			if (!pm.isScreenOn() || !delayUntilScreenOff) {
				if (delayUntilScreenOff) {
					stopOnScreenOn = true;
				}
				int touchLEDState = touchLEDProperties.getInt(PROPERTY_TOUCH_LED_STATE, STATE_TOUCH_LED_INACTIVE);
				startFeedback(touchLEDState != STATE_TOUCH_LED_PULSE ? TOUCH_LED_START_PULSE : 0, activeNotification);
			} else {
				Logger.logDebug("Delaying until screen turns off");
			}
		}
	}
	
	private void setActiveNotification(String activeNotification) {
		touchLEDProperties.put(PROPERTY_ACTIVE_NOTIFICATION, activeNotification);
		touchLEDProperties.save();
	}
	
	private String getActiveNotification() {
		return touchLEDProperties.get(PROPERTY_ACTIVE_NOTIFICATION);
	}
}
