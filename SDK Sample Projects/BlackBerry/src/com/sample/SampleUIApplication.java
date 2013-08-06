package com.sample;

import com.arellomobile.blackberry.push.Event;
import com.arellomobile.blackberry.push.PushStatus;
import com.arellomobile.blackberry.push.PushUIApplication;
import net.rim.device.api.applicationcontrol.ApplicationPermissions;
import net.rim.device.api.applicationcontrol.ApplicationPermissionsManager;
import net.rim.device.api.ui.UiApplication;

/**
 * Date: 07.02.12 Time: 10:26
 *
 * @author MiG35
 */
public class SampleUIApplication extends PushUIApplication
{
    private HomeScreen _hs = null;

    protected SampleUIApplication()
    {
        super(Keys.BLACKBERRY_PUSH_APPLICATION_ID, Keys.BLACKBERRY_PUSH_URL,
                Keys.BLACKBERRY_PUSH_PORT, Keys.PUSH_WOOSH_APPLICATION_ID, 
                "widdle_icon.png", "uiicon.png", "uaiconAlert.png",
                "/cash.mp3");
    }

    /**
     * Entry point for application
     *
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args)
    {
        SampleUIApplication nd = new SampleUIApplication();

        /*
           * String argsStr = ""; for (int i = 0; i < args.length; ++i) { argsStr
           * += args[i] + "\n"; } Util.debugPrint("main", argsStr);
           */

