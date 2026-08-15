using System;
using System.Diagnostics;

namespace PubgConnect.Client.Services
{
    public interface INotificationService
    {
        bool IsNotificationsEnabled { get; set; }
        bool IsSoundEnabled { get; set; }
        
        void SetSystemTrayFallback(ISystemTrayService systemTray);
        void ShowFriendStartedPubgToast(string friendUsername, bool isMuted);
        void ShowGenericToast(string title, string message);
    }

    public class NotificationService : INotificationService
    {
        private ISystemTrayService? _systemTray;

        public bool IsNotificationsEnabled { get; set; } = true;
        public bool IsSoundEnabled { get; set; } = true;

        public void SetSystemTrayFallback(ISystemTrayService systemTray)
        {
            _systemTray = systemTray;
        }

        public void ShowFriendStartedPubgToast(string friendUsername, bool isMuted)
        {
            if (!IsNotificationsEnabled || isMuted) return;

            try
            {
                var title = "🎮 PUBG Connect";
                var message = $"{friendUsername} is online! {friendUsername} just started PUBG Mobile.";

                // Primary: System Tray balloon notification for desktop stability
                _systemTray?.ShowTrayMessage(title, message);

                if (IsSoundEnabled)
                {
                    System.Media.SystemSounds.Asterisk.Play();
                }
            }
            catch (Exception ex)
            {
                Debug.WriteLine($"Notification error: {ex.Message}");
            }
        }

        public void ShowGenericToast(string title, string message)
        {
            if (!IsNotificationsEnabled) return;

            try
            {
                _systemTray?.ShowTrayMessage(title, message);
            }
            catch (Exception ex)
            {
                Debug.WriteLine($"Notification error: {ex.Message}");
            }
        }
    }
}
