package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

@JsonClass(generateAdapter = true)
data class CoinsSettingsResponse(
    @Json(name = "staking") val staking: StakingSettingsDTO?,
)

@JsonClass(generateAdapter = true)
data class StakingSettingsDTO(
    @Json(name = "vaults") val vaults: List<VaultSettingsDTO> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class VaultSettingsDTO(
    @Json(name = "vaultAddress") val vaultAddress: String,
    @Json(name = "limit") val limit: BigDecimal?,
    @Json(name = "coefficient") val coefficient: BigDecimal?,
)