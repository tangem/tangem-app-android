package com.tangem.domain.promo.models

import java.math.BigDecimal

/**
 * Campaign the backend returns for a single wallet. Whether it is available is decided on the app side from
 * [status] and [eligibility].
 */
data class PromoCampaign(
    val id: Int,
    val name: String,
    val status: PromoCampaignStatus,
    val timeline: PromoTimeline,
    val isAlreadyEnrolled: Boolean,
    val eligibility: PromoEligibility,
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

sealed interface PromoEligibility {

    data object Public : PromoEligibility

    data class Private(val conditions: PromoEligibilityConditions) : PromoEligibility
}

/** Every condition is optional; a `null` one is not checked. Versions are `major.minor[.fix]` strings. */
data class PromoEligibilityConditions(
    val walletIds: List<String>?,
    val walletType: PromoWalletType?,
    val batchIds: List<String>?,
    val countryExcluded: List<String>?,
    val minAndroidVersion: String?,
    val maxAndroidVersion: String?,
)

enum class PromoWalletType {
    Mobile,
    Card,
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