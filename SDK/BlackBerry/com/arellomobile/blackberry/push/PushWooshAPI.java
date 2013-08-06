package com.arellomobile.blackberry.push;

import net.rim.blackberry.api.browser.URLEncodedPostData;
import net.rim.device.api.i18n.Locale;
import net.rim.device.api.system.ControlledAccessException;
import net.rim.device.api.system.DeviceInfo;
import net.rim.device.api.system.WLANInfo;
import net.rim.device.api.util.TimeZoneUtilities;

import javax.microedition.io.ConnectionNotFoundException;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;

import com.arellomobile.blackberry.push.libs.json.JSONException;
import com.arellomobile.blackberry.push.libs.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.TimeZone;

public class PushWooshAPI {

	private static final int PUSH_WOOSH_DEVICE_TYPE = 2;

	private static final String PUSH_WOOSH_SERVER_URL = "https://cp.pushwoosh.com/json/1.3/";
	private static final String REGISTER_METHOD = "registerDevice";
	private static final String UNREGISTER_METHOD = "unregisterDevice";

	private static JSONObject processRequest(String url, JSONObject json)
			throws NetworkException {
		HttpConnection serverConnection = null;
		try {
			String ranges = null;
			int contentLength = -1;

			int downloadedBytes = 0;

			try {
				String connectionAds = "";
				if (WLANInfo.getWLANState() == WLANInfo.WLAN_STATE_CONNECTED) {
					connectionAds += ";interface=wifi";
				}
				String connectionType = "";
				if (WLANInfo.getWLANState() == WLANInfo.WLAN_STATE_CONNECTED) {
					connectionType += "WIFI";
				}
				System.out.println("Open connection.");
				System.out.println("url = " + url);
				URLEncodedPostData data = new URLEncodedPostData("UTF-8", false);
				data.setData(json.toString());

				String reqParams = new String(data.getBytes());
				System.out.println("params = " + reqParams);
				System.out.println("Connection Manager ads = " + connectionAds);
				System.out.println("Connection Manager connection type = "
						+ connectionType);
				do {
					System.out.println("method = POST");
					try {
						serverConnection = (HttpConnection) Connector.open(url
								+ connectionAds);
					} catch (ConnectionNotFoundException e) {
						throw new IOException("ConnectionNotFoundException : "
								+ e.getMessage());
					} catch (ControlledAccessException e) {
						throw new IOException("ControlledAccessException : "
								+ e.getMessage());
					}
					serverConnection.setRequestProperty("Content-Type",
							"application/json");
					serverConnection.setRequestProperty("Content-Length",
							String.valueOf(reqParams.getBytes().length));

					serverConnection.setRequestMethod(HttpConnection.POST);
					OutputStream connectionOutput = serverConnection
							.openOutputStream();
					connectionOutput.write(reqParams.getBytes());
					connectionOutput.close();

				} while (workWithResponseCode(serverConnection));

			} catch (IOException e) {
				System.out.println("Network : IOException : " + e.getMessage());
				// Box exception
				throw e;
			}

			// process request
			InputStream inputToProcess = serverConnection.openInputStream();

			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			int n = 0;
			while ((n = inputToProcess.read(buffer)) != -1) {
				bos.write(buffer, 0, n);
			}

			try {
				String responseString = new String(
						bos.toByteArray(), "utf-8");
				
				System.out.println("response string: " + responseString);
				JSONObject result = new JSONObject(responseString);
				return result;
			} catch (JSONException e) {
				throw new NetworkException("Invalid server response");
			}
		} catch (IOException e) {
			System.out.println("BasicServerApiImpl : IOException "
					+ e.getMessage());
			// Box exception
			throw new NetworkException(e.getMessage());
		} finally {
			if (serverConnection != null) {
				try {
					serverConnection.close();
				} catch (IOException e) {
					/* pass */
				}
			}
		}
	}

	private static boolean workWithResponseCode(HttpConnection currentConnection)
			throws IOException, NetworkException {
		int code = currentConnection.getResponseCode();
		if (code == HttpConnection.HTTP_OK
				|| code == HttpConnection.HTTP_PARTIAL) {
			return false;
		}

		// if not modified close connection
		if (code == HttpConnection.HTTP_NOT_MODIFIED) {
			return false;
		}

		// if system problem (no valid response code)
		if (code == -1) {
			return true;
		}

		// message to exception
		String message = "Bad response code " + code + " : "
				+ currentConnection.getResponseMessage();
		NetworkException networkException = new NetworkException(message);
		System.out.println(message);
		throw networkException;
	}

	/**
	 * Register Device with PushWoosh.
	 * 
	 * @param pushWooshAppId
	 *            PushWoosh app id. Needed to register devise
	 * @throws NetworkException
	 *             on error
	 */
	static void registerDevice(String pushWooshAppId) throws NetworkException {
		JSONObject json = new JSONObject();
		try {
			JSONObject request = new JSONObject();
			request.put("application", pushWooshAppId);
			request.put("hwid", Integer.toString(DeviceInfo.getDeviceId(), 16)
					.toUpperCase());
			request.put("push_token", Integer.toString(DeviceInfo.getDeviceId(), 16)
					.toUpperCase());
			request.put("language", Locale.getDefault().getLanguage());
			request.put("device_type", PUSH_WOOSH_DEVICE_TYPE);
			request.put("timezone", TimeZone.getDefault().getRawOffset());
			json.put("request", request);
		} catch (JSONException e) {/* pass */
		}
		// Call the API
		JSONObject status = processRequest(PUSH_WOOSH_SERVER_URL
				+ REGISTER_METHOD, json);
		try {
			int statusCode = status.getInt("status_code");
			if (statusCode != 200 && statusCode != 103) {
				throw new NetworkException("invalid status code: " + statusCode);
			}
		} catch (JSONException e) {
			throw new NetworkException("Invalid server response");
		}
	}

	/**
	 * Un-Register Device with PushWoosh
	 * 
	 * @param pushWooshAppId
	 *            PushWoosh app id. Needed to unregister devise
	 * @throws NetworkException
	 *             on error
	 */
	static void unregisterDevice(String pushWooshAppId) throws NetworkException {
		JSONObject json = new JSONObject();
		try {
			JSONObject request = new JSONObject();
			request.put("application", pushWooshAppId);
			request.put("hwid", Integer.toString(DeviceInfo.getDeviceId(), 16)
					.toUpperCase());
			json.put("request", request);
		} catch (JSONException e) {/* pass */
		}
		// Call the API
		JSONObject status = processRequest(PUSH_WOOSH_SERVER_URL
				+ UNREGISTER_METHOD, json);
		try {
			int statusCode = status.getInt("status_code");
			if (statusCode != 200 && statusCode != 104) {
				throw new NetworkException("invalid status code: " + statusCode);
			}
		} catch (JSONException e) {
			throw new NetworkException("Invalid server response");
		}
	}
}
