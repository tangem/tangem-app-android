package com.tangem.spend.datasource.pay.store

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.tangem.domain.models.wallet.UserWalletId

/** Per-wallet preference keys of TangemPay stores that must also be removed when the wallet is deleted. */
object TangemPayPreferencesKeys {

    fun bankOfframpRecents(userWalletId: UserWalletId): Preferences.Key<String> =
        stringPreferencesKey("tangem_pay_bank_offramp_recents_${userWalletId.stringValue}")
}