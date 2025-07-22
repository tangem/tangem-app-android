package com.tangem.features.welcome.impl.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal sealed class WelcomeUM {

    data object Plain : WelcomeUM()

    data class SelectWallet(
        val wallets: ImmutableList<WalletUM> = persistentListOf(),
        val showUnlockWithBiometricButton: Boolean = false,
        val addWalletBottomSheet: TangemBottomSheetConfig = TangemBottomSheetConfig.Empty,
        val onUnlockWithBiometricClick: () -> Unit = {},
        val addWalletClick: () -> Unit = {},
    ) : WelcomeUM()

    data class EnterAccessCode(
        val value: String = "",
        val onUnlockWithBiometricClick: () -> Unit = {},
        val onValueChange: (String) -> Unit = {},
        val onBackClick: () -> Unit = {},
    ) : WelcomeUM()
}