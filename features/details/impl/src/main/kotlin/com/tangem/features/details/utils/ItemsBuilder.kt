package com.tangem.features.details.utils

import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.components.block.model.BlockUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.details.entity.DetailsItemUM
import com.tangem.features.details.impl.BuildConfig
import com.tangem.features.details.impl.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import javax.inject.Inject

@ModelScoped
internal class ItemsBuilder @Inject constructor(private val router: Router) {

    fun buildAll(
        isWalletConnectAvailable: Boolean,
        onSupportClick: () -> Unit,
        onBuyClick: () -> Unit,
    ): ImmutableList<DetailsItemUM> = buildList {
        buildWalletConnectBlock(isWalletConnectAvailable)?.let(::add)
        buildUserWalletListBlock().let(::add)
        buildShopBlock(onBuyClick).let(::add)
        buildSettingsBlock().let(::add)
        buildSupportBlock(onSupportClick).let(::add)
    }.toImmutableList()

    private fun buildWalletConnectBlock(isWalletConnectAvailable: Boolean): DetailsItemUM? {
        return if (isWalletConnectAvailable) {
            DetailsItemUM.WalletConnect(
                onClick = { router.push(AppRoute.WalletConnectSessions) },
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

            if (BuildConfig.TESTER_MENU_ENABLED) {
                DetailsItemUM.Basic.Item(
                    id = "tester_menu",
                    block = BlockUM(
                        text = stringReference(value = "Tester menu"),
                        iconRes = R.drawable.ic_alert_24,
                        onClick = { router.push(AppRoute.TesterMenu) },
                    ),
                ).let(::add)
            }
        }.toImmutableList(),
    )

    private fun buildSupportBlock(onClick: () -> Unit): DetailsItemUM = DetailsItemUM.Basic(
        id = "support",
        items = persistentListOf(
            DetailsItemUM.Basic.Item(
                id = "send_feedback",
                block = BlockUM(
                    text = resourceReference(R.string.details_row_title_contact_to_support),
                    iconRes = R.drawable.ic_comment_24,
                    onClick = onClick,
                ),
            ),
            DetailsItemUM.Basic.Item(
                id = "disclaimer",
                block = BlockUM(
                    text = resourceReference(R.string.disclaimer_title),
                    iconRes = R.drawable.ic_text_24,
                    onClick = { router.push(AppRoute.Disclaimer(isTosAccepted = true)) },
                ),
            ),
        ),
    )
}