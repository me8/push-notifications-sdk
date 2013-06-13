var Alloy = require("alloy"), _ = Alloy._, Backbone = Alloy.Backbone;

var pushnotifications = require("com.arellomobie.push");

Ti.API.info("module is => " + pushnotifications);

pushnotifications.pushNotificationsRegister("60756016005", "4F0C807E51EC77.93591449", {
    success: function(e) {
        Ti.API.info("TITAIUM!!! JS registration success event: " + e.registrationId);
    },
    error: function(e) {
        Ti.API.error("TITAIUM!!! Error during registration: " + e.error);
    },
    callback: function(e) {
        Ti.API.info("TITAIUM!!! JS message event: " + JSON.stringify(e.data));
    }
});

Alloy.createController("index");