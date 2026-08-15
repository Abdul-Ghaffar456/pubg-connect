using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Net.Http.Json;
using System.Threading.Tasks;
using Microsoft.Win32;
using PubgConnect.Shared;

namespace PubgConnect.Client.Services
{
    public interface IApiClient
    {
        string BaseUrl { get; set; }
        string Token { get; set; }
        
        Task<AuthResponse> LoginAsync(string email, string password);
        Task<AuthResponse> RegisterAsync(string username, string email, string password);
        Task<UserDto?> GetMeAsync();
        Task<List<FriendDto>> GetFriendsAsync();
        Task<UserDto?> SearchFriendAsync(string friendId);
        Task<(bool Success, string Message)> SendFriendRequestAsync(string friendId);
        Task<List<FriendRequestDto>> GetPendingRequestsAsync();
        Task<(bool Success, string Message)> RespondFriendRequestAsync(string requestId, bool accept);
        Task<(bool Success, string Message)> RemoveFriendAsync(string friendUserId);
        Task<(bool Success, string Message)> ToggleMuteFriendAsync(string friendUserId, bool mute);
        Task<(bool Success, string Message)> UpdateSettingsAsync(bool shareStatus, bool allowRequests, bool showDuration);
    }

    public class ApiClient : IApiClient
    {
        private readonly HttpClient _http;
        private string _baseUrl = "http://84.235.248.234:5000";
        private string _token = string.Empty;
        private const string AppSettingsRegistryKeyPath = @"SOFTWARE\PUBGConnect";

        public string BaseUrl
        {
            get => _baseUrl;
            set
            {
                if (!string.IsNullOrWhiteSpace(value))
                {
                    _baseUrl = value.TrimEnd('/');
                    SaveServerUrl(_baseUrl);
                }
            }
        }

        public string Token
        {
            get => _token;
            set
            {
                _token = value;
                if (!string.IsNullOrEmpty(_token))
                {
                    _http.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", _token);
                }
                else
                {
                    _http.DefaultRequestHeaders.Authorization = null;
                }
            }
        }

        public ApiClient()
        {
            _http = new HttpClient { Timeout = TimeSpan.FromSeconds(15) };
            _baseUrl = LoadServerUrl();
        }

        private static string LoadServerUrl()
        {
            try
            {
                using var key = Registry.CurrentUser.OpenSubKey(AppSettingsRegistryKeyPath, false);
                var val = key?.GetValue("ServerUrl")?.ToString();
                if (!string.IsNullOrWhiteSpace(val))
                {
                    return val.TrimEnd('/');
                }
            }
            catch { }
            return "https://pubgconnect-backend.onrender.com";
        }

        private static void SaveServerUrl(string url)
        {
            try
            {
                using var key = Registry.CurrentUser.CreateSubKey(AppSettingsRegistryKeyPath);
                key?.SetValue("ServerUrl", url);
            }
            catch { }
        }

        public async Task<AuthResponse> LoginAsync(string email, string password)
        {
            try
            {
                var response = await _http.PostAsJsonAsync($"{BaseUrl}/api/auth/login", new LoginRequest { Email = email, Password = password });
                var result = await response.Content.ReadFromJsonAsync<AuthResponse>();
                if (result != null && result.Success)
                {
                    Token = result.Token;
                }
                return result ?? new AuthResponse { Success = false, Message = "Invalid server response." };
            }
            catch (Exception ex)
            {
                return new AuthResponse { Success = false, Message = $"Cannot connect to server at {BaseUrl}: {ex.Message}" };
            }
        }

        public async Task<AuthResponse> RegisterAsync(string username, string email, string password)
        {
            try
            {
                var response = await _http.PostAsJsonAsync($"{BaseUrl}/api/auth/register", new RegisterRequest { Username = username, Email = email, Password = password });
                var result = await response.Content.ReadFromJsonAsync<AuthResponse>();
                if (result != null && result.Success)
                {
                    Token = result.Token;
                }
                return result ?? new AuthResponse { Success = false, Message = "Invalid server response." };
            }
            catch (Exception ex)
            {
                return new AuthResponse { Success = false, Message = $"Cannot connect to server at {BaseUrl}: {ex.Message}" };
            }
        }

