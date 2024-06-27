package com.tangem.tap.domain.card

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.right
import com.tangem.common.*
import com.tangem.common.core.TangemError
import com.tangem.common.core.TangemSdkError
import com.tangem.common.core.UserCodeRequestPolicy
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
            tangemSdkManager.resetToFactorySettings(
                cardId = card.cardId,
                allowsRequestAccessCodeFromRepository = true,
            )
                .doOnSuccess { Unit.right() }
                .doOnFailure { raise(it.mapToDomainError()) }
        }
    }

    override suspend fun invoke(
        cardNumber: Int,
        card: CardDTO,
        userWalletId: UserWalletId,
    ): Either<ResetCardError, Unit> = either {
        enterRequiredAccessCode(card) {
            tangemSdkManager.resetBackupCard(cardNumber, userWalletId)
                .doOnSuccess { Unit.right() }
                .doOnFailure { raise(it.mapToDomainError()) }
        }
    }

    private suspend fun enterRequiredAccessCode(
        card: CardDTO,
        task: suspend TangemSdkManager.() -> CompletionResult<*>,
    ) {
        val policyBeforeReset = tangemSdkManager.userCodeRequestPolicy
        requestMandatoryAccessCodeEntry(card)

        tangemSdkManager.task()
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

    private fun TangemError.mapToDomainError(): ResetCardError {
        return if (this is TangemSdkError.UserCancelled) ResetCardError.UserCanceled else ResetCardError.AnotherSdkError
    }
}