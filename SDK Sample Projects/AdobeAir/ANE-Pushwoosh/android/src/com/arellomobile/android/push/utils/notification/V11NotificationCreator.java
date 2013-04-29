package com.arellomobile.android.push.utils.notification;

import android.annotation.TargetApi;
import android.app.Notification;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;

/**
 * Date: 28.09.12
 * Time: 12:08
 *
 * @author MiG35
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB) public class V11NotificationCreator
{
	private static final int sImageHeight = 128;

	public static Notification generateNotification(Context context, Bundle data, String header, String message, String tickerTitle)
	{
		int simpleIcon = Helper.tryToGetIconFormStringOrGetFromApplication(data.getString("i"), context);

		Resources res = context.getResources();
		//int height = (int) res.getDimension(android.R.dimen.notification_large_icon_height);
		//int width = (int) res.getDimension(android.R.dimen.notification_large_icon_width);
		
		Bitmap bitmap = null;
		String customIcon = data.getString("ci");
		if (customIcon != null)
		{
			bitmap = Helper.tryToGetBitmapFromInternet(customIcon, context, sImageHeight);
		}
		
		Notification.Builder notificationBuilder = new Notification.Builder(context);
		notificationBuilder.setContentTitle(header);
		notificationBuilder.setContentText(message);
		notificationBuilder.setTicker(tickerTitle);
		notificationBuilder.setWhen(System.currentTimeMillis());

		notificationBuilder.setSmallIcon(simpleIcon);
		
		if (null != bitmap)
		{
			notificationBuilder.setLargeIcon(bitmap);
		}

		return notificationBuilder.getNotification();
	}
}
