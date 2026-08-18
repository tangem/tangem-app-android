package com.tangem.domain.polymarket

import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.isMultiCurrency

/**
 * Whether a Prediction account belongs to this wallet at all. Says nothing about the wallet being locked, unlike
 * onboarding eligibility: the account is part of the wallet's structure and must not come and go with the lock.
 */
val UserWallet.isPredictionAccountSupported: Boolean
    get() = isMultiCurrency