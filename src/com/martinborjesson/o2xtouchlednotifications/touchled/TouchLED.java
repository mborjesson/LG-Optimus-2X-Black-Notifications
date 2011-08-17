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

package com.martinborjesson.o2xtouchlednotifications.touchled;

import java.io.*;

import com.martinborjesson.o2xtouchlednotifications.touchled.devices.*;


abstract public class TouchLED {
	public final static int SEARCH = 0;
	public final static int HOME = 1;
	public final static int BACK = 2;
	public final static int MENU = 3;
	
	private boolean usable = true;
	
	/**
	 * Return the maximum brightness the LED can have
	 * @return
	 */
	abstract public int getMax();
	
	/**
	 * Return the minimum brightness the LED can have
	 * @return
	 */
	abstract public int getMin();
	
	/**
	 * Return the current brightness of the LED
	 * @return
	 */
	abstract public int getCurrent();
	
	/**
	 * Set the brightness of the specified LED button<br/>
	 * Only P970 cares about this
	 * @param button
	 * @param value
	 */
	abstract public void set(int button, int value);
	
	/**
	 * Sets the brightness of all LED buttons
	 * @param value
	 */
	abstract public void setAll(int value);
	
	/**
	 * Returns <code>true</code> if the LED brightness can be changed permanently<br/>
	 * Currently only SU660/P990/P999 supports this
	 * @return
	 */
	abstract public boolean canChangeLEDBrightness();
	
	/**
	 * Name of the device
	 * @return
	 */
	abstract public String getDeviceName();
	
	/**
	 * Returns <code>true</code> if the required file has proper permissions 
	 * @return
	 */
	abstract public boolean hasProperPermissions();
	
	/**
	 * Return the required file for this device<br/>
	 * Only valid for SU660/P990/P999
	 * @return
	 */
	abstract public File getFile();
	
	/**
	 * Returns <code>true</code> if this device is a valid device<br/>
	 * More convenient than <code>obj instanceof TouchLEDNone</code>
	 * @return
	 */
	public boolean isValid() {
		return true;
	}
	
	/**
	 * Returns <code>true</code> if the LEDs on this device is usable<br/>
	 * @return
	 */
	public boolean isUsable() {
		return usable;
	}
	
	static private TouchLED touchLED = null;
	
	/**
	 * Return a <code>TouchLED</code> object for this device. The returned object will never be <code>null</code>.
	 * @return
	 */
	static public TouchLED getTouchLED() {
		if (touchLED == null) {
			if (TouchLEDP990.isAvailable()) { // P990 and P999 works the same
				touchLED = new TouchLEDP990();
			} else if (TouchLEDP970.isAvailable()) {
				touchLED = new TouchLEDP970();
			} else if (TouchLEDP920.isAvailable()) {
				touchLED = new TouchLEDP920();
			} else {
				touchLED = new TouchLEDNone();
			}
		}
		touchLED.usable = touchLED.isValid() && touchLED.hasProperPermissions();
		return touchLED;
	}
	
	static public void reset() {
		touchLED = null;
	}
}
