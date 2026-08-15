# Stage 1: Build
FROM mcr.microsoft.com/dotnet/sdk:10.0-preview AS build
WORKDIR /app

# Copy csproj files and restore
COPY src/PubgConnect.Shared/PubgConnect.Shared.csproj src/PubgConnect.Shared/
COPY src/PubgConnect.Server/PubgConnect.Server.csproj src/PubgConnect.Server/
RUN dotnet restore src/PubgConnect.Server/PubgConnect.Server.csproj

# Copy source code and build
COPY src/PubgConnect.Shared/ src/PubgConnect.Shared/
COPY src/PubgConnect.Server/ src/PubgConnect.Server/
RUN dotnet publish src/PubgConnect.Server/PubgConnect.Server.csproj -c Release -o /out

# Stage 2: Runtime
FROM mcr.microsoft.com/dotnet/aspnet:10.0-preview AS runtime
WORKDIR /app
COPY --from=build /out .

ENV ASPNETCORE_URLS=http://0.0.0.0:8080
EXPOSE 8080

ENTRYPOINT ["dotnet", "PubgConnect.Server.dll"]
