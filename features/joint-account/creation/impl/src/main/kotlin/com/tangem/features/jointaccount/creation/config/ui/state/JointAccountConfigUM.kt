package com.tangem.features.jointaccount.creation.config.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.common.ui.userwallet.picker.state.ChooseWalletUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.account.CryptoPortfolioIcon
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class JointAccountConfigUM(
    val name: String,
    val namePlaceholder: TextReference,
    val icon: AccountIconUM.CryptoPortfolio,
    val colors: ImmutableList<CryptoPortfolioIcon.Color>,
    val icons: ImmutableList<CryptoPortfolioIcon.Icon>,
    val wallet: WalletUM?,
    val chooseWallet: ChooseWalletUM?,
    val isContinueEnabled: Boolean,
    val onNameChange: (String) -> Unit,
    val onColorClick: (CryptoPortfolioIcon.Color) -> Unit,
    val onIconClick: (CryptoPortfolioIcon.Icon) -> Unit,
    val onContinueClick: () -> Unit,
    val onBackClick: () -> Unit,
) {

    /** Row showing the currently chosen wallet */
    data class WalletUM(
        val name: String,
        val onClick: () -> Unit,
    )
}