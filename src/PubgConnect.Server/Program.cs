using System;
using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.SignalR;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Hosting;
using PubgConnect.Server.Hubs;
using PubgConnect.Server.Services;
using PubgConnect.Shared;

Environment.SetEnvironmentVariable("DOTNET_USE_POLLING_FILE_WATCHER", "true");

var builder = WebApplication.CreateBuilder(args);

// Add services
builder.Services.AddSingleton<IUserService, UserService>();
builder.Services.AddHostedService<HeartbeatMonitorService>();
builder.Services.AddHostedService<LanDiscoveryServer>();

// Configure SignalR for Cloud Concurrency and Keep-Alive
builder.Services.AddSignalR(options =>
{
    options.EnableDetailedErrors = true;
    options.KeepAliveInterval = TimeSpan.FromSeconds(10);
    options.ClientTimeoutInterval = TimeSpan.FromSeconds(30);
    options.HandshakeTimeout = TimeSpan.FromSeconds(15);
    options.MaximumParallelInvocationsPerClient = 5;
});

builder.Services.AddCors(options =>
{
    options.AddDefaultPolicy(policy =>
    {
        policy.AllowAnyHeader()
              .AllowAnyMethod()
              .SetIsOriginAllowed(_ => true)
              .AllowCredentials();
    });
});

var app = builder.Build();

app.UseCors();

// SignalR Hub Endpoint
app.MapHub<StatusHub>("/hub/status");

// Authentication REST APIs
app.MapPost("/api/auth/register", (RegisterRequest req, IUserService userService) =>
{
    var res = userService.Register(req);
    return res.Success ? Results.Ok(res) : Results.BadRequest(res);
});

app.MapPost("/api/auth/login", (LoginRequest req, IUserService userService) =>
{
    var res = userService.Login(req);
    return res.Success ? Results.Ok(res) : Results.BadRequest(res);
});

app.MapGet("/api/auth/me", (HttpContext ctx, IUserService userService) =>
{
    var userId = ctx.Request.Headers["Authorization"].ToString().Replace("Bearer ", "");
    var user = userService.GetUserById(userId);
    return user != null ? Results.Ok(user) : Results.Unauthorized();
});

// Friend Management REST APIs
app.MapGet("/api/friends", (HttpContext ctx, IUserService userService) =>
{
    var userId = ctx.Request.Headers["Authorization"].ToString().Replace("Bearer ", "");
    if (string.IsNullOrEmpty(userId)) return Results.Unauthorized();
    return Results.Ok(userService.GetFriends(userId));
});

app.MapGet("/api/friends/search", (string friendId, IUserService userService) =>
{
    var user = userService.SearchUserByFriendId(friendId);
    return user != null ? Results.Ok(user) : Results.NotFound(new { Message = "User not found." });
});

app.MapPost("/api/friends/request", async (SendFriendRequestDto req, HttpContext ctx, IUserService userService, IHubContext<StatusHub> hubContext) =>
{
    var senderId = ctx.Request.Headers["Authorization"].ToString().Replace("Bearer ", "");
    if (string.IsNullOrEmpty(senderId) || userService.GetUserById(senderId) == null)
    {
        return Results.Json(
            new { Message = "Session expired. Please sign in again." },
            statusCode: StatusCodes.Status401Unauthorized);
    }

    var result = userService.SendFriendRequest(senderId, req.TargetFriendId);
    if (!result.Success) return Results.BadRequest(new { Message = result.Message });

    // Notify receiver live via SignalR group
    var targetUser = userService.SearchUserByFriendId(req.TargetFriendId);
    if (targetUser != null)
    {
        try
        {
            await hubContext.Clients.Group($"user_{targetUser.Id}").SendAsync(SignalREvents.FriendRequestReceived);
        }
        catch { }
    }

    return Results.Ok(new { Message = result.Message });
});

app.MapPost("/api/friends/request/respond", async (RespondFriendRequestDto req, HttpContext ctx, IUserService userService, IHubContext<StatusHub> hubContext) =>
{
    var userId = ctx.Request.Headers["Authorization"].ToString().Replace("Bearer ", "");
    if (string.IsNullOrEmpty(userId)) return Results.Unauthorized();

    var result = userService.RespondFriendRequest(userId, req.RequestId, req.Accept);
    if (!result.Success) return Results.BadRequest(new { Message = result.Message });

    // Notify both users via SignalR group
    try
    {
        await hubContext.Clients.Group($"user_{userId}").SendAsync(SignalREvents.FriendRequestAccepted);
    }
    catch { }

    return Results.Ok(new { Message = result.Message });
});

app.MapGet("/api/friends/requests/pending", (HttpContext ctx, IUserService userService) =>
{
    var userId = ctx.Request.Headers["Authorization"].ToString().Replace("Bearer ", "");
    if (string.IsNullOrEmpty(userId)) return Results.Unauthorized();

    return Results.Ok(userService.GetPendingRequests(userId));
});

app.MapDelete("/api/friends/remove/{friendUserId}", async (string friendUserId, HttpContext ctx, IUserService userService, IHubContext<StatusHub> hubContext) =>
{
    var userId = ctx.Request.Headers["Authorization"].ToString().Replace("Bearer ", "");
    if (string.IsNullOrEmpty(userId)) return Results.Unauthorized();

    var result = userService.RemoveFriend(userId, friendUserId);

    // Broadcast removal event to both users' SignalR groups
    try
    {
        await hubContext.Clients.Group($"user_{userId}").SendAsync(SignalREvents.FriendRemoved, friendUserId);
        await hubContext.Clients.Group($"user_{friendUserId}").SendAsync(SignalREvents.FriendRemoved, userId);
    }
    catch { }

    return Results.Ok(new { Message = result.Message });
});

app.MapPost("/api/friends/toggle-mute", (ToggleMuteRequest req, HttpContext ctx, IUserService userService) =>
{
    var userId = ctx.Request.Headers["Authorization"].ToString().Replace("Bearer ", "");
    if (string.IsNullOrEmpty(userId)) return Results.Unauthorized();

    var result = userService.ToggleMuteFriend(userId, req.FriendUserId, req.Mute);
    return Results.Ok(new { Message = result.Message });
});

app.MapPost("/api/settings", (UpdateSettingsRequest req, HttpContext ctx, IUserService userService) =>
{
    var userId = ctx.Request.Headers["Authorization"].ToString().Replace("Bearer ", "");
    if (string.IsNullOrEmpty(userId)) return Results.Unauthorized();

    userService.UpdateUserSettings(userId, req);
    return Results.Ok(new { Message = "Settings updated successfully." });
});

// Recent Activity Endpoint (Cross-platform feed)
app.MapGet("/api/activity", (HttpContext ctx, IUserService userService) =>
{
    var userId = ctx.Request.Headers["Authorization"].ToString().Replace("Bearer ", "");
    if (string.IsNullOrEmpty(userId)) return Results.Unauthorized();

    return Results.Ok(userService.GetRecentActivity(userId, 20));
});

// Mobile Push Notification Token Registration
app.MapPost("/api/notifications/register-device", (FCMDeviceTokenRequest req, HttpContext ctx, IUserService userService) =>
{
    var userId = ctx.Request.Headers["Authorization"].ToString().Replace("Bearer ", "");
    if (string.IsNullOrEmpty(userId)) return Results.Unauthorized();

    userService.RegisterDeviceToken(userId, req.DeviceToken);
    return Results.Ok(new { Message = "Device registered for push notifications." });
});

app.Run();
