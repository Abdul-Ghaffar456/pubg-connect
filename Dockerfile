# Build Stage
FROM mcr.microsoft.com/dotnet/sdk:9.0 AS build
WORKDIR /app

# Copy all source files
COPY src/ src/

# Build and publish
RUN dotnet publish src/PubgConnect.Server/PubgConnect.Server.csproj -c Release -o /app/publish

# Runtime Stage
FROM mcr.microsoft.com/dotnet/aspnet:9.0 AS runtime
WORKDIR /app
COPY --from=build /app/publish .

ENV DOTNET_USE_POLLING_FILE_WATCHER=true
ENV ASPNETCORE_URLS=http://0.0.0.0:10000;http://0.0.0.0:8080;http://0.0.0.0:5000
EXPOSE 10000 8080 5000

ENTRYPOINT ["dotnet", "PubgConnect.Server.dll"]
