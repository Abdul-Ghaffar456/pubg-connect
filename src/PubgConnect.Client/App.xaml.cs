using System;
using System.IO;
using System.Windows;
using System.Windows.Threading;

namespace PubgConnect.Client
{
    /// <summary>
    /// Interaction logic for App.xaml
    /// </summary>
    public partial class App : System.Windows.Application
    {
        protected override void OnStartup(StartupEventArgs e)
        {
            base.OnStartup(e);

            // Global UI thread exception handler
            DispatcherUnhandledException += App_DispatcherUnhandledException;

            // Global background thread exception handler
            AppDomain.CurrentDomain.UnhandledException += CurrentDomain_UnhandledException;
        }

        private void App_DispatcherUnhandledException(object sender, DispatcherUnhandledExceptionEventArgs e)
        {
            LogCrash(e.Exception);
            System.Windows.MessageBox.Show($"PUBG Connect encountered an unexpected error:\n\n{e.Exception.Message}\n\nDetails have been logged to pubgconnect_crash.log", "PUBG Connect Error", MessageBoxButton.OK, MessageBoxImage.Error);
            e.Handled = true;
        }

        private void CurrentDomain_UnhandledException(object sender, UnhandledExceptionEventArgs e)
        {
            if (e.ExceptionObject is Exception ex)
            {
                LogCrash(ex);
            }
        }

        private static void LogCrash(Exception ex)
        {
            try
            {
                var logPath = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "pubgconnect_crash.log");
                var message = $"[{DateTime.Now:yyyy-MM-dd HH:mm:ss}] ERROR: {ex}\n----------------------------------------\n";
                File.AppendAllText(logPath, message);
            }
            catch { }
        }
    }
}
