package com.arellomobile.blackberry.push;

import net.rim.blackberry.api.messagelist.ApplicationIcon;
import net.rim.blackberry.api.messagelist.ApplicationIndicator;
import net.rim.blackberry.api.messagelist.ApplicationIndicatorRegistry;
import net.rim.blackberry.api.push.PushApplicationDescriptor;
import net.rim.blackberry.api.push.PushApplicationRegistry;
import net.rim.device.api.system.*;
import net.rim.device.api.ui.UiApplication;

import javax.microedition.media.Manager;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;
import java.io.IOException;
import java.io.InputStream;

public class PushConnector implements GlobalEventListener {

	private static final long GUID = 0x30120912ea356c9cL;
	private static final long PUSH_GUID = 0x8aba2a7ecd1ac66cL;
	static final long PUSH_ENABLE_GUID = 0x8aba2a7ecd1ac66dL;
	static final long PUSH_DISABLE_GUID = 0x8aba2a7ecd1ac66eL;

	// Push Application Descriptor
	private PushApplicationDescriptor _pad;

	private PushUIApplication _ui;
	private String mPushSound;
	private String mPushAppImage;
	private String mDefaultAppImage;

	/**
	 * Push Connector for OS 5.X. Params needed to connect to bb rim push
	 * 
	 * @param bbPushAppId
	 *            blackberry application id
	 * @param bbPushUrl
	 *            blackberry push url
	 * @param bbPushPort
	 *            blackberry push port
	 * @param defaultAppImage
	 *            app image
	 * @param pushAppImage
	 *            image when push handle
	 * @param pushSound
	 *            sound of push
	 */
	public PushConnector(String bbPushAppId, String bbPushUrl, int bbPushPort,
			String indicatorImage, String defaultAppImage, String pushAppImage,
			String pushSound) {
		_pad = new PushApplicationDescriptor(bbPushAppId, bbPushPort,
				bbPushUrl, PushApplicationDescriptor.SERVER_TYPE_BPAS,
				ApplicationDescriptor.currentApplicationDescriptor());
		mPushSound = pushSound;
		mPushAppImage = pushAppImage;
		mDefaultAppImage = defaultAppImage;

		// Register our app indicator
		ApplicationIndicatorRegistry reg = ApplicationIndicatorRegistry
				.getInstance();
		EncodedImage mImage = EncodedImage
				.getEncodedImageResource(indicatorImage);
		ApplicationIcon mIcon = new ApplicationIcon(mImage);
		if (reg.getApplicationIndicator() == null) {
			reg.register(mIcon, true, false);
		}

		// Add a global listener so we can turn off indicators
		_ui = (PushUIApplication) UiApplication.getUiApplication();
		_ui.addGlobalEventListener(this);
	}

	/**
	 * De-Register for Push Service with RIM.
	 */
	public void unRegisterForService() {
		try {
			PushApplicationRegistry.unregisterApplication();
			_ui.onEvent(Event.getUnRegisterEvent());
			PushStatus pushStatus = PushStatus.getStatusBBNotRegistered();
			_ui.onPushServiceStatusChange(pushStatus);
			_ui.unRegisterPushWoosh();
		} catch (IllegalArgumentException e) {
			_ui.onEvent(Event.getUnRegisterFailEvent(e.getMessage()));
		}
	}

	/**
	 * Register for Push Service with RIM.
	 */
	public void registerForService() {
		try {
			PushApplicationRegistry.registerApplication(_pad);
			_ui.onEvent(Event.getRegisterEvent());
		} catch (IllegalArgumentException e) {
			// _ui.onEvent(Event.getRegisterFailEvent(e.getMessage()));
			PushStatus pushStatus = PushStatus.getStatusBBActive();
			_ui.onPushServiceStatusChange(pushStatus);
			_ui.registerPushWoosh();
		}
	}

	/**
	 * Handle inbound notification messages
	 * 
	 * @param message
	 *            Notification Message
	 */
	void handleMessage(String message) {
		try {
			// Save the inbound message for later review in the data store
			PushStore.setNotification(message);

			// Turn on the LED
			LED.setConfiguration(500, 250, LED.BRIGHTNESS_50);
			LED.setState(LED.STATE_BLINKING);

			// Set the indicator on
			ApplicationIndicatorRegistry reg = ApplicationIndicatorRegistry
					.getInstance();
			ApplicationIndicator appIndicator = reg.getApplicationIndicator();
			if (null != appIndicator) {
				appIndicator.setVisible(true);
			}

			Class cl = PushUIApplication.class;
			if (cl != null) {
				InputStream is = cl.getResourceAsStream(mPushSound);
				try {
					Player player = Manager.createPlayer(is, "audio/mpeg");
					player.realize();
					player.prefetch();
					player.start();
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (MediaException e) {
					e.printStackTrace();
				}
			}

			Bitmap bm = Bitmap.getBitmapResource(mPushAppImage);
			net.rim.blackberry.api.homescreen.HomeScreen.updateIcon(bm, 0);

			// And let's tell the app we have something for them, and turn on
			// indicators
			ApplicationManager.getApplicationManager().postGlobalEvent(GUID, 0,
					0, message, null);
		} catch (IllegalArgumentException e) {
			_ui.onEvent(Event.getHandleMessageFailEvent(e.getMessage()));
		} catch (IllegalStateException e) {
			_ui.onEvent(Event.getHandleMessageFailEvent(e.getMessage()));
		}
	}

	// Turn off all indicators
	public void eventOccurred(long guid, int data0, int data1, Object object0,
			Object object1) {

		// Enable Push
		if (guid == PUSH_ENABLE_GUID) {
			registerForService();
		}

		// Disable Push
		if (guid == PUSH_DISABLE_GUID) {
			unRegisterForService();
		}

		// Off event
		if (guid == PUSH_GUID) {
			// Turn off the LED
			LED.setState(LED.STATE_OFF);

			try {
				// Set the indicator on
				ApplicationIndicatorRegistry reg = ApplicationIndicatorRegistry
						.getInstance();
				ApplicationIndicator appIndicator = reg
						.getApplicationIndicator();
				appIndicator.setVisible(false);

				Bitmap bm = Bitmap.getBitmapResource(mDefaultAppImage);
				net.rim.blackberry.api.homescreen.HomeScreen.updateIcon(bm, 0);
			} catch (IllegalStateException e) {
				// pass
			}
		}
	}
}
