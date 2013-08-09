using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using Windows.Foundation;
using Windows.Foundation.Collections;
using Windows.UI.Xaml;
using Windows.UI.Xaml.Controls;
using Windows.UI.Xaml.Controls.Primitives;
using Windows.UI.Xaml.Data;
using Windows.UI.Xaml.Input;
using Windows.UI.Xaml.Media;
using Windows.UI.Xaml.Navigation;
using PushSDK;
using System.Text;
using Windows.UI.Popups;
using Windows.UI.StartScreen;
using Windows.UI.Notifications;
using Windows.Data.Xml.Dom;
// The Blank Page item template is documented at http://go.microsoft.com/fwlink/?LinkId=234238

namespace PushWooshSample
{
    /// <summary>
    /// An empty page that can be used on its own or navigated to within a Frame.
    /// </summary>
    public sealed partial class MainPage : Page
    {
        private PushApplicationService service;
        public MainPage()
        {
            this.InitializeComponent();
        }

        /// <summary>
        /// Invoked when this page is about to be displayed in a Frame.
        /// </summary>
        /// <param name="e">Event data that describes how this page was reached.  The Parameter
        /// property is typically used to configure the page.</param>
        protected override async void OnNavigatedTo(NavigationEventArgs e)
        {

        }


        private void ContentGet(string content)
        {
            try
            {

                XmlDocument tileXml = new XmlDocument();
                tileXml.LoadXml(content);

                XmlNodeList images = tileXml.GetElementsByTagName("image");
                if (images.Count>0)
                {
                    foreach (XmlElement item in images)
                    {
                        string imgSource = item.GetAttributeNode("src").Value.ToString();
                        ImagePush.Source = new Windows.UI.Xaml.Media.Imaging.BitmapImage(new Uri(this.BaseUri, imgSource));
                    }
                }

                String texts = tileXml.InnerText;
                Text1Text.Text = texts;

                XmlNodeList badges = tileXml.ChildNodes;
                if (badges.Count>0)
                {
                    foreach (XmlElement item in badges)
                    {
                        string badgeContent = item.GetAttributeNode("value").Value.ToString();
                        Text1Text.Text += "badge: " + badgeContent;
                    }
                }
                


            }
            catch
            {
                Text1Text.Text = "can't display push content";

            }

        }

        void NotificationService_OnPushAccepted(object sender, PushSDK.Classes.CustomEventArgs<string> e)
        {
            throw new NotImplementedException();
        }

        private void CheckBox_Checked(object sender, RoutedEventArgs e)
        {
            if (service != null)
            {
                if (service.NotificationService != null)
                service.NotificationService.GeoZone.Start();
            }

        }

        private void CheckBox_Unchecked(object sender, RoutedEventArgs e)
        {
            if (service != null)
            {
                if (service.NotificationService != null)
                    service.NotificationService.GeoZone.Stop();
            }
        }

        private void UnSubButton_Click(object sender, RoutedEventArgs e)
        {
            if (service != null)
            {
                if (service.NotificationService != null)
                    service.NotificationService.UnsubscribeFromPushes();
            }
            SubButton.IsEnabled = true; ;
            UnSubButton.IsEnabled = false;
            tbPushToken.Text = "";
        }

        private void btnSendTag_Click(object sender, RoutedEventArgs e)
        {
            var tagsList = new List<KeyValuePair<string, object>>();

            object value;
            int iValue;
            if (int.TryParse(tbTagValue.Text, out iValue))
                value = iValue;
            else if (tbTagValue.Text.IndexOf(',') != -1)
                value = tbTagValue.Text.Replace(", ", ",").Split(',');
            else
                value = tbTagValue.Text;

            tagsList.Add(new KeyValuePair<string, object>(tbTagTitle.Text, value));
            if (service != null)
            {
                if (service.NotificationService != null)
                    service.NotificationService.Tags.SendRequest(tagsList);
            }
        }

        private void DisplaySkippedTags(IEnumerable<KeyValuePair<string, string>> skippedTags)
        {
            StringBuilder builder = new StringBuilder();
            builder.AppendLine("These tags has been ignored:");

            foreach (var tag in skippedTags)
            {
                builder.AppendLine(string.Format("{0} : {1}", tag.Key, tag.Value));
            }
        }

        private void tileClick(object sender, RoutedEventArgs e)
        {
            var LiveTile = @"<tile> 
                                <visual version=""1""> 
                                 <binding template=""TileWideImageAndText01"">
                                    <image id=""1"" src=""Background.png"" alt=""alt text""/>
                                    <text id=""1"">Push Woosh</text>
                                 </binding> 
                                 <binding template=""TileSquarePeekImageAndText02"">
                                    <image id=""1"" src=""Background.png"" alt=""alt text""/>
                                    <text id=""1"">Push Woosh</text>
                                    <text id=""2"">Push Woosh Tile Test</text>
                                  </binding> 
                                </visual> 
                              </tile>";
            NotificationText.Text =LiveTile.ToString();
        }

        private void tostClick(object sender, RoutedEventArgs e)
        {
            var ToastTile = @"<toast>
                                <visual>
                                    <binding template=""ToastImageAndText02"">
                                        <image id=""1"" src=""Background.png"" alt=""image1""/>
                                        <text id=""1"">headlineText</text>
                                        <text id=""2"">bodyText</text>
                                    </binding>  
                                </visual>
                            </toast>";
            NotificationText.Text = ToastTile;
        }

        private void badgeClick(object sender, RoutedEventArgs e)
        {
            var BadgeTile = @"<badge value=""alarm""/>";
            NotificationText.Text = BadgeTile.ToString();
        }

        private void Subscribe_Tapped(object sender_, TappedRoutedEventArgs e)
        {
            try
            {
                if (Host.Text.EndsWith("/"))
                {
                    PushSDK.Constants.setHost(Host.Text);
                }
                else
                {
                    PushSDK.Constants.setHost(Host.Text+"/");
                }
                string _PWId = PWID.Text;
                service = new PushApplicationService(_PWId, "", new List<string>(), "");
                service.Subscribe();
              
                service.NotificationService.Tags.OnError += (sender, args) =>
                {
                    MessageDialog dialog = new MessageDialog("Error while sending the tags: \n" + args.Result);
                    dialog.ShowAsync();
                };
                service.NotificationService.Tags.OnSendingComplete += (sender, args) =>
                {
                    MessageDialog dialog = new MessageDialog("Tag has been sent!");
                    dialog.ShowAsync();
                    DisplaySkippedTags(args.Result);
                };

                service.NotificationService.OnPushTokenUpdated += (sender, args) =>
                {
                    tbPushToken.Text = args.Result.ToString();
                };

                service.NotificationService.OnPushAccepted += (sender, args) =>
                {
                    ContentGet(service.NotificationService.LastPushContent);
                };
                if (service.NotificationService.PushToken != null)
                    tbPushToken.Text = service.NotificationService.PushToken;

                SubButton.IsEnabled = false;
                UnSubButton.IsEnabled = true;
            }
            catch (Exception ex)
            {
                MessageDialog dialog = new MessageDialog("Host does not exist: \n" + ex.Message);
                dialog.ShowAsync();
            }
 
        }


    }
}
