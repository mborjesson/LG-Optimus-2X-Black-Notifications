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

public class SuperUser {
	/**
	 * Returns true if superuser exists
	 * @return
	 */
	static public boolean hasSuperUser() {
		try { // Run Script
	        java.lang.Process process = Runtime.getRuntime().exec("which su");
	        return process.waitFor() == 0;
	    } catch (IOException ex) {
	        ex.printStackTrace();
	    } catch (InterruptedException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	/**
	 * Perform a superuser command
	 * @param command A command (for example ls /etc/)
	 */
	public static void doSuperUserCommand(String command) {
		java.lang.Process process = null;
		OutputStreamWriter osw = null;
		try { // Run Script

	        process = Runtime.getRuntime().exec("su");
	        osw = new OutputStreamWriter(process.getOutputStream());
	        osw.write(command);
	        osw.flush();
	        osw.close();
	    } catch (IOException ex) {
	        ex.printStackTrace();
	    } finally {
	        if (osw != null) {
	            try {
	                osw.close();
	            } catch (IOException e) {
	                e.printStackTrace();                    
	            }
	        }
	    }
	    try {
	        if (process != null)
	            process.waitFor();
	    } catch (InterruptedException e) {
	        e.printStackTrace();
	    }
	}

}
