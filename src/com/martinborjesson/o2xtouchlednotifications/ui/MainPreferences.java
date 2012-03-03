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

package com.martinborjesson.o2xtouchlednotifications.ui;

import java.io.*;
import java.util.*;

import android.accounts.*;
import android.app.*;
import android.content.*;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.*;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.*;
import android.graphics.*;
import android.media.*;
import android.net.*;
import android.os.*;
import android.preference.*;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.provider.*;
import android.provider.Settings.SettingNotFoundException;
import android.text.format.*;
import android.text.method.*;
import android.util.*;
import android.widget.*;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.martinborjesson.o2xtouchlednotifications.*;
import com.martinborjesson.o2xtouchlednotifications.notifications.*;
import com.martinborjesson.o2xtouchlednotifications.services.*;
import com.martinborjesson.o2xtouchlednotifications.services.AccessibilityService.NotificationEvent;
import com.martinborjesson.o2xtouchlednotifications.touchled.*;
import com.martinborjesson.o2xtouchlednotifications.touchled.devices.*;
import com.martinborjesson.o2xtouchlednotifications.ui.preference.*;
import com.martinborjesson.o2xtouchlednotifications.ui.preference.SeekBarPreference.OnNoChangeListener;
import com.martinborjesson.o2xtouchlednotifications.utils.*;

/**
 * TODO: clean this mess up :)
 * @author Martin Borjesson
 *
 */
public class MainPreferences extends PreferenceActivity implements OnSharedPreferenceChangeListener {

	static private class PreferenceSummaryChanger implements OnPreferenceChangeListener {
		
		static public interface OnSummaryChangeListener {
			public String onSummaryChange(Preference preference, String value);
			public String onValueChange(Preference preference, String value);
			public boolean onPreferenceChange(Preference preference, Object newValue);
		}
		
		private Preference parent = null;
		private String summary = null;
		private OnSummaryChangeListener listener = null;
		
		/**
		 * Use %s in summary to replace with the value
		 * @param parent
		 * @param summary
		 */
		public PreferenceSummaryChanger(Preference parent, String summary, OnSummaryChangeListener listener) {
			this.parent = parent;
			this.summary = summary;
			this.listener = listener;
		}

		@Override
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			parent.setSummary(getSummary(newValue.toString()));
			if (listener != null) {
				return listener.onPreferenceChange(preference, newValue);
			}
			return true;
		}
		
