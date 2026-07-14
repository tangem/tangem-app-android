package com.tangem.datasource.local.visa

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.tangem.datasource.local.datastore.RuntimeDataStore
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.utils.getSyncOrNull
import com.tangem.datasource.local.preferences.utils.store
import com.tangem.domain.models.pay.TangemPayReissueCardFee
import com.tangem.domain.models.wallet.UserWalletId

internal class DefaultTangemPayReissueCardStore(
    private val feeStore: RuntimeDataStore<TangemPayReissueCardFee>,
    private val prefs: AppPreferencesStore,
) : TangemPayReissueCardStore {

    override suspend fun storeReissueFee(
        userWalletId: UserWalletId,
        tangemPayReissueCardFee: TangemPayReissueCardFee,
    ) {
        feeStore.store(userWalletId.stringValue, tangemPayReissueCardFee)
    }

    override suspend fun getReissueFee(userWalletId: UserWalletId): TangemPayReissueCardFee? {
        return feeStore.getSyncOrNull(userWalletId.stringValue)
    }

    override suspend fun storeReissueOrderId(cardId: String, orderId: String) {
        prefs.store(
            key = getReissueKey(cardId),
            value = orderId,
        )
    }

    override suspend fun removeReissueOrderId(cardId: String) {
        prefs.edit { it.remove(getReissueKey(cardId)) }
    }

    override suspend fun getOrderId(cardId: String): String? {
        return prefs.getSyncOrNull(key = getReissueKey(cardId))
    }

    private fun getReissueKey(cardId: String) = stringPreferencesKey("tangem_pay_reissue_card_$cardId")
}