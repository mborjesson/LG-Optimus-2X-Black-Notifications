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
import android.view.*;
import android.widget.*;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.martinborjesson.o2xtouchlednotifications.*;

public class SeekBarPreference extends DialogPreference implements OnSeekBarChangeListener {
	
	static public interface OnNoChangeListener {
		public void onNoChange(Preference preference);
	}
	
	private SeekBar seekBar = null;
	private TextView textView = null;
	private OnSeekBarChangeListener onSeekBarChangeListener = null;
	private int defaultValue = 0;
	private int max = 100;
	
	private OnNoChangeListener onNoChangeListener = null;
	
	public SeekBarPreference(Context context) {
		super(context, null);
		setPersistent(true);
	}

	@Override
	protected View onCreateDialogView() {
		LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.seekbar_layout, null);
		seekBar = (SeekBar)view.findViewById(R.id.seekBarSeekBar);
		seekBar.setOnSeekBarChangeListener(this);
		textView = (TextView)view.findViewById(R.id.textViewSeekBar);

		int progress = getPersistedInt(defaultValue);
		seekBar.setMax(max);
		seekBar.setProgress(progress);
		
		updateText(seekBar.getProgress(), seekBar.getMax());
		
		return view;
	}
	
	public void setMax(int max) {
		this.max = max;
	}
	
	public int getProgress() {
		if (seekBar != null) {
			return seekBar.getProgress();
		}
		return defaultValue;
	}
	
	@Override
	public void setDefaultValue(Object defaultValue) {
		super.setDefaultValue(defaultValue);
		
		if (!(defaultValue instanceof Integer)) {
			return;
		}
		
		this.defaultValue = (Integer)defaultValue;
	}
	
	@Override
	protected void onDialogClosed(boolean positiveResult) {
		if (positiveResult) {
			int newValue = seekBar.getProgress();
			persistInt(newValue);
			callChangeListener(newValue);
		} else {
			if (onNoChangeListener != null) {
				onNoChangeListener.onNoChange(this);
			}
		}
	}

	public void setOnNoChangeListener(OnNoChangeListener onNoChangeListener) {
		this.onNoChangeListener = onNoChangeListener;
	}
	
	private void updateText(int progress, int max) {
		textView.setText(progress + "/" + max);
	}

	public void setOnSeekBarChangeListener(OnSeekBarChangeListener onSeekBarChangeListener) {
		this.onSeekBarChangeListener = onSeekBarChangeListener;
	}
	
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		updateText(progress, seekBar.getMax());
		if (onSeekBarChangeListener != null) {
			onSeekBarChangeListener.onProgressChanged(seekBar, progress, fromUser);
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		if (onSeekBarChangeListener != null) {
			onSeekBarChangeListener.onStartTrackingTouch(seekBar);
		}
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		if (onSeekBarChangeListener != null) {
			onSeekBarChangeListener.onStopTrackingTouch(seekBar);
		}
	}
}
