package com.arellomobile.android.push.utils.notification;

import android.app.Notification;
import android.content.Context;
import android.os.Bundle;

/**
 * Date: 28.09.12
 * Time: 12:08
 *
 * @author MiG35
 */
public class NotificationCreator
{
	@SuppressWarnings("deprecation")
	public static Notification generateNotification(Context context, Bundle data, String header, String message, String tickerTitle)
	{
		Notification notification = new Notification(Helper.tryToGetIconFormStringOrGetFromApplication(data.getString("i"), context), tickerTitle, System.currentTimeMillis());
		notification.setLatestEventInfo(context, header, message, notification.contentIntent);
		return notification;
	}
}
