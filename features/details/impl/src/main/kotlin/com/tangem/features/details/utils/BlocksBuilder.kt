package com.tangem.features.details.utils

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.details.component.UserWalletListComponent
import com.tangem.features.details.component.WalletConnectComponent
import com.tangem.features.details.impl.BuildConfig
import com.tangem.features.details.impl.R
import com.tangem.features.details.state.DetailsBlock
import com.tangem.features.details.ui.UserWalletListBlock
import com.tangem.features.details.ui.WalletConnectBlock
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

internal class BlocksBuilder(
    private val walletConnectComponent: WalletConnectComponent,
    private val userWalletListComponent: UserWalletListComponent,
) {

    fun buldAll(): ImmutableList<DetailsBlock> = buildList {
        buildWalletConnectBlock()?.let(::add)
        buildUserWalletListBlock().let(::add)
        buildShopBlock().let(::add)
        buildSettingsBlock().let(::add)
        buildSupportBlock().let(::add)
    }.toImmutableList()

    private fun buildWalletConnectBlock(): DetailsBlock? {
        return if (walletConnectComponent.state.value is WalletConnectComponent.State.Content) {
            DetailsBlock.Component(
                id = "wallet_connect",
                content = {
                    WalletConnectBlock(
                        modifier = it,
                        component = walletConnectComponent,
                    )
                },
            )
        } else {
            null
        }
    }

    fun buildUserWalletListBlock(): DetailsBlock = DetailsBlock.Component(
        id = "user_wallet_list",
        content = {
            UserWalletListBlock(
                modifier = it,
                component = userWalletListComponent,
            )
        },
    )

    private fun buildShopBlock(): DetailsBlock = DetailsBlock.Basic(
        id = "shop",
        items = persistentListOf(
            DetailsBlock.Basic.Item(
                title = stringReference("Buy Tangem Wallet"), // TODO: Move to resources in AND-7165
                iconRes = R.drawable.ic_tangem_24,
                onClick = { /* TODO: Implement in AND-7165 */ },
            ),
        ),
    )

    private fun buildSettingsBlock(): DetailsBlock = DetailsBlock.Basic(
        id = "settings",
        items = buildList {
            DetailsBlock.Basic.Item(
                title = resourceReference(R.string.app_settings_title),
                iconRes = R.drawable.ic_settings_24,
                onClick = { /* TODO: Implement in AND-7165 */ },
            ).let(::add)

            if (BuildConfig.TESTER_MENU_ENABLED) {
                DetailsBlock.Basic.Item(
                    title = stringReference(value = "Tester menu"),
                    iconRes = R.drawable.ic_alert_24,
                    onClick = { /* TODO: Implement in AND-7165 */ },
                ).let(::add)
            }
        }.toImmutableList(),
    )

    private fun buildSupportBlock(): DetailsBlock = DetailsBlock.Basic(
        id = "support",
        items = persistentListOf(
            DetailsBlock.Basic.Item(
                title = resourceReference(R.string.details_chat),
                iconRes = R.drawable.ic_chat_24,
                onClick = { /* TODO: Implement in AND-7165 */ },
            ),
            DetailsBlock.Basic.Item(
                title = stringReference("Send feedback"), // TODO: Move to resources in AND-7165
                iconRes = R.drawable.ic_comment_24,
                onClick = { /* TODO: Implement in AND-7165 */ },
            ),
            DetailsBlock.Basic.Item(
                title = resourceReference(R.string.disclaimer_title),
                iconRes = R.drawable.ic_text_24,
                onClick = { /* TODO: Implement in AND-7165 */ },
            ),
        ),
    )
}
