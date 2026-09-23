package com.tangem.data.polymarket.store

import com.tangem.domain.models.account.PredictionAccountStatusValue
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PredictionDepositAddressWriter
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class DefaultPredictionDepositAddressWriter @Inject constructor(
    private val statusStore: PredictionAccountStatusStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : PredictionDepositAddressWriter {

    override suspend fun store(userWalletId: UserWalletId, address: String?) = withContext(dispatchers.io) {
        statusStore.update(userWalletId = userWalletId) { current ->
            (current as? PredictionAccountStatusValue.Active)?.copy(depositAddress = address)
        }
    }
}