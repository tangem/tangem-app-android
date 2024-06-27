package com.tangem.features.details.utils

import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.AppScreen
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.details.component.UserWalletListComponent
import com.tangem.features.details.component.WalletConnectComponent
import com.tangem.features.details.entity.DetailsItemUM
import com.tangem.features.details.impl.BuildConfig
import com.tangem.features.details.impl.R
import com.tangem.features.details.routing.DetailsRoute
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

internal class ItemsBuilder(
    private val walletConnectComponent: WalletConnectComponent,
    private val userWalletListComponent: UserWalletListComponent,
    private val router: Router,
) {

    suspend fun buldAll(): ImmutableList<DetailsItemUM> = buildList {
        buildWalletConnectBlock()?.let(::add)
        buildUserWalletListBlock().let(::add)
        buildShopBlock().let(::add)
        buildSettingsBlock().let(::add)
        buildSupportBlock().let(::add)
    }.toImmutableList()

    private suspend fun buildWalletConnectBlock(): DetailsItemUM? {
        return if (walletConnectComponent.checkIsAvailable()) {
            DetailsItemUM.Component(
                id = "wallet_connect",
                content = {
                    walletConnectComponent.View(modifier = it)
                },
            )
        } else {
            null
        }
    }

    private fun buildUserWalletListBlock(): DetailsItemUM = DetailsItemUM.Component(
        id = "user_wallet_list",
        content = {
            userWalletListComponent.View(modifier = it)
        },
    )

    private fun buildShopBlock(): DetailsItemUM = DetailsItemUM.Basic(
        id = "shop",
        items = persistentListOf(
            DetailsItemUM.Basic.Item(
                id = "buy_tangem_wallet",
                title = stringReference("Buy Tangem Wallet"), // TODO: Move to resources in [REDACTED_TASK_KEY]
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
                title = stringReference("Send feedback"), // TODO: Move to resources in [REDACTED_TASK_KEY]
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