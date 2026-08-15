namespace PubgConnect.Client.Views
{
    public partial class RegisterView : System.Windows.Controls.UserControl
    {
        public RegisterView()
        {
            InitializeComponent();
        }

        private void PasswordBox_PasswordChanged(object sender, System.Windows.RoutedEventArgs e)
        {
            if (DataContext is ViewModels.RegisterViewModel vm && sender is System.Windows.Controls.PasswordBox pb)
            {
                vm.Password = pb.Password;
            }
        }
    }
}
