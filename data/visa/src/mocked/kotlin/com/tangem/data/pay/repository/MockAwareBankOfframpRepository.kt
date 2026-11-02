package com.tangem.data.pay.repository

import arrow.core.Either
import com.tangem.core.remote.config.ApiEnvironment
import com.tangem.core.remote.config.managers.ApiConfigsManager
import com.tangem.domain.models.pay.TangemPayProduct
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.bankofframp.BankAccount
import com.tangem.domain.pay.model.bankofframp.BankOfframpError
import com.tangem.domain.pay.model.bankofframp.BankOfframpOptions
import com.tangem.domain.pay.model.bankofframp.BankRail
import com.tangem.domain.pay.repository.BankOfframpRepository
import com.tangem.spend.datasource.config.TangemPay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class MockAwareBankOfframpRepository @Inject constructor(
    private val real: DefaultBankOfframpRepository,
    private val apiConfigsManager: ApiConfigsManager,
) : BankOfframpRepository {

    private val isMockMode: Boolean
        get() = apiConfigsManager.getEnvironmentConfig(TangemPay.Bff.ID).environment == ApiEnvironment.MOCK

    override suspend fun getBankAccounts(
        userWalletId: UserWalletId,
        rail: BankRail?,
    ): Either<BankOfframpError, List<BankAccount>> {
        if (!isMockMode) return real.getBankAccounts(userWalletId = userWalletId, rail = rail)
        return real.getBankAccounts(userWalletId = userWalletId, rail = null)
            .map { accounts -> withMockSpeiRecipient(accounts = accounts, rail = rail) }
    }

    override suspend fun getOptions(
        userWalletId: UserWalletId,
        product: TangemPayProduct,
    ): Either<BankOfframpError, BankOfframpOptions> {
        val options = real.getOptions(userWalletId = userWalletId, product = product)
        if (!isMockMode) return options
        return options.map { current -> withMockSpeiDestination(options = current, product = product) }
    }
}