# Stage 1: Build
FROM mcr.microsoft.com/dotnet/sdk:9.0 AS build
WORKDIR /src

# Copy csproj files and restore
COPY src/PubgConnect.Shared/PubgConnect.Shared.csproj src/PubgConnect.Shared/
COPY src/PubgConnect.Server/PubgConnect.Server.csproj src/PubgConnect.Server/
RUN dotnet restore src/PubgConnect.Server/PubgConnect.Server.csproj

# Copy source code and build
COPY src/PubgConnect.Shared/ src/PubgConnect.Shared/
COPY src/PubgConnect.Server/ src/PubgConnect.Server/

WORKDIR /src/PubgConnect.Server
RUN dotnet publish PubgConnect.Server.csproj -c Release -o /app/publish

# Stage 2: Runtime
FROM mcr.microsoft.com/dotnet/aspnet:9.0 AS runtime
WORKDIR /app
COPY --from=build /app/publish .

ENV ASPNETCORE_URLS=http://0.0.0.0:10000;http://0.0.0.0:8080;http://0.0.0.0:5000
EXPOSE 10000 8080 5000

ENTRYPOINT ["dotnet", "PubgConnect.Server.dll"]
