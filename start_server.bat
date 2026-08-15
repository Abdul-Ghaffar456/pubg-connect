@echo off
title PUBG Connect - Backend Server
echo Starting PUBG Connect Server on http://localhost:5000...
dotnet run --project src\PubgConnect.Server\PubgConnect.Server.csproj --urls "http://localhost:5000"
pause
