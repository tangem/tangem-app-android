package com.tangem.grow.datasource.collectibles.models.response.model

import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A collectible sitting in a machine's prize pool, as served by GET /api/v1/machine/{externalId}/nfts.
 *
 * @property insuredValue appraised value **in cents** — 240 means $2.40.
 * @property gradingCompany grader that slabbed the card (e.g. `PSA`); absent for an ungraded collectible.
 */
@Serializable
data class MachineNftDTO(
    @SerialName("id")
    val id: String,
    @SerialName("nft_address")
    val nftAddress: String,
    @SerialName("name")
    val name: String,
    @SerialName("description")
    val description: String?,
    @SerialName("rarity")
    val rarity: String?,
    @SerialName("gradingCompany")
    val gradingCompany: String?,
    @SerialName("gradeNum")
    val gradeNum: String?,
    @SerialName("image")
    val image: String?,
    @SerialName("insured_value")
    val insuredValue: SerializedBigDecimal?,
)