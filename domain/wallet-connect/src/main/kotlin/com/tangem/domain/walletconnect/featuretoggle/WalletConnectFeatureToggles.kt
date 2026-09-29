package com.tangem.domain.walletconnect.featuretoggle

interface WalletConnectFeatureToggles {
    val isBitcoinEnabled: Boolean

    /** When off, the SDK starts during app startup the way it did before it became on-demand. */
    val isOnDemandInitEnabled: Boolean

    val isInterruptedApprovalRecoveryEnabled: Boolean
}