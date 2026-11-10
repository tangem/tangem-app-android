package com.tangem.domain.collectibles.usecase

import arrow.core.Either
import arrow.core.left
import com.tangem.domain.collectibles.CollectiblesAccountError
import com.tangem.domain.models.wallet.UserWalletId
import java.math.BigDecimal
import javax.inject.Inject

class WithdrawFromCollectiblesVaultUseCase @Inject constructor() {

    // TODO([REDACTED_TASK_KEY]): build and send withdraw_token through the SDK once its builder is published
    @Suppress("UnusedParameter", "RedundantSuspendModifier")
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        recipientAddress: String,
        amount: BigDecimal,
    ): Either<CollectiblesAccountError, Unit> = CollectiblesAccountError.Unsupported.left()
}