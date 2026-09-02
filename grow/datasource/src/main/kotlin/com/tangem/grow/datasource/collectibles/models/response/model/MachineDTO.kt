package com.tangem.grow.datasource.collectibles.models.response.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

/**
 * A gacha machine (pack) as served by GET /api/v1/machine/machines and GET /api/v1/machine/{externalId}.
 */
@JsonClass(generateAdapter = true)
data class MachineDTO(
    @Json(name = "id")
    val id: String,
    @Json(name = "externalId")
    val externalId: String,
    @Json(name = "name")
    val name: String,
    @Json(name = "shortName")
    val shortName: String,
    @Json(name = "price")
    val price: BigDecimal,
    @Json(name = "fee")
    val fee: BigDecimal,
    @Json(name = "feeAmount")
    val feeAmount: BigDecimal,
    @Json(name = "contains")
    val contains: Int?,
    @Json(name = "instantBuybackPercent")
    val instantBuybackPercent: Int,
    @Json(name = "lowThreshold")
    val lowThreshold: Int,
    @Json(name = "odds")
    val odds: MachineOddsDTO,
    @Json(name = "tierRanges")
    val tierRanges: MachineTierRangesDTO,
    @Json(name = "stock")
    val stock: MachineStockDTO,
    @Json(name = "expectedValue")
    val expectedValue: BigDecimal,
    @Json(name = "image")
    val image: String?,
    @Json(name = "categoryId")
    val categoryId: String,
    @Json(name = "createdAt")
    val createdAt: String,
    @Json(name = "updatedAt")
    val updatedAt: String,
) {
    /** Remaining card count per rarity tier. */
    @JsonClass(generateAdapter = true)
    data class MachineStockDTO(
        @Json(name = "common")
        val common: Int,
        @Json(name = "rare")
        val rare: Int,
        @Json(name = "epic")
        val epic: Int,
        @Json(name = "legendary")
        val legendary: Int?,
    )

    /** Drop chance per rarity tier, in percent. */
    @JsonClass(generateAdapter = true)
    data class MachineOddsDTO(
        @Json(name = "common")
        val common: Int,
        @Json(name = "rare")
        val rare: Int,
        @Json(name = "epic")
        val epic: Int,
        @Json(name = "legendary")
        val legendary: Int?,
    )

    /** Card value range per rarity tier. */
    @JsonClass(generateAdapter = true)
    data class MachineTierRangesDTO(
        @Json(name = "common")
        val common: MachineTierRangeDTO,
        @Json(name = "rare")
        val rare: MachineTierRangeDTO,
        @Json(name = "epic")
        val epic: MachineTierRangeDTO,
        @Json(name = "legendary")
        val legendary: MachineTierRangeDTO?,
    ) {
        @JsonClass(generateAdapter = true)
        data class MachineTierRangeDTO(
            @Json(name = "start")
            val start: Int,
            @Json(name = "end")
            val end: Int,
        )
    }
}