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

import android.accessibilityservice.*;
import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.content.pm.PackageManager.NameNotFoundException;
import android.preference.*;
import android.view.accessibility.*;

import com.martinborjesson.o2xtouchlednotifications.*;
import com.martinborjesson.o2xtouchlednotifications.utils.*;

public class AccessibilityService extends android.accessibilityservice.AccessibilityService {
	
	static public final String MONITORED_ACTIVITIES_FILE = "monitored_activities.dat";
	static public final String EXCLUDED_MONITORED_ACTIVITIES_FILE = "excluded_monitored_activities.dat";
	
	static public class NotificationEvent {
		public long time = 0;
		public String packageName = null;
		public String label = null;
		public boolean lights = false;
	}
	
	private final static int MAX_LOG_NOTIFICATIONS = 20;
	
	public static final String ACTION_READ_SETTINGS = AccessibilityService.class.getName() + ".ACTION_READ_SETTINGS";
	public static final String ACTION_ADD = AccessibilityService.class.getName() + ".ACTION_ADD";
	public static final String ACTION_ADD_EXCLUDED = AccessibilityService.class.getName() + ".ACTION_ADD_EXCLUDED";

	private SerializableArrayList<String> notificationPackageNames = null;
	private SerializableArrayList<String> excludedNotificationPackageNames = null;
	private boolean logAllNotifications = false;
	static private List<NotificationEvent> notificationEvents = null;
	private boolean reactOnLightNotifications = false;
	
	@Override
	public void onCreate() {
		super.onCreate();
		notificationPackageNames = new SerializableArrayList<String>(this, MONITORED_ACTIVITIES_FILE);
		excludedNotificationPackageNames = new SerializableArrayList<String>(this, EXCLUDED_MONITORED_ACTIVITIES_FILE);
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null) {
			if (intent.getAction().equals(ACTION_READ_SETTINGS)) {
				readSettings();
			} else if (intent.getAction().equals(ACTION_ADD)) {
				String packageName = intent.getStringExtra("packageName");
				if (packageName != null) {
					Logger.logDebug("[Accessibility] Adding package: " + packageName);
					notificationPackageNames.add(packageName);
				}
			} else if (intent.getAction().equals(ACTION_ADD_EXCLUDED)) {
				String packageName = intent.getStringExtra("packageName");
				if (packageName != null) {
					Logger.logDebug("[Accessibility] Adding excluded package: " + packageName);
					excludedNotificationPackageNames.add(packageName);
				}
			}
		}
		return START_STICKY;
	}

	private void readSettings() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		reactOnLightNotifications = prefs.getBoolean("checkBoxReactOnLightNotifications", Constants.DEFAULT_REACT_ON_LIGHT_NOTIFICATIONS);
		Logger.logDebug("React on notifications with FLAG_SHOW_LIGHTS: " + reactOnLightNotifications);
		
		notificationPackageNames.clear();
		{
			try {
				notificationPackageNames.unserialize();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		Logger.logDebug("Monitored activities:");
		for (String s : notificationPackageNames) {
			Logger.logDebug(s);
		}
		excludedNotificationPackageNames.clear();
		if (reactOnLightNotifications) {
			try {
				excludedNotificationPackageNames.unserialize();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		Logger.logDebug("Excluded activities:");
		for (String s : excludedNotificationPackageNames) {
			Logger.logDebug(s);
		}

		
		logAllNotifications = prefs.getBoolean("checkBoxLogNotificationsFromActivities", Constants.DEFAULT_LOG_NOTIFICATIONS_FROM_ACTIVITIES);

		Logger.logDebug("Logging all notifications: " + logAllNotifications);
		if (logAllNotifications && notificationEvents == null) {
			notificationEvents = new ArrayList<NotificationEvent>();
		} else {
			notificationEvents = null;
		}
		
	}

	@Override
	public void onAccessibilityEvent(AccessibilityEvent event) {
		Logger.logDebug("onAccessibilityEvent(): " + event.toString());
		if (logAllNotifications && notificationEvents != null) {
			PackageManager pm = getPackageManager();
			NotificationEvent e = new NotificationEvent();
			e.time = System.currentTimeMillis();
			e.packageName = String.valueOf(event.getPackageName());
			try {
				ApplicationInfo ai = pm.getApplicationInfo(e.packageName, 0);
				e.label = String.valueOf(ai.loadLabel(pm));
			} catch (NameNotFoundException e1) {
				e.label = e.packageName;
			}
			if (event.getParcelableData() instanceof Notification) {
				Notification n = (Notification)event.getParcelableData();
				e.lights = (n.flags&Notification.FLAG_SHOW_LIGHTS) == Notification.FLAG_SHOW_LIGHTS;
			}
			if (notificationEvents.size() >= MAX_LOG_NOTIFICATIONS) {
				notificationEvents.remove(notificationEvents.size()-1);
			}

			notificationEvents.add(0, e);
		}
		if (!excludedNotificationPackageNames.contains(event.getPackageName())) {
			boolean newNotification = false;
			if (notificationPackageNames.contains(event.getPackageName())) {
				// if only react on light
				SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
				boolean reactOnLight = preferences.getBoolean(event.getPackageName() + "." + Constants.PREFERENCE_KEY_ONLY_LIGHT_NOTIFICATION, Constants.DEFAULT_PREFERENCE_KEY_ONLY_LIGHT_NOTIFICATION);
				
				if (reactOnLight && event.getParcelableData() instanceof Notification) {
					Notification n = (Notification)event.getParcelableData();
					if ((n.flags&Notification.FLAG_SHOW_LIGHTS) == Notification.FLAG_SHOW_LIGHTS || (n.defaults&Notification.DEFAULT_LIGHTS) == Notification.DEFAULT_LIGHTS) {
						Logger.logDebug("(Normal) This notification want to enable LED lights. On: " + n.ledOnMS + ", off: " + n.ledOffMS + ", defaults: " + ((n.defaults&Notification.DEFAULT_LIGHTS) == Notification.DEFAULT_LIGHTS));
						newNotification = true;
					}
				} else {
					newNotification = true;
				}
			} else if (reactOnLightNotifications && event.getParcelableData() instanceof Notification) {
				Notification n = (Notification)event.getParcelableData();
				if ((n.flags&Notification.FLAG_SHOW_LIGHTS) == Notification.FLAG_SHOW_LIGHTS || (n.defaults&Notification.DEFAULT_LIGHTS) == Notification.DEFAULT_LIGHTS) {
					Logger.logDebug("(React on light) This notification want to enable LED lights. On: " + n.ledOnMS + ", off: " + n.ledOffMS + ", defaults: " + ((n.defaults&Notification.DEFAULT_LIGHTS) == Notification.DEFAULT_LIGHTS));
					// according to the SDK if both on and off is zero the leds should stop
					// at the moment this is ignored though :)
					newNotification = true;
				}
			} else {
				Logger.logDebug("Ignore");
			}
			if (newNotification) {
				MainService.newNotification(this, String.valueOf(event.getPackageName()));
			}
		} else {
			Logger.logDebug(event.getPackageName() + " was excluded.");
		}
	}

	@Override
	public void onInterrupt() {
		Logger.logDebug("onInterrupt()");
	}
	
	@Override
	protected void onServiceConnected() {
		Logger.logDebug("onServiceConnected()");
		
		AccessibilityServiceInfo info = new AccessibilityServiceInfo();
		info.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;
		info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
		info.notificationTimeout = 80;
		
		setServiceInfo(info);
	}
	
	public static List<NotificationEvent> getNotificationEvents() {
		return notificationEvents;
	}

}
