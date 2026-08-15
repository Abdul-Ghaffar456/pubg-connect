using System;
using System.Collections.ObjectModel;
using System.Linq;
using System.Threading.Tasks;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using PubgConnect.Client.Services;
using PubgConnect.Shared;

namespace PubgConnect.Client.ViewModels
{
    public partial class FriendsViewModel : ViewModelBase
    {
        private readonly IApiClient _apiClient;

        [ObservableProperty]
        private ObservableCollection<FriendDto> _friends = new();

        [ObservableProperty]
        private int _onlineCount;

        [ObservableProperty]
        private int _totalCount;

        [ObservableProperty]
        private bool _isBusy;

        public FriendsViewModel(IApiClient apiClient)
        {
            _apiClient = apiClient;
        }

        public async Task LoadFriendsAsync()
        {
            IsBusy = true;
            var list = await _apiClient.GetFriendsAsync();
            IsBusy = false;

            UpdateFriendsList(list);
        }

        public void UpdateFriendsList(System.Collections.Generic.IEnumerable<FriendDto> friendList)
        {
            var ordered = friendList
                .OrderByDescending(f => f.Status != UserStatus.Offline)
                .ThenBy(f => f.Username)
                .ToList();

            Friends = new ObservableCollection<FriendDto>(ordered);
            OnlineCount = ordered.Count(f => f.Status != UserStatus.Offline);
            TotalCount = ordered.Count;
        }

        [RelayCommand]
        private async Task ToggleMuteFriendAsync(FriendDto? friend)
        {
            if (friend == null) return;

            bool newMuteState = !friend.IsNotificationMuted;
            friend.IsNotificationMuted = newMuteState;

            await _apiClient.ToggleMuteFriendAsync(friend.Id, newMuteState);
        }

        [RelayCommand]
        private async Task RemoveFriendAsync(FriendDto? friend)
        {
            if (friend == null) return;

            var res = await _apiClient.RemoveFriendAsync(friend.Id);
            if (res.Success)
            {
                Friends.Remove(friend);
                OnlineCount = Friends.Count(f => f.Status != UserStatus.Offline);
                TotalCount = Friends.Count;
            }
        }
    }
}
