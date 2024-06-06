package com.tangem.features.details.utils

import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.AppScreen
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.details.entity.DetailsItemUM
import com.tangem.features.details.impl.BuildConfig
import com.tangem.features.details.impl.R
import com.tangem.features.details.routing.DetailsRoute
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import javax.inject.Inject

@ComponentScoped
internal class ItemsBuilder @Inject constructor(
    private val router: Router,
) {

    suspend fun buldAll(isWalletConnectAvailable: Boolean): ImmutableList<DetailsItemUM> = buildList {
        buildWalletConnectBlock(isWalletConnectAvailable)?.let(::add)
        buildUserWalletListBlock().let(::add)
        buildShopBlock().let(::add)
        buildSettingsBlock().let(::add)
        buildSupportBlock().let(::add)
    }.toImmutableList()

    private suspend fun buildWalletConnectBlock(isWalletConnectAvailable: Boolean): DetailsItemUM? {
        return if (isWalletConnectAvailable) {
            DetailsItemUM.WalletConnect(
                onClick = { router.push(DetailsRoute.Screen(AppScreen.WalletConnectSessions)) },
            )
        } else {
            null
        }
    }

    private fun buildUserWalletListBlock(): DetailsItemUM = DetailsItemUM.UserWalletList

    private fun buildShopBlock(): DetailsItemUM = DetailsItemUM.Basic(
        id = "shop",
        items = persistentListOf(
            DetailsItemUM.Basic.Item(
                id = "buy_tangem_wallet",
                title = resourceReference(R.string.details_buy_wallet),
                iconRes = R.drawable.ic_tangem_24,
                onClick = { router.push(DetailsRoute.Url(BUY_TANGEM_URL)) },
            ),
        ),
    )

    private fun buildSettingsBlock(): DetailsItemUM = DetailsItemUM.Basic(
        id = "settings",
        items = buildList {
            DetailsItemUM.Basic.Item(
                id = "app_settings",
                title = resourceReference(R.string.app_settings_title),
                iconRes = R.drawable.ic_settings_24,
                onClick = { router.push(DetailsRoute.Screen(AppScreen.AppSettings)) },
            ).let(::add)

            if (BuildConfig.TESTER_MENU_ENABLED) {
                DetailsItemUM.Basic.Item(
                    id = "tester_menu",
                    title = stringReference(value = "Tester menu"),
                    iconRes = R.drawable.ic_alert_24,
                    onClick = { router.push(DetailsRoute.TesterMenu) },
                ).let(::add)
            }
        }.toImmutableList(),
    )

    private fun buildSupportBlock(): DetailsItemUM = DetailsItemUM.Basic(
        id = "support",
        items = persistentListOf(
            DetailsItemUM.Basic.Item(
                id = "send_feedback",
                title = resourceReference(R.string.details_send_feedback),
                iconRes = R.drawable.ic_comment_24,
                onClick = { router.push(DetailsRoute.Feedback) },
            ),
            DetailsItemUM.Basic.Item(
                id = "disclaimer",
                title = resourceReference(R.string.disclaimer_title),
                iconRes = R.drawable.ic_text_24,
                onClick = { router.push(DetailsRoute.Screen(AppScreen.Disclaimer)) },
            ),
        ),
    )

    private companion object {
        const val BUY_TANGEM_URL = "https://buy.tangem.com/"
    }
}