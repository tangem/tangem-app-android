package com.tangem.domain.account.status.usecase

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.catch
import com.tangem.domain.account.producer.SingleAccountListProducer
import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.core.utils.eitherOn
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.networks.multi.MultiNetworkStatusFetcher
import com.tangem.domain.networks.utils.NetworksCleaner
import com.tangem.domain.quotes.multi.MultiQuoteStatusFetcher
import com.tangem.domain.staking.StakingIdFactory
import com.tangem.domain.staking.multi.MultiYieldBalanceFetcher
import com.tangem.domain.staking.utils.StakingCleaner
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.wallets.derivations.DerivationsRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.*
import timber.log.Timber

/**
 * Use case for saving crypto currencies to a specific account.
 *
 * @property singleAccountListSupplier Supplier to get account details.
 * @property currenciesRepository Repository for managing currencies.
 * @property derivationsRepository Repository for deriving public keys.
 * @property multiNetworkStatusFetcher Fetcher for updating network statuses.
 * @property multiQuoteStatusFetcher Fetcher for updating quote statuses.
 * @property multiYieldBalanceFetcher Fetcher for updating yield balances.
 * @property stakingIdFactory Factory for creating staking IDs.
 * @property networksCleaner Cleaner for removing obsolete network data.
 * @property stakingCleaner Cleaner for removing obsolete staking data.
 * @property dispatchers Coroutine dispatchers for managing threading.
 *
[REDACTED_AUTHOR]
 */
