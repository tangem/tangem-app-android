package com.tangem.grow.datasource.collectibles.models.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Body for POST /api/v1/buyback/buyback-data */
@Serializable
data class GenerateBuybackDataRequestBody(
    @SerialName("fromAddress")
    val fromAddress: String,
    @SerialName("nftAddress")
    val nftAddress: String,
    @SerialName("toAddress")
    val toAddress: String,
)