package com.sychev.assistantapp.network.model

import com.google.gson.annotations.SerializedName

data class DetectedClothesDto(
    @SerializedName("url")
    val url: String? = null,
    @SerializedName("image")
    val image: String? = null,
    @SerializedName("name")
    val clothesName: String? = null
) {
}