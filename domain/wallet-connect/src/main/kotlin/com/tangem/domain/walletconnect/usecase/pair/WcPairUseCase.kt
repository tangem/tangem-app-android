package com.tangem.domain.walletconnect.usecase.pair

import com.tangem.domain.walletconnect.model.WcSessionProposal
import com.tangem.domain.wallets.models.UserWallet
import kotlinx.coroutines.flow.Flow

interface WcPairUseCase {

    fun pairFlow(uri: String, source: Source, selectedWallet: UserWallet): Flow<WcPairState>
// [REDACTED_TODO_COMMENT]
    fun onWalletSelect(selectedWallet: UserWallet)
// [REDACTED_TODO_COMMENT]
    fun onAccountSelect(account: Any)

    fun approve(sessionForApprove: WcSessionProposal)
    fun reject()

    enum class Source { QR, DEEPLINK, ETC }
}
