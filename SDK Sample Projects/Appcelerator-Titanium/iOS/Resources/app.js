Titanium.UI.setBackgroundColor('#000');
var tabGroup = Titanium.UI.createTabGroup();

var win = Titanium.UI.createWindow({
	title : 'sample',
	backgroundColor : '#fff',
	url : ''
});
var tab1 = Titanium.UI.createTab({
	icon : 'KS_nav_views.png',
	title : 'sample',
	window : win
});

Ti.include('pushwoosh.js');

PushWoosh.appCode = 'YOUR_PUSHWOOSH_APP_ID';

var register = Ti.UI.createButton({
	title : 'register',
	top : 60,
	left : 130,
	width : 120,
	height : 25
});

//record stats for app open with Pushwoosh
PushWoosh.sendAppOpen();

register.addEventListener('click', function() {
	Ti.Network.registerForPushNotifications({
		types : [Ti.Network.NOTIFICATION_TYPE_BADGE, Ti.Network.NOTIFICATION_TYPE_ALERT, Ti.Network.NOTIFICATION_TYPE_SOUND],
		success : function(e) {
			var deviceToken = e.deviceToken;
			Ti.API.info('successfully registered for apple device token with ' + e.deviceToken);
			
			PushWoosh.register(function(data) {
				Ti.API.debug("PushWoosh register success: " + JSON.stringify(data));
				
				PushWoosh.setTags({alias:"device1"}, function(data) {
						Ti.API.debug("PushWoosh sendTags success: " + JSON.stringify(data));
					},function(e) {
						Ti.API.warn("Couldn't setTags with PushWoosh: " + JSON.stringify(e));
				});
				
			}, function(e) {
				Ti.API.warn("Couldn't register with PushWoosh: " + JSON.stringify(e));
			});
		},
		error : function(e) {
			Ti.API.warn("push notifications disabled: " + JSON.stringify(e));
		},
		callback : function(e) {
			Ti.API.warn("push message received: " + JSON.stringify(e));
			
			//send stats to Pushwoosh about push opened
			PushWoosh.sendPushStat(e.data.p);
			
			var a = Ti.UI.createAlertDialog({
				title : 'New Message',
				message : e.data.alert
				//message : JSON.stringify(e.data)	//if you want to access additional custom data in the payload
			});
			a.show();
		}
	});
	Ti.API.info('registering with PushWoosh');
});
win.add(register);

var unregister = Ti.UI.createButton({
	title : 'unregister',
	top : 130,
	width : 120,
	left : 130,
	height : 25
});
unregister.addEventListener('click', function() {
	PushWoosh.unregister(function(data) {
		Ti.UI.createAlertDialog({
			title : 'Successfully unregistered',
			message : JSON.stringify(data)
		}).show();
	}, function(errorregistration) {
		Ti.API.warn("Couldn't unregister with PushWoosh");
	});
});
win.add(unregister);

tabGroup.addTab(tab1);

tabGroup.open();
