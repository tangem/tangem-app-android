package com.tangem.domain.promo.models

import java.math.BigDecimal

/** Already filtered for the wallet by the backend. */
data class PromoCampaign(
    val id: Int,
    val name: String,
    val isAlreadyEnrolled: Boolean,
    val payout: PromoPayout,
    val matchMode: PromoMatchMode,
    val content: PromoCampaignContent,
)

data class PromoPayout(
    val isNeeded: Boolean,
    val tokens: List<PromoPayoutToken>,
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

/** Texts are already localized by the backend. */
data class PromoCampaignContent(
    val name: String,
    val summary: String?,
    val description: List<PromoDescriptionBlock>,
    val imageUrl: String,
    val tncLink: PromoLink,
    val actionButtonName: String,
)

data class PromoLink(
    val text: String,
    val url: String,
)

sealed interface PromoDescriptionBlock {

    data class Text(
        val title: String?,
        val text: String,
        val link: PromoLink?,
    ) : PromoDescriptionBlock

    data class Items(
        val title: String?,
        val items: List<Item>,
    ) : PromoDescriptionBlock

    data class Table(
        val title: String?,
        val headers: List<String>,
        val rows: List<List<Cell>>,
    ) : PromoDescriptionBlock

    data class Highlight(
        val title: String?,
        val text: String,
    ) : PromoDescriptionBlock

    data class Item(
        val label: String,
        val value: PromoValue,
    )

    sealed interface Cell {
        data class Text(val text: String) : Cell
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