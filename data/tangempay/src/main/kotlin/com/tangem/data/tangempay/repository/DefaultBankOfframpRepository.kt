package com.tangem.data.tangempay.repository

import arrow.core.Either
import com.tangem.data.tangempay.util.BankAccountConverter
import com.tangem.data.tangempay.util.BankOfframpOptionsConverter
import com.tangem.domain.models.tangempay.TangemPayProduct
import com.tangem.domain.models.tangempay.id
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.model.bankofframp.BankAccount
import com.tangem.domain.tangempay.model.bankofframp.BankOfframpError
import com.tangem.domain.tangempay.model.bankofframp.BankOfframpOptions
import com.tangem.domain.tangempay.model.bankofframp.BankRail
import com.tangem.domain.tangempay.model.bankofframp.CustomerName
import com.tangem.domain.tangempay.repository.BankOfframpRepository
import com.tangem.domain.tangempay.error.TangemPayApiError
import com.tangem.spend.datasource.tangempay.TangemPayApi
import javax.inject.Inject

internal class DefaultBankOfframpRepository @Inject constructor(
    private val tangemPayApi: TangemPayApi,
    private val requestPerformer: TangemPayRequestPerformer,
) : BankOfframpRepository {

    override suspend fun getBankAccounts(
        userWalletId: UserWalletId,
        rail: BankRail?,
    ): Either<BankOfframpError, List<BankAccount>> {
        return requestPerformer.performRequest(userWalletId) { authHeader ->
            tangemPayApi.getBankAccounts(authHeader = authHeader, rail = rail?.name)
        }
            .map { response -> response.result.orEmpty().mapNotNull(BankAccountConverter::convert) }
            .mapLeft { error -> error.toBankOfframpError(operation = "getBankAccounts") }
    }

    override suspend fun getOptions(
        userWalletId: UserWalletId,
        product: TangemPayProduct,
    ): Either<BankOfframpError, BankOfframpOptions> {
        return requestPerformer.performRequest(userWalletId) { authHeader ->
            tangemPayApi.getBankOfframpOptions(authHeader = authHeader, accountType = product.id)
        }
            .map { response -> response.result?.let(BankOfframpOptionsConverter::convert) ?: EMPTY_OPTIONS }
            .mapLeft { error -> error.toBankOfframpError(operation = "getOptions") }
    }

    override suspend fun getCustomerName(userWalletId: UserWalletId): Either<BankOfframpError, CustomerName> {
        return requestPerformer.performRequest(userWalletId) { authHeader ->
            tangemPayApi.getCustomerName(authHeader = authHeader)
        }
            .map { response ->
                CustomerName(firstName = response.result?.firstName, lastName = response.result?.lastName)
            }
            .mapLeft { error -> error.toBankOfframpError(operation = "getCustomerName") }
    }

    /**
     * The catalog calls have no failure of their own for the UI to distinguish: whatever went wrong, the bank
     * section shows one retryable error. The API error is kept as the cause so it still reaches the logs.
     */
    private fun TangemPayApiError.toBankOfframpError(operation: String) = BankOfframpError.Unknown(
        cause = IllegalStateException("Bank off-ramp $operation failed: $this"),
    )

    private companion object {
        val EMPTY_OPTIONS = BankOfframpOptions(sources = emptyList(), destinations = emptyList())
    }
}