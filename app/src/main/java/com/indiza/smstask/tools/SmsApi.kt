package com.indiza.smstask.tools

import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface SmsApi {
    @GET("sms/pending")
    suspend fun getPendingSms(@Query("limit") limit: Int = 200): List<SmsPendingResponse>

    @GET("sms/failed")
    suspend fun getFailesSms(@Query("limit") limit: Int = 200): List<SmsPendingResponse>

    @POST("sms/{id}/mark-sent")
    suspend fun markSmsAsSent(@Path("id") id: Long): ApiResponse

    @POST("sms/{id}/mark-failed")
    suspend fun markSmsAsFailed(@Path("id") id: Long): ApiResponse

    @POST("sms/{id}/mark-delivered")
    suspend fun markSmsAsDelivered(@Path("id") id: Long): ApiResponse

    @POST("sms/{id}/mark-swiped")
    suspend fun markSmsAsSwiped(@Path("id") id: Long): ApiResponse


    // NOUVEAUX ENDPOINTS
    @GET("stats")
    suspend fun getStats(@Query("period") period: String): StatsResponse


    @GET("stats/all")
    suspend fun getAllStats(): AllStatsResponse

    @GET("sms/recent")
    suspend fun getRecentMessages(@Query("limit") limit: Int = 10): List<RecentMessageResponse>

}