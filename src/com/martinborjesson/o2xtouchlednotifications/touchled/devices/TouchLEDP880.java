/**
   Copyright 2011 Martin Börjesson
				  Thomas Piérard

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

package com.martinborjesson.o2xtouchlednotifications.touchled.devices;

import java.io.*;

import com.martinborjesson.o2xtouchlednotifications.touchled.*;
import com.martinborjesson.o2xtouchlednotifications.utils.*;


public class TouchLEDP880 extends TouchLED {
	static public final File WLED_FILE = new File("/sys/devices/platform/button-backlight/leds/button-backlight/brightness");
	static public final int MIN = 0;
	static public final int MAX = 255;
	static public final int DEFAULT_VALUE = MAX;

	static public boolean isAvailable() {
		return WLED_FILE.exists() && WLED_FILE.isFile();
	}
	
	public boolean hasProperPermissions() {
		return WLED_FILE.canRead() && WLED_FILE.canWrite();
	}
	
	@Override
	public int getDefault() {
		return DEFAULT_VALUE;
	}
	
	@Override
	public int getMax() {
		return MAX;
	}

	@Override
	public int getMin() {
		return MIN;
	}

	@Override
	public int getCurrent() {
    	int value = DEFAULT_VALUE;
		try {
	    	InputStream is = new FileInputStream(WLED_FILE);
	    	int read = -1;
	    	byte[] buf = new byte[128];
	    	int p = 0;
	    	while ((read = is.read()) != -1) {
	    		buf[p++] = (byte)read;
	    	}
	    	is.close();
	    	String valueStr = new String(buf, 0, p);
	    	Logger.logDebug("Read Touch LED value: " + valueStr);
	    	valueStr = valueStr.substring(0, valueStr.lastIndexOf("\n"));
	    	
	    	try {
	        	value = Integer.valueOf(valueStr);
	        	Logger.logDebug("Read Touch LED value (int): " + value);
	    	} catch (NumberFormatException e) {
	    	}
		} catch (IOException e) {
		}
    	return value;
	}

	@Override
	public void set(int button, int value) {
		// no support for separate buttons
		try {
			String valueStr = String.valueOf(Math.min(MAX, Math.max(value, MIN)));
			
			OutputStream os = new FileOutputStream(WLED_FILE);
			os.write(valueStr.getBytes());
			os.close();
		} catch (IOException e) {
			
		}
	}

	@Override
	public void setAll(int value) {
		set(0, value);
	}

	@Override
	public String getDeviceName() {
		return "LG P880";
	}

	@Override
	public boolean canChangeLEDBrightness() {
		return false;
	}

	@Override
	public File[] getFiles() {
		return new File[] { WLED_FILE };
	}
	
}
