package com.tangem.domain.moneyaccount.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.model.IssueMoneyAccountError
import com.tangem.domain.moneyaccount.model.MoneyAccountActivationError
import com.tangem.domain.tangempay.model.CustomerOrderRequest
import com.tangem.domain.tangempay.order.TangemPayOrderRepository
import com.tangem.utils.coroutines.AppCoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.UUID

class IssueMoneyAccountUseCase(
    private val activateMoneyAccountUseCase: ActivateMoneyAccountUseCase,
    private val orderRepository: TangemPayOrderRepository,
    private val pollTangemPayAccountStatusesUseCase: PollTangemPayAccountStatusesUseCase,
    private val appCoroutineScope: AppCoroutineScope,
) {
    suspend operator fun invoke(userWalletId: UserWalletId): Either<IssueMoneyAccountError, Unit> =
        issue(userWalletId).onRight {
            appCoroutineScope.launch { pollTangemPayAccountStatusesUseCase(userWalletId).collect() }
        }

    private suspend fun issue(userWalletId: UserWalletId): Either<IssueMoneyAccountError, Unit> = either {
        val depositCredentials = activateMoneyAccountUseCase(userWalletId)
            .mapLeft { error ->
                when (error) {
                    MoneyAccountActivationError.UserCancelled -> IssueMoneyAccountError.UserCancelled
                    MoneyAccountActivationError.PersistenceFailed,
                    MoneyAccountActivationError.Unknown,
                    -> IssueMoneyAccountError.Failed
                }
            }
            .bind()

        orderRepository.createOrder(
            userWalletId = userWalletId,
            request = CustomerOrderRequest.AccountIssueBanking(depositCredentials = depositCredentials),
            idempotencyKey = UUID.randomUUID().toString(),
        )
            .mapLeft { IssueMoneyAccountError.Failed }
            .map {}
            .bind()
    }
}