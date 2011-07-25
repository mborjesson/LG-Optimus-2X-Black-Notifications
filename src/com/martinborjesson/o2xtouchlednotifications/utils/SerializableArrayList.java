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

/**
 * Simple wrapper to serialize/unserialize arraylists
 * @author Martin Börjesson
 *
 * @param <T>
 */
public class SerializableArrayList<T> extends ArrayList<T> implements Serializable {

	transient private Context context = null;
	transient private String fileName = null;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public SerializableArrayList(Context context, String fileName) {
		this.context = context;
		this.fileName = fileName;
	}
	
	public void serialize() throws IOException {
		ObjectSerializer.serialize(context, fileName, this);
	}
	
	@SuppressWarnings("unchecked")
	public void unserialize() throws IOException {
		clear();
		addAll((List<T>)ObjectSerializer.unserialize(context, fileName));
	}
}
