using System;
using System.Drawing;
using System.Windows;
using System.Windows.Forms;

namespace PubgConnect.Client.Services
{
    public interface ISystemTrayService
    {
        void Initialize(Window mainWindow, Action onOpenRequested, Action onSettingsRequested, Action onExitRequested);
        void UpdateTrayStatus(string myStatus, int onlineFriendsCount);
        void ShowTrayMessage(string title, string message);
        void HideToTray();
        void ShowFromTray();
    }

    public class SystemTrayService : ISystemTrayService, IDisposable
    {
        private NotifyIcon? _notifyIcon;
        private Window? _mainWindow;
        private Action? _onOpen;
        private Action? _onSettings;
        private Action? _onExit;

        private ToolStripMenuItem? _statusItem;
        private ToolStripMenuItem? _onlineFriendsItem;

        public void Initialize(Window mainWindow, Action onOpenRequested, Action onSettingsRequested, Action onExitRequested)
        {
            _mainWindow = mainWindow;
            _onOpen = onOpenRequested;
            _onSettings = onSettingsRequested;
            _onExit = onExitRequested;

            Icon trayIcon = SystemIcons.Application;
            try
            {
                var iconPath = System.IO.Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "app_icon.ico");
                if (System.IO.File.Exists(iconPath))
                {
                    trayIcon = new Icon(iconPath);
                }
                else
                {
                    var exePath = System.Diagnostics.Process.GetCurrentProcess().MainModule?.FileName;
                    if (!string.IsNullOrEmpty(exePath))
                    {
                        var extracted = Icon.ExtractAssociatedIcon(exePath);
                        if (extracted != null) trayIcon = extracted;
                    }
                }
            }
            catch { }

            _notifyIcon = new NotifyIcon
            {
                Text = "PUBG Connect",
                Icon = trayIcon,
                Visible = true
            };

            var contextMenu = new ContextMenuStrip();

            var openItem = new ToolStripMenuItem("Open PUBG Connect", null, (s, e) => ShowFromTray())
            {
                Font = new Font(contextMenu.Font, System.Drawing.FontStyle.Bold)
            };

            _statusItem = new ToolStripMenuItem("My Status: Offline")
            {
                Enabled = false
            };

            _onlineFriendsItem = new ToolStripMenuItem("Friends Online: 0")
            {
                Enabled = false
            };

            var settingsItem = new ToolStripMenuItem("Settings", null, (s, e) =>
            {
                ShowFromTray();
                _onSettings?.Invoke();
            });

            var exitItem = new ToolStripMenuItem("Exit", null, (s, e) => _onExit?.Invoke());

            contextMenu.Items.Add(openItem);
            contextMenu.Items.Add(new ToolStripSeparator());
            contextMenu.Items.Add(_statusItem);
            contextMenu.Items.Add(_onlineFriendsItem);
            contextMenu.Items.Add(new ToolStripSeparator());
            contextMenu.Items.Add(settingsItem);
            contextMenu.Items.Add(exitItem);

            _notifyIcon.ContextMenuStrip = contextMenu;
            _notifyIcon.DoubleClick += (s, e) => ShowFromTray();
        }

        public void UpdateTrayStatus(string myStatus, int onlineFriendsCount)
        {
            if (_statusItem != null) _statusItem.Text = $"My Status: {myStatus}";
            if (_onlineFriendsItem != null) _onlineFriendsItem.Text = $"Friends Online: {onlineFriendsCount}";
            if (_notifyIcon != null)
            {
                var text = $"PUBG Connect - {myStatus}";
                if (text.Length >= 64) text = text.Substring(0, 63);
                _notifyIcon.Text = text;
            }
        }

        public void ShowTrayMessage(string title, string message)
        {
            _notifyIcon?.ShowBalloonTip(3000, title, message, ToolTipIcon.Info);
        }

        public void HideToTray()
        {
            if (_mainWindow != null)
            {
                _mainWindow.Hide();
            }
        }

        public void ShowFromTray()
        {
            if (_mainWindow != null)
            {
                _mainWindow.Show();
                _mainWindow.WindowState = WindowState.Normal;
                _mainWindow.Activate();
                _onOpen?.Invoke();
            }
        }

        public void Dispose()
        {
            if (_notifyIcon != null)
            {
                _notifyIcon.Visible = false;
                _notifyIcon.Dispose();
                _notifyIcon = null;
            }
        }
    }
}
