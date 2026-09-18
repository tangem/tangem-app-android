package com.tangem.domain.moneyaccount.usecase

import arrow.core.Either
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.networks.single.SingleNetworkStatusFetcher
import com.tangem.domain.networks.single.SingleNetworkStatusProducer
import com.tangem.domain.networks.single.SingleNetworkStatusSupplier
import com.tangem.domain.pay.TangemPayCurrencyFactory
import java.math.BigDecimal

class GetMoneyAccountBalanceUseCase(
    private val tangemPayCurrencyFactory: TangemPayCurrencyFactory,
    private val singleNetworkStatusFetcher: SingleNetworkStatusFetcher,
    private val singleNetworkStatusSupplier: SingleNetworkStatusSupplier,
) {

    suspend operator fun invoke(userWalletId: UserWalletId): Either<Throwable, BigDecimal> = Either.catch {
        val token = tangemPayCurrencyFactory.createMoneyAccountToken(userWalletId)

        singleNetworkStatusFetcher(
            SingleNetworkStatusFetcher.Params(
                userWalletId = userWalletId,
                network = token.network,
                extraTokens = setOf(token),
            ),
        )

        val networkStatus = singleNetworkStatusSupplier.getSyncOrNull(
            params = SingleNetworkStatusProducer.Params(userWalletId = userWalletId, network = token.network),
        )?.value as? NetworkStatus.Verified

        val amount = networkStatus?.amounts?.get(token.id) as? NetworkStatus.Amount.Loaded

        requireNotNull(amount?.value) { "Money account balance is unavailable for $userWalletId" }
    }
}