// The contents of this file will be executed before any of
// your view controllers are ever executed, including the index.
// You have access to all functionality on the `Alloy` namespace.
//
// This is a great place to do any initialization for your app
// or create any global variables/functions that you'd like to
// make available throughout your app. You can easily make things
// accessible globally by attaching them to the `Alloy.Globals`
// object. For example:
//
// Alloy.Globals.someGlobalFunction = function(){};

	var pushnotifications = require('com.arellomobie.push');
	Ti.API.info("module is => " + pushnotifications);
	
	pushnotifications.pushNotificationsRegister("60756016005", "4F0C807E51EC77.93591449", {
		//NOTE: all the functions fire on the background thread, do not use any UI or Alerts here
		success:function(e)
		{
			Ti.API.info('TITAIUM!!! JS registration success event: ' + e.registrationId);
		},
		error:function(e)
		{
			Ti.API.error("TITAIUM!!! Error during registration: "+e.error);
		},
		callback:function(e) // called when a push notification is received
		{
			Ti.API.info('TITAIUM!!! JS message event: ' + JSON.stringify(e.data));
		}
	});
