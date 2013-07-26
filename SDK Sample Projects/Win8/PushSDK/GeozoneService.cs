using PushSDK.Classes;
using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using Windows.Devices.Geolocation;
using System.Net.Http;
using System.Net;
using System.IO;
using System.Diagnostics;
using Windows.UI.Popups;

namespace PushSDK
{
   public class GeozoneService
    {
        private const int MovementThreshold = 100;
        private readonly TimeSpan _minSendTime = TimeSpan.FromMinutes(10);

        private readonly Geolocator _watcher = new Geolocator();

        private readonly GeozoneRequest _geozoneRequest = new GeozoneRequest();

        public event EventHandler<CustomEventArgs<string>> OnError;

        private TimeSpan _lastTimeSend;

        public GeozoneService(string appId)
        {
            _geozoneRequest.AppId = appId;

            _watcher.MovementThreshold = MovementThreshold;
            _watcher.PositionChanged += WatcherOnPositionChanged;
        }

      
        public async void Start()
        {
          // _watcher.PositionChanged += WatcherOnPositionChanged;
            await _watcher.GetGeopositionAsync(TimeSpan.FromMinutes(1), TimeSpan.FromMinutes(1));
        }

        public void Stop()
        {
         //   _watcher.PositionChanged -= WatcherOnPositionChanged;   
        }


        private async void WatcherOnPositionChanged(Geolocator sender, PositionChangedEventArgs e)
        {
            if (DateTime.Now.TimeOfDay.Subtract(_lastTimeSend) >= _minSendTime)
            {
                _geozoneRequest.Lat = e.Position.Coordinate.Latitude;
                _geozoneRequest.Lon = e.Position.Coordinate.Longitude;

                var webRequest = (HttpWebRequest)HttpWebRequest.Create(Constants.GeozoneUrl);

                webRequest.Method = "POST";
                webRequest.ContentType = "application/x-www-form-urlencoded";
                string request = String.Format("{{ \"request\":{0}}}", JsonConvert.SerializeObject(_geozoneRequest));

                byte[] requestBytes = System.Text.Encoding.UTF8.GetBytes(request);

                // Write the channel URI to the request stream.
                Stream requestStream = await webRequest.GetRequestStreamAsync();
                requestStream.Write(requestBytes, 0, requestBytes.Length);

                try
                {
                    // Get the response from the server.
                    WebResponse response = await webRequest.GetResponseAsync();
                    StreamReader requestReader = new StreamReader(response.GetResponseStream());
                    String webResponse = requestReader.ReadToEnd();

                    string errorMessage = String.Empty;

                    Debug.WriteLine("Response: " + webResponse);

                    JObject jRoot = JObject.Parse(webResponse);
                    int code = JsonHelpers.GetStatusCode(jRoot);

                    if (JsonHelpers.GetStatusCode(jRoot) == 200)
                    {
                        double dist = jRoot["response"].Value<double>("distance");
                        if (dist > 0)
                            _watcher.MovementThreshold = dist / 2;
                    }
                    else
                        errorMessage = JsonHelpers.GetStatusMessage(jRoot);

                    if (!String.IsNullOrEmpty(errorMessage) && OnError != null)
                    {
                        Debug.WriteLine("Error: " + errorMessage);
                        OnError(this, new CustomEventArgs<string> { Result = errorMessage });
                    }
                }
                catch( Exception ex)
                {
                    Debug.WriteLine("Error: " + ex.Message);
                    OnError(this, new CustomEventArgs<string> { Result = ex.Message });
                }     
               
               _lastTimeSend = DateTime.Now.TimeOfDay;
            }
        }

    }
}
