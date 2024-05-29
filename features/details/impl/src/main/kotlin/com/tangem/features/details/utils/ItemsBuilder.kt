package com.tangem.features.details.utils

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.details.component.UserWalletListComponent
import com.tangem.features.details.component.WalletConnectComponent
import com.tangem.features.details.entity.DetailsItemUM
import com.tangem.features.details.impl.BuildConfig
import com.tangem.features.details.impl.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

internal class ItemsBuilder(
    private val walletConnectComponent: WalletConnectComponent,
    private val userWalletListComponent: UserWalletListComponent,
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

    fun buildUserWalletListBlock(): DetailsItemUM = DetailsItemUM.Component(
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
                title = stringReference("Buy Tangem Wallet"), // TODO: Move to resources in AND-7165
                iconRes = R.drawable.ic_tangem_24,
                onClick = { /* TODO: Implement in AND-7165 */ },
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
                onClick = { /* TODO: Implement in AND-7165 */ },
            ).let(::add)

            if (BuildConfig.TESTER_MENU_ENABLED) {
                DetailsItemUM.Basic.Item(
                    id = "tester_menu",
                    title = stringReference(value = "Tester menu"),
                    iconRes = R.drawable.ic_alert_24,
                    onClick = { /* TODO: Implement in AND-7165 */ },
                ).let(::add)
            }
        }.toImmutableList(),
    )

    private fun buildSupportBlock(): DetailsItemUM = DetailsItemUM.Basic(
        id = "support",
        items = persistentListOf(
            DetailsItemUM.Basic.Item(
                id = "send_feedback",
                title = stringReference("Send feedback"), // TODO: Move to resources in AND-7165
                iconRes = R.drawable.ic_comment_24,
                onClick = { /* TODO: Implement in AND-7165 */ },
            ),
            DetailsItemUM.Basic.Item(
                id = "disclaimer",
                title = resourceReference(R.string.disclaimer_title),
                iconRes = R.drawable.ic_text_24,
                onClick = { /* TODO: Implement in AND-7165 */ },
            ),
        ),
    )
}
