package com.sychev.assistantapp.presentation.utils

import com.sychev.assistantapp.network.model.DetectedClothesDto

class FakeDetectedClothesData {
    companion object{
        val detectedObjects: ArrayList<DetectedClothesDto>
            get() {
                return arrayListOf(
                    DetectedClothesDto("https://www.wildberries.ru/catalog/11485685/detail.aspx?targetUrl=XS", "https://images.wbstatic.net/c516x688/new/11480000/11485685-1.jpg", "ALIVERO / Женские кожаные куртки / Куртка демисезонная женская / Куртка женская / Пуховик кожаный"),
                    DetectedClothesDto("https://www.wildberries.ru/catalog/5582655/detail.aspx?targetUrl=XS", "https://images.wbstatic.net/c516x688/new/5580000/5582655-1.jpg", "Город Горький / Анорак/Анорак мужской"),
                    DetectedClothesDto("https://www.wildberries.ru/catalog/10983191/detail.aspx?targetUrl=XS", "https://images.wbstatic.net/c516x688/new/10980000/10983191-1.jpg", "Nikolom / Куртка мужская"),
                    DetectedClothesDto("https://www.wildberries.ru/catalog/16334844/detail.aspx?targetUrl=XS", "https://images.wbstatic.net/c516x688/new/16330000/16334844-1.jpg", "Adrecom / Женская кожаная косуха / Куртка кожаная / Куртка короткая")
                )
            }

    }
}