        nd.promptPermissions();
        if (args.length > 0 && args[0].equals("autostartup"))
        {
            // Create background process on device restart, no UI
            nd.enterEventDispatcher();
        }
        else
        {
            // Display the User Interface on foreground starts
            nd.showGUI();
        }
    }

    public void showGUI()
    {
        beforeShowGUI();

        _hs = new HomeScreen();

        UiApplication.getUiApplication().pushScreen(_hs);

        // Prompt for app permissions
        promptPermissions();

        // Enter event dispatcher
        enterEventDispatcher();

        // Handle any inbound notifications
        handleNotifications();
    }

    /**
     * Prompt for app permissions
     */
    private void promptPermissions()
    {
        ApplicationPermissionsManager apm = ApplicationPermissionsManager
                .getInstance();
        ApplicationPermissions ap = apm.getApplicationPermissions();

        boolean permissionsOK;
        if (ap.getPermission(ApplicationPermissions.PERMISSION_FILE_API) == ApplicationPermissions.VALUE_ALLOW
                && ap.getPermission(ApplicationPermissions.PERMISSION_INTERNET) == ApplicationPermissions.VALUE_ALLOW
                && ap.getPermission(
                ApplicationPermissions.PERMISSION_CROSS_APPLICATION_COMMUNICATION) == ApplicationPermissions.VALUE_ALLOW
                && ap
                .getPermission(ApplicationPermissions.PERMISSION_INPUT_SIMULATION) == ApplicationPermissions.VALUE_ALLOW
                && ap.getPermission(ApplicationPermissions.PERMISSION_WIFI) == ApplicationPermissions.VALUE_ALLOW  )
        {
            permissionsOK = true;
        }
        else
        {
            ap.addPermission(ApplicationPermissions.PERMISSION_FILE_API);
            ap.addPermission(ApplicationPermissions.PERMISSION_INTERNET);
            ap.addPermission(ApplicationPermissions.PERMISSION_WIFI);
            ap.addPermission(ApplicationPermissions.PERMISSION_INPUT_SIMULATION);
            ap.addPermission(ApplicationPermissions.PERMISSION_CROSS_APPLICATION_COMMUNICATION);

            permissionsOK = apm.invokePermissionsRequest(ap);
        }

        if (!permissionsOK)
        {
            synchronized (getEventLock())
            {
                invokeLater(new Runnable()
                {
                    public void run()
                    {
                        BBPushDialog wrd = new BBPushDialog(
                                "Insufficient Permissions to run Pushwoosh... the application will now exit.");
                        try
                        {
                            pushModalScreen(wrd);
                        } catch (IllegalStateException e)
                        {
                        }
                        requestForeground();
                    }
                });
            }
            System.exit(0);
        }
    }

    protected void handleNotification(final String notification)
    {
        Runnable r = new Runnable()
        {
            public void run()
            {
                BBPushDialog uad = new BBPushDialog(notification);
                try
                {
                    pushModalScreen(uad);
                } catch (IllegalStateException e)
                {
                    // pass
                }
            }
        };
        invokeLater(r);
    }

    private void setStatusMessage(final String message)
    {
        try
        {
            Runnable t2 = new Runnable()
            {
                public void run()
                {
                    if (_hs != null && message != null)
                    {
                        _hs.setStatusMessage(message);
                    }
                }
            };

            invokeLater(t2);
        } catch (IllegalStateException ex)
        {
            // pass
        }
    }

    public void onEvent(final Event event)
    {
        if (event.getStatus() == Event.STATUS_FAIL && event.getWhat() == Event.CHANGING_PUSH_STATUS)
        {
            showAlert(event.getError());
        }
        else
        {
            String message = "";
            switch (event.getWhat())
            {
                case Event.REGISTER_EVENT:
                    if (event.getStatus() == Event.STATUS_SUCCESS)
                    {
                        message += "Register for push notifications with RIM";
                    }
                    else
                    {
                        message += "Register for push notifications with RIM FAIL.\n";
                        message += event.getError();
                    }
                    break;
                case Event.UN_REGISTER_EVENT:
                    if (event.getStatus() == Event.STATUS_SUCCESS)
                    {
                        message += "Un-Register for push notifications with RIM";
                    }
                    else
                    {
                        message += "Un-Register for push notifications with RIM FAIL.\n";
                        message += event.getError();
                    }
                    break;
                case Event.HANDLE_MESSAGE:
                    if (event.getStatus() == Event.STATUS_FAIL)
                    {
                        message += "Handle message FAIL.\n";
                        message += event.getError();
                    }
                    break;
            }
            setStatusMessage(message);
        }
    }

    public void showAlert(final String message)
    {
        synchronized (getEventLock())
        {
            invokeLater(new Runnable()
            {
                public void run()
                {
                    BBPushDialog wrd = new BBPushDialog(
                            message);
                    try
                    {
                        pushModalScreen(wrd);
                    } catch (IllegalStateException e)
                    {
                    }
                    requestForeground();
                }
            });
        }
    }

    public void onPushServiceStatusChange(PushStatus status)
    {
        String statusMessage = getStatusMessage(status);
        setStatusMessage(statusMessage);
    }

    private String getStatusMessage(PushStatus status)
    {
        String statusMessage = "";
        switch (status.getStatus())
        {
            // bb statuses
            case PushStatus.STATUS_BB_ACTIVE:
                statusMessage += "Application registered with BlackBerry. Registering with Pushwoosh...";
                break;
            case PushStatus.STATUS_BB_FAILED:
                statusMessage += "Subscription status failed.\n";
                switch (status.getReason())
                {
                    case PushStatus.REASON_NETWORK_ERROR:
                        statusMessage += "Communication failed due to network error.\n";
                        break;
                    case PushStatus.REASON_SIM_CHANGE:
                        statusMessage += "SIM card change.\n";
                        break;
                    case PushStatus.REASON_API_CALL:
                        statusMessage += "Status change was initiated by API call.\n";
                        break;
                }
                statusMessage += status.getError();
                break;
            case PushStatus.STATUS_BB_NOT_REGISTERED:
                statusMessage += "Application didn't register for push messages with BlackBerry. Unregistering with Pushwoosh...";
                break;
            case PushStatus.STATUS_BB_PENDING:
                statusMessage += "Push communications requested but is not confirmed yet.";
                break;
            // pushwoosh
            case PushStatus.STATUS_PUSHWOOSH_ACTIVE:
                statusMessage += "Application is actively listening.";
                break;
            case PushStatus.STATUS_PUSHWOOSH_NOT_REGISTERED:
                statusMessage += "Application didn't register for push messages.";
                break;
            case PushStatus.STATUS_PUSHWOOSH_FAILED:
                statusMessage += "Fail.\n";
                switch (status.getReason())
                {
                    case PushStatus.REASON_NETWORK_ERROR:
                        statusMessage += "Communication failed due to network error.\n";
                        break;
                    case PushStatus.REASON_SIM_CHANGE:
                        statusMessage += "SIM card change.\n";
                        break;
                    case PushStatus.REASON_API_CALL:
                        statusMessage += "Status change was initiated by API call.\n";
                        break;
                }
                statusMessage += status.getError();
                break;
        }
        return statusMessage;
    }

	public void onCustomDataReceive(final String customData) {
        Runnable r = new Runnable()
        {
            public void run()
            {
                BBPushDialog uad = new BBPushDialog(customData);
                try
                {
                    pushModalScreen(uad);
                } catch (IllegalStateException e)
                {
                    // pass
                }
            }
        };
        invokeLater(r);
	}
}
