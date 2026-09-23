package com.tangem.grow.datasource.collectibles.models.response.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

/**
 * A collectible sitting in a machine's prize pool, as served by GET /api/v1/machine/{externalId}/nfts.
 *
 * @property insuredValue appraised value **in cents** — 240 means $2.40.
 * @property gradingCompany grader that slabbed the card (e.g. `PSA`); absent for an ungraded collectible.
 */
@JsonClass(generateAdapter = true)
data class MachineNftDTO(
    @Json(name = "id")
    val id: String,
    @Json(name = "nft_address")
    val nftAddress: String,
    @Json(name = "name")
    val name: String,
    @Json(name = "description")
    val description: String?,
    @Json(name = "rarity")
    val rarity: String?,
    @Json(name = "gradingCompany")
    val gradingCompany: String?,
    @Json(name = "gradeNum")
    val gradeNum: String?,
    @Json(name = "image")
    val image: String?,
    @Json(name = "insured_value")
    val insuredValue: BigDecimal?,
)