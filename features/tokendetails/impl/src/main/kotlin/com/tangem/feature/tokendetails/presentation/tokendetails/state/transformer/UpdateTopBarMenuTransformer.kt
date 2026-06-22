package com.tangem.feature.tokendetails.presentation.tokendetails.state.transformer

import com.tangem.core.ui.components.dropdownmenu.TangemDropdownMenuItem
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.themedColor
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.feature.tokendetails.presentation.tokendetails.model.TokenDetailsClickIntents
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsUM
import com.tangem.features.tokendetails.impl.R
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

internal class UpdateTopBarMenuTransformer(
    private val userWallet: UserWallet,
    private val hasDerivations: Boolean,
    private val isXpubSupported: Boolean,
    private val isDynamicAddressesAvailable: Boolean,
    private val clickIntents: TokenDetailsClickIntents,
) : Transformer<TokenDetailsUM> {

    override fun transform(prevState: TokenDetailsUM): TokenDetailsUM = prevState.copy(
        topAppBarUM = prevState.topAppBarUM.copy(menuItems = createMenuItems()),
    )

    private fun createMenuItems() = if (userWallet is UserWallet.Cold &&
        userWallet.cardTypesResolver.isSingleWalletWithToken()
    ) {
        persistentListOf()
    } else {
        buildList {
            if (isDynamicAddressesAvailable) {
                add(
                    TangemDropdownMenuItem(
                        title = resourceReference(R.string.dynamic_addresses),
                        textColor = themedColor { TangemTheme.colors.text.primary1 },
                        onClick = clickIntents::onDynamicAddressesClick,
                    ),
                )
            }
            if (isXpubSupported && hasDerivations) {
                add(
                    TangemDropdownMenuItem(
                        title = resourceReference(R.string.token_details_generate_xpub),
                        textColor = themedColor { TangemTheme.colors.text.primary1 },
                        onClick = clickIntents::onGenerateExtendedKey,
                    ),
                )
            }
            add(
                TangemDropdownMenuItem(
                    title = resourceReference(R.string.token_details_hide_token),
                    textColor = themedColor { TangemTheme.colors.text.warning },
                    onClick = clickIntents::onHideClick,
                ),
            )
        }.toImmutableList()
    }
}