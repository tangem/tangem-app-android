package com.tangem.feature.wallet.presentation.wallet.domain

import com.tangem.utils.logging.TangemLogger
import com.tangem.domain.account.status.producer.SingleAccountStatusListProducer
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase

internal fun GetSelectedWalletSyncUseCase.unwrap(): UserWallet? {
    return this().fold(
        ifLeft = { error ->
            TangemLogger.e("Impossible to get selected wallet $error")
            null
        },
        ifRight = { it },
    )
}

internal suspend fun SingleAccountStatusListSupplier.unwrap(userWalletId: UserWalletId): CryptoCurrencyStatus? {
    return getSyncOrNull(params = SingleAccountStatusListProducer.Params(userWalletId))
        ?.mainAccount
        ?.flattenCurrencies()
        ?.firstOrNull()
}