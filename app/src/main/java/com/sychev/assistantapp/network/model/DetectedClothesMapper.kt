package com.sychev.assistantapp.network.model

import com.sychev.assistantapp.domain.model.DetectedClothes
import com.sychev.assistantapp.domain.util.DomainMapper

class DetectedClothesMapper: DomainMapper<DetectedClothesDto, DetectedClothes> {
    override fun toDomainModel(model: DetectedClothesDto): DetectedClothes {
        TODO()
    }

    override fun fromDomainModel(domainModel: DetectedClothes): DetectedClothesDto {
        TODO()
    }
}