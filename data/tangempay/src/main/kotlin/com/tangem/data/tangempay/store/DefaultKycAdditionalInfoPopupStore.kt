package com.tangem.data.tangempay.store

import com.tangem.core.local.datastore.RuntimeStateStore
import com.tangem.domain.tangempay.model.KycAdditionalInfoAccount
import com.tangem.domain.tangempay.repository.KycAdditionalInfoPopupStore
import javax.inject.Inject

internal class DefaultKycAdditionalInfoPopupStore @Inject constructor() : KycAdditionalInfoPopupStore {

    private val shownAccounts = RuntimeStateStore<Set<KycAdditionalInfoAccount>>(defaultValue = emptySet())

    override suspend fun tryMarkShown(account: KycAdditionalInfoAccount): Boolean {
        var isFirstShow = false
        shownAccounts.update { accounts ->
            isFirstShow = account !in accounts
            accounts + account
        }
        return isFirstShow
    }

    override suspend fun reset(account: KycAdditionalInfoAccount) {
        shownAccounts.update { accounts -> accounts - account }
    }
}