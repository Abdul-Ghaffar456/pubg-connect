using System;
using System.Diagnostics;
using System.IO;
using System.IO.Compression;
using System.Reflection;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Input;
using Microsoft.Win32;

namespace PubgConnect.Installer
{
    public partial class MainWindow : Window
    {
        private readonly string _installDir;

        public MainWindow()
        {
            InitializeComponent();

            var localAppData = Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData);
            _installDir = Path.Combine(localAppData, "Programs", "PUBGConnect");
            InstallPathText.Text = _installDir;
        }

        private void TitleBar_MouseDown(object sender, MouseButtonEventArgs e)
        {
            if (e.ChangedButton == MouseButton.Left && e.ButtonState == MouseButtonState.Pressed)
            {
                try { DragMove(); } catch { }
            }
        }

        private void CloseButton_Click(object sender, RoutedEventArgs e)
        {
            Close();
        }

        private async void InstallButton_Click(object sender, RoutedEventArgs e)
        {
            OptionsPanel.Visibility = Visibility.Collapsed;
            ProgressPanel.Visibility = Visibility.Visible;

            var createDesktopShortcut = DesktopShortcutCheck.IsChecked == true;
            var createStartMenuShortcut = StartMenuCheck.IsChecked == true;
            var autoStart = AutoStartCheck.IsChecked == true;

            await Task.Run(async () =>
            {
                try
                {
                    UpdateProgress(10, "Preparing installation directory...");
                    
                    // Kill any existing running client instances
                    try
                    {
                        foreach (var p in Process.GetProcessesByName("PubgConnect.Client"))
                        {
                            try { p.Kill(); p.WaitForExit(2000); } catch { }
                        }
                    }
                    catch { }

                    if (!Directory.Exists(_installDir))
                    {
                        Directory.CreateDirectory(_installDir);
                    }

                    UpdateProgress(30, "Extracting application files...");

                    // Extract embedded payload.zip
                    var assembly = Assembly.GetExecutingAssembly();
                    using (var stream = assembly.GetManifestResourceStream("PubgConnect.Installer.payload.zip"))
                    {
                        if (stream != null)
                        {
                            using var archive = new ZipArchive(stream);
                            int totalEntries = archive.Entries.Count;
                            int currentEntry = 0;

                            foreach (var entry in archive.Entries)
                            {
                                if (string.IsNullOrEmpty(entry.Name))
                                {
                                    // Directory
                                    var dirPath = Path.Combine(_installDir, entry.FullName);
                                    Directory.CreateDirectory(dirPath);
                                    continue;
                                }

                                var destPath = Path.Combine(_installDir, entry.FullName);
                                var destDir = Path.GetDirectoryName(destPath);
                                if (!string.IsNullOrEmpty(destDir))
                                {
                                    Directory.CreateDirectory(destDir);
                                }

                                entry.ExtractToFile(destPath, true);
                                currentEntry++;

                                int percent = 30 + (int)((currentEntry / (float)totalEntries) * 45);
                                UpdateProgress(percent, $"Extracting: {entry.Name}");
                            }
                        }
                    }

                    UpdateProgress(80, "Creating shortcuts...");
                    var exePath = Path.Combine(_installDir, "PubgConnect.Client.exe");
                    var iconPath = Path.Combine(_installDir, "app_icon.ico");

                    if (createDesktopShortcut)
                    {
                        var desktopPath = Environment.GetFolderPath(Environment.SpecialFolder.DesktopDirectory);
                        var linkPath = Path.Combine(desktopPath, "PUBG Connect.lnk");
                        CreateShortcut(linkPath, exePath, iconPath, "PUBG Connect - GameLoop Friend Status Notifier");
                    }

                    if (createStartMenuShortcut)
                    {
                        var startMenuPath = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.StartMenu), "Programs");
                        var linkPath = Path.Combine(startMenuPath, "PUBG Connect.lnk");
                        CreateShortcut(linkPath, exePath, iconPath, "PUBG Connect - GameLoop Friend Status Notifier");
                    }

                    UpdateProgress(90, "Registering Windows system integration...");
                    if (autoStart)
                    {
                        try
                        {
                            using var runKey = Registry.CurrentUser.OpenSubKey(@"SOFTWARE\Microsoft\Windows\CurrentVersion\Run", true);
                            runKey?.SetValue("PUBGConnect", $"\"{exePath}\" --autostart");
                        }
                        catch { }
                    }

                    // Register in Add/Remove Programs
                    RegisterUninstallInfo(exePath, iconPath);

                    // Create uninstall.bat
                    CreateUninstallScript();

                    UpdateProgress(100, "Installation complete!");
                    await Task.Delay(500);
                }
                catch (Exception ex)
                {
                    Dispatcher.Invoke(() =>
                    {
                        MessageBox.Show($"Installation failed: {ex.Message}", "Error", MessageBoxButton.OK, MessageBoxImage.Error);
                        Close();
                    });
                }
            });

            ProgressPanel.Visibility = Visibility.Collapsed;
            FinishedPanel.Visibility = Visibility.Visible;
        }

        private void UpdateProgress(int percent, string status)
        {
            Dispatcher.Invoke(() =>
            {
                InstallProgressBar.Value = percent;
                PercentText.Text = $"{percent}%";
                StatusText.Text = status;
            });
        }

        private static void CreateShortcut(string shortcutPath, string targetPath, string iconPath, string description)
        {
            try
            {
                var type = Type.GetTypeFromProgID("WScript.Shell");
                if (type != null)
                {
                    dynamic shell = Activator.CreateInstance(type)!;
                    dynamic shortcut = shell.CreateShortcut(shortcutPath);
                    shortcut.TargetPath = targetPath;
                    shortcut.WorkingDirectory = Path.GetDirectoryName(targetPath);
                    shortcut.Description = description;
                    if (File.Exists(iconPath))
                    {
                        shortcut.IconLocation = iconPath;
                    }
                    shortcut.Save();
                }
            }
            catch { }
        }

        private void RegisterUninstallInfo(string exePath, string iconPath)
        {
            try
            {
                using var key = Registry.CurrentUser.CreateSubKey(@"Software\Microsoft\Windows\CurrentVersion\Uninstall\PUBGConnect");
                if (key != null)
                {
                    key.SetValue("DisplayName", "PUBG Connect");
                    key.SetValue("DisplayVersion", "1.0.1");
                    key.SetValue("Publisher", "PUBG Connect");
                    key.SetValue("DisplayIcon", File.Exists(iconPath) ? iconPath : exePath);
                    key.SetValue("InstallLocation", _installDir);
                    key.SetValue("UninstallString", $"\"{Path.Combine(_installDir, "uninstall.bat")}\"");
                    key.SetValue("NoModify", 1, RegistryValueKind.DWord);
                    key.SetValue("NoRepair", 1, RegistryValueKind.DWord);
                }
            }
            catch { }
        }

        private void CreateUninstallScript()
        {
            try
            {
                var uninstallerPath = Path.Combine(_installDir, "uninstall.bat");
                var script = @"@echo off
echo Uninstalling PUBG Connect...
taskkill /F /IM PubgConnect.Client.exe >nul 2>&1
reg delete ""HKCU\Software\Microsoft\Windows\CurrentVersion\Run"" /v PUBGConnect /f >nul 2>&1
reg delete ""HKCU\Software\Microsoft\Windows\CurrentVersion\Uninstall\PUBGConnect"" /f >nul 2>&1
reg delete ""HKCU\SOFTWARE\PUBGConnect"" /f >nul 2>&1
del /F /Q ""%USERPROFILE%\Desktop\PUBG Connect.lnk"" >nul 2>&1
del /F /Q ""%APPDATA%\Microsoft\Windows\Start Menu\Programs\PUBG Connect.lnk"" >nul 2>&1
cd %TEMP%
rd /S /Q """ + _installDir + @""" >nul 2>&1
echo PUBG Connect has been uninstalled successfully.
pause
";
                File.WriteAllText(uninstallerPath, script);
            }
            catch { }
        }

        private void FinishButton_Click(object sender, RoutedEventArgs e)
        {
            if (LaunchAfterFinishCheck.IsChecked == true)
            {
                var exePath = Path.Combine(_installDir, "PubgConnect.Client.exe");
                if (File.Exists(exePath))
                {
                    try
                    {
                        Process.Start(new ProcessStartInfo
                        {
                            FileName = exePath,
                            WorkingDirectory = _installDir,
                            UseShellExecute = true
                        });
                    }
                    catch { }
                }
            }

            Close();
        }
    }
}
