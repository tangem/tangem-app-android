package com.tangem.domain.walletconnect.usecase.pair

import com.tangem.domain.walletconnect.model.WcSessionApprove
import com.tangem.domain.wallets.models.UserWallet
import kotlinx.coroutines.flow.Flow

interface WcPairUseCase {

    fun pairFlow(uri: String, source: Source, selectedWallet: UserWallet): Flow<WcPairState>

    fun approve(sessionForApprove: WcSessionApprove)
    fun reject()

    enum class Source { QR, DEEPLINK, ETC }
}