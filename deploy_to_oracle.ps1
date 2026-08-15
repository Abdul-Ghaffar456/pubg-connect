$KeyPath = "C:\ssh-key-2026-08-15.key"
$HostIp = "84.235.248.234"
$User = "opc"

Write-Host "Fixing Key Permissions..." -ForegroundColor Cyan
icacls $KeyPath /inheritance:r | Out-Null
icacls $KeyPath /grant "$($env:USERNAME):(R)" | Out-Null

$RemoteCommands = @'
echo "=== 1. Opening Port 5000 in OS Firewall ==="
sudo firewall-cmd --zone=public --add-port=5000/tcp --permanent
sudo firewall-cmd --reload
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 5000 -j ACCEPT

echo "=== 2. Installing .NET 10 ==="
if [ ! -d "$HOME/.dotnet" ]; then
    curl -sSL https://dot.net/v1/dotnet-install.sh | bash /dev/stdin --channel 10.0
fi
export DOTNET_ROOT=$HOME/.dotnet
export PATH=$PATH:$HOME/.dotnet

echo "=== 3. Cloning / Updating Repository ==="
cd /home/opc
if [ -d "pubg-connect" ]; then
    cd pubg-connect
    git pull origin main || true
else
    git clone https://github.com/Abdul-Ghaffar456/pubg-connect.git
    cd pubg-connect
fi

echo "=== 4. Setting up Systemd Service for 24/7 Execution ==="
sudo bash -c 'cat <<EOF > /etc/systemd/system/pubg-server.service
[Unit]
Description=PUBG Connect Backend Server
After=network.target

[Service]
WorkingDirectory=/home/opc/pubg-connect/src/PubgConnect.Server
ExecStart=/home/opc/.dotnet/dotnet run --urls "http://0.0.0.0:5000"
Restart=always
RestartSec=10
KillSignal=SIGINT
SyslogIdentifier=pubg-server
User=opc
Environment=ASPNETCORE_ENVIRONMENT=Production

[Install]
WantedBy=multi-user.target
EOF'

sudo systemctl daemon-reload
sudo systemctl enable --now pubg-server
sudo systemctl restart pubg-server
sleep 3
sudo systemctl status pubg-server --no-pager
'@

Write-Host "Connecting to Oracle Cloud VM and executing setup..." -ForegroundColor Green
$sshArgs = @(
    "-o", "StrictHostKeyChecking=no",
    "-o", "UserKnownHostsFile=NUL",
    "-o", "BatchMode=yes",
    "-i", $KeyPath,
    "$User@$HostIp",
    $RemoteCommands
)

& ssh $sshArgs
