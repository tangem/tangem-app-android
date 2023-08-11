package com.tangem.feature.wallet.presentation.wallet.state

/**
 * Locked wallet state
 *
[REDACTED_AUTHOR]
 */
internal sealed interface WalletLockedState {

    /** Lambda be invoked when unlock wallet notification is clicked */
    val onUnlockWalletsNotificationClick: () -> Unit

    /** Lambda be invoked when unlock wallet button is clicked */
    val onUnlockClick: () -> Unit

    /** Lambda be invoked when scan button is clicked */
    val onScanClick: () -> Unit

    /** Bottom sheet visibility */
    val isBottomSheetShow: Boolean

    /** Lambda be invoked when bottom sheet is dismissed */
    val onBottomSheetDismiss: () -> Unit

    /** Get selected wallet index */
    fun getSelectedWalletIndex(): Int {
        return when (this) {
            is WalletMultiCurrencyState.Locked -> walletsListConfig.selectedWalletIndex
            is WalletSingleCurrencyState.Locked -> walletsListConfig.selectedWalletIndex
        }
    }
}