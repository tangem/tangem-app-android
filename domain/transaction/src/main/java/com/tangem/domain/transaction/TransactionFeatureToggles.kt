package com.tangem.domain.transaction

interface TransactionFeatureToggles {

    /** Tron gasless pays the fee in TRX, without quoting gasless, when the account's own energy covers the transfer. */
    val isTronClientEnergyEnabled: Boolean

    /** Tron gasless pays the fee in TRX when it is no dearer in fiat than the gasless compensation. */
    val isTronLowerFeeTypeEnabled: Boolean
}