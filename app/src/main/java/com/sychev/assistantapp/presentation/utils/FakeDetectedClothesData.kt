package com.sychev.assistantapp.presentation.utils

import com.sychev.assistantapp.network.model.DetectedClothesDto

class FakeDetectedClothesData {
    companion object{
        val detectedObjects: List<DetectedClothesDto>
            get() {
                return listOf(
                    DetectedClothesDto("https://www.wildberries.ru/catalog/11485685/detail.aspx?targetUrl=XS"),
                    DetectedClothesDto("https://www.wildberries.ru/catalog/5582655/detail.aspx?targetUrl=XS"),
                    DetectedClothesDto("https://www.wildberries.ru/catalog/10983191/detail.aspx?targetUrl=XS"),
                    DetectedClothesDto("https://www.wildberries.ru/catalog/16334844/detail.aspx?targetUrl=XS")
                )
            }

    }
}