package com.example.voicedrop.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface UploadApi {

    @Multipart
    @POST("api/upload")
    suspend fun uploadRecording(
        @Part("name") name: RequestBody,
        @Part("timestamp") timestamp: RequestBody,
        @Part zipFile: MultipartBody.Part
    ): Response<UploadResponse>
}
