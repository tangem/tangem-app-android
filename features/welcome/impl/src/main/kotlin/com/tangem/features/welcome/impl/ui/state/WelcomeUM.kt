package com.tangem.features.welcome.impl.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.userwallet.state.UserWalletItemUM
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal sealed class WelcomeUM {

    data object Empty : WelcomeUM()

    data object Plain : WelcomeUM()

    data class SelectWallet(
        val wallets: ImmutableList<UserWalletItemUM> = persistentListOf(),
        val showUnlockWithBiometricButton: Boolean = false,
        val addWalletBottomSheet: TangemBottomSheetConfig = TangemBottomSheetConfig.Empty,
        val onUnlockWithBiometricClick: () -> Unit = {},
        val addWalletClick: () -> Unit = {},
    ) : WelcomeUM()
}