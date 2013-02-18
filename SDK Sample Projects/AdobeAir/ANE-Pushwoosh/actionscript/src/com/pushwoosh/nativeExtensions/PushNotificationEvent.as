package com.pushwoosh.nativeExtensions
{
	import flash.events.Event;
	
	public class PushNotificationEvent extends Event
	{
		
		public static const PERMISSION_GIVEN_WITH_TOKEN_EVENT : String = "permissiongivenwithtokenevent";
		public static const PERMISSION_REFUSED_EVENT : String = "permissionRefusedEvent";
		public static const PUSH_NOTIFICATION_RECEIVED_EVENT : String = "pushNotificationReceivedEvent";
		
		public var token : String;
		public var errorCode : String;
		public var errorMessage : String;
		public var parameters:Object;
		
		public function PushNotificationEvent(type:String, bubbles:Boolean=false, cancelable:Boolean=false)
		{
			super(type, bubbles, cancelable);
		}
	}
}