package com.tangem.tap.domain.tasks

import com.tangem.common.card.Card
import com.tangem.common.core.SessionEnvironment
import com.tangem.common.core.TangemSdkError
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.wallets.builder.UserWalletIdBuilder
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.operations.preflightread.PreflightReadFilter

/**
 * [PreflightReadFilter] for checking if card has expected user wallet id
 *
[REDACTED_AUTHOR]
 */
class UserWalletIdPreflightReadFilter(private val expectedUserWalletId: UserWalletId) : PreflightReadFilter {

    override fun onCardRead(card: Card, environment: SessionEnvironment) = Unit

    override fun onFullCardRead(card: Card, environment: SessionEnvironment) {
        val actualUserWalletId = UserWalletIdBuilder.card(card = CardDTO(card)).build() ?: return

        if (expectedUserWalletId != actualUserWalletId) throw TangemSdkError.WalletNotFound()
    }
}