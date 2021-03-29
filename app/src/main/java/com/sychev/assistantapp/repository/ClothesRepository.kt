package com.sychev.assistantapp.repository

import com.sychev.assistantapp.network.model.DetectedClothesDto

interface ClothesRepository {
    suspend fun sendDetectedClothes(detectedClothes: String): List<DetectedClothesDto>
}