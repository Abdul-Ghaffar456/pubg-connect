using System;
using System.Diagnostics;
using Microsoft.Win32;

namespace PubgConnect.Client.Services
{
    public interface IStartupService
    {
        bool IsStartWithWindowsEnabled();
        void SetStartWithWindows(bool enable);
        bool IsStartWithGameLoopEnabled();
        void SetStartWithGameLoop(bool enable);
    }

    public class StartupService : IStartupService
    {
        private const string AppName = "PUBGConnect";
        private const string RunRegistryKeyPath = @"SOFTWARE\Microsoft\Windows\CurrentVersion\Run";
        private const string AppSettingsRegistryKeyPath = @"SOFTWARE\PUBGConnect";

        public bool IsStartWithWindowsEnabled()
        {
            try
            {
                using var key = Registry.CurrentUser.OpenSubKey(RunRegistryKeyPath, false);
                var value = key?.GetValue(AppName);
                return value != null;
            }
            catch (Exception ex)
            {
                Debug.WriteLine($"Error checking registry for Windows startup: {ex.Message}");
                return false;
            }
        }

        public void SetStartWithWindows(bool enable)
        {
            try
            {
                using var key = Registry.CurrentUser.OpenSubKey(RunRegistryKeyPath, true);
                if (key == null) return;

                if (enable)
                {
                    var exePath = Process.GetCurrentProcess().MainModule?.FileName;
                    if (!string.IsNullOrEmpty(exePath))
                    {
                        key.SetValue(AppName, $"\"{exePath}\" --autostart");
                    }
                }
                else
                {
                    key.DeleteValue(AppName, false);
                }
            }
            catch (Exception ex)
            {
                Debug.WriteLine($"Error modifying registry for Windows startup: {ex.Message}");
            }
        }

        public bool IsStartWithGameLoopEnabled()
        {
            try
            {
                using var key = Registry.CurrentUser.OpenSubKey(AppSettingsRegistryKeyPath, false);
                var value = key?.GetValue("AutoStartWithGameLoop");
                if (value != null && int.TryParse(value.ToString(), out int val))
                {
                    return val == 1;
                }
                return true; // Default enabled
            }
            catch (Exception ex)
            {
                Debug.WriteLine($"Error reading GameLoop startup setting: {ex.Message}");
                return true;
            }
        }

        public void SetStartWithGameLoop(bool enable)
        {
            try
            {
                using var key = Registry.CurrentUser.CreateSubKey(AppSettingsRegistryKeyPath);
                if (key != null)
                {
                    key.SetValue("AutoStartWithGameLoop", enable ? 1 : 0, RegistryValueKind.DWord);
                }

                // If enabled, ensure the background listener / autostart is registered
                if (enable)
                {
                    RegisterGameLoopTriggerTask();
                }
                else
                {
                    UnregisterGameLoopTriggerTask();
                }
            }
            catch (Exception ex)
            {
                Debug.WriteLine($"Error saving GameLoop startup setting: {ex.Message}");
            }
        }

        private void RegisterGameLoopTriggerTask()
        {
            try
            {
                // Register lightweight autostart with Windows so detector stays listening quietly in tray
                SetStartWithWindows(true);
            }
            catch { }
        }

        private void UnregisterGameLoopTriggerTask()
        {
            // Task unregistration if needed
        }
    }
}
