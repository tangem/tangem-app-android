package com.tangem.features.details.utils

import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.components.block.model.BlockUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.details.entity.DetailsItemUM
import com.tangem.features.details.impl.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import javax.inject.Inject

@ModelScoped
internal class ItemsBuilder @Inject constructor(private val router: Router) {

    @Suppress("LongParameterList")
    fun buildAll(
        isWalletConnectAvailable: Boolean,
        isSupportChatAvailable: Boolean,
        userWalletId: UserWalletId,
        onSupportEmailClick: () -> Unit,
        onSupportChatClick: () -> Unit,
        onBuyClick: () -> Unit,
    ): ImmutableList<DetailsItemUM> = buildList {
        buildWalletConnectBlock(isWalletConnectAvailable, userWalletId)?.let(::add)
        buildUserWalletListBlock().let(::add)
        buildShopBlock(onBuyClick).let(::add)
        buildSettingsBlock().let(::add)
        buildSupportBlock(
            onSupportEmailClick = onSupportEmailClick,
            onSupportChatClick = onSupportChatClick,
            isSupportChatAvailable = isSupportChatAvailable,
        ).let(::add)
    }.toImmutableList()

    private fun buildWalletConnectBlock(isWalletConnectAvailable: Boolean, userWalletId: UserWalletId): DetailsItemUM? {
        return if (isWalletConnectAvailable) {
            DetailsItemUM.WalletConnect(
                onClick = { router.push(AppRoute.WalletConnectSessions(userWalletId)) },
            )
        } else {
            null
        }
    }

    private fun buildUserWalletListBlock(): DetailsItemUM = DetailsItemUM.UserWalletList

    private fun buildShopBlock(onBuyClick: () -> Unit): DetailsItemUM = DetailsItemUM.Basic(
        id = "shop",
        items = persistentListOf(
            DetailsItemUM.Basic.Item(
                id = "buy_tangem_wallet",
                block = BlockUM(
                    text = resourceReference(R.string.details_buy_wallet),
                    iconRes = R.drawable.ic_tangem_24,
                    onClick = onBuyClick,
                ),
            ),
        ),
    )

    private fun buildSettingsBlock(): DetailsItemUM = DetailsItemUM.Basic(
        id = "settings",
        items = buildList {
            DetailsItemUM.Basic.Item(
                id = "app_settings",
                block = BlockUM(
                    text = resourceReference(R.string.app_settings_title),
                    iconRes = R.drawable.ic_settings_24,
                    onClick = { router.push(AppRoute.AppSettings) },
                ),
            ).let(::add)
        }.toImmutableList(),
    )

    private fun buildSupportBlock(
        onSupportEmailClick: () -> Unit,
        onSupportChatClick: () -> Unit,
        isSupportChatAvailable: Boolean,
    ): DetailsItemUM = DetailsItemUM.Basic(
        id = "support",
        items = buildList {
            DetailsItemUM.Basic.Item(
                id = "support_email",
                block = BlockUM(
                    text = resourceReference(R.string.details_row_title_contact_to_support),
                    iconRes = R.drawable.ic_comment_24,
                    onClick = onSupportEmailClick,
                ),
            ).let(::add)

            if (isSupportChatAvailable) {
                DetailsItemUM.Basic.Item(
                    id = "support_chat",
                    block = BlockUM(
                        text = resourceReference(R.string.details_row_title_contact_to_support_chat),
                        iconRes = R.drawable.ic_chat_24,
                        onClick = onSupportChatClick,
                    ),
                ).let(::add)
            }

            DetailsItemUM.Basic.Item(
                id = "disclaimer",
                block = BlockUM(
                    text = resourceReference(R.string.disclaimer_title),
                    iconRes = R.drawable.ic_text_24,
                    onClick = { router.push(AppRoute.Disclaimer(isTosAccepted = true)) },
                ),
            ).let(::add)
        }.toPersistentList(),
    )
}