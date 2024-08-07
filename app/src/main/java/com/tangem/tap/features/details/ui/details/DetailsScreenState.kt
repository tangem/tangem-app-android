package com.tangem.tap.features.details.ui.details

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.tangem.core.ui.event.StateEvent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.wallet.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class DetailsScreenState(
    val elements: ImmutableList<SettingsItem>,
    val tangemLinks: ImmutableList<SocialNetworkLink>,
    val tangemVersion: String,
    val showSnackbar: StateEvent<TextReference>,
    val onSocialNetworkClick: (SocialNetworkLink) -> Unit,
) {
    val appNameRes: Int = R.string.tangem_app_name
}

@Immutable
internal sealed class SettingsItem(
    @DrawableRes val iconResId: Int,
    val title: TextReference,
    val subtitle: TextReference? = null,
    val isLarge: Boolean = false,
) {

    abstract val onClick: () -> Unit

    open val showProgress: Boolean = false

    data class WalletConnect(
        override val onClick: () -> Unit,
    ) : SettingsItem(
        iconResId = R.drawable.ic_wallet_connect_24,
        title = resourceReference(R.string.wallet_connect_title),
        subtitle = resourceReference(R.string.wallet_connect_subtitle),
        isLarge = true,
    )

    data class AddWallet(
        override val showProgress: Boolean,
        override val onClick: () -> Unit,
    ) : SettingsItem(
        iconResId = R.drawable.ic_plus_24,
        title = resourceReference(R.string.user_wallet_list_add_button),
    )

    data class ScanWallet(
        override val showProgress: Boolean,
        override val onClick: () -> Unit,
    ) : SettingsItem(
        iconResId = R.drawable.ic_plus_24,
        title = resourceReference(R.string.scan_card_settings_button),
    )

    data class LinkMoreCards(
        override val onClick: () -> Unit,
    ) : SettingsItem(
        iconResId = R.drawable.ic_more_cards_24,
        title = resourceReference(R.string.details_row_title_create_backup),
    )

    data class CardSettings(
        override val onClick: () -> Unit,
    ) : SettingsItem(
        iconResId = R.drawable.ic_card_settings_24,
        title = resourceReference(R.string.card_settings_title),
    )

    data class AppSettings(
        override val onClick: () -> Unit,
    ) : SettingsItem(
        iconResId = R.drawable.ic_settings,
        title = resourceReference(R.string.app_settings_title),
    )

    data class Chat(
        override val onClick: () -> Unit,
    ) : SettingsItem(
        iconResId = R.drawable.ic_chat_24,
        title = resourceReference(R.string.details_chat),
    )

    data class SendFeedback(
        override val onClick: () -> Unit,
    ) : SettingsItem(
        iconResId = R.drawable.ic_comment_24,
        title = resourceReference(R.string.details_row_title_contact_to_support),
    )

    data class ReferralProgram(
        override val onClick: () -> Unit,
    ) : SettingsItem(
        iconResId = R.drawable.ic_add_friends_24,
        title = resourceReference(R.string.details_referral_title),
    )

    data class TermsOfService(
        override val onClick: () -> Unit,
    ) : SettingsItem(
        iconResId = R.drawable.ic_text_24,
        title = resourceReference(R.string.disclaimer_title),
    )

    data class TesterMenu(
        override val onClick: () -> Unit,
    ) : SettingsItem(
        iconResId = R.drawable.ic_alert_24,
        title = resourceReference(R.string.tester_menu),
    )
}

@Immutable
internal data class SocialNetworkLink(
    val network: SocialNetwork,
    val url: String,
)

internal sealed class EventError {
    object Empty : EventError()
}

sealed class SocialNetwork(val id: String, val iconRes: Int) {
    object Twitter : SocialNetwork("Twitter", R.drawable.ic_twitter_24)
    object Telegram : SocialNetwork("Telegram", R.drawable.ic_telegram_24)
    object Discord : SocialNetwork("Discord", R.drawable.ic_discord_24)
    object Reddit : SocialNetwork("Reddit", R.drawable.ic_reddit_24)
    object Instagram : SocialNetwork("Instagram", R.drawable.ic_instagram_24)
    object GitHub : SocialNetwork("GitHub", R.drawable.ic_github_24)
    object Facebook : SocialNetwork("Facebook", R.drawable.ic_facebook_24)
    object LinkedIn : SocialNetwork("LinkedIn", R.drawable.ic_linkedin_24)
    object YouTube : SocialNetwork("YouTube", R.drawable.ic_youtube_24)
}

internal object TangemSocialAccounts {
    val accountsEn: ImmutableList<SocialNetworkLink> = persistentListOf(
        SocialNetworkLink(SocialNetwork.Twitter, "https://x.com/tangem"),
        SocialNetworkLink(SocialNetwork.Telegram, "https://t.me/tangem_chat"),
        SocialNetworkLink(SocialNetwork.Discord, "https://discord.gg/tangem"),
        SocialNetworkLink(SocialNetwork.Reddit, "https://www.reddit.com/r/Tangem/"),
        SocialNetworkLink(SocialNetwork.Instagram, "https://www.instagram.com/tangemwallet"),
        SocialNetworkLink(SocialNetwork.GitHub, "https://github.com/tangem"),
        SocialNetworkLink(SocialNetwork.Facebook, "https://www.facebook.com/tangemwallet"),
        SocialNetworkLink(SocialNetwork.LinkedIn, "https://www.linkedin.com/company/tangem"),
        SocialNetworkLink(SocialNetwork.YouTube, "https://youtube.com/@tangem_official"),
    )
    val accountsRu: ImmutableList<SocialNetworkLink> = persistentListOf(
        SocialNetworkLink(SocialNetwork.Twitter, "https://x.com/tangem"),
        SocialNetworkLink(SocialNetwork.Telegram, "https://t.me/tangem_chat_ru"),
        SocialNetworkLink(SocialNetwork.Discord, "https://discord.gg/tangem"),
        SocialNetworkLink(SocialNetwork.Reddit, "https://www.reddit.com/r/Tangem/"),
        SocialNetworkLink(SocialNetwork.Instagram, "https://www.instagram.com/tangemwallet"),
        SocialNetworkLink(SocialNetwork.GitHub, "https://github.com/tangem"),
        SocialNetworkLink(SocialNetwork.Facebook, "https://www.facebook.com/tangemwallet"),
        SocialNetworkLink(SocialNetwork.LinkedIn, "https://www.linkedin.com/company/tangem"),
        SocialNetworkLink(SocialNetwork.YouTube, "https://youtube.com/@tangem_official"),
    )
}