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
}