LG Optimus 2X/Black Notifications
=================================
Touch LED Notifications for LG Optimus 2X/T-Mobile G2X/Star (P990/P999/SU660) and LG Optimus Black (P970). This application uses mainly the touch LED buttons to show if there is any missed notifications by turning them on and off. Vibrator and notification ringtones are supported. It is enabled by default and will autostart on boot but has to be run once after installation to start the service.

Where to start looking
======================
For UI-preferences the files in `com.martinborjesson.o2xtouchlednotifications.ui.*` and the `main_preferences.xml`-file is where you should look. The `MainPreferences`-class is quite a mess unfortunately so it can be a bit difficult to find anything there.
For the service itself have a look in `MainService`.
Feedbacks (LED-pulse, vibrator and notification ringtone) can be found in `com.martinborjesson.o2xtouchlednotifications.feedbacks`.
Notifications (Gmail, SMS/MMS and missed calls) can be found in `com.martinborjesson.o2xtouchlednotifications.notifications`.

License
=======
All code is licensed under the Apache Software License 2.0. Please read the LICENSE-file for more information on terms of use.

Thanks to
=========
Paul at MoDaCo for sharing how to change the brightness on the touch LED buttons !
aerosoul@des1gn.de for helping me with the LG Optimus Black implementation !
Everyone who helped me out with the app during its development !

Disclaimer
==========
You use the code at your own risk. I take no responsibility if anything happens to your device if you use an app built from this code.