        public async Task<UserDto?> GetMeAsync()
        {
            try
            {
                return await _http.GetFromJsonAsync<UserDto>($"{BaseUrl}/api/auth/me");
            }
            catch
            {
                return null;
            }
        }

        public async Task<List<FriendDto>> GetFriendsAsync()
        {
            try
            {
                var result = await _http.GetFromJsonAsync<List<FriendDto>>($"{BaseUrl}/api/friends");
                return result ?? new List<FriendDto>();
            }
            catch
            {
                return new List<FriendDto>();
            }
        }

        public async Task<UserDto?> SearchFriendAsync(string friendId)
        {
            try
            {
                var res = await _http.GetAsync($"{BaseUrl}/api/friends/search?friendId={Uri.EscapeDataString(friendId)}");
                if (res.IsSuccessStatusCode)
                {
                    return await res.Content.ReadFromJsonAsync<UserDto>();
                }
            }
            catch { }
            return null;
        }

        public async Task<(bool Success, string Message)> SendFriendRequestAsync(string friendId)
        {
            try
            {
                var res = await _http.PostAsJsonAsync($"{BaseUrl}/api/friends/request", new SendFriendRequestDto { TargetFriendId = friendId });
                var body = await res.Content.ReadFromJsonAsync<SimpleResponse>();
                return (res.IsSuccessStatusCode, body?.Message ?? "Request sent.");
            }
            catch (Exception ex)
            {
                return (false, ex.Message);
            }
        }

        public async Task<List<FriendRequestDto>> GetPendingRequestsAsync()
        {
            try
            {
                var result = await _http.GetFromJsonAsync<List<FriendRequestDto>>($"{BaseUrl}/api/friends/requests/pending");
                return result ?? new List<FriendRequestDto>();
            }
            catch
            {
                return new List<FriendRequestDto>();
            }
        }

        public async Task<(bool Success, string Message)> RespondFriendRequestAsync(string requestId, bool accept)
        {
            try
            {
                var res = await _http.PostAsJsonAsync($"{BaseUrl}/api/friends/request/respond", new RespondFriendRequestDto { RequestId = requestId, Accept = accept });
                var body = await res.Content.ReadFromJsonAsync<SimpleResponse>();
                return (res.IsSuccessStatusCode, body?.Message ?? "Response processed.");
            }
            catch (Exception ex)
            {
                return (false, ex.Message);
            }
        }

        public async Task<(bool Success, string Message)> RemoveFriendAsync(string friendUserId)
        {
            try
            {
                var res = await _http.DeleteAsync($"{BaseUrl}/api/friends/remove/{friendUserId}");
                var body = await res.Content.ReadFromJsonAsync<SimpleResponse>();
                return (res.IsSuccessStatusCode, body?.Message ?? "Friend removed.");
            }
            catch (Exception ex)
            {
                return (false, ex.Message);
            }
        }

        public async Task<(bool Success, string Message)> ToggleMuteFriendAsync(string friendUserId, bool mute)
        {
            try
            {
                var res = await _http.PostAsJsonAsync($"{BaseUrl}/api/friends/toggle-mute", new ToggleMuteRequest { FriendUserId = friendUserId, Mute = mute });
                var body = await res.Content.ReadFromJsonAsync<SimpleResponse>();
                return (res.IsSuccessStatusCode, body?.Message ?? "Mute setting updated.");
            }
            catch (Exception ex)
            {
                return (false, ex.Message);
            }
        }

        public async Task<(bool Success, string Message)> UpdateSettingsAsync(bool shareStatus, bool allowRequests, bool showDuration)
        {
            try
            {
                var res = await _http.PostAsJsonAsync($"{BaseUrl}/api/settings", new UpdateSettingsRequest { ShareStatus = shareStatus, AllowFriendRequests = allowRequests, ShowPlayingDuration = showDuration });
                var body = await res.Content.ReadFromJsonAsync<SimpleResponse>();
                return (res.IsSuccessStatusCode, body?.Message ?? "Settings updated.");
            }
            catch (Exception ex)
            {
                return (false, ex.Message);
            }
        }

        private class SimpleResponse
        {
            public string Message { get; set; } = string.Empty;
        }
    }
}
