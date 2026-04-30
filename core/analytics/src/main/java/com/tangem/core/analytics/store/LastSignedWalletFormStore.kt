package com.tangem.core.analytics.store

import com.tangem.core.analytics.models.Basic

interface LastSignedWalletFormStore {

    fun update(form: Basic.TransactionSent.WalletForm)
}