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

/**
 * Dummy
 * @author Martin Borjesson
 *
 */
public class TouchLEDNone extends TouchLED {

	@Override
	public int getMax() {
		return 0;
	}

	@Override
	public int getMin() {
		return 0;
	}

	@Override
	public int getCurrent() {
		return 0;
	}

	@Override
	public void set(int button, int value) {
		
	}

	@Override
	public void setAll(int value) {
	}

	@Override
	public String getDeviceName() {
		return "None";
	}

	@Override
	public boolean canChangeLEDBrightness() {
		return false;
	}

	@Override
	public boolean isValid() {
		return false;
	}

	@Override
	public boolean hasProperPermissions() {
		return false;
	}

	@Override
	public File getFile() {
		return null;
	}
}
