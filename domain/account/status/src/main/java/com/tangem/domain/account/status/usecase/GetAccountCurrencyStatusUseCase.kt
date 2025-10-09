package com.tangem.domain.account.status.usecase

import arrow.core.Option
import arrow.core.none
import arrow.core.toOption
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.model.AccountCryptoCurrencyStatus
import com.tangem.domain.account.status.producer.SingleAccountStatusListProducer
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.lib.crypto.derivation.AccountNodeRecognizer

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
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        currency: CryptoCurrency,
    ): Option<AccountCryptoCurrencyStatus> {
        return invoke(userWalletId = userWalletId, currencyId = currency.id, network = currency.network)
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
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        currencyId: CryptoCurrency.ID,
        network: Network?,
    ): Option<AccountCryptoCurrencyStatus> {
        val accountStatusList = singleAccountStatusListSupplier.getSyncOrNull(
            params = SingleAccountStatusListProducer.Params(userWalletId),
        ) ?: return none()

        return accountStatusList.getExpectedAccountStatuses(network)
            .asSequence()
            .filterIsInstance<AccountStatus.CryptoPortfolio>()
            .mapNotNull { accountStatus ->
                val status = accountStatus.flattenCurrencies().firstOrNull { it.currency.id == currencyId }
                    ?: return@mapNotNull null

                AccountCryptoCurrencyStatus(account = accountStatus.account, status = status)
            }
            .firstOrNull()
            .toOption()
    }

    /**
     * Retrieves the expected account statuses based on the provided [network].
     * If the [network] is null, all account statuses are returned.
     * If the network has a specific derivation index, it filters the accounts accordingly.
     *
     * @param network the network to filter accounts by, can be null.
     * @return a set of [AccountStatus] that match the expected criteria.
     */
    private fun AccountStatusList.getExpectedAccountStatuses(network: Network?): Set<AccountStatus> {
        val possibleAccountIndex = network?.getAccountIndexOrNull()

        return when (possibleAccountIndex) {
            // currency can be in any account
            null -> accountStatuses
            // currency only in the main account
            DerivationIndex.Main.value -> setOf(mainAccount)
            // currency only in the account with specific derivation index or in the main account
            else -> {
                val accountStatus = accountStatuses.firstOrNull {
                    val cryptoPortfolio = it.account as? Account.CryptoPortfolio ?: return@firstOrNull false

                    cryptoPortfolio.derivationIndex.value == possibleAccountIndex
                }

                setOfNotNull(accountStatus, mainAccount)
            }
        }
    }

    private fun Network.getAccountIndexOrNull(): Int? {
        val blockchain = Blockchain.fromNetworkId(networkId = rawId) ?: return null
        val recognizer = AccountNodeRecognizer(blockchain)

        return recognizer.recognize(derivationPath)?.toInt()
    }
}