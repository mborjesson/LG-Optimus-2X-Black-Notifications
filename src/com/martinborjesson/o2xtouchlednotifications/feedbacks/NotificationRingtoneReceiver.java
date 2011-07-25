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

import java.io.*;

import android.content.*;
import android.media.*;
import android.net.*;
import android.os.*;
import android.preference.*;

import com.martinborjesson.o2xtouchlednotifications.*;
import com.martinborjesson.o2xtouchlednotifications.services.*;
import com.martinborjesson.o2xtouchlednotifications.utils.*;

public class NotificationRingtoneReceiver extends BroadcastReceiver {

	static final public String ACTION_START_NOTIFICATION_RINGTONE = NotificationRingtoneReceiver.class.getName() + ".ACTION_START_AUDIO";
	
    static private SharedPreferences prefs = null;
    
    static private MediaPlayer mediaPlayer = null;
    static private int audioDelay = 0;
    static private float audioSlowerOverTime = Constants.DEFAULT_VIBRATE_SLOWER_OVER_TIME;
    
    static private final int TYPE_ONCE = 0;
    static private final int TYPE_CONSTANT = 1;
    static private final int TYPE_SLOWER_OVER_TIME = 2;
    
    static private long numPlays = 0;
    static private int audioType = TYPE_SLOWER_OVER_TIME;

    static public void stop() {
    	if (mediaPlayer != null) {
    		mediaPlayer.stop();
    	}
    	reset();
    }
    
    static public void reset() {
    	if (mediaPlayer != null) {
    		mediaPlayer.release();
    	}
    	mediaPlayer = null;
    	prefs = null;
    	numPlays = 0;
    }

    static public void init(Context context, String pulseKey) {
    	if (prefs == null) {
    		Logger.logDebug("Loading audio properties...");
    		prefs = PreferenceManager.getDefaultSharedPreferences(context);

			audioDelay = MainService.toInt(prefs.getString(pulseKey + "." + Constants.PREFERENCE_KEY_NOTIFICATION_RINGTONE_DELAY, String.valueOf(Constants.DEFAULT_NOTIFICATION_RINGTONE_DELAY)), Constants.DEFAULT_NOTIFICATION_RINGTONE_DELAY);
			audioType = MainService.toInt(prefs.getString(pulseKey + "." + Constants.PREFERENCE_KEY_NOTIFICATION_RINGTONE_MODE, String.valueOf(Constants.DEFAULT_NOTIFICATION_RINGTONE_TYPE)), Constants.DEFAULT_NOTIFICATION_RINGTONE_TYPE);
			String uriString = prefs.getString(pulseKey + "." + Constants.PREFERENCE_KEY_NOTIFICATION_RINGTONE, Constants.DEFAULT_NOTIFICATION_RINGTONE);
			mediaPlayer = new MediaPlayer();
			//Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
			Uri uri = null;
			if (uriString != null) {
				uri = Uri.parse(uriString);
			}
			if (uri != null) {
				AudioManager audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
				if (audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION) != 0) {
		            mediaPlayer.setAudioStreamType(AudioManager.STREAM_NOTIFICATION);
		            try {
			            mediaPlayer.setDataSource(context, uri);
			            mediaPlayer.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK);
						mediaPlayer.prepare();
					} catch (IllegalStateException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
    	}
    }

	@Override
	public void onReceive(Context context, Intent intent) {
		if (mediaPlayer == null) {
			return;
		}
		if (mediaPlayer.isPlaying()) {
			return;
		}
		mediaPlayer.start();
		
		// register new alarm
		int delay = 0;
		if (audioType == TYPE_CONSTANT) {
    		delay = audioDelay;
    	} else if (audioType == TYPE_SLOWER_OVER_TIME) {
    		delay = (int)Math.round(Math.max(audioDelay, Math.pow(numPlays, audioSlowerOverTime)*1000));
    	}
		Logger.logDebug("Audio delay: " + delay);
    	if (delay > 0) {
    		MainService.startAlarm(context, NotificationRingtoneReceiver.ACTION_START_NOTIFICATION_RINGTONE, NotificationRingtoneReceiver.class, mediaPlayer.getDuration()+delay, 0, MainService.ALARM_TYPE_BROADCAST, false);
    	}
		numPlays++;
	}

}
