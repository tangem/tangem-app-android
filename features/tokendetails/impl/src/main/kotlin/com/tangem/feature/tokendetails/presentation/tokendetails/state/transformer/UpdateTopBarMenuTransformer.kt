package com.tangem.feature.tokendetails.presentation.tokendetails.state.transformer

import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_split_top_24
import com.tangem.core.ui.res.generated.icons.ic_document_24
import com.tangem.core.ui.res.generated.icons.ic_eye_cross_24
import com.tangem.core.ui.res.generated.icons.ic_globe_24
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.feature.tokendetails.presentation.tokendetails.model.TokenDetailsClickIntents
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsTopAppBarUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsUM
import com.tangem.features.tokendetails.impl.R
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.toImmutableList

internal class UpdateTopBarMenuTransformer(
    private val userWallet: UserWallet,
    private val hasDerivations: Boolean,
    private val isXpubSupported: Boolean,
    private val isDynamicAddressesAvailable: Boolean,
    private val clickIntents: TokenDetailsClickIntents,
) : Transformer<TokenDetailsUM> {

    /** A single-wallet-with-token card manages no portfolio — it can neither hide the token nor switch its addresses. */
    private val isSingleWalletWithToken: Boolean =
        userWallet is UserWallet.Cold && userWallet.cardTypesResolver.isSingleWalletWithToken()

    override fun transform(prevState: TokenDetailsUM): TokenDetailsUM = prevState.copy(
        topAppBarUM = prevState.topAppBarUM.copy(menuItems = createMenuItems()),
    )

    /**
     * "Explore" is present for every wallet type, so the "•••" button is never empty and the explorer stays reachable
     * whatever state the coin's history is in — it took over the explorer link the pre-redesign history block used to
     * carry in its own title row. It sits last before the destructive "Hide token" row, which closes the menu.
     */
    private fun createMenuItems() = buildList {
        if (!isSingleWalletWithToken) {
            if (isDynamicAddressesAvailable) {
                add(
                    TokenDetailsTopAppBarUM.MenuItemUM(
                        title = resourceReference(R.string.dynamic_addresses),
                        icon = Icons.ic_arrow_split_top_24,
                        onClick = clickIntents::onDynamicAddressesClick,
                    ),
                )
            }
            if (isXpubSupported && hasDerivations) {
                add(
                    TokenDetailsTopAppBarUM.MenuItemUM(
                        title = resourceReference(R.string.token_details_generate_xpub),
                        icon = Icons.ic_document_24,
                        onClick = clickIntents::onGenerateExtendedKey,
                    ),
                )
            }
        }
        add(
            TokenDetailsTopAppBarUM.MenuItemUM(
                title = resourceReference(R.string.common_explore),
                icon = Icons.ic_globe_24,
                onClick = clickIntents::onExploreClick,
            ),
        )
        if (!isSingleWalletWithToken) {
            add(
                TokenDetailsTopAppBarUM.MenuItemUM(
                    title = resourceReference(R.string.token_details_hide_token),
                    icon = Icons.ic_eye_cross_24,
                    onClick = clickIntents::onHideClick,
                    isDestructive = true,
                ),
            )
        }
    }.toImmutableList()
}