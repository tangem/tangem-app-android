package com.tangem.grow.datasource.collectibles.models.response.model

import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A gacha machine (pack) as served by GET /api/v1/machine/machines and GET /api/v1/machine/{externalId}.
 *
 * @property price **in cents** — 5000 means $50.00. So are [fee], [feeAmount], [expectedValue] and
 *   [tierRanges]; the data layer divides them by 100 — see `MachineConverter`.
 */
@Serializable
data class MachineDTO(
    @SerialName("id")
    val id: String,
    @SerialName("externalId")
    val externalId: String,
    @SerialName("name")
    val name: String,
    @SerialName("shortName")
    val shortName: String,
    @SerialName("price")
    val price: SerializedBigDecimal,
    @SerialName("fee")
    val fee: SerializedBigDecimal,
    @SerialName("feeAmount")
    val feeAmount: SerializedBigDecimal,
    @SerialName("contains")
    val contains: Int?,
    @SerialName("instantBuybackPercent")
    val instantBuybackPercent: Int,
    @SerialName("lowThreshold")
    val lowThreshold: Int,
    @SerialName("odds")
    val odds: MachineOddsDTO,
    @SerialName("tierRanges")
    val tierRanges: MachineTierRangesDTO,
    @SerialName("stock")
    val stock: MachineStockDTO,
    @SerialName("expectedValue")
    val expectedValue: SerializedBigDecimal,
    @SerialName("image")
    val image: String?,
    @SerialName("categoryId")
    val categoryId: String,
    @SerialName("createdAt")
    val createdAt: String,
    @SerialName("updatedAt")
    val updatedAt: String,
    @SerialName("openPackMedia")
    val openPackMedia: OpenPackMediaDTO? = null,
) {
    /** Media of the pack opening: the reveal video per theme and the sound played over a skip. */
    @Serializable
    data class OpenPackMediaDTO(
        @SerialName("videoLight")
        val videoLight: String,
        @SerialName("videoDark")
        val videoDark: String,
        @SerialName("skipAnimationAudio")
        val skipAnimationAudio: String,
    )

    /** Remaining card count per rarity tier. */
    @Serializable
    data class MachineStockDTO(
        @SerialName("common")
        val common: Int,
        @SerialName("uncommon")
        val uncommon: Int,
        @SerialName("rare")
        val rare: Int,
        @SerialName("epic")
        val epic: Int?,
    )

    /** Drop chance per rarity tier, in percent. */
    @Serializable
    data class MachineOddsDTO(
        @SerialName("common")
        val common: Int,
        @SerialName("uncommon")
        val uncommon: Int,
        @SerialName("rare")
        val rare: Int,
        @SerialName("epic")
        val epic: Int?,
    )

    /** Card value range per rarity tier. */
    @Serializable
    data class MachineTierRangesDTO(
        @SerialName("common")
        val common: MachineTierRangeDTO,
        @SerialName("uncommon")
        val uncommon: MachineTierRangeDTO,
        @SerialName("rare")
        val rare: MachineTierRangeDTO,
        @SerialName("epic")
        val epic: MachineTierRangeDTO?,
    ) {
        @Serializable
        data class MachineTierRangeDTO(
            @SerialName("start")
            val start: SerializedBigDecimal,
            @SerialName("end")
            val end: SerializedBigDecimal,
        )
    }
}