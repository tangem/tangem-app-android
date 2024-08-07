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
import com.tangem.domain.card.ResetCardUserCodeParams
import com.tangem.domain.card.models.ResetCardError
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.tap.domain.sdk.TangemSdkManager

internal class DefaultResetCardUseCase(
    private val tangemSdkManager: TangemSdkManager,
) : ResetCardUseCase {

    override suspend fun invoke(cardId: String, params: ResetCardUserCodeParams): Either<ResetCardError, Boolean> =
        resourceScope {
            either {
                withUserCodeRequestPolicy(params)

                tangemSdkManager.resetToFactorySettings(
                    cardId = cardId,
                    allowsRequestAccessCodeFromRepository = true,
                ).bind(raise = this)
            }
        }

    override suspend fun invoke(
        cardNumber: Int,
        params: ResetCardUserCodeParams,
        userWalletId: UserWalletId,
    ): Either<ResetCardError, Boolean> = resourceScope {
        either {
            withUserCodeRequestPolicy(params)

            tangemSdkManager.resetBackupCard(
                cardNumber = cardNumber,
                userWalletId = userWalletId,
            ).bind(raise = this)
        }
    }

    private suspend fun ResourceScope.withUserCodeRequestPolicy(params: ResetCardUserCodeParams) {
        install(
            acquire = {
                val policyBeforeReset = tangemSdkManager.userCodeRequestPolicy
                requestMandatoryAccessCodeEntry(params)

                policyBeforeReset
            },
            release = { prevPolicy, _ ->
                tangemSdkManager.setUserCodeRequestPolicy(prevPolicy)
            },
        )
    }

    private fun requestMandatoryAccessCodeEntry(params: ResetCardUserCodeParams) {
        val type = if (params.isAccessCodeSet) {
            UserCodeType.AccessCode
        } else if (params.isPasscodeSet == true) {
            UserCodeType.Passcode
        } else {
            null
        }

        type?.let {
            tangemSdkManager.setUserCodeRequestPolicy(policy = UserCodeRequestPolicy.Always(type))
        }
    }

    private fun CompletionResult<Boolean>.bind(raise: Raise<ResetCardError>): Boolean {
        return when (this) {
            is CompletionResult.Failure -> {
                val domainError = error.mapToDomainError()

                raise.raise(domainError)
            }
            is CompletionResult.Success -> data
        }
    }

    private fun TangemError.mapToDomainError(): ResetCardError {
        return if (this is TangemSdkError.UserCancelled) ResetCardError.UserCanceled else ResetCardError.SdkError
    }
}