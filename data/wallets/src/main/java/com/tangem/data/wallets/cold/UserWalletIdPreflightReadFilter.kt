package com.tangem.data.wallets.cold

import com.tangem.common.card.Card
import com.tangem.common.core.SessionEnvironment
import com.tangem.common.core.TangemSdkError
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.builder.UserWalletIdBuilder
import com.tangem.operations.preflightread.PreflightReadFilter

/**
 * [PreflightReadFilter] for checking if the read card belongs to [expectedUserWallet]
 *
[REDACTED_AUTHOR]
 */
class UserWalletIdPreflightReadFilter(private val expectedUserWallet: UserWallet.Cold) : PreflightReadFilter {

    override fun onCardRead(card: Card, environment: SessionEnvironment) = Unit

    override fun onFullCardRead(card: Card, environment: SessionEnvironment) {
        val actualUserWalletId = UserWalletIdBuilder
            .card(card = CardDTO(card), walletScanResponse = expectedUserWallet.scanResponse)
            .build()
            ?: return

        if (expectedUserWallet.walletId != actualUserWalletId) throw TangemSdkError.WalletNotFound()
    }
}