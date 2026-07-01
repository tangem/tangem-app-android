package com.tangem.features.wallet.featuretoggles

/**
 * Wallet feature toggles
 *
[REDACTED_AUTHOR]
 */
interface WalletFeatureToggles {

    val isAddAndManageTokensEnabled: Boolean

    val isAddFundsStage1Enabled: Boolean

    val isManageFundsEnabled: Boolean
}