package com.tangem.features.promobanners.impl.campaigns.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.TextReference

/**
 * State of the campaign activation bottom sheet.
 *
 * The intro promo screen (image + title + description) is always shown. When [selectedToken] is `null`
 * the footer shows "Select token"; once a token is chosen it shows the account block, the terms agreement
 * and the "Enroll" button. When [isChoosingToken] is `true` the token selector is shown on top of the
 * intro (as a stacked bottom sheet), not instead of it.
 *
 * [selectedAccount] is shown above the token only in accounts (multi-account) mode — it names the account
 * the asset was picked from. It is `null` in single-account mode or before a token is chosen.
 */

@Immutable
internal data class ActivateCampaignUM(
    val logo: TangemIconUM,
    val title: TextReference,
    val description: TextReference,
    val selectedToken: TokenItemState?,
    val selectedAccount: SelectedAccountUM?,
    val isChoosingToken: Boolean,
    val footerUM: FooterUM,
    val onChooseTokenDismiss: () -> Unit,
    val onLearnMoreClick: () -> Unit,
    val onChooseTokenClick: () -> Unit,
)

@Immutable
internal data class FooterUM(
    val label: TextReference,
    val onPrimaryButtonClick: () -> Unit,
    val terms: TermsUM? = null,
)

@Immutable
data class TermsUM(
    val text: TextReference,
    val linkText: TextReference,
    val onTermsClick: () -> Unit,
)

@Immutable
internal data class SelectedAccountUM(
    val iconState: CurrencyIconState,
    val name: TextReference,
)