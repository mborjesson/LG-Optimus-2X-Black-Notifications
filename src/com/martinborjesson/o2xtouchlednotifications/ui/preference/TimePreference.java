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

package com.martinborjesson.o2xtouchlednotifications.ui.preference;

import android.content.*;
import android.preference.*;
import android.text.format.*;
import android.view.*;
import android.widget.*;

public class TimePreference extends DialogPreference {
	
	private TimePicker timePicker = null;
	private String defaultValue = null;

	public TimePreference(Context context) {
		super(context, null);
		setPersistent(true);
		
	}

	@Override
	protected View onCreateDialogView() {
		timePicker = new TimePicker(getContext());
		timePicker.setIs24HourView(DateFormat.is24HourFormat(getContext()));
		
		String[] time = ((String)getPersistedString(defaultValue)).split(":");
		int h = Integer.valueOf(time[0]);
		int m = Integer.valueOf(time[1]);
		if (h >= 0) {
			timePicker.setCurrentHour(h);
		}
		if (m >= 0) {
			timePicker.setCurrentMinute(m);
		}

		return timePicker;
	}
	
	@Override
	public void setDefaultValue(Object defaultValue) {
		super.setDefaultValue(defaultValue);
		
		if (!(defaultValue instanceof String)) {
			return;
		}
		
		if (!((String)defaultValue).matches("[0-2]*[0-9]:[0-5]*[0-9]")) {
			return;
		}
		
		this.defaultValue = (String)defaultValue;
	}
	
	@Override
	protected void onDialogClosed(boolean positiveResult) {
		if (positiveResult) {
			timePicker.clearFocus(); // clear focus to save the keyboard input if any
			String newValue = timePicker.getCurrentHour() + ":" + timePicker.getCurrentMinute();
			persistString(newValue);
			callChangeListener(newValue);
		}
	}
}
