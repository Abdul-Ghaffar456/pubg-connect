using System;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;

namespace PubgConnect.Server.Services
{
    public class LanDiscoveryServer : BackgroundService
    {
        private const int DiscoveryPort = 5005;
        private const string DiscoverQuery = "PUBGCONNECT_DISCOVER";
        private const string OfferPrefix = "PUBGCONNECT_OFFER:";
        private readonly ILogger<LanDiscoveryServer> _logger;

        public LanDiscoveryServer(ILogger<LanDiscoveryServer> logger)
        {
            _logger = logger;
        }

        protected override async Task ExecuteAsync(CancellationToken stoppingToken)
        {
            UdpClient? udpServer = null;

            try
            {
                udpServer = new UdpClient(new IPEndPoint(IPAddress.Any, DiscoveryPort))
                {
                    EnableBroadcast = true
                };

                _logger.LogInformation("LAN Auto-Discovery Server listening on UDP port {Port}...", DiscoveryPort);

                while (!stoppingToken.IsCancellationRequested)
                {
                    try
                    {
                        var receiveResult = await udpServer.ReceiveAsync(stoppingToken);
                        var message = Encoding.UTF8.GetString(receiveResult.Buffer).Trim();

                        if (message == DiscoverQuery)
                        {
                            _logger.LogInformation("Received auto-discovery request from {Endpoint}", receiveResult.RemoteEndPoint);

                            // Determine local IP for the client
                            var localIp = GetLocalIpAddressForClient(receiveResult.RemoteEndPoint.Address);
                            var responseMessage = $"{OfferPrefix}http://{localIp}:5000";
                            var responseBytes = Encoding.UTF8.GetBytes(responseMessage);

                            await udpServer.SendAsync(responseBytes, responseBytes.Length, receiveResult.RemoteEndPoint);
                            _logger.LogInformation("Responded with server address: {Address}", responseMessage);
                        }
                    }
                    catch (OperationCanceledException)
                    {
                        break;
                    }
                    catch (Exception ex)
                    {
                        _logger.LogError(ex, "Error processing LAN discovery request.");
                    }
                }
            }
            catch (Exception ex)
            {
                _logger.LogWarning("Could not bind UDP discovery port {Port} (might be already bound or blocked): {Message}", DiscoveryPort, ex.Message);
            }
            finally
            {
                udpServer?.Dispose();
            }
        }

        private static string GetLocalIpAddressForClient(IPAddress clientAddress)
        {
            try
            {
                using var socket = new Socket(AddressFamily.InterNetwork, SocketType.Dgram, 0);
                socket.Connect(clientAddress, 5000);
                if (socket.LocalEndPoint is IPEndPoint endPoint)
                {
                    return endPoint.Address.ToString();
                }
            }
            catch { }

            // Fallback
            foreach (var ip in Dns.GetHostEntry(Dns.GetHostName()).AddressList)
            {
                if (ip.AddressFamily == AddressFamily.InterNetwork && !IPAddress.IsLoopback(ip))
                {
                    return ip.ToString();
                }
            }

            return "localhost";
        }
    }
}
