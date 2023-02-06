package com.tangem.tap.features.details.ui.details

import androidx.compose.runtime.Immutable
import com.tangem.wallet.R

@Immutable
data class DetailsScreenState(
    val elements: List<SettingsElement>,
    val tangemLinks: List<SocialNetworkLink>,
    val tangemVersion: String,
    val appCurrency: String,
    val onItemsClick: (SettingsElement) -> Unit,
    val onSocialNetworkClick: (SocialNetworkLink) -> Unit,
) {
    val appNameRes: Int = R.string.tangem_app_name
}

@Immutable
enum class SettingsElement(
    val iconRes: Int,
    val titleRes: Int,
) {
    WalletConnect(R.drawable.ic_walletconnect, R.string.wallet_connect_title),
    Chat(R.drawable.ic_chat, R.string.details_chat),
    SendFeedback(R.drawable.ic_comment, R.string.details_row_title_send_feedback),
    ReferralProgram(R.drawable.ic_add_friends, R.string.details_referral_title),
    CardSettings(R.drawable.ic_card_settings, R.string.card_settings_title),
    AppCurrency(R.drawable.ic_currency, R.string.details_row_title_currency),
    AppSettings(R.drawable.ic_settings, R.string.app_settings_title),
    LinkMoreCards(R.drawable.ic_more_cards, R.string.details_row_title_create_backup),
    TermsOfService(R.drawable.ic_text, R.string.disclaimer_title), // General Terms of Service of the App
    PrivacyPolicy(R.drawable.ic_lock_24, R.string.details_row_privacy_policy);
}

@Immutable
data class SocialNetworkLink(
    val network: SocialNetwork,
    val url: String,
)

sealed class SocialNetwork(val id: String, val iconRes: Int) {
    object Telegram : SocialNetwork("Telegram", R.drawable.ic_telegram)
    object Twitter : SocialNetwork("Twitter", R.drawable.ic_twitter)
    object Facebook : SocialNetwork("Facebook", R.drawable.ic_facebook)
    object Instagram : SocialNetwork("Instagram", R.drawable.ic_instagram)
    object GitHub : SocialNetwork("GitHub", R.drawable.ic_github)
    object YouTube : SocialNetwork("YouTube", R.drawable.ic_youtube)
    object LinkedIn : SocialNetwork("LinkedIn", R.drawable.ic_linkedin)
}

object TangemSocialAccounts {
    val accountsEn: List<SocialNetworkLink> = listOf(
        SocialNetworkLink(SocialNetwork.Telegram, "https://t.me/TangemCards"),
        SocialNetworkLink(SocialNetwork.Twitter, "https://twitter.com/tangem"),
        SocialNetworkLink(SocialNetwork.Facebook, "https://m.facebook.com/TangemCards/"),
        SocialNetworkLink(SocialNetwork.Instagram, "https://instagram.com/tangemcards"),
        SocialNetworkLink(SocialNetwork.GitHub, "https://github.com/tangem"),
        SocialNetworkLink(SocialNetwork.YouTube, "https://youtube.com/channel/UCFGwLS7yggzVkP6ozte0m1w"),
        SocialNetworkLink(SocialNetwork.LinkedIn, "https://www.linkedin.com/company/tangem"),
    )
    val accountsRu: List<SocialNetworkLink> = listOf(
        SocialNetworkLink(SocialNetwork.Telegram, "https://t.me/tangem_ru"),
        SocialNetworkLink(SocialNetwork.Twitter, "https://twitter.com/tangem"),
        SocialNetworkLink(SocialNetwork.Facebook, "https://m.facebook.com/TangemCards/"),
        SocialNetworkLink(SocialNetwork.Instagram, "https://instagram.com/tangemcards"),
        SocialNetworkLink(SocialNetwork.GitHub, "https://github.com/tangem"),
        SocialNetworkLink(SocialNetwork.YouTube, "https://youtube.com/channel/UCFGwLS7yggzVkP6ozte0m1w"),
        SocialNetworkLink(SocialNetwork.LinkedIn, "https://www.linkedin.com/company/tangem"),
    )
}
