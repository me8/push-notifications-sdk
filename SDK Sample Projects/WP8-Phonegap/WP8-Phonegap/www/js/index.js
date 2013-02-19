var app = {
    initialize: function () {
        this.bind();
    },
    bind: function () {
        document.addEventListener('deviceready', this.deviceready, false);
    },
    deviceready: function () {
        $("#secondPage").hide();

        //Subscribe to push notification
        PushWooshPlugin.subscribe(app.onPushRecive, app.onError);
        PushWooshPlugin.userToken(app.getUserToken, app.onError);
    },
    onPushRecive: function (pushContent) {
        console.log("Push has been recived: " + pushContent);

        $("#secondPage").show();
        $("#firstPage").hide();
        $("#pushContent").html(pushContent);

        app.getUserData();
    },
    onError: function () {
        console.log("Some error");
    },
    unsubscribe: function () {
        //Unsubscribe to push notification
        PushWooshPlugin.unsubscribe(app.onError);
    },
    getUserData: function () {
        PushWooshPlugin.getUserData(app.onGetUserDataComplete, app.onError);
    },
    onGetUserDataComplete: function (result) {
        console.log("user data: " + result);
        $("#userData").html(result);
    },
    setGeozone: function () {
        if ($('#geozone').attr('checked'))
            PushWooshPlugin.enableGeozone(app.onGeozoneSuccess, app.onError);
        else
            PushWooshPlugin.disableGeozone(app.onGeozoneSuccess, app.onError);
    },
    onGeozoneSuccess: function (result) {
        console.log(result);
    },
    sendingTag: function () {
        var key = $("#tagTitle").val();
        console.log("key: " + key);

        var value = $("#tagValue").val();

        var tagValue = parseInt(value);
        console.log("value: " + tagValue);
        if (isNaN(tagValue)) {
            if (value.indexOf(",") != -1)
                tagValue = $("#tagValue").val().split(',');
            else {
                tagValue = value;
            }
            console.log("value: " + tagValue);
        }

        var tag = {};
        tag[key] = tagValue;
        console.log("json: " + JSON.stringify(tag));

        PushWooshPlugin.sendingTags(app.onSendingComplete, app.onError, tag);
    },
    onSendingComplete: function () {
        console.log("Tag sending complete");
    },
    getUserToken: function (result) {
        console.log("Get user token: " + result);
        $("#userToken").html(result);
    }
};
