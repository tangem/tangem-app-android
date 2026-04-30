package com.tangem.feature.tokendetails.presentation.tokendetails.state.transformer

import com.tangem.core.ui.components.dropdownmenu.TangemDropdownMenuItem
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.themedColor
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsUM
import com.tangem.features.tokendetails.impl.R
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

internal class UpdateTopBarMenuTransformer(
    private val userWallet: UserWallet,
    private val hasDerivations: Boolean,
    private val isXPubSupported: Boolean,
    private val onGenerateExtendedKey: () -> Unit,
    private val onHideClick: () -> Unit,
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
            if (isXPubSupported && hasDerivations) {
                add(
                    TangemDropdownMenuItem(
                        title = resourceReference(R.string.token_details_generate_xpub),
                        textColor = themedColor { TangemTheme.colors.text.primary1 },
                        onClick = onGenerateExtendedKey,
                    ),
                )
            }
            add(
                TangemDropdownMenuItem(
                    title = resourceReference(R.string.token_details_hide_token),
                    textColor = themedColor { TangemTheme.colors.text.warning },
                    onClick = onHideClick,
                ),
            )
        }.toImmutableList()
    }
}