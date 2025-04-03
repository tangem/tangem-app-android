package com.tangem.domain.walletconnect.usecase.pair

import com.tangem.domain.walletconnect.model.WcSessionApprove
import kotlinx.coroutines.flow.Flow

interface WcPairUseCase {

    fun pairFlow(uri: String, source: Source): Flow<WcPairState>

    fun approve(sessionForApprove: WcSessionApprove)
    fun reject()

    enum class Source { QR, DEEPLINK, CLIPBOARD, ETC }
}