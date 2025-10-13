package com.tangem.domain.account.status.usecase

import arrow.core.Option
import arrow.core.none
import arrow.core.toOption
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.model.AccountCryptoCurrencyStatus
import com.tangem.domain.account.status.producer.SingleAccountStatusListProducer
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.utils.AccountCryptoCurrencyStatusFinder
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

/**
 * Use case to retrieve the status of a specific cryptocurrency associated with an account.
 *
 * @property singleAccountStatusListSupplier supplier to get the list of account statuses.
 *
[REDACTED_AUTHOR]
 */
class GetAccountCurrencyStatusUseCase(
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
) {

    /**
     * Invokes the use case to get the status of a specific cryptocurrency for a given user wallet.
     *
     * @param userWalletId the ID of the user wallet.
     * @param currency the cryptocurrency for which the status is to be retrieved.
     * @return an [Option] containing [AccountCryptoCurrencyStatus] if found, otherwise None.
     */
    suspend fun invokeSync(userWalletId: UserWalletId, currency: CryptoCurrency): Option<AccountCryptoCurrencyStatus> {
        return invokeSync(userWalletId = userWalletId, currencyId = currency.id, network = currency.network)
    }

    /**
     * Invokes the use case to get the status of a specific cryptocurrency by its ID for a given user wallet and network.
     * If the [network] is null, it searches across all accounts for the cryptocurrency.
     *
     * @param userWalletId the ID of the user wallet.
     * @param currencyId the ID of the cryptocurrency.
     * @param network the network associated with the cryptocurrency, can be null.
     * @return an [Option] containing [AccountCryptoCurrencyStatus] if found, otherwise None.
     */
    suspend fun invokeSync(
        userWalletId: UserWalletId,
        currencyId: CryptoCurrency.ID,
        network: Network?,
    ): Option<AccountCryptoCurrencyStatus> {
        val accountStatusList = singleAccountStatusListSupplier.getSyncOrNull(
            params = SingleAccountStatusListProducer.Params(userWalletId),
        ) ?: return none()

        return accountStatusList
            .toAccountCryptoCurrencyStatus(currencyId, network)
            .toOption()
    }

    /**
     * Retrieves the status of a specific cryptocurrency for a given user wallet as a [Flow].
     *
     * @param userWalletId The ID of the user wallet.
     * @param currency The cryptocurrency for which the status is to be retrieved.
     * @return A [Flow] emitting [AccountCryptoCurrencyStatus] if found.
     */
    operator fun invoke(userWalletId: UserWalletId, currency: CryptoCurrency): Flow<AccountCryptoCurrencyStatus> {
        return invoke(userWalletId = userWalletId, currencyId = currency.id, network = currency.network)
    }

    /**
     * Retrieves the status of a specific cryptocurrency by its ID for a given user wallet and network as a [Flow].
     *
     * @param userWalletId The ID of the user wallet.
     * @param currencyId The ID of the cryptocurrency.
     * @param network The network associated with the cryptocurrency, can be null.
     * @return A [Flow] emitting [AccountCryptoCurrencyStatus] if found.
     */
    operator fun invoke(
        userWalletId: UserWalletId,
        currencyId: CryptoCurrency.ID,
        network: Network?,
    ): Flow<AccountCryptoCurrencyStatus> {
        return singleAccountStatusListSupplier(
            params = SingleAccountStatusListProducer.Params(userWalletId),
        )
            .mapNotNull { accountStatusList ->
                accountStatusList.toAccountCryptoCurrencyStatus(currencyId, network)
            }
    }

    private fun AccountStatusList.toAccountCryptoCurrencyStatus(
        currencyId: CryptoCurrency.ID,
        network: Network?,
    ): AccountCryptoCurrencyStatus? {
        return AccountCryptoCurrencyStatusFinder(
            accountStatusList = this,
            currencyId = currencyId,
            network = network,
        )
    }
}