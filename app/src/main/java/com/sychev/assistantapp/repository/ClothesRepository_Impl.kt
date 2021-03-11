package com.sychev.assistantapp.repository

import com.sychev.assistantapp.network.ClothesService
import com.sychev.assistantapp.network.model.DetectedClothesDto

class ClothesRepository_Impl(
    val service: ClothesService
): ClothesRepository {
    override suspend fun sendDetectedClothes(detectedClothes: String): List<DetectedClothesDto> {
        return service.sendDetectedClothes(detectedClothes).detectedClothes
    }
}