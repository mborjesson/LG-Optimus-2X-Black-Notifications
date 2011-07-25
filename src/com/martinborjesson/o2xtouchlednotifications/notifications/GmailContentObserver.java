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

package com.martinborjesson.o2xtouchlednotifications.notifications;

import android.accounts.*;
import android.content.*;
import android.database.*;
import android.net.*;

import com.martinborjesson.o2xtouchlednotifications.services.*;
import com.martinborjesson.o2xtouchlednotifications.utils.*;

public class GmailContentObserver extends AbstractContentObserver {
	
	public static final String ID = GmailContentObserver.class.getName() + ".GMAIL";
	
    private static final String[] GMAIL_PROJECTION = {
        "canonicalName",
        "numUnreadConversations"
    };

	private Account account = null;
	
	private Context context = null;
	private int numUnread = 0;
	private boolean hasUnread = false;

	public GmailContentObserver(ContentResolver contentResolver, Context context, Account account) {
		super(null, contentResolver);
		this.context = context;
		this.account = account;
	}
	
	public void register() {
		super.register(Uri.parse("content://gmail-ls/"));
	}
	
	public void reset() {
		numUnread = getNumUnread();
	}
	
	@Override
	public void onChange(boolean selfChange) {
		int currentUnread = getNumUnread();
		if (currentUnread > numUnread) {
			Logger.logDebug("Got new gmail");
			hasUnread = true;
			MainService.newNotification(context, ID + "-" + account.name);
		} else if (currentUnread < numUnread) {
			hasUnread = false;
			MainService.removeNotification(context, ID + "-" + account.name);
			Logger.logDebug("Gmail was removed");
		}
		numUnread = currentUnread;
	}
	
	private int getNumUnread() {
		try {
			Cursor c = contentResolver.query(
					Uri.withAppendedPath(Uri.parse("content://gmail-ls/labels"), account.name), GMAIL_PROJECTION, 
					null, null, null);
			if (c != null) {
				try {
					while (c.moveToNext()) {
			            int nameColumn = c.getColumnIndex(GMAIL_PROJECTION[0]);
						String canonicalName = c.getString(nameColumn);
						
						int unreadColumn = c.getColumnIndex(GMAIL_PROJECTION[1]);
						int unread = c.getInt(unreadColumn);
						if (canonicalName.equals("^^unseen-^i")) {
							return unread;
						}
					}
				} finally {
					c.close();
				}
			}
		} catch (Exception e) {
			Logger.logDebug("Gmail getNumRead() exception: " + e.getMessage());
		}
		return 0;
	}

	@Override
	public boolean hasChanged() {
		return hasUnread;
	}

}
