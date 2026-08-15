using System;
using System.Diagnostics;
using System.IO;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.Win32;

namespace PubgConnect.Client.Services
{
    public class ServerConfigFile
    {
        public string ServerUrl { get; set; } = string.Empty;
        public string DynamicDiscoveryUrl { get; set; } = string.Empty;
        public bool EnableLanAutoDiscovery { get; set; } = true;
    }

    public interface IServerDiscoveryService
    {
        Task<string> AutoDiscoverServerUrlAsync(string currentConfiguredUrl);
    }

    public class ServerDiscoveryService : IServerDiscoveryService
    {
        private const int DiscoveryPort = 5005;
        private const string DiscoverQuery = "PUBGCONNECT_DISCOVER";
        private const string OfferPrefix = "PUBGCONNECT_OFFER:";

        public async Task<string> AutoDiscoverServerUrlAsync(string currentConfiguredUrl)
        {
            // 1. Check local server_config.json file in application directory
            var fileConfig = LoadServerConfigFile();
            if (fileConfig != null && !string.IsNullOrWhiteSpace(fileConfig.ServerUrl) && fileConfig.ServerUrl != "http://localhost:5000")
            {
                return fileConfig.ServerUrl.TrimEnd('/');
            }

            // 2. If current configured URL is working, keep it
            if (!string.IsNullOrWhiteSpace(currentConfiguredUrl) && currentConfiguredUrl != "http://localhost:5000")
            {
                if (await TestServerReachabilityAsync(currentConfiguredUrl, timeoutMs: 1500))
                {
                    return currentConfiguredUrl.TrimEnd('/');
                }
            }

            // 3. Try Dynamic Cloud URL Resolver if configured in server_config.json
            if (fileConfig != null && !string.IsNullOrWhiteSpace(fileConfig.DynamicDiscoveryUrl))
            {
                try
                {
                    using var httpClient = new System.Net.Http.HttpClient { Timeout = TimeSpan.FromSeconds(3) };
                    var remoteUrl = await httpClient.GetStringAsync(fileConfig.DynamicDiscoveryUrl);
                    remoteUrl = remoteUrl?.Trim();
                    if (!string.IsNullOrWhiteSpace(remoteUrl) && remoteUrl.StartsWith("http"))
                    {
                        return remoteUrl.TrimEnd('/');
                    }
                }
                catch { }
            }

            // 4. Try UDP Broadcast LAN Auto-Discovery
            try
            {
                var lanServer = await DiscoverServerViaUdpAsync(timeoutMs: 2000);
                if (!string.IsNullOrWhiteSpace(lanServer))
                {
                    Debug.WriteLine($"LAN Auto-Discovery found server: {lanServer}");
                    return lanServer.TrimEnd('/');
                }
            }
            catch (Exception ex)
            {
                Debug.WriteLine($"UDP discovery error: {ex.Message}");
            }

            // 5. Check if localhost:5000 is running
            if (await TestServerReachabilityAsync("http://localhost:5000", timeoutMs: 1000))
            {
                return "http://localhost:5000";
            }

            return currentConfiguredUrl;
        }

        private static async Task<string?> DiscoverServerViaUdpAsync(int timeoutMs)
        {
            using var udpClient = new UdpClient();
            udpClient.EnableBroadcast = true;

            var endpoint = new IPEndPoint(IPAddress.Broadcast, DiscoveryPort);
            var queryBytes = Encoding.UTF8.GetBytes(DiscoverQuery);

            await udpClient.SendAsync(queryBytes, queryBytes.Length, endpoint);

            using var cts = new CancellationTokenSource(timeoutMs);

            try
            {
                var receiveTask = udpClient.ReceiveAsync(cts.Token);
                var result = await receiveTask;
                var response = Encoding.UTF8.GetString(result.Buffer).Trim();

                if (response.StartsWith(OfferPrefix))
                {
                    return response.Substring(OfferPrefix.Length).Trim();
                }
            }
            catch (OperationCanceledException)
            {
                // Timeout reached
            }
            catch { }

            return null;
        }

        private static async Task<bool> TestServerReachabilityAsync(string url, int timeoutMs)
        {
            try
            {
                using var client = new System.Net.Http.HttpClient { Timeout = TimeSpan.FromMilliseconds(timeoutMs) };
                var res = await client.GetAsync($"{url.TrimEnd('/')}/api/friends/search?friendId=PING");
                return res.StatusCode == HttpStatusCode.NotFound || res.IsSuccessStatusCode;
            }
            catch
            {
                return false;
            }
        }

        private static ServerConfigFile? LoadServerConfigFile()
        {
            try
            {
                var appDir = AppDomain.CurrentDomain.BaseDirectory;
                var configPath = Path.Combine(appDir, "server_config.json");

                if (File.Exists(configPath))
                {
                    var json = File.ReadAllText(configPath);
                    return JsonSerializer.Deserialize<ServerConfigFile>(json);
                }
            }
            catch { }

            return null;
        }
    }
}
