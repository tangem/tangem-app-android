package com.tangem.domain.account.status.usecase

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.catch
import com.tangem.domain.account.producer.SingleAccountListProducer
import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.domain.account.status.utils.CryptoCurrencyBalanceFetcher
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.core.utils.eitherOn
import com.tangem.domain.express.ExpressServiceFetcher
import com.tangem.domain.express.models.ExpressAsset
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.networks.utils.NetworksCleaner
import com.tangem.domain.staking.StakingIdFactory
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
 * @property accountsCRUDRepository Repository for performing CRUD operations on accounts.
 * @property currenciesRepository Repository for managing currencies.
 * @property derivationsRepository Repository for deriving public keys.
 * @property cryptoCurrencyBalanceFetcher Fetcher for updating crypto currency balances.
 * @property stakingIdFactory Factory for creating staking IDs.
 * @property networksCleaner Cleaner for removing obsolete network data.
 * @property stakingCleaner Cleaner for removing obsolete staking data.
 * @property expressServiceFetcher Fetcher for updating express service data.
 * @property parallelUpdatingScope Coroutine scope for parallel updates.
 * @property dispatchers Coroutine dispatchers for managing threading.
 *
[REDACTED_AUTHOR]
 */
@Suppress("LongParameterList")
class ManageCryptoCurrenciesUseCase(
    private val singleAccountListSupplier: SingleAccountListSupplier,
    private val accountsCRUDRepository: AccountsCRUDRepository,
    private val currenciesRepository: CurrenciesRepository,
    private val derivationsRepository: DerivationsRepository,
    private val cryptoCurrencyBalanceFetcher: CryptoCurrencyBalanceFetcher,
    private val stakingIdFactory: StakingIdFactory,
    private val networksCleaner: NetworksCleaner,
    private val stakingCleaner: StakingCleaner,
    private val expressServiceFetcher: ExpressServiceFetcher,
    private val parallelUpdatingScope: CoroutineScope,
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

            val isDerivingFailed = derivePublicKeys(
                userWalletId = userWalletId,
                currencies = modifiedCurrencyList.added,
            ).isLeft()

            parallelUpdatingScope.launch {
                /*
                 * If only removal of currencies happened, we need to sync tokens. Otherwise, tokens will be synced
                 * when balances are refreshed for added currencies.
                 */
                val isOnlyRemoval = modifiedCurrencyList.added.isEmpty() && modifiedCurrencyList.removed.isNotEmpty()

                if (isDerivingFailed || isOnlyRemoval) {
                    launch { accountsCRUDRepository.syncTokens(userWalletId) }
                }

                if (isDerivingFailed) return@launch

                cryptoCurrencyBalanceFetcher(userWalletId = userWalletId, currencies = modifiedCurrencyList.added)
                refreshExpress(userWalletId = userWalletId, currencies = modifiedCurrencyList.total)
                clearMetadata(userWalletId = userWalletId, currencies = modifiedCurrencyList.removed)
            }
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

            val modifiedCurrencyList = account.cryptoCurrencies.modify(add = listOf(tokenToAdd))

            saveAccount(account = account.copy(cryptoCurrencies = modifiedCurrencyList.total.toSet()))

            parallelUpdatingScope.launch {
                cryptoCurrencyBalanceFetcher(userWalletId = userWalletId, currencies = listOf(tokenToAdd))
                refreshExpress(userWalletId = userWalletId, currencies = modifiedCurrencyList.total)
            }

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
        remove: List<CryptoCurrency> = emptyList(),
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
                val isCoinBeingRemoved = currenciesById.any { it.value is CryptoCurrency.Coin }
                if (isCoinBeingRemoved) {
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

    private suspend fun derivePublicKeys(
        userWalletId: UserWalletId,
        currencies: List<CryptoCurrency>,
    ): Either<Throwable, Unit> = Either.catch {
        derivationsRepository.derivePublicKeys(userWalletId = userWalletId, currencies = currencies)
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

    private suspend fun refreshExpress(userWalletId: UserWalletId, currencies: List<CryptoCurrency>) {
        if (currencies.isEmpty()) return

        coroutineScope {
            launch {
                val assetIds = currencies.mapTo(hashSetOf()) {
                    ExpressAsset.ID(
                        networkId = it.network.backendId,
                        contractAddress = (it as? CryptoCurrency.Token)?.contractAddress,
                    )
                }

                expressServiceFetcher.fetch(userWalletId = userWalletId, assetIds = assetIds)
            }
        }
    }

    private suspend fun clearMetadata(userWalletId: UserWalletId, currencies: List<CryptoCurrency>) {
        if (currencies.isEmpty()) return

        coroutineScope {
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