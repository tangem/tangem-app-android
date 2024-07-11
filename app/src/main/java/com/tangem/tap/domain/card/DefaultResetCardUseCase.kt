package com.tangem.tap.domain.card

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.fx.coroutines.ResourceScope
import arrow.fx.coroutines.resourceScope
import com.tangem.common.CompletionResult
import com.tangem.common.UserCodeType
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

    override suspend fun invoke(card: CardDTO): Either<ResetCardError, Unit> = resourceScope {
        either {
            withUserCodeRequestPolicy(card)

            tangemSdkManager.resetToFactorySettings(
                cardId = card.cardId,
                allowsRequestAccessCodeFromRepository = true,
            ).bind(raise = this)
        }
    }

    override suspend fun invoke(
        cardNumber: Int,
        card: CardDTO,
        userWalletId: UserWalletId,
    ): Either<ResetCardError, Unit> = resourceScope {
        either {
            withUserCodeRequestPolicy(card)

            tangemSdkManager.resetBackupCard(
                cardNumber = cardNumber,
                userWalletId = userWalletId,
            ).bind(raise = this)
        }
    }

    private suspend fun ResourceScope.withUserCodeRequestPolicy(card: CardDTO) {
        install(
            acquire = {
                val policyBeforeReset = tangemSdkManager.userCodeRequestPolicy
                requestMandatoryAccessCodeEntry(card)

                policyBeforeReset
            },
            release = { prevPolicy, _ ->
                tangemSdkManager.setUserCodeRequestPolicy(prevPolicy)
            },
        )
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

    private fun CompletionResult<*>.bind(raise: Raise<ResetCardError>) {
        return when (this) {
            is CompletionResult.Failure -> {
                val domainError = error.mapToDomainError()

                raise.raise(domainError)
            }
            is CompletionResult.Success -> { /* no-op */
            }
        }
    }

    private fun TangemError.mapToDomainError(): ResetCardError {
        return if (this is TangemSdkError.UserCancelled) ResetCardError.UserCanceled else ResetCardError.SdkError
    }
}