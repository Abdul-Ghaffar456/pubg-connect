using System;
using System.Globalization;
using System.Windows;
using System.Windows.Data;
using System.Windows.Media;
using PubgConnect.Shared;

namespace PubgConnect.Client.Converters
{
    public class StatusToColorConverter : IValueConverter
    {
        private static readonly BrushConverter BrushConv = new();

        public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
        {
            if (value is UserStatus status)
            {
                switch (status)
                {
                    case UserStatus.PlayingPubg:
                        return (SolidColorBrush)BrushConv.ConvertFrom("#10B981")!; // Neon Emerald Green
                    case UserStatus.Online:
                        return (SolidColorBrush)BrushConv.ConvertFrom("#3B82F6")!; // Vibrant Blue
                    case UserStatus.Offline:
                    default:
                        return (SolidColorBrush)BrushConv.ConvertFrom("#6B7280")!; // Cool Gray
                }
            }
            return new SolidColorBrush(Colors.Gray);
        }

        public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture) => throw new NotImplementedException();
    }

    public class StatusToTextConverter : IValueConverter
    {
        public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
        {
            if (value is UserStatus status)
            {
                switch (status)
                {
                    case UserStatus.PlayingPubg:
                        return "🟢 Playing PUBG Mobile";
                    case UserStatus.Online:
                        return "🔵 Online";
                    case UserStatus.Offline:
                    default:
                        return "⚫ Offline";
                }
            }
            return "⚫ Offline";
        }

        public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture) => throw new NotImplementedException();
    }

    public class BoolToVisConverter : IValueConverter
    {
        public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
        {
            bool boolVal;
            if (value is bool b)
            {
                boolVal = b;
            }
            else if (value is string str)
            {
                boolVal = !string.IsNullOrWhiteSpace(str);
            }
            else if (value is int i)
            {
                boolVal = i > 0;
            }
            else
            {
                boolVal = value != null;
            }

            if (parameter?.ToString() == "Invert") boolVal = !boolVal;
            return boolVal ? Visibility.Visible : Visibility.Collapsed;
        }

        public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture) => throw new NotImplementedException();
    }

    public class PlatformToTextConverter : IValueConverter
    {
        public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
        {
            if (value is PlatformType platform)
            {
                switch (platform)
                {
                    case PlatformType.GameLoop:
                        return "🖥 GameLoop";
                    case PlatformType.Android:
                        return "📱 Android";
                    default:
                        return "";
                }
            }
            return "";
        }

        public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture) => throw new NotImplementedException();
    }

    public class InverseBoolConverter : IValueConverter
    {
        public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
        {
            return value is bool b ? !b : true;
        }

        public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture) => throw new NotImplementedException();
    }
}
