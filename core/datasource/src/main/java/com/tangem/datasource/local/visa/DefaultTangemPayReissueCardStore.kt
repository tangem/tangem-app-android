package com.tangem.datasource.local.visa

import com.tangem.datasource.local.datastore.RuntimeDataStore
import com.tangem.domain.models.pay.TangemPayReissueCardFee
import com.tangem.domain.models.wallet.UserWalletId

internal class DefaultTangemPayReissueCardStore(
    private val feeStore: RuntimeDataStore<TangemPayReissueCardFee>,
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
        // TODO v_rodionov: #[REDACTED_TASK_KEY] store orderId in app prefs
    }

    override suspend fun getOrderId(cardId: String): String? {
        // TODO v_rodionov: #[REDACTED_TASK_KEY] store orderId in app prefs
        return null
    }
}