package com.tangem.tap.features.details.ui.details

import com.tangem.wallet.R

data class DetailsScreenState(
    val elements: List<SettingsElement>,
    val tangemLinks: List<TangemLink>,
    val tangemVersion: String,
    val appCurrency: String,
    val onItemsClick: (SettingsElement) -> Unit,
    val onSocialNetworkClick: (String) -> Unit,
) {
    val appNameRes: Int = R.string.app_name
}

enum class SettingsElement(
    val iconRes: Int,
    val titleRes: Int,
) {
    WalletConnect(R.drawable.ic_walletconnect, R.string.wallet_connect_title),
    Chat(R.drawable.ic_chat, R.string.details_ask_a_question),
    SendFeedback(R.drawable.ic_comment, R.string.details_row_title_send_feedback),
    CardSettings(R.drawable.ic_card_settings, R.string.card_settings_title),
    AppCurrency(R.drawable.ic_currency, R.string.details_row_title_currency),
    AppSettings(R.drawable.ic_settings, R.string.app_settings_title),
    LinkMoreCards(R.drawable.ic_more_cards, R.string.details_row_title_create_backup),
    TermsOfService(R.drawable.ic_text, R.string.disclaimer_title),
    PrivacyPolicy(R.drawable.ic_lock, R.string.details_row_privacy_policy),
    ;
}

data class TangemLink(
    val iconRes: Int,
    val url: String,
)

object TangemSocialAccounts {
    val accountsEn: List<TangemLink> = listOf(
        TangemLink(R.drawable.ic_telegram, "https://t.me/TangemCards"),
        TangemLink(R.drawable.ic_twitter, "https://twitter.com/tangem"),
        TangemLink(R.drawable.ic_facebook, "https://m.facebook.com/TangemCards/"),
        TangemLink(R.drawable.ic_instagram, "https://instagram.com/tangemcards"),
        TangemLink(R.drawable.ic_github, "https://github.com/tangem"),
        TangemLink(R.drawable.ic_youtube, "https://youtube.com/channel/UCFGwLS7yggzVkP6ozte0m1w"),
        TangemLink(R.drawable.ic_linkedin, "https://www.linkedin.com/company/tangem"),
    )
    val accountsRu: List<TangemLink> = listOf(
        TangemLink(R.drawable.ic_telegram, "https://t.me/tangem_ru"),
        TangemLink(R.drawable.ic_twitter, "https://twitter.com/tangem"),
        TangemLink(R.drawable.ic_facebook, "https://m.facebook.com/TangemCards/"),
        TangemLink(R.drawable.ic_instagram, "https://instagram.com/tangemcards"),
        TangemLink(R.drawable.ic_github, "https://github.com/tangem"),
        TangemLink(R.drawable.ic_youtube, "https://youtube.com/channel/UCFGwLS7yggzVkP6ozte0m1w"),
        TangemLink(R.drawable.ic_linkedin, "https://www.linkedin.com/company/tangem"),
    )
}