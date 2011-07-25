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

import android.content.*;


public class AppProperties {
	private final Properties properties = new Properties();
	private Context context = null;
	private String fileName = null;
	
	public AppProperties(Context context, String fileName) {
		this.context = context;
		this.fileName = fileName;
	}
	
	public void load() {
		try {
			properties.load(context.openFileInput(fileName));
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void save() {
		try {
			properties.store(context.openFileOutput(fileName, Context.MODE_PRIVATE), null);
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public float getFloat(String key) {
		return getFloat(key, 0);
	}
	
	public float getFloat(String key, float defaultValue) {
		if (!hasProperty(key)) return defaultValue;
		try {
			return Float.valueOf(get(key));
		} catch (Exception e) {
			return defaultValue;
		}
	}
	
	public int getInt(String key) {
		return getInt(key, 0);
	}
	
	public int getInt(String key, int defaultValue) {
		if (!hasProperty(key)) return defaultValue;
		try {
			return Integer.valueOf(get(key));
		} catch (Exception e) {
			return defaultValue;
		}
	}
	
	public boolean getBoolean(String key) {
		return getBoolean(key, false);
	}
	
	public boolean getBoolean(String key, boolean defaultValue) {
		if (!hasProperty(key)) return defaultValue;
		try {
			return Boolean.valueOf(get(key));
		} catch (Exception e) {
			return defaultValue;
		}
	}
	
	public void putBoolean(String key, boolean b) {
		put(key, String.valueOf(b));
	}
	
	public void putInt(String key, int i) {
		put(key, String.valueOf(i));
	}
	
	public void putFloat(String key, float f) {
		put(key, String.valueOf(f));
	}
	
	public void put(String key, String value) {
		properties.setProperty(key, value);
	}
	
	public String get(String key) {
		return properties.getProperty(key);
	}
	
	public String get(String key, String defaultValue) {
		if (!hasProperty(key)) return defaultValue;
		return properties.getProperty(key);
	}
	
	public boolean hasProperty(String key) {
		return properties.containsKey(key);
	}
}
