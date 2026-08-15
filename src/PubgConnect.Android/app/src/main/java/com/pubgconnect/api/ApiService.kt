package com.pubgconnect.api

import com.pubgconnect.models.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // Auth endpoints
    @POST("api/auth/register")
    suspend fun register(@Body req: RegisterRequest): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(@Body req: LoginRequest): Response<AuthResponse>

    @GET("api/auth/me")
    suspend fun getMe(@Header("Authorization") token: String): Response<UserDto>

    // Friends endpoints
    @GET("api/friends")
    suspend fun getFriends(@Header("Authorization") token: String): Response<List<FriendDto>>

    @GET("api/friends/search")
    suspend fun searchFriend(
        @Header("Authorization") token: String,
        @Query("friendId") friendId: String
    ): Response<UserDto>

    @POST("api/friends/request")
    suspend fun sendFriendRequest(
        @Header("Authorization") token: String,
        @Body req: SendFriendRequestDto
    ): Response<SimpleResponse>

    @GET("api/friends/requests/pending")
    suspend fun getPendingRequests(@Header("Authorization") token: String): Response<List<FriendRequestDto>>

    @POST("api/friends/request/respond")
    suspend fun respondFriendRequest(
        @Header("Authorization") token: String,
        @Body req: RespondFriendRequestDto
    ): Response<SimpleResponse>

    @DELETE("api/friends/remove/{friendUserId}")
    suspend fun removeFriend(
        @Header("Authorization") token: String,
        @Path("friendUserId") friendUserId: String
    ): Response<SimpleResponse>

    @POST("api/friends/toggle-mute")
    suspend fun toggleMuteFriend(
        @Header("Authorization") token: String,
        @Body req: ToggleMuteRequest
    ): Response<SimpleResponse>

    // Settings endpoint
    @POST("api/settings")
    suspend fun updateSettings(
        @Header("Authorization") token: String,
        @Body req: UpdateSettingsRequest
    ): Response<SimpleResponse>

    // Activity feed endpoint
    @GET("api/activity")
    suspend fun getActivity(@Header("Authorization") token: String): Response<List<ActivityItemDto>>

    // Push notification token registration
    @POST("api/notifications/register-device")
    suspend fun registerDeviceToken(
        @Header("Authorization") token: String,
        @Body req: FCMDeviceTokenRequest
    ): Response<SimpleResponse>
}
