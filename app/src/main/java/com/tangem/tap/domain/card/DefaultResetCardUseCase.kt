package com.tangem.tap.domain.card

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.right
import com.tangem.common.CompletionResult
import com.tangem.common.UserCodeType
import com.tangem.common.core.TangemSdkError
import com.tangem.common.core.UserCodeRequestPolicy
import com.tangem.common.doOnResult
import com.tangem.domain.card.ResetCardUseCase
import com.tangem.domain.card.models.ResetCardError
import com.tangem.domain.models.scan.CardDTO
import com.tangem.tap.domain.sdk.TangemSdkManager

internal class DefaultResetCardUseCase(
    private val tangemSdkManager: TangemSdkManager,
) : ResetCardUseCase {

    override suspend fun invoke(card: CardDTO): Either<ResetCardError, Unit> = either {
        val policyBeforeReset = tangemSdkManager.userCodeRequestPolicy
        requestMandatoryAccessCodeEntry(card)

        val result = tangemSdkManager.resetToFactorySettings(
            cardId = card.cardId,
            allowsRequestAccessCodeFromRepository = true,
        )
            .doOnResult { tangemSdkManager.setUserCodeRequestPolicy(policyBeforeReset) }

        when (result) {
            is CompletionResult.Success -> Unit.right()
            is CompletionResult.Failure -> {
                if (result.error is TangemSdkError.UserCancelled) {
                    raise(ResetCardError.UserCanceled)
                } else {
                    raise(ResetCardError.AnotherSdkError)
                }
            }
        }
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
}
