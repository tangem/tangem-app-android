package com.tangem.domain.moneyaccount.usecase

import arrow.core.Either
import arrow.core.left
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusFetcher
import com.tangem.domain.moneyaccount.repository.MoneyAccountOrderRepository
import com.tangem.domain.pay.model.TangemPayOrderInfo
import com.tangem.domain.pay.usecase.FindMoneyAccountCreationOrderUseCase
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

class RestoreMoneyAccountOrderUseCase(
    private val trackOrder: TrackMoneyAccountOrderUseCase,
    private val findMoneyAccountCreationOrderUseCase: FindMoneyAccountCreationOrderUseCase,
    private val moneyAccountOrderRepository: MoneyAccountOrderRepository,
    private val moneyAccountStatusFetcher: MoneyAccountStatusFetcher,
) {
    suspend operator fun invoke(userWalletId: UserWalletId) {
        while (true) {
            findMoneyAccountCreationOrderUseCase.invoke(userWalletId)
                .onLeft { error ->
                    if (moneyAccountOrderRepository.getOrder(userWalletId)?.getOrNull() == null) {
                        moneyAccountOrderRepository.storeOrder(userWalletId, error.left())
                    }
                    moneyAccountStatusFetcher.invoke(userWalletId)
                    delay(5.seconds)
                }
                .onRight { order ->
                    if (order != null) {
                        trackOrder(
                            userWalletId = userWalletId,
                            order = TangemPayOrderInfo.fromOrder(order),
                        )
                    } else {
                        moneyAccountOrderRepository.storeOrder(userWalletId, Either.Right(null))
                        moneyAccountStatusFetcher.invoke(userWalletId)
                    }

                    return
                }
        }
    }
}