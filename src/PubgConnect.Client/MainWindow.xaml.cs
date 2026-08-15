using System;
using System.ComponentModel;
using System.Windows;
using System.Windows.Input;
using PubgConnect.Client.Services;
using PubgConnect.Client.ViewModels;

namespace PubgConnect.Client
{
    public partial class MainWindow : Window
    {
        private readonly SystemTrayService _systemTrayService;
        private bool _isExplicitExit;

        public MainWindow()
        {
            InitializeComponent();

            // Initialize services
            var apiClient = new ApiClient();
            var realtimeClient = new SignalRRealtimeClient();
            var gameDetector = new GameDetectorService();
            var notificationService = new NotificationService();
            var startupService = new StartupService();
            var discoveryService = new ServerDiscoveryService();
            _systemTrayService = new SystemTrayService();

            var mainVm = new MainViewModel(
                apiClient,
                realtimeClient,
                gameDetector,
                notificationService,
                startupService,
                _systemTrayService,
                discoveryService
            );

            DataContext = mainVm;

            // Initialize System Tray
            _systemTrayService.Initialize(
                this,
                onOpenRequested: () => ShowFromTray(),
                onSettingsRequested: () => mainVm.NavigateTab("Settings"),
                onExitRequested: () => ExitApplication()
            );

            Loaded += (s, e) =>
            {
                var args = Environment.GetCommandLineArgs();
                foreach (var arg in args)
                {
                    if (arg.Equals("--autostart", StringComparison.OrdinalIgnoreCase) ||
                        arg.Equals("--silent", StringComparison.OrdinalIgnoreCase) ||
                        arg.Equals("--tray", StringComparison.OrdinalIgnoreCase) ||
                        arg.Equals("--minimized", StringComparison.OrdinalIgnoreCase))
                    {
                        _systemTrayService.HideToTray();
                        break;
                    }
                }
            };
        }

        private void TitleBar_MouseDown(object sender, MouseButtonEventArgs e)
        {
            if (e.ChangedButton == MouseButton.Left && e.ButtonState == MouseButtonState.Pressed)
            {
                try
                {
                    DragMove();
                }
                catch { }
            }
        }

        private void MinimizeButton_Click(object sender, RoutedEventArgs e)
        {
            _systemTrayService.HideToTray();
        }

        private void CloseButton_Click(object sender, RoutedEventArgs e)
        {
            _systemTrayService.HideToTray();
        }

        protected override void OnClosing(CancelEventArgs e)
        {
            if (!_isExplicitExit)
            {
                e.Cancel = true;
                _systemTrayService.HideToTray();
            }
            else
            {
                _systemTrayService.Dispose();
                base.OnClosing(e);
            }
        }

        private void ShowFromTray()
        {
            Show();
            WindowState = WindowState.Normal;
            Activate();
            Topmost = true;
            Topmost = false;
            Focus();
        }

        private void ExitApplication()
        {
            _isExplicitExit = true;
            Close();
            System.Windows.Application.Current.Shutdown();
        }
    }
}