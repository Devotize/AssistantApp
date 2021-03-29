package com.sychev.assistantapp.network

import com.sychev.assistantapp.network.response.DetectedClothesResponse
import retrofit2.http.POST
import retrofit2.http.Query


interface ClothesService {
    @POST
    fun sendDetectedClothes(@Query("clothes") clothes: String): DetectedClothesResponse
}