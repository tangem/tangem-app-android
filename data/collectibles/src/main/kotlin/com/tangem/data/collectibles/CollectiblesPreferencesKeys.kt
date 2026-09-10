package com.tangem.data.collectibles

import androidx.datastore.preferences.core.stringPreferencesKey
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.wallet.UserWalletId

/** Preference keys owned by the collectibles feature, kept out of the app-wide `PreferencesKeys`. */
internal object CollectiblesPreferencesKeys {

    /** Recorded status of a wallet's collectibles account. No entry means the wallet has no account. */
    fun getAccountStatusKey(userWalletId: UserWalletId) = stringPreferencesKey(
        name = "collectibles_account_status_${userWalletId.stringValue}",
    )

    /** The wallet id is in the key too: it keeps entries wallet-scoped once account ids come from the backend. */
    fun getHotDelegateKey(accountId: AccountId) = stringPreferencesKey(
        name = "collectibles_hot_delegate_${accountId.value}_${accountId.userWalletId.stringValue}",
    )
}