package com.tangem.domain.promo.models

import java.math.BigDecimal

/**
 * Campaign the backend reports as eligible for a single wallet.
 */
data class PromoCampaign(
    val name: String,
    val status: PromoCampaignStatus,
    val timeline: PromoTimeline,
    val isAlreadyEnrolled: Boolean,
    val payout: PromoPayout,
    val matchMode: PromoMatchMode,
    val content: PromoCampaignContent,
)

data class PromoPayout(
    val isNeeded: Boolean,
    val tokens: List<PromoPayoutToken>,
)

enum class PromoCampaignStatus {
    Draft,
    Active,
    Paused,
    Archived,
}

/** How the device's wallets are registered on enrollment. */
enum class PromoMatchMode {
    Strict,
    LinkedWallet,
}

/** Every `*Key` is a localization key, not display text. */
data class PromoCampaignContent(
    val nameKey: String,
    val description: List<PromoDescriptionBlock>,
    val imageUrl: String,
    val learnMoreLink: PromoLink?,
    val tncLink: PromoLink,
    val actionButtonNameKey: String,
)

data class PromoLink(
    val textKey: String,
    val url: String,
)

sealed interface PromoDescriptionBlock {

    data class Text(
        val titleKey: String?,
        val textKey: String,
    ) : PromoDescriptionBlock

    data class Items(
        val titleKey: String?,
        val items: List<Item>,
    ) : PromoDescriptionBlock

    data class Table(
        val titleKey: String?,
        val headerKeys: List<String>,
        val rows: List<List<Cell>>,
    ) : PromoDescriptionBlock

    data class Highlight(
        val titleKey: String?,
        val textKey: String,
    ) : PromoDescriptionBlock

    data class Item(
        val labelKey: String,
        val value: PromoValue,
    )

    sealed interface Cell {
        data class Text(val textKey: String) : Cell
        data class Value(val value: PromoValue) : Cell
    }
}

/** Value typed by the backend's `valueType` / table `columnTypes`. */
sealed interface PromoValue {
    data class Text(val text: String) : PromoValue
    data class Number(val value: BigDecimal) : PromoValue

    /** [value] is passed through as sent (e.g. `0.10`); formatting is up to the UI. */
    data class Percent(val value: BigDecimal) : PromoValue
    data class Money(val value: BigDecimal, val currency: String) : PromoValue
}