		public String getSummary(String value) {
			String summary = this.summary;
			if (listener != null) {
				String v = listener.onValueChange(parent, value);
				if (v != null) {
					value = v;
				}
				String s = listener.onSummaryChange(parent, value);
				if (s != null) {
					summary = s;
				}
			}
			return summary.replace("%s", value);
		}
		
	}
	
	private class PackageData implements Comparable<PackageData> {
		public PackageData(String packageName, String label) {
			this.packageName = packageName;
			if (label != null) {
				this.label = label;
			} else {
				this.label = packageName;
			}
		}
		public String packageName;
		public String label;
		
		@Override
		public int compareTo(PackageData another) {
			return label.compareTo(another.label);
		}
	}
	
	private class PackageLabelLoader extends Thread {
		
		private PackageData[] packageInfo = null;
		
		private PackageManager packageManager = null;
		private ProgressDialog progressDialog = null;
		
		private boolean cancel = false;

		public PackageLabelLoader(ProgressDialog progressDialog, PackageManager packageManager) {
			this.packageManager = packageManager;
			this.progressDialog = progressDialog;
		}
		
		@Override
		public void run() {
			List<ApplicationInfo> list = packageManager.getInstalledApplications(PackageManager.GET_ACTIVITIES);
			int len = list.size();
			progressDialog.setMax(len);
			packageInfo = new PackageData[len];
			for (int i = 0; i < list.size(); ++i) {
				if (cancel) {
					break;
				}
				ApplicationInfo info = list.get(i);
				CharSequence label = info.loadLabel(packageManager);
				packageInfo[i] = new PackageData(info.packageName, String.valueOf(label));

				progressDialog.setProgress(i);
			}

			if (!cancel) {
				Arrays.sort(packageInfo);
			}

			progressDialog.dismiss();
		}
		
		public void cancel() {
			cancel = true;
		}
		
	}
	
	private PackageLabelLoader packageLabelLoader = null;
	private ProgressDialog packageLoaderDialog = null;
	private Dialog testPulseDialog = null;
	
	private SerializableArrayList<String> activities = null;

	private SharedPreferences sharedPreferences = null;
	
	private TouchLED touchLED = null;
	
	private int touchLEDStrength = TouchLED.getTouchLED().getMax();
	
	private String currentPulseKey = Constants.PREFERENCE_KEY_DEFAULT_PULSE;
	
	private String[] pulseEntries = null;
	private String[] pulseValues = null;
	
	private String[] vibrateTypeEntries = null;
	private String[] vibrateTypeValues = null;
	
	private String[] notificationRingtoneTypeEntries = null;
	private String[] notificationRingtoneTypeValues = null;
	
	private String[] pulseTimeoutEntries = null;
	private String[] pulseTimeoutValues = null;
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.main_preferences);
        
        pulseEntries = getResources().getStringArray(R.array.pulse_type_entries);
        pulseValues = getResources().getStringArray(R.array.pulse_type_values);
        
        vibrateTypeEntries = getResources().getStringArray(R.array.vibrate_type_entries);
        vibrateTypeValues = getResources().getStringArray(R.array.vibrate_type_values);

        notificationRingtoneTypeEntries = getResources().getStringArray(R.array.notification_ringtone_type_entries);
        notificationRingtoneTypeValues = getResources().getStringArray(R.array.notification_ringtone_type_values);

        pulseTimeoutEntries = getResources().getStringArray(R.array.fade_timeout_entries);
        pulseTimeoutValues = getResources().getStringArray(R.array.fade_timeout_values);
        
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        
        touchLED = TouchLED.getTouchLED();
        
		// check if gmail works
        if (!sharedPreferences.getBoolean("fixGMailNoPermissions", Constants.DEFAULT_FIX_GMAIL_NO_PERMISSIONS)) {
	    	boolean gmailAvailable = false;
	    	
			AccountManager am = AccountManager.get(this);
			Account[] accounts = am.getAccountsByType("com.google");
			for (Account account : accounts) {
		    	GmailContentObserver gmailObserver = new GmailContentObserver(getContentResolver(), this, account);
		    	if (gmailObserver.isAvailable(this)) {
		    		gmailAvailable = true;
		    	}
			}
			if (!gmailAvailable) {
				Editor edit = sharedPreferences.edit();
				edit.putBoolean("fixGMailNoPermissions", true);
				edit.commit();
	
				askIfNotEnabledInAccessibilitySettings(R.string.dialog_message_no_gmail_permissions);
			}
        }

        boolean doInit = true;
    	if (touchLED.isValid() && !touchLED.hasProperPermissions()) {
			doInit = false;
    		if (SuperUser.hasSuperUser()) {
    			// rooted
    			AlertDialog.Builder builder = new AlertDialog.Builder(this);
    			builder.setMessage(R.string.dialog_message_has_root_do_chmod)
    			       .setCancelable(false)
    			       .setPositiveButton(android.R.string.yes, new OnClickListener() {
    					
    					@Override
    					public void onClick(DialogInterface dialog, int which) {
    	        		    Editor edit = MainPreferences.this.sharedPreferences.edit();
    	        		    edit.putBoolean("rootPermissionFix", true);
    	        		    edit.commit();
    	        		    
    	        		    FeedbackService.performFixes(MainPreferences.this);
    	        		    
    	        		    TouchLED.reset();
    	        		    FeedbackService.startService(MainPreferences.this, null);
    	        		    MainPreferences.this.initialize();
    					}
    				})
    			       .setNegativeButton(android.R.string.no, new OnClickListener() {
    			           public void onClick(DialogInterface dialog, int id) {
    			                dialog.cancel();
    			                MainPreferences.this.initialize();
    			           }
    			       });
    			Dialog enableAccessibilityDialog = builder.create();
    			enableAccessibilityDialog.show();

    		} else {
    			// not rooted
    			AlertDialog.Builder builder = new AlertDialog.Builder(this);
    			builder.setMessage(R.string.dialog_message_no_root)
    			       .setCancelable(false)
    			       .setPositiveButton(android.R.string.ok, new OnClickListener() {
    			           public void onClick(DialogInterface dialog, int id) {
    			                dialog.cancel();
        	        		    TouchLED.reset();
    			                MainPreferences.this.initialize();
    			           }
    			       });
    			Dialog enableAccessibilityDialog = builder.create();
    			enableAccessibilityDialog.show();
    		}
        }
    	
        if (doInit) {
        	initialize();
        }
	}
	
	private void showUnsupportedDeviceDialog() {
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setTitle(R.string.dialog_title_unable_to_start)
    			.setMessage(R.string.dialog_message_unable_to_start)
    			.setCancelable(false)
    			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
    				public void onClick(DialogInterface dialog, int id) {
    					
    				}
    			});
    	AlertDialog alert = builder.create();
    	alert.show();
	}
	
	/**
	 * Initialize preferences
	 */
	public void initialize() {
		PackageInfo packageInfo = null;
		try {
			packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
		} catch (NameNotFoundException e1) {
			e1.printStackTrace();
		}

        
        // see if the touch led strength is set, if not set it to the current value
        if (touchLED.isValid() && !sharedPreferences.contains("seekBarTouchLEDStrengthPref")) {
        	Editor editor = sharedPreferences.edit();
        	editor.putInt("seekBarTouchLEDStrengthPref", touchLED.getCurrent());
        	editor.commit();
        }

        if (!touchLED.isValid()) {
        	showUnsupportedDeviceDialog();
        }

    	
    	AppProperties props = new AppProperties(this, "version.dat");
    	props.load();
    	int vc = props.getInt("VersionCode", -1);
    	Log.d(Logger.LOGTAG, vc + "/" + packageInfo.versionCode);
    	if (vc < packageInfo.versionCode) {
    		props.putInt("VersionCode", packageInfo.versionCode);
    		props.save();
    		
    		Editor edit = sharedPreferences.edit();
    		edit.putBoolean("changeLogDisplayed", true);
    		setNewDefaults(edit, vc);
    		edit.commit();
    		showChangelogDialog();
    	}

//      if (!sharedPreferences.getBoolean("firstTimeDialogDisplayed", false)) {
//    		Editor edit = sharedPreferences.edit();
//    		edit.putBoolean("firstTimeDialogDisplayed", true);
//    		edit.commit();
//    		showFirstTimeDialog();
//    	}
    	
        
        {
        	PreferenceScreen screen = (PreferenceScreen)findPreference("SleepPrefs");
        	createSleepOptions(screen);
        }
        
        {
        	Preference p = findPreference("preferenceNotificationsMonitoredActivitiesPref");
        	p.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				
				@Override
				public boolean onPreferenceClick(Preference preference) {
					showActivities(AccessibilityService.MONITORED_ACTIVITIES_FILE);
					return true;
				}
			});
        }
        {
        	Preference p = findPreference("preferenceNotificationsClearMonitoredActivitiesPref");
        	p.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				
				@Override
				public boolean onPreferenceClick(Preference preference) {
//					ListFileManager activities = new ListFileManager(MainPreferences.this, AccessibilityService.MONITORED_ACTIVITIES_FILE);
//					activities.save();
					try {
						SerializableArrayList<String> clearedList = new SerializableArrayList<String>(MainPreferences.this, AccessibilityService.MONITORED_ACTIVITIES_FILE);
						clearedList.serialize();
					} catch (IOException e) {
						e.printStackTrace();
					}
					
			    	if (sharedPreferences.getBoolean("checkBoxServiceEnabled", Constants.DEFAULT_SERVICE_ENABLED)) {
			    		MainService.readNotificationActivitiesSettings(MainPreferences.this);
			    	}

			    	Toast toast = Toast.makeText(MainPreferences.this, R.string.toast_monitored_activites_removed, Toast.LENGTH_SHORT);
			    	toast.show();

					return true;
				}
			});
        }
        {
        	Preference p = findPreference("preferenceNotificationsExcludedActivitiesPref");
        	p.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				
				@Override
				public boolean onPreferenceClick(Preference preference) {
					showActivities(AccessibilityService.EXCLUDED_MONITORED_ACTIVITIES_FILE);
					return true;
				}
			});
        }
        {
        	Preference p = findPreference("preferenceNotificationsClearExcludedActivitiesPref");
        	p.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				
				@Override
				public boolean onPreferenceClick(Preference preference) {
//					ListFileManager activities = new ListFileManager(MainPreferences.this, AccessibilityService.EXCLUDED_MONITORED_ACTIVITIES_FILE);
//					activities.save();
					try {
						SerializableArrayList<String> clearedList = new SerializableArrayList<String>(MainPreferences.this, AccessibilityService.EXCLUDED_MONITORED_ACTIVITIES_FILE);
						clearedList.serialize();
					} catch (IOException e) {
						e.printStackTrace();
					}
					
			    	if (sharedPreferences.getBoolean("checkBoxServiceEnabled", Constants.DEFAULT_SERVICE_ENABLED)) {
			    		MainService.readNotificationActivitiesSettings(MainPreferences.this);
		    		}

			    	Toast toast = Toast.makeText(MainPreferences.this, R.string.toast_excluded_activites_removed, Toast.LENGTH_SHORT);
			    	toast.show();

					return true;
				}
			});
        }
        
        {
        	createTouchLEDOptions((PreferenceScreen)findPreference("TouchLEDPrefs"));
        	createAccelerometerOptions((PreferenceScreen)findPreference("AccelerometerPrefs"));
        	createBatterySavingOptions((PreferenceScreen)findPreference("BatterySavePrefs"));
        	
        	PreferenceScreen feedbackScreen = (PreferenceScreen)findPreference("CustomizePulsePrefs");
        	feedbackScreen.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				
				@Override
				public boolean onPreferenceClick(Preference preference) {
					currentPulseKey = Constants.PREFERENCE_KEY_DEFAULT_PULSE;
					return true;
				}
			});
        	createFeedbackOptions(feedbackScreen, Constants.PREFERENCE_KEY_DEFAULT_PULSE);
        	updateTouchLEDButtonsOptions(MainService.toInt(sharedPreferences.getString(Constants.PREFERENCE_KEY_DEFAULT_PULSE + "." + Constants.PREFERENCE_KEY_TOUCH_LED_MODE, String.valueOf(Constants.DEFAULT_PULSE_MODE)), Constants.DEFAULT_PULSE_MODE), Constants.PREFERENCE_KEY_DEFAULT_PULSE);
        }
        {
//        	Preference p = findPreference("buttonTest");
//        	p.setOnPreferenceClickListener(new OnPreferenceClickListener() {
//				
//				@Override
//				public boolean onPreferenceClick(Preference preference) {
//					AlertDialog.Builder builder = new AlertDialog.Builder(MainPreferences.this);
//					builder.setTitle("Testing pulse...");
//					builder.setNegativeButton(android.R.string.cancel, new OnClickListener() {
//						
//						@Override
//						public void onClick(DialogInterface dialog, int which) {
//							BootReceiver.stopPulse(MainPreferences.this);
//						}
//					});
//					testPulseDialog = builder.create();
//					testPulseDialog.show();
//
//			    	BootReceiver.startService(MainPreferences.this, null);
//
//					return true;
//				}
//			});
        }
        { // log all notifications
        	Preference p = findPreference("buttonListNotificationsFromActivities");
        	p.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				
				@Override
				public boolean onPreferenceClick(Preference preference) {
					Context context = MainPreferences.this;
					AlertDialog.Builder builder = new AlertDialog.Builder(context);
					builder.setTitle(R.string.dialog_title_logged_notifications);
					builder.setPositiveButton(android.R.string.ok, null);
					List<NotificationEvent> events = AccessibilityService.getNotificationEvents();
					ScrollView sv = new ScrollView(context);
					LinearLayout ll = new LinearLayout(context);
					sv.addView(ll);
					ll.setOrientation(LinearLayout.VERTICAL);
					if (events != null && !events.isEmpty()) {
						for (NotificationEvent e : events) {
							TextView tv = new TextView(context);
							tv.setTypeface(Typeface.DEFAULT_BOLD);
							tv.setText(e.label);
							ll.addView(tv);
							tv = new TextView(context);
							tv.setText(getString(R.string.dialog_message_logged_notifications_package_name) + " " + e.packageName);
							ll.addView(tv);
							tv = new TextView(context);
							tv.setText(getString(R.string.dialog_message_logged_notifications_trigger_lights) + " " + e.lights);
							ll.addView(tv);
							tv = new TextView(context);
							Calendar cal = Calendar.getInstance();
							cal.setTimeInMillis(e.time);
							tv.setText(getString(R.string.dialog_message_logged_notifications_received) + " " + cal.getTime().toString());
							ll.addView(tv);
						}
					} else {
						TextView tv = new TextView(context);
						tv.setText(R.string.dialog_message_logged_notifications_none);
						ll.addView(tv);
					}
					builder.setView(sv);
					builder.show();
					return true;
				}
			});
        }
    	{ // show message if accessibility is not enabled when clicking on react on light notifications checkbox
    		CheckBoxPreference pref = (CheckBoxPreference)findPreference("checkBoxReactOnLightNotifications");
    		pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				
				@Override
				public boolean onPreferenceClick(Preference preference) {
					askIfNotEnabledInAccessibilitySettings();
					return true;
				}
			});
    	}
    	{ // clear log file
    		Preference pref = findPreference("buttonClearLogFile");
    		pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				
				@Override
				public boolean onPreferenceClick(Preference preference) {
					Logger.clearLogFile();
					return true;
				}
			});
    	}
    	{ // about
    		PreferenceScreen about = (PreferenceScreen)findPreference("screenAbout");
    		about.removeAll();
    		
    		{
    			Preference changelog = new Preference(this);
    			changelog.setTitle(R.string.preference_title_show_changelog);
    			changelog.setOnPreferenceClickListener(new OnPreferenceClickListener() {
					
					@Override
					public boolean onPreferenceClick(Preference preference) {
						showChangelogDialog();
						return true;
					}
				});
    			about.addPreference(changelog);
    		}
    		
    		{
	    		Preference version = new Preference(this);
	    		version.setTitle(R.string.preference_title_about_version);
				version.setSummary(packageInfo.versionName);
	    		about.addPreference(version);
    		}
    		
    		{
    			Preference device = new Preference(this);
    			device.setTitle(R.string.preference_title_about_device);
    			device.setSummary(TouchLED.getTouchLED().getDeviceName());
    			about.addPreference(device);
    		}
    		
    		{
	    		Preference developer = new Preference(this);
	    		developer.setTitle(R.string.preference_title_about_developer);
				developer.setSummary("Martin Börjesson"); // not changeable :)
				about.addPreference(developer);
    		}
    		
    		String language = getString(R.string.translator_language);
    		String translatorName = getString(R.string.translator_name);
    		if (language.length() > 0 && translatorName.length() > 0) {
	    		Preference translator = new Preference(this);
	    		translator.setTitle(getString(R.string.preference_title_about_translator) + " (" + language + ")");
				translator.setSummary(translatorName);
				about.addPreference(translator);
    		}

    	}
    	{ // pulse/customize
    		
    		PreferenceCategory category = (PreferenceCategory)findPreference("PulseCustomizePrefs");
    		{
	    		SeekBarPreference seekBar = new SeekBarPreference(this);
	    		seekBar.setKey("seekBarPulseDelayPrefs");
	    		seekBar.setTitle(R.string.preference_title_feedback_delay);
	    		seekBar.setDialogTitle(seekBar.getTitle());
	    		seekBar.setDialogMessage(R.string.dialog_message_feedback_delay);
	    		seekBar.setMax(Constants.DEFAULT_MAX_PULSE_DELAY);
	    		seekBar.setDefaultValue(Constants.DEFAULT_PULSE_DELAY);
	
	    		PreferenceSummaryChanger changer = new PreferenceSummaryChanger(seekBar, getString(R.string.dialog_description_feedback_delay), new PreferenceSummaryChanger.OnSummaryChangeListener() {
					
					@Override
					public String onSummaryChange(Preference preference, String value) {
						if (value.equals("0")) {
							return getString(R.string.dialog_description_feedback_delay_disabled);
						}
						return null;
					}
		
					@Override
					public String onValueChange(Preference preference, String value) {
						if (value.equals("1")) {
							return "1 " + getString(R.string.time_second);
						} else if (value.equals("0")) {
							return null;
						}
						return value + " " + getString(R.string.time_seconds);
					}
	
					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						return true;
					}
				});
	    		seekBar.setSummary(changer.getSummary(String.valueOf(sharedPreferences.getInt("seekBarPulseDelayPrefs", Constants.DEFAULT_PULSE_DELAY))));
	    		seekBar.setOnPreferenceChangeListener(changer);
	    		category.addPreference(seekBar);
    		}
    		{
	    		ListPreference timeoutList = new ListPreference(this);
	    		timeoutList.setKey("listPulseOptionsTimeoutTimePref");
	    		timeoutList.setTitle(R.string.preference_title_feedback_timeout);
	    		timeoutList.setDefaultValue(String.valueOf(Constants.DEFAULT_PULSE_TIMEOUT));
	    		timeoutList.setEntries(R.array.fade_timeout_entries);
	    		timeoutList.setEntryValues(R.array.fade_timeout_values);
	    		PreferenceSummaryChanger changer = new PreferenceSummaryChanger(timeoutList, getString(R.string.preference_summary_feedback_timeout), new PreferenceSummaryChanger.OnSummaryChangeListener() {
	    			
	    			@Override
	    			public String onSummaryChange(Preference preference, String value) {
						for (int i = 0; i < pulseTimeoutValues.length; ++i) {
							if (pulseTimeoutValues[i].equals(value)) {
								return pulseTimeoutEntries[i];
							}
						}

	    				return null;
	    			}
	
	    			@Override
	    			public String onValueChange(Preference preference, String value) {
	    				try {
	    					float v = Float.valueOf(value);
	    					// has no decimals ?
	    					if ((int)v == v) {
	    						return String.valueOf((int)v);
	    					}
	    				} catch (NumberFormatException e) {}
	    				return null;
	    			}
	
	    			@Override
	    			public boolean onPreferenceChange(Preference preference,
	    					Object newValue) {
	    				return true;
	    			}
	    		});
	    		timeoutList.setSummary(changer.getSummary(sharedPreferences.getString(timeoutList.getKey(), String.valueOf(Constants.DEFAULT_PULSE_TIMEOUT))));
	    		timeoutList.setOnPreferenceChangeListener(changer);
	    		category.addPreference(timeoutList);
    		}
    	}
    	{ // charge prefs
    		PreferenceScreen screen = (PreferenceScreen)findPreference("ChargingPrefs");
    		SeekBarPreference seekBar = new SeekBarPreference(this);
    		seekBar.setKey("seekBarTouchLEDBrightnessWhileChargingPref");
    		seekBar.setTitle(R.string.preference_title_miscellaneous_charging_touch_led_brightness);
    		seekBar.setSummary(R.string.preference_summary_miscellaneous_charging_touch_led_brightness);
    		seekBar.setDialogTitle(seekBar.getTitle());
    		seekBar.setMax(touchLED.getMax());
    		seekBar.setDefaultValue(Constants.DEFAULT_LED_BRIGHTNESS_WHILE_CHARGING);
    		seekBar.setOnPreferenceClickListener(new OnPreferenceClickListener() {
    			
    			@Override
    			public boolean onPreferenceClick(Preference preference) {
    				touchLEDStrength = touchLED.getCurrent();
					touchLED.setAll(((SeekBarPreference)preference).getProgress());
    				return true;
    			}
    		});
    		seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
    			
    			@Override
    			public void onStopTrackingTouch(SeekBar seekBar) {
    				// TODO Auto-generated method stub
    				
    			}
    			
    			@Override
    			public void onStartTrackingTouch(SeekBar seekBar) {
    				// TODO Auto-generated method stub
    				
    			}
    			
    			@Override
    			public void onProgressChanged(SeekBar seekBar, int progress,
    					boolean fromUser) {
    				if (fromUser) {
//						touchLED.setAll(progress);
    				}
    			}
    		});
    		
    		seekBar.setOnNoChangeListener(new OnNoChangeListener() {
    			
    			@Override
    			public void onNoChange(Preference preference) {
					touchLED.setAll(touchLEDStrength);
    			}
    		});
    		seekBar.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					touchLED.setAll(touchLEDStrength);
					return true;
				}
			});
    		screen.addPreference(seekBar);
    	}
    	
		{
			CheckBoxPreference cb = (CheckBoxPreference)findPreference("checkBoxNotificationsGmailPref");
			cb.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				
				@Override
				public boolean onPreferenceClick(Preference preference) {
					createCustomizeNotifications((PreferenceScreen)findPreference("preferenceNotificationsCustomizeNotifications"));
					return true;
				}
			});
		}
		{
			CheckBoxPreference cb = (CheckBoxPreference)findPreference("checkBoxNotificationsSMSMMSPref");
			cb.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				
				@Override
				public boolean onPreferenceClick(Preference preference) {
					createCustomizeNotifications((PreferenceScreen)findPreference("preferenceNotificationsCustomizeNotifications"));
					return true;
				}
			});
		}
		{
			CheckBoxPreference cb = (CheckBoxPreference)findPreference("checkBoxNotificationsMissedCallsPref");
			cb.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				
				@Override
				public boolean onPreferenceClick(Preference preference) {
					createCustomizeNotifications((PreferenceScreen)findPreference("preferenceNotificationsCustomizeNotifications"));
					return true;
				}
			});
		}

		createCustomizeNotifications((PreferenceScreen)findPreference("preferenceNotificationsCustomizeNotifications"));
        
    	if (!touchLED.isValid()) {
    		findPreference("TouchLEDPrefs").setEnabled(false);
    		findPreference("ChargingPrefs").setEnabled(false);
    	}
    }
	
    @Override
    protected void onResume() {
    	super.onResume();
    	
    	if (sharedPreferences.getBoolean("checkBoxServiceEnabled", Constants.DEFAULT_SERVICE_ENABLED)) {
    		FeedbackService.startService(this, null);
    	}
    	
    	{
			CheckBoxPreference checkBox = (CheckBoxPreference)findPreference("checkBoxAutoLEDBrightnessPref");
			if (checkBox != null) {
				boolean isAuto = false;
				try {
					if (Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
						isAuto = true;
					}
				} catch (SettingNotFoundException e) {
				}
				if (!sharedPreferences.getBoolean(checkBox.getKey(), Constants.DEFAULT_AUTO_BRIGHTNESS)) {
					isAuto = false;
				}
				checkBox.setChecked(isAuto);
			}
    	}
    	
    	sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	
    	if (testPulseDialog != null && testPulseDialog.isShowing()) {
    		testPulseDialog.dismiss();
    		FeedbackService.stopFeedback(MainPreferences.this);
    	}
    	
    	sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

	
	private void createSleepOptions(PreferenceScreen screen) {
		screen.removeAll();
		
		CheckBoxPreference enabled = new CheckBoxPreference(this);
		enabled.setKey("checkBoxSleepEnabledPref");
		enabled.setTitle(R.string.preference_title_miscellaneous_sleep_enable);
		screen.addPreference(enabled);
		
		{
			TimePreference offTime = new TimePreference(this);
			offTime.setKey("timeSleepOffTimePref");
			offTime.setTitle(R.string.preference_title_miscellaneous_sleep_off_time);
			offTime.setDialogTitle(offTime.getTitle());
			offTime.setDefaultValue("22:00");
			
			PreferenceSummaryChanger offTimeChanger = new PreferenceSummaryChanger(offTime, "%s", new PreferenceSummaryChanger.OnSummaryChangeListener() {
				
				@Override
				public String onSummaryChange(Preference preference, String summary) {
	
					return null;
				}
	
				@Override
				public String onValueChange(Preference preference, String value) {
					String[] s = value.split(":");
					return getTime(Integer.valueOf(s[0]), Integer.valueOf(s[1]));
				}

				@Override
				public boolean onPreferenceChange(Preference preference,
						Object newValue) {
					return true;
				}
			});
			offTime.setSummary(offTimeChanger.getSummary(sharedPreferences.getString(offTime.getKey(), "22:00")));
			offTime.setOnPreferenceChangeListener(offTimeChanger);
			screen.addPreference(offTime);
			offTime.setDependency(enabled.getKey());
		}
		
		{
			TimePreference onTime = new TimePreference(this);
			onTime.setKey("timeSleepOnTimePref");
			onTime.setTitle(R.string.preference_title_miscellaneous_sleep_on_time);
			onTime.setDialogTitle(onTime.getTitle());
			onTime.setDefaultValue("08:00");
			
			PreferenceSummaryChanger onTimeChanger = new PreferenceSummaryChanger(onTime, "%s", new PreferenceSummaryChanger.OnSummaryChangeListener() {
				
				@Override
				public String onSummaryChange(Preference preference, String summary) {
	
					return null;
				}
	
				@Override
				public String onValueChange(Preference preference, String value) {
					String[] s = value.split(":");
					return getTime(Integer.valueOf(s[0]), Integer.valueOf(s[1]));
				}

				@Override
				public boolean onPreferenceChange(Preference preference,
						Object newValue) {
					return true;
				}
			});
			onTime.setSummary(onTimeChanger.getSummary(sharedPreferences.getString(onTime.getKey(), "08:00")));
			onTime.setOnPreferenceChangeListener(onTimeChanger);
			screen.addPreference(onTime);
			onTime.setDependency(enabled.getKey());
		}
		
	}
	
	private String getTime(int h, int m) {
		StringBuilder time = new StringBuilder();
		boolean is24Hour = DateFormat.is24HourFormat(this);
		boolean pm = false;
		if (!is24Hour) {
			if (h > 12) {
				h -= 12;
				pm = true;
			}
		}
		if (h < 10) {
			time.append("0");
		}
		time.append(h);
		time.append(":");
		if (m < 10) {
			time.append("0");
		}
		time.append(m);
		
		if (!is24Hour) {
			time.append(" " + (pm ? "PM" : "AM"));
		}

		return time.toString();
	}
	
	private void updateTouchLEDButtonsOptions(int mode, String key) {
		if (!TouchLED.getTouchLED().isUsable()) {
			return;
		}
		Preference fadeInP = findPreference(key + "." + Constants.PREFERENCE_KEY_TOUCH_LED_FADE_IN_TIME);
		Preference fullyLitP = findPreference(key + "." + Constants.PREFERENCE_KEY_TOUCH_LED_FULLY_LIT_TIME);
		Preference fadeOutP = findPreference(key + "." + Constants.PREFERENCE_KEY_TOUCH_LED_FADE_OUT_TIME);
		Preference inactiveTimeP = findPreference(key + "." + Constants.PREFERENCE_KEY_TOUCH_LED_INACTIVE_TIME);
		fadeInP.setEnabled(mode == 0 || mode == 3);
		fullyLitP.setEnabled(mode != 2);
		fadeOutP.setEnabled(mode == 0 || mode == 3);
		inactiveTimeP.setEnabled(mode == 0 || mode == 1);
	}
	
	private void createFeedbackOptions(PreferenceScreen screen, String key) {
		screen.removeAll();
		
    	Preference testButton = new Preference(this);
    	testButton.setPersistent(false);
    	testButton.setKey(key);
    	testButton.setTitle(R.string.preference_title_feedback_test);
    	testButton.setSummary(R.string.preference_summary_feedback_test);
    	testButton.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			
			@Override
			public boolean onPreferenceClick(Preference preference) {
				AlertDialog.Builder builder = new AlertDialog.Builder(MainPreferences.this);
				builder.setTitle(R.string.dialog_title_feedback_test);
				builder.setNegativeButton(android.R.string.cancel, new OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						FeedbackService.stopFeedback(MainPreferences.this);
					}
				});
				testPulseDialog = builder.create();
				testPulseDialog.show();

				FeedbackService.startService(MainPreferences.this, preference.getKey());

				return true;
			}
		});
    	screen.addPreference(testButton);

    	if (TouchLED.getTouchLED().isUsable()) {
			PreferenceCategory touchLEDCategory = new PreferenceCategory(this);
			touchLEDCategory.setTitle(R.string.preference_title_feedback_touch_led);
			screen.addPreference(touchLEDCategory);
			{
				ListPreference pref = new ListPreference(this);
				pref.setKey(key + "." + Constants.PREFERENCE_KEY_TOUCH_LED_MODE);
				pref.setTitle(R.string.preference_title_feedback_touch_led_mode);
				pref.setDefaultValue(String.valueOf(Constants.DEFAULT_PULSE_MODE));
				pref.setEntries(R.array.pulse_type_entries);
				pref.setEntryValues(R.array.pulse_type_values);
				PreferenceSummaryChanger changer = new PreferenceSummaryChanger(pref, "%s", new PreferenceSummaryChanger.OnSummaryChangeListener() {
					
					@Override
					public String onValueChange(Preference preference, String value) {
						return null;
					}
					
					@Override
					public String onSummaryChange(Preference preference, String value) {
	//					if (value.equals("0")) {
	//						return "Fading pulse";
	//					} else if (value.equals("1")) {
	//						return "Non-fading pulse";
	//					} else if (value.equals("2")) {
	//						return "Constant";
	//					}
						
						for (int i = 0; i < pulseValues.length; ++i) {
							if (pulseValues[i].equals(value)) {
								return pulseEntries[i];
							}
						}
						
						return null;
					}
					
					@Override
					public boolean onPreferenceChange(Preference preference, Object newValue) {
						updateTouchLEDButtonsOptions(Integer.valueOf(String.valueOf(newValue)), currentPulseKey);
						return true;
					}
				});
				pref.setSummary(changer.getSummary(sharedPreferences.getString(pref.getKey(), String.valueOf(Constants.DEFAULT_PULSE_MODE))));
				pref.setOnPreferenceChangeListener(changer);
				touchLEDCategory.addPreference(pref);
			}
	
			touchLEDCategory.addPreference(createEditDigitsPreference(key + "." + Constants.PREFERENCE_KEY_TOUCH_LED_FADE_IN_TIME, getString(R.string.preference_title_feedback_touch_led_fade_in_time), getString(R.string.dialog_title_feedback_touch_led_fade_in_time), String.valueOf(Constants.DEFAULT_PULSE_FADE_IN), getString(R.string.preference_summary_feedback_touch_led_time)));
			touchLEDCategory.addPreference(createEditDigitsPreference(key + "." + Constants.PREFERENCE_KEY_TOUCH_LED_FULLY_LIT_TIME, getString(R.string.preference_title_feedback_touch_led_fully_lit_time), getString(R.string.dialog_title_feedback_touch_led_fully_lit_time), String.valueOf(Constants.DEFAULT_PULSE_ACTIVE), getString(R.string.preference_summary_feedback_touch_led_time)));
			touchLEDCategory.addPreference(createEditDigitsPreference(key + "." + Constants.PREFERENCE_KEY_TOUCH_LED_FADE_OUT_TIME, getString(R.string.preference_title_feedback_touch_led_fade_out_time), getString(R.string.dialog_title_feedback_touch_led_fade_out_time), String.valueOf(Constants.DEFAULT_PULSE_FADE_OUT), getString(R.string.preference_summary_feedback_touch_led_time)));
			touchLEDCategory.addPreference(createEditDigitsPreference(key + "." + Constants.PREFERENCE_KEY_TOUCH_LED_INACTIVE_TIME, getString(R.string.preference_title_feedback_touch_led_inactive_time), getString(R.string.dialog_title_feedback_touch_led_inactive_time), String.valueOf(Constants.DEFAULT_PULSE_INACTIVE), getString(R.string.preference_summary_feedback_touch_led_time)));
    	
			SeekBarPreference seekBar = new SeekBarPreference(this);
			seekBar.setKey(key + "." + Constants.PREFERENCE_KEY_TOUCH_LED_BRIGHTNESS);
			seekBar.setTitle(R.string.preference_title_feedback_touch_led_brightness);
			seekBar.setDialogTitle(seekBar.getTitle());
			seekBar.setMax(touchLED.getMax());
			seekBar.setDefaultValue(Constants.DEFAULT_PULSE_MAX_LED_STRENGTH);
			seekBar.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				
				@Override
				public boolean onPreferenceClick(Preference preference) {
					touchLEDStrength = touchLED.getCurrent();
					touchLED.setAll(((SeekBarPreference)preference).getProgress());
					return true;
				}
			});
			seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
				
				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress,
						boolean fromUser) {
					if (fromUser) {
						touchLED.setAll(progress);
					}
				}
			});
			
			seekBar.setOnNoChangeListener(new OnNoChangeListener() {
				
				@Override
				public void onNoChange(Preference preference) {
					touchLED.setAll(touchLEDStrength);
				}
			});
			PreferenceSummaryChanger seekBarChanger = new PreferenceSummaryChanger(seekBar, getString(R.string.preference_summary_feedback_touch_led_brightness), new PreferenceSummaryChanger.OnSummaryChangeListener() {
				
				@Override
				public String onValueChange(Preference preference, String value) {
					try {
						float val = Float.valueOf(value);
						return String.valueOf((int)(100*val/touchLED.getMax()));
					} catch (NumberFormatException e) {}
					return null;
				}
				
				@Override
				public String onSummaryChange(Preference preference, String summary) {
					return null;
				}
	
				@Override
				public boolean onPreferenceChange(Preference preference,
						Object newValue) {
					touchLED.setAll(touchLEDStrength);
					return true;
				}
			});
			seekBar.setSummary(seekBarChanger.getSummary(String.valueOf(sharedPreferences.getInt(seekBar.getKey(), Constants.DEFAULT_TOUCH_LED_STRENGTH))));
			seekBar.setOnPreferenceChangeListener(seekBarChanger);
	
			touchLEDCategory.addPreference(seekBar);
    	}

		PreferenceCategory vibratorCategory = new PreferenceCategory(this);
		vibratorCategory.setTitle(R.string.preference_title_feedback_vibrator);
		screen.addPreference(vibratorCategory);
		{
			ListPreference pref = new ListPreference(this);
			pref.setKey(key + "." + Constants.PREFERENCE_KEY_VIBRATION_MODE);
			pref.setTitle(R.string.preference_title_feedback_vibrator_mode);
			pref.setDefaultValue(String.valueOf(Constants.DEFAULT_VIBRATE_TYPE));
			pref.setEntries(R.array.vibrate_type_entries);
			pref.setEntryValues(R.array.vibrate_type_values);
			PreferenceSummaryChanger changer = new PreferenceSummaryChanger(pref, "%s", new PreferenceSummaryChanger.OnSummaryChangeListener() {
				
				@Override
				public String onValueChange(Preference preference, String value) {
					return null;
				}
				
				@Override
				public String onSummaryChange(Preference preference, String value) {
					for (int i = 0; i < vibrateTypeValues.length; ++i) {
						if (vibrateTypeValues[i].equals(value)) {
							return vibrateTypeEntries[i];
						}
					}
					
					return null;
				}
				
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					return true;
				}
			});
			pref.setSummary(changer.getSummary(sharedPreferences.getString(pref.getKey(), String.valueOf(Constants.DEFAULT_VIBRATE_TYPE))));
			pref.setOnPreferenceChangeListener(changer);
			vibratorCategory.addPreference(pref);
		}

		{
			Preference pref = createEditDigitsPreference(key + "." + Constants.PREFERENCE_KEY_VIBRATION_DURATION, getString(R.string.preference_title_feedback_vibrator_duration), getString(R.string.dialog_title_feedback_vibrator_duration), String.valueOf(Constants.DEFAULT_VIBRATE_DURATION), getString(R.string.preference_summary_feedback_vibrator_duration));
			vibratorCategory.addPreference(pref);
		}
		{
			Preference pref = createEditDigitsPreference(key + "." + Constants.PREFERENCE_KEY_VIBRATION_DELAY, getString(R.string.preference_title_feedback_vibrator_delay), getString(R.string.dialog_title_feedback_vibrator_delay), String.valueOf(Constants.DEFAULT_VIBRATE_DELAY), getString(R.string.preference_summary_feedback_vibrator_delay));
			vibratorCategory.addPreference(pref);
		}

		PreferenceCategory ringtoneCategory = new PreferenceCategory(this);
		ringtoneCategory.setTitle(R.string.preference_title_feedback_notification_ringtone);
		screen.addPreference(ringtoneCategory);
		{
			ListPreference pref = new ListPreference(this);
			pref.setKey(key + "." + Constants.PREFERENCE_KEY_NOTIFICATION_RINGTONE_MODE);
			pref.setTitle(R.string.preference_title_feedback_notification_ringtone_mode);
			pref.setDefaultValue(String.valueOf(Constants.DEFAULT_NOTIFICATION_RINGTONE_TYPE));
			pref.setEntries(R.array.notification_ringtone_type_entries);
			pref.setEntryValues(R.array.notification_ringtone_type_values);
			PreferenceSummaryChanger changer = new PreferenceSummaryChanger(pref, "%s", new PreferenceSummaryChanger.OnSummaryChangeListener() {
				
				@Override
				public String onValueChange(Preference preference, String value) {
					return null;
				}
				
				@Override
				public String onSummaryChange(Preference preference, String value) {
					for (int i = 0; i < notificationRingtoneTypeValues.length; ++i) {
						if (notificationRingtoneTypeValues[i].equals(value)) {
							return notificationRingtoneTypeEntries[i];
						}
					}
					
					return null;
				}
				
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					return true;
				}
			});
			pref.setSummary(changer.getSummary(sharedPreferences.getString(pref.getKey(), String.valueOf(Constants.DEFAULT_NOTIFICATION_RINGTONE_TYPE))));
			pref.setOnPreferenceChangeListener(changer);
			ringtoneCategory.addPreference(pref);
		}

		{
			Preference pref = createEditDigitsPreference(key + "." + Constants.PREFERENCE_KEY_NOTIFICATION_RINGTONE_DELAY, getString(R.string.preference_title_feedback_notification_ringtone_delay), getString(R.string.dialog_title_feedback_notification_ringtone_delay), String.valueOf(Constants.DEFAULT_NOTIFICATION_RINGTONE_DELAY), getString(R.string.preference_summary_feedback_notification_ringtone_delay));
			ringtoneCategory.addPreference(pref);
		}
		{
			Preference pref = new Preference(this);
			pref.setKey(key + "." + Constants.PREFERENCE_KEY_NOTIFICATION_RINGTONE);
			pref.setTitle(R.string.preference_title_feedback_notification_ringtone_select);
			String uriString = sharedPreferences.getString(key + "." + Constants.PREFERENCE_KEY_NOTIFICATION_RINGTONE, null);
			String ringtoneName = getString(R.string.other_none);
			if (uriString != null) {
				Uri uri = Uri.parse(uriString);
				Ringtone rt = RingtoneManager.getRingtone(this, uri);
				if (rt != null) {
					ringtoneName = rt.getTitle(this);
				}
			}

			pref.setSummary(getString(R.string.preference_summary_feedback_notification_ringtone_select) + " " + ringtoneName);
			pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				
				@Override
				public boolean onPreferenceClick(Preference preference) {
					showNotificationRingtones();
					return true;
				}
			});
			ringtoneCategory.addPreference(pref);
		}

		PreferenceCategory otherCategory = new PreferenceCategory(this);
		otherCategory.setTitle(R.string.preference_title_feedback_other);
		screen.addPreference(otherCategory);
		{
			Preference pref = createEditDigitsPreference(key + "." + Constants.PREFERENCE_KEY_PRIORITY, getString(R.string.preference_title_feedback_other_priority), getString(R.string.dialog_title_feedback_other_priority), String.valueOf(Constants.DEFAULT_FEEDBACK_PRIORITY), getString(R.string.preference_summary_feedback_other_priority));
			otherCategory.addPreference(pref);
		}
		{
			Preference pref = new CheckBoxPreference(this);
			pref.setKey(key + "." + Constants.PREFERENCE_KEY_ONLY_LIGHT_NOTIFICATION);
			pref.setTitle(getString(R.string.preference_title_feedback_other_only_light_notification));
			pref.setSummary(getString(R.string.preference_summary_feedback_other_only_light_notification));
			pref.setDefaultValue(Constants.DEFAULT_PREFERENCE_KEY_ONLY_LIGHT_NOTIFICATION);
			otherCategory.addPreference(pref);
		}
	}
	
	private Preference createEditDigitsPreference(String key, String title, String dialogMessage, String defaultValue, String summary) {
		return createEditDigitsPreference(key, title, dialogMessage, defaultValue, summary, false);
	}
	
	private Preference createEditDigitsPreference(String key, String title, String dialogMessage, String defaultValue, String summary, boolean decimal) {
		EditTextPreference pref = new EditTextPreference(this);
		pref.setKey(key);
		pref.setTitle(title);
		pref.getEditText().setKeyListener(DigitsKeyListener.getInstance(false, decimal));
		pref.setDefaultValue(defaultValue);
		pref.setDialogTitle(pref.getTitle());
		pref.setDialogMessage(dialogMessage);
		if (summary != null) {
			PreferenceSummaryChanger changer = new PreferenceSummaryChanger(pref, summary, null);
			pref.setSummary(changer.getSummary(sharedPreferences.getString(key, defaultValue)));
			pref.setOnPreferenceChangeListener(changer);
		}
		
		return pref;
	}
	
	private void createAccelerometerOptions(PreferenceScreen screen) {
		screen.removeAll();
		
		CheckBoxPreference checkBox = new CheckBoxPreference(this);
		checkBox.setKey("checkBoxAccelerometerKeyGuardEnabled");
		checkBox.setTitle(R.string.preference_title_miscellaneous_accelerometer_enable_in_lockscreen);
		checkBox.setDefaultValue(Constants.DEFAULT_USE_ACCELEROMETER_IN_KEYGUARD);
		checkBox.setSummaryOn(R.string.preference_summaryon_miscellaneous_accelerometer_enable_in_lockscreen);
		checkBox.setSummaryOff(R.string.preference_summaryoff_miscellaneous_accelerometer_enable_in_lockscreen);
		screen.addPreference(checkBox);
		
		
		AccelerometerTolerancePreference pref = new AccelerometerTolerancePreference(this);
		pref.setKey("accelerometerTolerancePref");
		pref.setTitle(R.string.preference_title_miscellaneous_accelerometer_tolerance);
		pref.setDialogTitle(pref.getTitle());
		pref.setDefaultValue(2.5f);
		
		PreferenceSummaryChanger changer = new PreferenceSummaryChanger(pref, getString(R.string.preference_summary_miscellaneous_accelerometer_tolerance), null);
		pref.setSummary(changer.getSummary(String.valueOf(sharedPreferences.getFloat("accelerometerTolerancePref", Constants.DEFAULT_ACCELEROMETER_TOLERANCE))));
		pref.setOnPreferenceChangeListener(changer);
		
		screen.addPreference(pref);
//		pref.setDependency("checkBoxAccelerometerEnabled");
	}
	
	private void createBatterySavingOptions(PreferenceScreen screen) {
		SeekBarPreference seekBar = new SeekBarPreference(this);
		seekBar.setKey("seekBarDisablePulseOnLowBatteryPercent");
		seekBar.setTitle(R.string.preference_title_miscellaneous_battery_saving_disable_feedback_value);
		seekBar.setDialogTitle(seekBar.getTitle());
		seekBar.setMax(100);
		seekBar.setDefaultValue(Constants.DEFAULT_LOW_BATTERY_DISABLE_PERCENT);
		PreferenceSummaryChanger seekBarChanger = new PreferenceSummaryChanger(seekBar, getString(R.string.preference_summary_miscellaneous_battery_saving_disable_feedback_value), new PreferenceSummaryChanger.OnSummaryChangeListener() {
			
			@Override
			public String onValueChange(Preference preference, String value) {
				return null;
			}
			
			@Override
			public String onSummaryChange(Preference preference, String summary) {
				return null;
			}

			@Override
			public boolean onPreferenceChange(Preference preference,
					Object newValue) {
				return true;
			}
		});
		seekBar.setSummary(seekBarChanger.getSummary(String.valueOf(sharedPreferences.getInt(seekBar.getKey(), Constants.DEFAULT_LOW_BATTERY_DISABLE_PERCENT))));
		seekBar.setOnPreferenceChangeListener(seekBarChanger);

		screen.addPreference(seekBar);
		seekBar.setDependency("checkBoxDisablePulseOnLowBattery");
	}

	
	private void createTouchLEDOptions(PreferenceScreen screen) {
		screen.removeAll();
		
		if (touchLED instanceof TouchLEDP970) {
			Preference preference = new Preference(this);
			preference.setTitle(R.string.preference_title_miscellaneous_touch_led_not_available);
			preference.setSummary(R.string.preference_summary_miscellaneous_touch_led_not_available);
			screen.addPreference(preference);
		} else {
			{
				CheckBoxPreference checkBox = new CheckBoxPreference(this);
				checkBox.setKey("checkBoxTouchLEDStrengthSetOnBootPref");
				checkBox.setTitle(R.string.preference_title_miscellaneous_touch_led_set_on_boot);
				checkBox.setDefaultValue(Constants.DEFAULT_SET_TOUCH_LED_STRENGTH_ON_BOOT);
				checkBox.setSummaryOn(R.string.preference_summaryon_miscellaneous_touch_led_set_on_boot);
				checkBox.setSummaryOff(R.string.preference_summaryoff_miscellaneous_touch_led_set_on_boot);
				screen.addPreference(checkBox);
			}
			
			SeekBarPreference seekBar = new SeekBarPreference(this);
			seekBar.setKey("seekBarTouchLEDStrengthPref");
			seekBar.setTitle(R.string.preference_title_miscellaneous_touch_led_brightness);
			seekBar.setSummary(R.string.preference_summary_miscellaneous_touch_led_brightness);
			seekBar.setDialogTitle(seekBar.getTitle());
			seekBar.setMax(touchLED.getMax());
			seekBar.setDefaultValue(touchLED.getCurrent());
			seekBar.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				
				@Override
				public boolean onPreferenceClick(Preference preference) {
					touchLEDStrength = touchLED.getCurrent();
					return true;
				}
			});
			seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
				
				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {
				}
				
				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {
				}
				
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress,
						boolean fromUser) {
					if (fromUser) {
						touchLED.setAll(progress);
					}				
				}
			});
			seekBar.setOnNoChangeListener(new OnNoChangeListener() {
				
				@Override
				public void onNoChange(Preference preference) {
					touchLED.setAll(touchLEDStrength);
				}
			});
			screen.addPreference(seekBar);
		}
		/*
		{
			CheckBoxPreference checkBox = new CheckBoxPreference(this);
			checkBox.setKey("checkBoxAutoLEDBrightnessPref");
			checkBox.setTitle("Automatic brightness");
			checkBox.setDefaultValue(Constants.DEFAULT_AUTO_BRIGHTNESS);
			checkBox.setSummary("EXPERIMENTAL: Automatically set the brightness of the LED buttons with the screen brightness");
			checkBox.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				
				@Override
				public boolean onPreferenceClick(Preference preference) {
					CheckBoxPreference cb = (CheckBoxPreference)preference;
					if (cb.isChecked()) {
						int isAuto = Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
						try {
							isAuto = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE);
						} catch (SettingNotFoundException e) {
							e.printStackTrace();
						}
						if (isAuto == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
							Logger.logDebug("Screen brightness is automatic");
						} else {
							AlertDialog.Builder builder = new AlertDialog.Builder(MainPreferences.this);
							builder.setMessage("Automatic screen brightness must be enabled. Do you want to do that now ?")
							       .setCancelable(false)
							       .setPositiveButton("Yes", new OnClickListener() {
									
									@Override
									public void onClick(DialogInterface dialog, int which) {
										startActivityForResult(new Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS), 0);
									}
								})
							       .setNegativeButton("No", new OnClickListener() {
							           public void onClick(DialogInterface dialog, int id) {
							                dialog.cancel();
							           }
							       });
							builder.show();
							((CheckBoxPreference)preference).setChecked(false);
						}
					} else {
						try {
							int value = sharedPreferences.getInt("seekBarTouchLEDStrengthPref", Constants.DEFAULT_TOUCH_LED_STRENGTH);
							touchLED.setAll(value);
						} catch (IOException e) {
							e.printStackTrace();
						}

					}
					
					return true;
				}
			});
			screen.addPreference(checkBox);
		}
		*/

	}
	
	private boolean enabledInAccessibilitySettings() {
//		AccessibilityManager am = (AccessibilityManager)getSystemService(ACCESSIBILITY_SERVICE);
//		if (am.isEnabled()) {
			String packageName = getPackageName();
			String settingValue = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
			if (packageName != null && settingValue != null && settingValue.contains(packageName)) {
				return true;
			}
//		}

		return false;
	}
	
	/**
	 * If the app is not enabled in the accessibility settings, popup an alert and return false, otherwise show nothing and return true
	 * @return
	 */
	private boolean askIfNotEnabledInAccessibilitySettings() {
		return askIfNotEnabledInAccessibilitySettings(R.string.dialog_message_enable_accessibility);
	}
	
	/**
	 * If the app is not enabled in the accessibility settings, popup an alert and return false, otherwise show nothing and return true
	 * @return
	 */
	private boolean askIfNotEnabledInAccessibilitySettings(int message) {
		if (!enabledInAccessibilitySettings()) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(message)
			       .setCancelable(false)
			       .setPositiveButton(android.R.string.yes, new OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						startActivityForResult(new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS), 0);
					}
				})
			       .setNegativeButton(android.R.string.no, new OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			                dialog.cancel();
			           }
			       });
			Dialog enableAccessibilityDialog = builder.create();
			enableAccessibilityDialog.show();
			return false;
		}
		return true;
	}
	
    private void showActivities(String file) {
    	if (!askIfNotEnabledInAccessibilitySettings()) {
    		return;
    	}
//		activities = new ListFileManager(this, file);
//		activities.load();
    	activities = new SerializableArrayList<String>(this, file);
    	try {
			activities.unserialize();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		packageLoaderDialog = new ProgressDialog(this);
		packageLoaderDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		packageLoaderDialog.setTitle(R.string.dialog_title_loading_activities);
		packageLoaderDialog.setCancelable(true);
		packageLoaderDialog.setOnCancelListener(new OnCancelListener() {
			
			@Override
			public void onCancel(DialogInterface dialog) {
				if (packageLabelLoader != null) {
					packageLabelLoader.cancel();
					packageLabelLoader = null;
				}
			}
		});
		packageLoaderDialog.setOnDismissListener(new OnDismissListener() {
			
			@Override
			public void onDismiss(DialogInterface dialog) {
				if (packageLabelLoader != null) {
					PackageData[] info = packageLabelLoader.packageInfo;
					CharSequence[] items = new CharSequence[info.length];
					boolean[] checked = new boolean[info.length];
					for (int i = 0; i < info.length; ++i) {
						if (info[i] != null) {
							items[i] = info[i].label;
							if (activities != null) {
								checked[i] = activities.contains(info[i].packageName);
							} else {
								checked[i] = false;
							}
						}
					}
					AlertDialog.Builder d = new AlertDialog.Builder(MainPreferences.this);
					d.setTitle(R.string.dialog_title_select_activities);
					d.setMultiChoiceItems(items, checked, new OnMultiChoiceClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which, boolean isChecked) {
							Logger.logDebug(packageLabelLoader.packageInfo[which].packageName + " (" + packageLabelLoader.packageInfo[which].label + "): " + isChecked);
							String name = packageLabelLoader.packageInfo[which].packageName;
							if (isChecked) {
								activities.add(name);
						    	Toast toast = Toast.makeText(MainPreferences.this, getString(R.string.toast_added_activity) + " " + packageLabelLoader.packageInfo[which].label + " (" + name + ")", Toast.LENGTH_SHORT);
						    	toast.show();
							} else {
								activities.remove(name);
							}
							
				    		try {
								activities.serialize();
							} catch (IOException e) {
								e.printStackTrace();
							}
					    	
					    	if (sharedPreferences.getBoolean("checkBoxServiceEnabled", Constants.DEFAULT_SERVICE_ENABLED)) {
					    		MainService.readNotificationActivitiesSettings(MainPreferences.this);
				    		}

						}
					});
					d.setOnCancelListener(new OnCancelListener() {
						
						@Override
						public void onCancel(DialogInterface dialog) {
							createCustomizeNotifications((PreferenceScreen)findPreference("preferenceNotificationsCustomizeNotifications"));							
						}
					});
					Dialog activitiesDialog = d.create();
					activitiesDialog.show();
				}
				
			}
		});
		packageLabelLoader = new PackageLabelLoader(packageLoaderDialog, getPackageManager());
		packageLabelLoader.start();
		packageLoaderDialog.show();
    }

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    	if (sharedPreferences.getBoolean("checkBoxServiceEnabled", Constants.DEFAULT_SERVICE_ENABLED)) {
    		readSettings();
    	}
	}
	
	private void readSettings() {
		Intent i = new Intent(this, MainService.class);
		i.setAction(MainService.ACTION_READ_SETTINGS);
		startService(i);
	}

	private void createCustomizeNotifications(PreferenceScreen screen) {
		Logger.logDebug("Create customize notifications");
		screen.removeAll();
		
		// add the normal ones
		{
			CheckBoxPreference cb = (CheckBoxPreference)findPreference("checkBoxNotificationsGmailPref");
			if (cb.isChecked()) {
				createCustomizablePreference(screen, GmailContentObserver.ID, cb.getTitle().toString());
			}
		}
		{
			CheckBoxPreference cb = (CheckBoxPreference)findPreference("checkBoxNotificationsSMSMMSPref");
			if (cb.isChecked()) {
				createCustomizablePreference(screen, SMSMMSReceiver.ID, cb.getTitle().toString());
			}
		}
		{
			CheckBoxPreference cb = (CheckBoxPreference)findPreference("checkBoxNotificationsMissedCallsPref");
			if (cb.isChecked()) {
				createCustomizablePreference(screen, CallListener.ID, cb.getTitle().toString());
			}
		}
		
		SerializableArrayList<String> activities = new SerializableArrayList<String>(this, AccessibilityService.MONITORED_ACTIVITIES_FILE);
		try {
			activities.unserialize();
		} catch (IOException e) {
			e.printStackTrace();
		}
		for (String s : activities) {
			PackageManager pm = getPackageManager();
			try {
				ApplicationInfo ai = pm.getApplicationInfo(s, 0);
				createCustomizablePreference(screen, s, String.valueOf(ai.loadLabel(pm)));
			} catch (NameNotFoundException e) {
				e.printStackTrace();
			}
		}

	}
	
	private class CustomizablePreference implements OnPreferenceClickListener {
		private final String pulseKey;
		private final String title;
		private final PreferenceScreen screen;
		private final PreferenceScreen root;
		private final PreferenceScreen feedbackScreen;
		
		public CustomizablePreference(PreferenceScreen root, String pulseKey, String title) {
			this.root = root;
			this.title = title;
			this.pulseKey = pulseKey;
			screen = getPreferenceManager().createPreferenceScreen(MainPreferences.this);
			screen.setOnPreferenceClickListener(this);
			screen.setPersistent(false);
			screen.setTitle(title);
			updateSummary(sharedPreferences.getBoolean(pulseKey + "." + Constants.PREFERENCE_KEY_CUSTOMIZED_PULSE, Constants.DEFAULT_CUSTOMIZED_PULSE));
			
			{
				CheckBoxPreference cb = new CheckBoxPreference(MainPreferences.this);
				cb.setKey(pulseKey + "." + Constants.PREFERENCE_KEY_CUSTOMIZED_PULSE);
				cb.setDefaultValue(Constants.DEFAULT_CUSTOMIZED_PULSE);
				cb.setTitle(R.string.preference_title_customized_feedback);
				cb.setSummaryOn(R.string.preference_summaryon_customized_feedback);
				cb.setSummaryOff(R.string.preference_summaryoff_customized_feedback);
				cb.setOnPreferenceClickListener(new OnPreferenceClickListener() {
					
					@Override
					public boolean onPreferenceClick(Preference preference) {
						boolean checked = ((CheckBoxPreference)preference).isChecked();
						updateSummary(checked);
						if (checked) {
							createFeedbackOptions(feedbackScreen, CustomizablePreference.this.pulseKey);
							updateTouchLEDButtonsOptions(MainService.toInt(sharedPreferences.getString(CustomizablePreference.this.pulseKey + "." + Constants.PREFERENCE_KEY_TOUCH_LED_MODE, String.valueOf(Constants.DEFAULT_PULSE_MODE)), Constants.DEFAULT_PULSE_MODE), CustomizablePreference.this.pulseKey);
						}
						((BaseAdapter)CustomizablePreference.this.root.getRootAdapter()).notifyDataSetChanged();
						
						return true;
					}
				});
				screen.addPreference(cb);
			}
			feedbackScreen = getPreferenceManager().createPreferenceScreen(MainPreferences.this);
			feedbackScreen.setPersistent(false);
			feedbackScreen.setTitle(R.string.preference_title_customized_feedback_options);
			feedbackScreen.setSummary(R.string.preference_summary_customized_feedback_options);
			screen.addPreference(feedbackScreen);
			
			root.addPreference(screen);
		}
		
		private void updateSummary(boolean enabled) {
			screen.setSummary((String)(enabled ? getString(R.string.preference_summaryon_customized_feedback) : getString(R.string.preference_summaryoff_customized_feedback)));
		}

		@Override
		public boolean onPreferenceClick(Preference preference) {
			feedbackScreen.setDependency(pulseKey + "." + Constants.PREFERENCE_KEY_CUSTOMIZED_PULSE);
			if (sharedPreferences.getBoolean(pulseKey + "." + Constants.PREFERENCE_KEY_CUSTOMIZED_PULSE, Constants.DEFAULT_CUSTOMIZED_PULSE)) {
				createFeedbackOptions(feedbackScreen, pulseKey);
				updateTouchLEDButtonsOptions(MainService.toInt(sharedPreferences.getString(pulseKey + "." + Constants.PREFERENCE_KEY_TOUCH_LED_MODE, String.valueOf(Constants.DEFAULT_PULSE_MODE)), Constants.DEFAULT_PULSE_MODE), pulseKey);
			}
			currentPulseKey = pulseKey;
			return true;
		}
	}
	
	private void createCustomizablePreference(PreferenceScreen screen, String pulseKey, String title) {
		new CustomizablePreference(screen, pulseKey, title);
	}
	
	private void setNewDefaults(Editor editor, int oldVersionCode) {
		if (oldVersionCode < 33) {
			editor.putInt("seekBarDisablePulseOnLowBatteryPercent", Constants.DEFAULT_LOW_BATTERY_DISABLE_PERCENT);
		}
		if (oldVersionCode < 37) {
			editor.putInt("seekBarPulseDelayPrefs", Constants.DEFAULT_PULSE_DELAY);
		}
	}

	private void showChangelogDialog() {
		// load changelog
		AssetManager am = getAssets();
		String changelog = null;
		try {
			InputStream is = am.open("changelog");
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			changelog = "";
			String s = null;
			while ((s = br.readLine()) != null) {
				changelog += s + "\n";
			}
			is.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (changelog == null) {
			changelog = "Unable to read changelog";
		}
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.dialog_title_changelog);

		builder.setMessage(changelog);
		builder.setPositiveButton(android.R.string.ok, null);
		
		builder.show();
	}
	
	private void showFirstTimeDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("First-time dialog");
		builder.setMessage(
				"..\n\n" +
				""
				);
		builder.setPositiveButton(android.R.string.ok, null);
		
		builder.show();
	}
	
	private void showNotificationRingtones() {
		Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
		intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
		intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, R.string.preference_title_feedback_notification_ringtone_select);
		String uriString = sharedPreferences.getString(currentPulseKey + "." + Constants.PREFERENCE_KEY_NOTIFICATION_RINGTONE, null);
		Uri uri = null;
		if (uriString != null) {
			uri = Uri.parse(uriString);
		}
		intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, uri);
		startActivityForResult(intent, 100);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 100 && resultCode == Activity.RESULT_OK) {
			Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
			String uriString = null;
			if (uri != null) {
				uriString = uri.toString();
			}
			Logger.logDebug("Selected notification ringtone " + uriString + " to " + currentPulseKey);
			Editor edit = sharedPreferences.edit();
			edit.putString(currentPulseKey + "." + Constants.PREFERENCE_KEY_NOTIFICATION_RINGTONE, uriString);
			edit.commit();

			String ringtone = getString(R.string.other_none);
			if (uri != null) {
				Ringtone rt = RingtoneManager.getRingtone(this, uri);
				if (rt != null) {
					ringtone = rt.getTitle(this);
				}
			}
			Preference pref = findPreference(currentPulseKey + "." + Constants.PREFERENCE_KEY_NOTIFICATION_RINGTONE);
			if (pref != null) {
				pref.setSummary(getString(R.string.preference_summary_feedback_notification_ringtone_select) + " " + ringtone);
			}
		}
	}

}
