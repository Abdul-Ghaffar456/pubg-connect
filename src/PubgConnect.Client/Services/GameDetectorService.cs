using System;
using System.Diagnostics;
using System.Linq;
using System.Management;
using System.Threading;
using System.Threading.Tasks;

namespace PubgConnect.Client.Services
{
    public interface IGameDetectorService
    {
        bool IsGameLoopRunning { get; }
        bool IsPubgRunning { get; }
        string StatusDescription { get; }
        bool IsSimulatedMode { get; set; }
        
        event EventHandler<bool>? PubgStatusChanged;
        event EventHandler? StatusUpdated;

        void StartMonitoring();
        void StopMonitoring();
        void SetSimulatedPubgState(bool isRunning);
        void CheckNow();
    }

    public class GameDetectorService : IGameDetectorService, IDisposable
    {
        private ManagementEventWatcher? _startWatcher;
        private ManagementEventWatcher? _stopWatcher;
        private System.Threading.Timer? _pollingTimer;
        private bool _isMonitoring;
        private bool _isPubgRunning;
        private bool _isGameLoopRunning;
        private bool _isSimulatedMode;

        // Known process names for GameLoop launcher and PUBG Mobile emulator core
        private static readonly string[] GameLoopProcesses = new[]
        {
            "txgameassistant",
            "appmarket",
            "tencentplayer",
            "synergyv",
            "androidemulator",
            "androidemulatorlauncher",
            "androidemulatoren",
            "gameloop",
            "uiwebflow",
            "tqm"
        };

        private static readonly string[] PubgProcesses = new[]
        {
            "aow_exe",
            "androidprocess",
            "aow_proc",
            "pubgmobile",
            "com.tencent.ig",
            "com.pubg.krmobile",
            "com.pubg.imobile"
        };

        public bool IsGameLoopRunning => _isSimulatedMode ? true : _isGameLoopRunning;
        
        public bool IsPubgRunning
        {
            get => _isPubgRunning;
            private set
            {
                if (_isPubgRunning != value)
                {
                    _isPubgRunning = value;
                    PubgStatusChanged?.Invoke(this, _isPubgRunning);
                }
            }
        }

        public string StatusDescription
        {
            get
            {
                if (_isSimulatedMode)
                    return IsPubgRunning ? "🟢 Playing PUBG Mobile (Simulated)" : "⚫ Offline (Simulated)";

                if (IsPubgRunning)
                    return "🟢 Playing PUBG Mobile (Inside GameLoop)";
                if (IsGameLoopRunning)
                    return "🟡 GameLoop Open (PUBG Not Running)";
                return "⚫ GameLoop & PUBG Offline";
            }
        }

        public bool IsSimulatedMode
        {
            get => _isSimulatedMode;
            set
            {
                _isSimulatedMode = value;
                CheckNow();
            }
        }

        public event EventHandler<bool>? PubgStatusChanged;
        public event EventHandler? StatusUpdated;

        public void StartMonitoring()
        {
            if (_isMonitoring) return;
            _isMonitoring = true;

            // Initial snapshot check
            CheckNow();

            // Set up low-frequency safety polling (every 15s) to guarantee ~0% CPU usage
            _pollingTimer = new System.Threading.Timer(_ => CheckNow(), null, TimeSpan.FromSeconds(5), TimeSpan.FromSeconds(15));

            // Set up WMI event listeners for instant process start/stop detection
            try
            {
                var startQuery = new WqlEventQuery("__InstanceCreationEvent", TimeSpan.FromSeconds(2), "TargetInstance ISA 'Win32_Process'");
                _startWatcher = new ManagementEventWatcher(startQuery);
                _startWatcher.EventArrived += OnProcessEvent;
                _startWatcher.Start();

                var stopQuery = new WqlEventQuery("__InstanceDeletionEvent", TimeSpan.FromSeconds(2), "TargetInstance ISA 'Win32_Process'");
                _stopWatcher = new ManagementEventWatcher(stopQuery);
                _stopWatcher.EventArrived += OnProcessEvent;
                _stopWatcher.Start();
            }
            catch (Exception ex)
            {
                Debug.WriteLine($"WMI Watcher init warning (falling back to low-frequency polling): {ex.Message}");
            }
        }

        public void StopMonitoring()
        {
            _isMonitoring = false;
            _pollingTimer?.Dispose();
            _pollingTimer = null;

            try
            {
                if (_startWatcher != null)
                {
                    _startWatcher.EventArrived -= OnProcessEvent;
                    _startWatcher.Stop();
                    _startWatcher.Dispose();
                    _startWatcher = null;
                }

                if (_stopWatcher != null)
                {
                    _stopWatcher.EventArrived -= OnProcessEvent;
                    _stopWatcher.Stop();
                    _stopWatcher.Dispose();
                    _stopWatcher = null;
                }
            }
            catch { }
        }

        public void SetSimulatedPubgState(bool isRunning)
        {
            _isSimulatedMode = true;
            IsPubgRunning = isRunning;
            StatusUpdated?.Invoke(this, EventArgs.Empty);
        }

        private void OnProcessEvent(object sender, EventArrivedEventArgs e)
        {
            if (_isSimulatedMode) return;

            try
            {
                if (e.NewEvent["TargetInstance"] is ManagementBaseObject targetInstance)
                {
                    var procName = targetInstance["Name"]?.ToString()?.ToLowerInvariant();
                    if (string.IsNullOrEmpty(procName)) return;

                    var nameWithoutExt = procName.Replace(".exe", "");

                    if (GameLoopProcesses.Contains(nameWithoutExt) || PubgProcesses.Contains(nameWithoutExt))
                    {
                        // Immediate re-check when relevant process starts or stops
                        CheckNow();
                    }
                }
            }
            catch { }
        }

        public void CheckNow()
        {
            if (_isSimulatedMode)
            {
                StatusUpdated?.Invoke(this, EventArgs.Empty);
                return;
            }

            try
            {
                var processes = Process.GetProcesses();

                bool gameLoopFound = false;
                bool pubgFound = false;

                foreach (var p in processes)
                {
                    try
                    {
                        var pName = p.ProcessName.ToLowerInvariant();

                        if (GameLoopProcesses.Contains(pName))
                        {
                            gameLoopFound = true;
                        }

                        if (PubgProcesses.Contains(pName))
                        {
                            // Verify if it's the active PUBG Mobile renderer process (e.g. aow_exe.exe)
                            pubgFound = true;
                        }
                    }
                    catch { }
                    finally
                    {
                        p.Dispose();
                    }
                }

                _isGameLoopRunning = gameLoopFound;
                IsPubgRunning = pubgFound;

                StatusUpdated?.Invoke(this, EventArgs.Empty);
            }
            catch (Exception ex)
            {
                Debug.WriteLine($"Process detection check error: {ex.Message}");
            }
        }

        public void Dispose()
        {
            StopMonitoring();
        }
    }
}
