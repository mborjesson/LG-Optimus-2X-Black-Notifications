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

package com.martinborjesson.o2xtouchlednotifications.utils;

import java.io.*;
import java.util.*;

import android.os.*;
import android.util.*;

public class Logger {
	static public final String LOGTAG = "O2XTouchLEDNotifications";
	static public final File LOG_FILE = new File(Environment.getExternalStorageDirectory() + "/o2xtouchlednot.log");
	
	static private FileWriter fw;
	
	static private boolean enabled = false;
	
	static public void setEnabled(boolean enabled) {
		Logger.enabled = enabled;
	}
	
	static public boolean isEnabled() {
		return Logger.enabled;
	}

	static public void startLogToFile() {
		if (fw != null) {
			stopLogToFile();
		}
		try {
			Log.d(LOGTAG, "Opening log " + LOG_FILE + " for writing");
			fw = new FileWriter(LOG_FILE, true);
			logFile("Starting new log", "start");
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	static public void stopLogToFile() {
		if (fw != null) {
			logFile("Log ended", "end");
			try {
				fw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			fw = null;
		}
	}
	
	static public void clearLogFile() {
		boolean open = fw != null;
		stopLogToFile();
		FileWriter fw = null;
		try {
			fw = new FileWriter(LOG_FILE, false);
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (open) {
			startLogToFile();
		}
	}
	
	static public void logDebug(String str) {
		if (!enabled) {
			return;
		}
		logFile(str, "debug");
		Log.d(LOGTAG, str);
	}
	
	static private void logFile(String str, String type) {
		if (fw != null) {
			try {
				long millis = System.currentTimeMillis();
				Calendar c = Calendar.getInstance();
				c.setTimeInMillis(millis);
				
				fw.write((c.getTime().toString() + " :: " + type + " :: " + str + "\n"));
				fw.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
