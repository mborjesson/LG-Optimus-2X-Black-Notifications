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

package com.martinborjesson.o2xtouchlednotifications.touchled.devices;

import java.io.*;

import com.martinborjesson.o2xtouchlednotifications.touchled.*;
import com.martinborjesson.o2xtouchlednotifications.utils.*;

public class TouchLEDP970 extends TouchLED {
	static public final File WLED_PATH = new File("/sys/bus/i2c/devices/2-001a/");
	static public final File BUTTON1 = new File(WLED_PATH.toString() + "/" + "0x03"); // search
	static public final File BUTTON2 = new File(WLED_PATH.toString() + "/" + "0x06"); // menu
	static public final File BUTTON3 = new File(WLED_PATH.toString() + "/" + "0x10"); // back
	static public final File BUTTON4 = new File(WLED_PATH.toString() + "/" + "0x0D"); // home
	static public final File ONOFF = new File(WLED_PATH.toString() + "/" + "led_onoff");
	static public final int MIN = 0;
	static public final int MAX = 50;
	static public final int DEFAULT_VALUE = MAX;

	static public boolean isAvailable() {
		return WLED_PATH.exists() && WLED_PATH.isDirectory();
	}
	
	public boolean hasProperPermissions() {
		return 
				BUTTON1.canRead() && BUTTON1.canWrite() &&
				BUTTON2.canRead() && BUTTON2.canWrite() &&
				BUTTON3.canRead() && BUTTON3.canWrite() &&
				BUTTON4.canRead() && BUTTON4.canWrite() &&
				ONOFF.canRead() && ONOFF.canWrite();
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
	    	InputStream is = new FileInputStream(BUTTON1);
	    	int read = -1;
	    	byte[] buf = new byte[128];
	    	int p = 0;
	    	while ((read = is.read()) != -1) {
	    		buf[p++] = (byte)read;
	    	}
	    	is.close();
	    	String valueStr = new String(buf, 0, p);
	    	Logger.logDebug("Read Touch LED value: " + valueStr);
	    	
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
		if (button == SEARCH) {
			set(BUTTON1, value);
		} else if (button == MENU) {
			set(BUTTON2, value);
		} else if (button == BACK) {
			set(BUTTON3, value);
		} else if (button == HOME) {
			set(BUTTON4, value);
		}
	}

	@Override
	public void setAll(int value) {
		set(BUTTON1, value);
		set(BUTTON2, value);
		set(BUTTON3, value);
		set(BUTTON4, value);
	}

	private void set(File file, int value) {
		try {
			String valueStr = String.valueOf(Math.min(MAX, Math.max(value, MIN)));
			
			OutputStream os = new FileOutputStream(file);
			os.write(valueStr.getBytes());
			os.close();
		} catch (IOException e) {
			
		}
	}
	
	public void setOnOffLED(boolean on) {
		set(ONOFF, on ? 1 : 0);
		if (on) {
			setAll(0);
		}
	}

	@Override
	public String getDeviceName() {
		return "P970";
	}

	@Override
	public boolean canChangeLEDBrightness() {
		return false;
	}

	@Override
	public File getFile() {
		return null;
	}
}
