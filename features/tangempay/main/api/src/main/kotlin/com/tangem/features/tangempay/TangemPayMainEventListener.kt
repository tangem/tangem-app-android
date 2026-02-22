package com.tangem.features.tangempay

import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow

interface TangemPayMainEventListener {

    val event: Flow<TangemPayMainEvent>

    suspend fun send(event: TangemPayMainEvent)
}

sealed class TangemPayMainEvent {
    data class SelectWallet(val userWalletId: UserWalletId) : TangemPayMainEvent()
    data class Update(val isInBackground: Boolean, val userWalletId: UserWalletId) : TangemPayMainEvent()
}