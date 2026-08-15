namespace PubgConnect.Client.Views
{
    public partial class LoginView : System.Windows.Controls.UserControl
    {
        public LoginView()
        {
            InitializeComponent();
        }

        private void PasswordBox_PasswordChanged(object sender, System.Windows.RoutedEventArgs e)
        {
            if (DataContext is ViewModels.LoginViewModel vm && sender is System.Windows.Controls.PasswordBox pb)
            {
                vm.Password = pb.Password;
            }
        }
    }
}
