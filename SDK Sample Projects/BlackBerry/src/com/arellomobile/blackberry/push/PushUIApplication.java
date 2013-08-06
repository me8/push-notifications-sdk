package com.arellomobile.blackberry.push;

import java.io.IOException;

import javax.microedition.io.StreamConnection;

import net.rim.blackberry.api.browser.Browser;
import net.rim.blackberry.api.push.PushApplication;
import net.rim.blackberry.api.push.PushApplicationStatus;
import net.rim.device.api.io.http.PushInputStream;
import net.rim.device.api.system.ApplicationManager;
import net.rim.device.api.system.DeviceInfo;
import net.rim.device.api.system.GlobalEventListener;
import net.rim.device.api.ui.UiApplication;

import com.arellomobile.blackberry.push.libs.json.JSONException;
import com.arellomobile.blackberry.push.libs.json.JSONObject;

public abstract class PushUIApplication extends UiApplication implements
		GlobalEventListener, PushApplication {
	private static final String PUSH_APP_MAIN = "BBPushMain";
	private static final long GUID = 0x30120912ea356c9cL;
	private static final long PUSH_GUID = 0x8aba2a7ecd1ac66cL;

	private PushConnector pc = null;

	private boolean _acceptsForeground = false;
	private String _pushWooshAppId;
	private final Object _syncObject;

	/**
	 * @param bbPushAppId
	 *            blackberry application id
	 * @param bbPushUrl
	 *            blackberry push url
	 * @param bbPushPort
	 *            blackberry push port
	 * @param pushWooshAppId
	 *            app id in pushwoosh
	 * @param defaultAppImage
	 *            app image
	 * @param pushAppImage
	 *            image when push handle
	 * @param pushSound
	 *            sound of push
	 */
	protected PushUIApplication(String bbPushAppId, String bbPushUrl,
			int bbPushPort, String pushWooshAppId, String indicatorImage,
			String defaultAppImage, String pushAppImage, String pushSound) {
		_syncObject = new Object();
		// Register push application
		if (pc == null) {
			pc = new PushConnector(bbPushAppId, bbPushUrl, bbPushPort,
					indicatorImage, defaultAppImage, pushAppImage, pushSound);
		}
		_pushWooshAppId = pushWooshAppId;

		if (PushStore.isPushEnabled().booleanValue()) {
			onPushServiceStatusChange(PushStatus.getStatusPushActive());
		} else {
			onPushServiceStatusChange(PushStatus.getStatusPushNotRegistered());
		}
		
		// Register our PIN with Pushwoosh
		Thread t0 = new Thread() {
			public void run() {
				// Register / De-Register Push with RIM
				registerPushApplication();
			}
		};
		t0.start();
	}

	/**
	 * Instantiates the event dispatcher.
	 */
	protected void beforeShowGUI() {
		// So we can see the app.
		_acceptsForeground = true;

		requestForeground();

		// So we can receive alerts from the notification thread
		addGlobalEventListener(this);

		String model = DeviceInfo.getDeviceName();
		Util.debugPrint(PUSH_APP_MAIN, "Model: " + model);
	}

	/**
	 * Handle our inbound notifications
	 */
	protected void handleNotifications() {
		String notification = PushStore.getNotification();
		if (!notification.equalsIgnoreCase("")) {
			parsePush(notification);
			PushStore.setNotification("");
		}
		// Send event notification to turn off indicators
		ApplicationManager.getApplicationManager().postGlobalEvent(PUSH_GUID);
	}

	/**
	 * Will call when some push come in
	 * 
	 * @param message
	 *            push message
	 */
	protected abstract void handleNotification(String message);

	/**
	 * Will call when some event happen. Needed to notify user about it.
	 * 
	 * @param event
	 *            some event. fail or success
	 */
	public abstract void onEvent(Event event);

	/**
	 * Will call when push status change. Will notify about bb push status and
	 * pushwoosh status
	 * 
	 * @param status
	 *            some status
	 */
	public abstract void onPushServiceStatusChange(PushStatus status);

	/**
	 * Will call when notification with custom data received.
	 * 
	 * @param status
	 *            some status
	 */
	public abstract void onCustomDataReceive(String customData);

	/**
	 * Used to hide background process from application switcher.
	 */
	protected boolean acceptsForeground() {
		return _acceptsForeground;
	}

	/**
	 * Register (Deregister) our app with the RIM Push Service
	 */
	public void registerPushApplication() {
		// do NOT call PushStore methods and this sync in other order, in case
		// of dead-lock
		synchronized (_syncObject) {
			if (PushStore.isPushLockEnable().booleanValue() == false) {
				PushStore.setPushLock();
				// Push enabling... register with RIM
				ApplicationManager.getApplicationManager().postGlobalEvent(
						PushConnector.PUSH_ENABLE_GUID);
			} else {
				onEvent(Event
						.getAlreadyStartChangePushEvent("Already changing push status. Please, wait"));
			}
		}
	}

	public final void deRegisterPushApplication() {
		// do NOT call PushStore methods and this sync in other order, in case
		// of dead-lock
		synchronized (_syncObject) {
			if (PushStore.isPushLockEnable().booleanValue() == false) {
				PushStore.setPushLock();
				// Push disabling... register with RIM
				ApplicationManager.getApplicationManager().postGlobalEvent(
						PushConnector.PUSH_DISABLE_GUID);
			} else {
				onEvent(Event
						.getAlreadyStartChangePushEvent("Already changing push status. Please, wait"));
			}
		}
	}

	/**
	 * onMessage handle inbound push notifications
	 * 
	 * @param stream
	 *            inbound PushInputStream
	 * @param conn
	 *            inbound StreamConnection
	 */
	public void onMessage(final PushInputStream stream,
			final StreamConnection conn) {
		// Buffer for reading
		final byte[] buffer = new byte[15360];

		Thread t0 = new Thread() {
			public void run() {
				try {
					// Temp storage
					int size = stream.read(buffer);
					byte[] binaryData = new byte[size];
					System.arraycopy(buffer, 0, binaryData, 0, size);

					// Close
					stream.accept();
					stream.close();
					conn.close();

					// Get the String
					pc.handleMessage(new String(binaryData, "utf-8"));
				} catch (IOException e1) {
					// pass
				} catch (Exception e1) {
					// pass
				}
			}
		};
		t0.start();
	}

	/**
	 * onStatusChange Called when Push Status changes
	 * 
	 * @param status
	 *            changed push state.
	 */
	public void onStatusChange(PushApplicationStatus status) {
		PushStatus pushStatus = null;
		switch (status.getStatus()) {
		case PushApplicationStatus.STATUS_ACTIVE:
			pushStatus = PushStatus.getStatusBBActive();
			registerPushWoosh();
			break;
		case PushApplicationStatus.STATUS_FAILED:
			int reason = -1;
			switch (status.getReason()) {
			case PushApplicationStatus.REASON_NETWORK_ERROR:
				reason = PushStatus.REASON_NETWORK_ERROR;
				break;
			case PushApplicationStatus.REASON_SIM_CHANGE:
				reason = PushStatus.REASON_SIM_CHANGE;
				break;
			case PushApplicationStatus.REASON_API_CALL:
				reason = PushStatus.REASON_API_CALL;
				break;
			}
			pushStatus = PushStatus
					.getStatusBBFailed(reason, status.getError());
			PushStore.releasePushLock();
			break;
		case PushApplicationStatus.STATUS_NOT_REGISTERED:
			pushStatus = PushStatus.getStatusBBNotRegistered();
			unRegisterPushWoosh();
			break;
		case PushApplicationStatus.STATUS_PENDING:
			pushStatus = PushStatus.getStatusBBPending();
			PushStore.releasePushLock();
			break;
		default:
			PushStore.releasePushLock();
			break;
		}
		if (null != pushStatus) {
			onPushServiceStatusChange(pushStatus);
			Util.debugPrint(PUSH_APP_MAIN, pushStatus.toString());
		}
	}

	public void registerPushWoosh() {
		// Register our Device with PushWoosh
		Thread t0 = new Thread() {
			public void run() {
				PushStatus pushStatus;
				try {
					// Register our Device with PushWoosh
					PushWooshAPI.registerDevice(_pushWooshAppId);
					pushStatus = PushStatus.getStatusPushActive();
					PushStore.setPushEnabled(new Boolean(true));
				} catch (NetworkException e) {
					e.printStackTrace();
					pushStatus = PushStatus.getStatusPushFail(e.getMessage());

				} finally {
					PushStore.releasePushLock();
				}
				onPushServiceStatusChange(pushStatus);
			}
		};
		t0.start();
	}

	public void unRegisterPushWoosh() {
		// unRegister our Device with PushWoosh
		Thread t0 = new Thread() {
			public void run() {
				PushStatus pushStatus;
				try {
					// Register our Device with PushWoosh
					PushWooshAPI.unregisterDevice(_pushWooshAppId);
					pushStatus = PushStatus.getStatusPushNotRegistered();
					PushStore.setPushEnabled(new Boolean(false));
					PushStore.releasePushLock();
				} catch (NetworkException e) {
					e.printStackTrace();
					pushStatus = PushStatus.getStatusPushFail(e.getMessage());
				} finally {
					PushStore.releasePushLock();
				}
				onPushServiceStatusChange(pushStatus);
			}
		};
		t0.start();
	}

	// To turn on/off indicator
	public void eventOccurred(long guid, int data0, int data1,
			final Object input, Object object1) {
		// On Event
		if (guid == GUID) {
			if (null != input && input instanceof String) {
				String notification = (String) input;
				if (!"".equals(notification)) {
					parsePush(notification);
				}
				// Send event notification to turn off indicators
				ApplicationManager.getApplicationManager().postGlobalEvent(
						PUSH_GUID);
			}
		}
	}

	private void parsePush(String notification) {
		try {
			JSONObject jsonObject = new JSONObject(notification);

			System.out.println("json parsed");
			
			String url = null;
			String message = jsonObject.getString("m").trim();
			Integer h = null;
			if (jsonObject.has("h")) {
				h = new Integer(jsonObject.getInt("h"));
				StringBuffer stringBuffer = new StringBuffer();
				stringBuffer.append("https://cp.pushwoosh.com/content/");
				stringBuffer.append(h);
				url = stringBuffer.toString();
			}
			String lUrl = jsonObject.optString("l").trim();
			if (null != lUrl && lUrl.length() > 0) {
				// rewrite prev url if this one is set
				url = lUrl;
			}
			
			String u = jsonObject.optString("u");

			if(null != url && url.length() > 0)
			{
				Browser.getDefaultSession().displayPage(url);
			}
			
			handleNotification(message);
			
			if(null != u && u.length() > 0)
			{
				onCustomDataReceive(u);
			}
		} catch (JSONException e) {
			System.out.println("can't parse input data as json");
			handleNotification(notification);
		}
	}
}
