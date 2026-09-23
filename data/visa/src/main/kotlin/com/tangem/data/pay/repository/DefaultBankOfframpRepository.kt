package com.tangem.data.pay.repository

import arrow.core.Either
import com.tangem.data.pay.util.BankAccountConverter
import com.tangem.data.pay.util.BankOfframpOptionsConverter
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.bankofframp.BankAccount
import com.tangem.domain.pay.model.bankofframp.BankOfframpError
import com.tangem.domain.pay.model.bankofframp.BankOfframpOptions
import com.tangem.domain.pay.repository.BankOfframpRepository
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.spend.datasource.pay.TangemPayApi
import javax.inject.Inject

internal class DefaultBankOfframpRepository @Inject constructor(
    private val tangemPayApi: TangemPayApi,
    private val requestPerformer: TangemPayRequestPerformer,
) : BankOfframpRepository {

    override suspend fun getBankAccounts(userWalletId: UserWalletId): Either<BankOfframpError, List<BankAccount>> {
        return requestPerformer.performRequest(userWalletId) { authHeader ->
            tangemPayApi.getBankAccounts(authHeader = authHeader)
        }
            .map { response -> response.result.orEmpty().mapNotNull(BankAccountConverter::convert) }
            .mapLeft { error -> error.toBankOfframpError(operation = "getBankAccounts") }
    }

    override suspend fun getOptions(userWalletId: UserWalletId): Either<BankOfframpError, BankOfframpOptions> {
        return requestPerformer.performRequest(userWalletId) { authHeader ->
            tangemPayApi.getBankOfframpOptions(authHeader = authHeader)
        }
            .map { response -> response.result?.let(BankOfframpOptionsConverter::convert) ?: EMPTY_OPTIONS }
            .mapLeft { error -> error.toBankOfframpError(operation = "getOptions") }
    }

    /**
     * The catalog calls have no failure of their own for the UI to distinguish: whatever went wrong, the bank
     * section shows one retryable error. The API error is kept as the cause so it still reaches the logs.
     */
    private fun VisaApiError.toBankOfframpError(operation: String) = BankOfframpError.Unknown(
        cause = IllegalStateException("Bank off-ramp $operation failed: $this"),
    )

    private companion object {
        val EMPTY_OPTIONS = BankOfframpOptions(sources = emptyList(), destinations = emptyList())
    }
}