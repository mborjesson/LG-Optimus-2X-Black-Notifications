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
import android.hardware.*;
import android.preference.*;
import android.text.method.*;
import android.view.*;
import android.widget.*;

import com.martinborjesson.o2xtouchlednotifications.*;

public class AccelerometerTolerancePreference extends DialogPreference implements SensorEventListener {
	
	private EditText editText = null;
	private TextView textView = null;
	private float defaultValue = 2.5f;
	
	private SensorManager sensorManager = null;
	private Sensor accelerometerSensor = null;
	private boolean initSensors = true;
	private float prevX = 0;
	private float prevY = 0;
	private float prevZ = 0;
	
	private float averageTolerance = 0;

	public AccelerometerTolerancePreference(Context context) {
		super(context, null);
		setPersistent(true);
		
	}

	@Override
	protected View onCreateDialogView() {
		LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.accelerometer_tolerance_settings, null);
		editText = (EditText)view.findViewById(R.id.editTextAccelerometerTolerance);
		textView = (TextView)view.findViewById(R.id.textViewAccelerometerTolerance);
		editText.setKeyListener(DigitsKeyListener.getInstance(false, true));
		
		editText.setText(String.valueOf(getPersistedFloat(defaultValue)));
		
		updateText(0, 0);
		
		sensorManager = (SensorManager)getContext().getSystemService(Context.SENSOR_SERVICE);
		accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		if (accelerometerSensor != null) {
			sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
		}

		return view;
	}
	
	private void updateText(float tolerance, float averageTolerance) {
		textView.setText("Current: " + tolerance);// + " (average: " + averageTolerance + ")");
	}
	
	@Override
	public void setDefaultValue(Object defaultValue) {
		super.setDefaultValue(defaultValue);
		
		if (!(defaultValue instanceof Float)) {
			return;
		}
		
		this.defaultValue = (Float)defaultValue;
	}
	
	@Override
	protected void onDialogClosed(boolean positiveResult) {
		if (sensorManager != null) {
			sensorManager.unregisterListener(this);
		}
		if (positiveResult) {
			editText.clearFocus(); // clear focus to save the keyboard input if any
			float newValue = defaultValue;
			try {
				newValue = Float.valueOf(editText.getText().toString());
			} catch (NumberFormatException e) {
				
			}
			persistFloat(newValue);
			callChangeListener(newValue);
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		float cAx = event.values[0];
		float cAy = event.values[1];
		float cAz = event.values[2];
		if (initSensors) {
			prevX = cAx;
			prevY = cAy;
			prevZ = cAz;
			initSensors = false;
		}
		
		float diffX = cAx-prevX;
		float diffY = cAy-prevY;
		float diffZ = cAz-prevZ;
		
		float tolerance = (float)Math.sqrt(diffX*diffX + diffY*diffY + diffZ*diffZ);
		
		averageTolerance = (averageTolerance+tolerance)*0.5f;
		
		// set values
		prevX = cAx;
		prevY = cAy;
		prevZ = cAz;
		
		updateText((int)(tolerance*100)/100f, (int)(averageTolerance*100)/100f);
	}
}
