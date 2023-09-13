package com.tangem.tap.features.details.ui.details

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.tangem.core.ui.event.StateEvent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
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
        iconResId = R.drawable.ic_walletconnect,
        title = resourceReference(R.string.wallet_connect_title),
        subtitle = resourceReference(R.string.wallet_connect_subtitle),
        isLarge = true,
    )

    data class AddWallet(
        override val showProgress: Boolean,
        override val onClick: () -> Unit,
    ) : SettingsItem(
        iconResId = R.drawable.ic_plus_24,
        title = stringReference(value = "Add new wallet"),
    )

    data class ScanWallet(
        override val showProgress: Boolean,
        override val onClick: () -> Unit,
    ) : SettingsItem(
        iconResId = R.drawable.ic_plus_24,
        title = stringReference(value = "Scan new wallet"),
    )

    data class LinkMoreCards(
        override val onClick: () -> Unit,
    ) : SettingsItem(
        iconResId = R.drawable.ic_more_cards,
        title = resourceReference(R.string.details_row_title_create_backup),
    )

    data class CardSettings(
        override val onClick: () -> Unit,
    ) : SettingsItem(
        iconResId = R.drawable.ic_card_settings,
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
        iconResId = R.drawable.ic_chat,
        title = resourceReference(R.string.details_chat),
    )

    data class SendFeedback(
        override val onClick: () -> Unit,
    ) : SettingsItem(
        iconResId = R.drawable.ic_comment,
        title = resourceReference(R.string.details_row_title_send_feedback),
    )

    data class ReferralProgram(
        override val onClick: () -> Unit,
    ) : SettingsItem(
        iconResId = R.drawable.ic_add_friends,
        title = resourceReference(R.string.details_referral_title),
    )

    data class TermsOfService(
        override val onClick: () -> Unit,
    ) : SettingsItem(
        iconResId = R.drawable.ic_text,
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
    data class DemoReferralNotAvailable(val onErrorShow: () -> Unit) : EventError()
}

sealed class SocialNetwork(val id: String, val iconRes: Int) {
    object Telegram : SocialNetwork("Telegram", R.drawable.ic_telegram)
    object Twitter : SocialNetwork("Twitter", R.drawable.ic_twitter)
    object Facebook : SocialNetwork("Facebook", R.drawable.ic_facebook)
    object Instagram : SocialNetwork("Instagram", R.drawable.ic_instagram)
    object GitHub : SocialNetwork("GitHub", R.drawable.ic_github)
    object YouTube : SocialNetwork("YouTube", R.drawable.ic_youtube)
    object LinkedIn : SocialNetwork("LinkedIn", R.drawable.ic_linkedin)
    object Discord : SocialNetwork("Discord", R.drawable.ic_discord)
}

internal object TangemSocialAccounts {
    val accountsEn: ImmutableList<SocialNetworkLink> = persistentListOf(
        SocialNetworkLink(SocialNetwork.Telegram, "https://t.me/tangem_chat"),
        SocialNetworkLink(SocialNetwork.Twitter, "https://twitter.com/tangem"),
        SocialNetworkLink(SocialNetwork.Facebook, "https://m.facebook.com/TangemCards/"),
        SocialNetworkLink(SocialNetwork.Instagram, "https://instagram.com/tangemcards"),
        SocialNetworkLink(SocialNetwork.GitHub, "https://github.com/tangem"),
        SocialNetworkLink(SocialNetwork.YouTube, "https://youtube.com/channel/UCFGwLS7yggzVkP6ozte0m1w"),
        SocialNetworkLink(SocialNetwork.LinkedIn, "https://www.linkedin.com/company/tangem"),
        SocialNetworkLink(SocialNetwork.Discord, "https://discord.gg/7AqTVyqdGS"),
    )
    val accountsRu: ImmutableList<SocialNetworkLink> = persistentListOf(
        SocialNetworkLink(SocialNetwork.Telegram, "https://t.me/tangem_chat_ru"),
        SocialNetworkLink(SocialNetwork.Twitter, "https://twitter.com/tangem"),
        SocialNetworkLink(SocialNetwork.Facebook, "https://m.facebook.com/TangemCards/"),
        SocialNetworkLink(SocialNetwork.Instagram, "https://instagram.com/tangemcards"),
        SocialNetworkLink(SocialNetwork.GitHub, "https://github.com/tangem"),
        SocialNetworkLink(SocialNetwork.YouTube, "https://youtube.com/channel/UCFGwLS7yggzVkP6ozte0m1w"),
        SocialNetworkLink(SocialNetwork.LinkedIn, "https://www.linkedin.com/company/tangem"),
        SocialNetworkLink(SocialNetwork.Discord, "https://discord.gg/7AqTVyqdGS"),
    )
}