package com.tangem.domain.account.status.usecase

import arrow.core.NonEmptyList
import arrow.core.None
import arrow.core.Option
import arrow.core.raise.OptionRaise
import arrow.core.raise.RaiseDSL
import arrow.core.raise.option
import arrow.core.toNonEmptyListOrNull
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.producer.SingleAccountListProducer
import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.domain.account.status.model.AccountCryptoCurrency
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.getAddress
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isMultiCurrency
import com.tangem.domain.networks.multi.MultiNetworkStatusProducer
import com.tangem.domain.networks.multi.MultiNetworkStatusSupplier
import timber.log.Timber
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

private typealias WalletIdWithNetworkId = Pair<UserWalletId, Network.ID>

/**
 * Use case to retrieve an [AccountCryptoCurrency] based on a provided address.
 *
 * @property accountsCRUDRepository Repository to access user wallets.
 * @property multiNetworkStatusSupplier Supplier to get network status for multiple networks.
 * @property singleAccountListSupplier Supplier to get account lists for a single wallet.
 *
[REDACTED_AUTHOR]
 */
class GetAccountCurrencyByAddressUseCase(
    private val accountsCRUDRepository: AccountsCRUDRepository,
    private val multiNetworkStatusSupplier: MultiNetworkStatusSupplier,
    private val singleAccountListSupplier: SingleAccountListSupplier,
) {

    /**
     * Invokes the use case to get an [AccountCryptoCurrency] for the given address.
     *
     * @param address The address to look up.
     * @return An [Option] containing the [AccountCryptoCurrency] if found, or [None] if not found or if any validation
     * fails.
     */
    suspend operator fun invoke(address: String): Option<AccountCryptoCurrency> = option {
        validate(address)

        val userWalletIds = getUserWalletIds()

        val (walletId, networkId) = getWalletIdWithNetworkId(userWalletIds, address)

        val accountList = getAccountList(userWalletId = walletId)

        getAccountCryptoCurrency(accountList, networkId)
    }

    private fun OptionRaise.validate(address: String) {
        ensureNotNull(address.takeIf { it.isNotEmpty() }) { "Address is empty" }
    }

    private fun OptionRaise.getUserWalletIds(): NonEmptyList<UserWalletId> {
        val userWalletIds = accountsCRUDRepository.getUserWalletsSync()
            .filter(UserWallet::isMultiCurrency)
            .map(UserWallet::walletId)
            .toNonEmptyListOrNull()

        return ensureNotNull(userWalletIds) { "No multi-currency wallets found" }
    }

    private suspend fun OptionRaise.getWalletIdWithNetworkId(
        userWalletIds: NonEmptyList<UserWalletId>,
        address: String,
    ): WalletIdWithNetworkId {
        var pair: WalletIdWithNetworkId? = null

        for (id in userWalletIds) {
            val networkStatus = multiNetworkStatusSupplier.getSyncOrNull(
                params = MultiNetworkStatusProducer.Params(userWalletId = id),
                timeMillis = 1000L,
            )
                ?.firstOrNull { it.getAddress() == address }

            if (networkStatus != null) {
                pair = id to networkStatus.network.id
                break
            }
        }

        return ensureNotNull(value = pair) { "No network status found for address: $address" }
    }

    private suspend fun OptionRaise.getAccountList(userWalletId: UserWalletId): AccountList {
        val accountList = singleAccountListSupplier.getSyncOrNull(
            params = SingleAccountListProducer.Params(userWalletId = userWalletId),
        )

        return ensureNotNull(accountList) { "No account list found for walletId: $userWalletId" }
    }

    private fun OptionRaise.getAccountCryptoCurrency(
        accountList: AccountList,
        networkId: Network.ID,
    ): AccountCryptoCurrency {
        val result = accountList.accounts.asSequence()
            .filterIsInstance<Account.CryptoPortfolio>()
            .mapNotNull { account ->
                val currency = account.cryptoCurrencies.firstOrNull { it.network.id == networkId }
                    ?: return@mapNotNull null

                AccountCryptoCurrency(account = account, cryptoCurrency = currency)
            }
            .firstOrNull()

        return ensureNotNull(result) {
            "No account found for network: $networkId in walletId: ${accountList.userWalletId}"
        }
    }

    @OptIn(ExperimentalContracts::class)
    @RaiseDSL
    inline fun <B : Any> OptionRaise.ensureNotNull(value: B?, message: () -> String): B {
        contract {
            returns() implies (value != null)
        }

        return value ?: run {
            Timber.d(message())
            raise(None)
        }
    }
}