@Suppress("LongParameterList")
class SaveCryptoCurrenciesUseCase(
    private val singleAccountListSupplier: SingleAccountListSupplier,
    private val accountsCRUDRepository: AccountsCRUDRepository,
    private val currenciesRepository: CurrenciesRepository,
    private val derivationsRepository: DerivationsRepository,
    private val multiNetworkStatusFetcher: MultiNetworkStatusFetcher,
    private val multiQuoteStatusFetcher: MultiQuoteStatusFetcher,
    private val multiYieldBalanceFetcher: MultiYieldBalanceFetcher,
    private val stakingIdFactory: StakingIdFactory,
    private val networksCleaner: NetworksCleaner,
    private val stakingCleaner: StakingCleaner,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    suspend operator fun invoke(
        accountId: AccountId,
        add: CryptoCurrency? = null,
        remove: CryptoCurrency? = null,
    ): Either<Throwable, Unit> {
        return invoke(accountId = accountId, add = listOfNotNull(add), remove = listOfNotNull(remove))
    }

    suspend operator fun invoke(
        accountId: AccountId,
        add: List<CryptoCurrency> = emptyList(),
        remove: List<CryptoCurrency> = emptyList(),
    ): Either<Throwable, Unit> = eitherOn(dispatchers.default) {
        if (add.isEmpty() && remove.isEmpty()) {
            Timber.d("No currencies to add or remove, skipping")
            return@eitherOn
        }

        val userWalletId = accountId.userWalletId
        withContext(NonCancellable) {
            val account = getAccount(accountId = accountId)

            val modifiedCurrencyList = account.cryptoCurrencies.modify(add = add, remove = remove)

            saveAccount(
                account = account.copy(cryptoCurrencies = modifiedCurrencyList.total.toSet()),
            )

            derivePublicKeys(userWalletId = userWalletId, currencies = modifiedCurrencyList.added)

            val jobs = refreshBalances(userWalletId = userWalletId, currencies = modifiedCurrencyList.added) +
                clearMetadata(userWalletId = userWalletId, currencies = modifiedCurrencyList.removed)

            jobs.joinAll()
        }
    }

    suspend fun add(
        accountId: AccountId,
        networkId: String,
        contractAddress: String,
    ): Either<Throwable, CryptoCurrency> = eitherOn(dispatchers.default) {
        val userWalletId = accountId.userWalletId

        withContext(NonCancellable) {
            val account = getAccount(accountId = accountId)

            val foundToken = account.cryptoCurrencies
                .filterIsInstance<CryptoCurrency.Token>()
                .firstOrNull {
                    it.network.backendId == networkId &&
                        !it.isCustom &&
                        it.contractAddress.equals(contractAddress, true)
                }

            if (foundToken != null) return@withContext foundToken

            val tokenToAdd = findToken(userWalletId, contractAddress, networkId)

            refreshBalances(userWalletId = userWalletId, currencies = listOf(tokenToAdd)).joinAll()

            tokenToAdd
        }
    }

    private suspend fun Raise<Throwable>.getAccount(accountId: AccountId): Account.CryptoPortfolio {
        val accountList = singleAccountListSupplier.getSyncOrNull(
            params = SingleAccountListProducer.Params(userWalletId = accountId.userWalletId),
        ) ?: raise(IllegalStateException("No accounts for wallet ${accountId.userWalletId}"))

        return accountList.accounts.firstOrNull { it.accountId == accountId } as? Account.CryptoPortfolio
            ?: raise(IllegalStateException("No account with id $accountId"))
    }

    private fun Set<CryptoCurrency>.modify(
        add: List<CryptoCurrency>,
        remove: List<CryptoCurrency>,
    ): ModifiedCurrencyList {
        val mutableCurrencies = this.toMutableList()
        val added = mutableListOf<CryptoCurrency>()
        val removed = mutableListOf<CryptoCurrency>()

        val existingCurrenciesById = mutableCurrencies.associateBy(::TempID)

        add.groupByNetwork { !existingCurrenciesById.containsKey(it) }
            .forEach { (network, currenciesById) ->
                val coinTempId = TempID(network)

                if (!existingCurrenciesById.containsKey(coinTempId)) {
                    val coin = currenciesById[coinTempId]

                    if (coin != null) {
                        mutableCurrencies.add(coin)
                        added.add(coin)

                        currenciesById.remove(coinTempId)
                    } else {
                        val createdCoin = currenciesRepository.createCoinCurrency(network)
                        mutableCurrencies.add(createdCoin)
                        added.add(createdCoin)
                    }
                }

                mutableCurrencies.addAll(currenciesById.values)
                added.addAll(currenciesById.values)
            }

        remove.groupByNetwork(valuePredicate = existingCurrenciesById::containsKey)
            .forEach { (network, currenciesById) ->
                val coinTempId = TempID(network)

                if (currenciesById.containsKey(coinTempId)) {
                    val existingNetworkCurrenciesCount = mutableCurrencies.count { it.network == network }

                    if (existingNetworkCurrenciesCount != currenciesById.size) {
                        return@forEach
                    }
                }

                mutableCurrencies.removeAll(currenciesById.values)
                removed.addAll(currenciesById.values)
            }

        return ModifiedCurrencyList(added = added, removed = removed, total = mutableCurrencies)
    }

    private suspend fun Raise<Throwable>.saveAccount(account: Account.CryptoPortfolio) {
        catch(
            block = { accountsCRUDRepository.saveAccount(account) },
            catch = ::raise,
        )
    }

    private suspend fun Raise<Throwable>.derivePublicKeys(
        userWalletId: UserWalletId,
        currencies: List<CryptoCurrency>,
    ) {
        catch(
            block = { derivationsRepository.derivePublicKeys(userWalletId = userWalletId, currencies = currencies) },
            catch = ::raise,
        )
    }

    private fun List<CryptoCurrency>.groupByNetwork(
        valuePredicate: (TempID) -> Boolean,
    ): LinkedHashMap<Network, MutableMap<TempID, CryptoCurrency>> {
        val destination = LinkedHashMap<Network, MutableMap<TempID, CryptoCurrency>>()

        for (currency in this) {
            val key = currency.network
            val mutableMap = destination.getOrPut(key) { mutableMapOf() }

            val id = TempID(currency)

            if (valuePredicate(id)) {
                mutableMap.put(id, currency)
            }
        }

        return destination
    }

    private suspend fun Raise<Throwable>.findToken(
        userWalletId: UserWalletId,
        networkId: String,
        contractAddress: String,
    ): CryptoCurrency.Token {
        return catch(
            block = {
                currenciesRepository.createTokenCurrency(
                    userWalletId = userWalletId,
                    networkId = networkId,
                    contractAddress = contractAddress,
                )
            },
            catch = ::raise,
        )
    }

    private suspend fun refreshBalances(userWalletId: UserWalletId, currencies: List<CryptoCurrency>): List<Job> {
        if (currencies.isEmpty()) return emptyList()

        return coroutineScope {
            listOf(
                launch { refreshNetworks(userWalletId = userWalletId, currencies = currencies) },
                launch { refreshYieldBalances(userWalletId = userWalletId, currencies = currencies) },
                launch { refreshQuotes(currencies = currencies) },
            )
        }
    }

    private suspend fun refreshNetworks(userWalletId: UserWalletId, currencies: List<CryptoCurrency>) {
        multiNetworkStatusFetcher(
            params = MultiNetworkStatusFetcher.Params(
                userWalletId = userWalletId,
                networks = currencies.mapTo(hashSetOf(), CryptoCurrency::network),
            ),
        )

        accountsCRUDRepository.syncTokens(userWalletId)
    }

    private suspend fun refreshYieldBalances(userWalletId: UserWalletId, currencies: List<CryptoCurrency>) {
        val stakingIds = currencies.mapNotNullTo(hashSetOf()) {
            stakingIdFactory.create(userWalletId = userWalletId, cryptoCurrency = it).getOrNull()
        }

        multiYieldBalanceFetcher(
            params = MultiYieldBalanceFetcher.Params(userWalletId = userWalletId, stakingIds = stakingIds),
        )
    }

    private suspend fun refreshQuotes(currencies: List<CryptoCurrency>) {
        multiQuoteStatusFetcher(
            params = MultiQuoteStatusFetcher.Params(
                currenciesIds = currencies.mapNotNullTo(hashSetOf()) { it.id.rawCurrencyId },
                appCurrencyId = null,
            ),
        )
    }

    private suspend fun clearMetadata(userWalletId: UserWalletId, currencies: List<CryptoCurrency>): List<Job> {
        if (currencies.isEmpty()) return emptyList()

        return coroutineScope {
            listOf(
                launch { networksCleaner(userWalletId = userWalletId, currencies = currencies) },
                launch { clearStaking(userWalletId = userWalletId, currencies = currencies) },
            )
        }
    }

    private suspend fun clearStaking(userWalletId: UserWalletId, currencies: List<CryptoCurrency>) {
        val stakingIds = currencies.mapNotNullTo(hashSetOf()) {
            stakingIdFactory.create(userWalletId = userWalletId, cryptoCurrency = it).getOrNull()
        }

        stakingCleaner(userWalletId = userWalletId, stakingIds = stakingIds)
    }

    private data class TempID(
        val networkId: String,
        val derivationPath: Network.DerivationPath,
        val contractAddress: String?,
    ) {

        constructor(network: Network) : this(
            networkId = network.backendId,
            derivationPath = network.derivationPath,
            contractAddress = null,
        )

        constructor(currency: CryptoCurrency) : this(
            networkId = currency.network.backendId,
            derivationPath = currency.network.derivationPath,
            contractAddress = (currency as? CryptoCurrency.Token)?.contractAddress,
        )
    }

    private data class ModifiedCurrencyList(
        val added: List<CryptoCurrency>,
        val removed: List<CryptoCurrency>,
        val total: List<CryptoCurrency>,
    )
}