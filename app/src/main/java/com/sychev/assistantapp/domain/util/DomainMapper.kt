package com.sychev.assistantapp.domain.util

import com.sychev.assistantapp.domain.model.DetectedClothes

interface DomainMapper <S, DomainModel> {
    fun toDomainModel(model: S): DomainModel

    fun fromDomainModel(domainModel: DomainModel): S
}