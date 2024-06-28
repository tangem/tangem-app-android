package com.tangem.tap.domain.card

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import com.tangem.common.CompletionResult
import com.tangem.common.UserCodeType
import com.tangem.common.core.TangemError
import com.tangem.common.core.TangemSdkError
import com.tangem.common.core.UserCodeRequestPolicy
import com.tangem.common.doOnResult
import com.tangem.domain.card.ResetCardUseCase
import com.tangem.domain.card.models.ResetCardError
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.tap.domain.sdk.TangemSdkManager

internal class DefaultResetCardUseCase(
    private val tangemSdkManager: TangemSdkManager,
) : ResetCardUseCase {

    override suspend fun invoke(card: CardDTO): Either<ResetCardError, Unit> = either {
        enterRequiredAccessCode(card) {
            resetToFactorySettings(
                cardId = card.cardId,
                allowsRequestAccessCodeFromRepository = true,
            )
        }
            .mapToEither()
    }

    override suspend fun invoke(
        cardNumber: Int,
        card: CardDTO,
        userWalletId: UserWalletId,
    ): Either<ResetCardError, Unit> = either {
        enterRequiredAccessCode(card) {
            resetBackupCard(cardNumber = cardNumber, userWalletId = userWalletId)
        }
            .mapToEither()
    }

    private suspend fun enterRequiredAccessCode(
        card: CardDTO,
        task: suspend TangemSdkManager.() -> CompletionResult<*>,
    ): CompletionResult<*> {
        val policyBeforeReset = tangemSdkManager.userCodeRequestPolicy
        requestMandatoryAccessCodeEntry(card)

        return tangemSdkManager.task()
            .doOnResult { tangemSdkManager.setUserCodeRequestPolicy(policyBeforeReset) }
    }

    private fun requestMandatoryAccessCodeEntry(card: CardDTO) {
        val type = if (card.isAccessCodeSet) {
            UserCodeType.AccessCode
        } else if (card.isPasscodeSet == true) {
            UserCodeType.Passcode
        } else {
            null
        }

        type?.let {
            tangemSdkManager.setUserCodeRequestPolicy(policy = UserCodeRequestPolicy.Always(type))
        }
    }

    private fun CompletionResult<*>.mapToEither(): Either<ResetCardError, Unit> {
        return when (this) {
            is CompletionResult.Failure -> error.mapToDomainError().left()
            is CompletionResult.Success -> Unit.right()
        }
    }

    private fun TangemError.mapToDomainError(): ResetCardError {
        return if (this is TangemSdkError.UserCancelled) ResetCardError.UserCanceled else ResetCardError.AnotherSdkError
    }
}