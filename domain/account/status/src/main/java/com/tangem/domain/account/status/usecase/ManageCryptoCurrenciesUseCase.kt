package com.tangem.domain.account.status.usecase

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.catch
import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.domain.account.status.producer.SingleAccountStatusListProducer
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.utils.CryptoCurrencyBalanceFetcher
import com.tangem.domain.account.status.utils.CryptoCurrencyMetadataCleaner
import com.tangem.domain.core.utils.eitherOn
import com.tangem.domain.express.ExpressServiceFetcher
import com.tangem.domain.express.models.ExpressAsset
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.derivations.DerivationsRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import kotlinx.coroutines.*
import timber.log.Timber

/**
 * Use case for saving crypto currencies to a specific account.
 *
 * @property singleAccountStatusListSupplier Supplier for fetching the status of a single account.
 * @property accountsCRUDRepository Repository for performing CRUD operations on accounts.
 * @property currenciesRepository Repository for managing currencies.
 * @property derivationsRepository Repository for deriving public keys.
 * @property cryptoCurrencyBalanceFetcher Fetcher for updating crypto currency balances.
 * @property cryptoCurrencyMetadataCleaner Cleaner for removing metadata of deleted currencies.
 * @property expressServiceFetcher Fetcher for updating express service data.
 * @property parallelUpdatingScope Coroutine scope for parallel updates.
 * @property dispatchers Coroutine dispatchers for managing threading.
 *
[REDACTED_AUTHOR]
 */
@Suppress("LongParameterList")
class ManageCryptoCurrenciesUseCase(
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
    private val accountsCRUDRepository: AccountsCRUDRepository,
    private val currenciesRepository: CurrenciesRepository,
    private val derivationsRepository: DerivationsRepository,
    private val walletManagersFacade: WalletManagersFacade,
    private val cryptoCurrencyBalanceFetcher: CryptoCurrencyBalanceFetcher,
    private val cryptoCurrencyMetadataCleaner: CryptoCurrencyMetadataCleaner,
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
            val accountStatus = getAccountStatus(accountId = accountId)

            val modifiedCurrencyList = accountStatus.tokenList.flattenCurrencies()
                .modify(add = add, remove = remove)

            if (!modifiedCurrencyList.hasChanges) {
                Timber.d("No changes in currencies, skipping")
                return@withContext
            }

            saveAccount(
                account = accountStatus.account.copy(cryptoCurrencies = modifiedCurrencyList.total.toSet()),
            )

            derivePublicKeys(userWalletId = userWalletId, currencies = modifiedCurrencyList.added)

            parallelUpdatingScope.launch {
                syncTokens(userWalletId, modifiedCurrencyList)

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
            val accountStatus = getAccountStatus(accountId = accountId)

            val foundToken = accountStatus.tokenList.flattenCurrencies()
                .mapNotNull { it.currency as? CryptoCurrency.Token }
                .firstOrNull { token ->
                    token.network.backendId == networkId &&
                        !token.isCustom &&
                        token.contractAddress.equals(contractAddress, true)
                }

            if (foundToken != null) return@withContext foundToken

            val tokenToAdd = findToken(userWalletId, contractAddress, networkId)

            val modifiedCurrencyList = accountStatus.tokenList.flattenCurrencies()
                .modify(add = listOf(tokenToAdd))

            saveAccount(account = accountStatus.account.copy(cryptoCurrencies = modifiedCurrencyList.total.toSet()))

            parallelUpdatingScope.launch {
                syncTokens(userWalletId, modifiedCurrencyList)

                cryptoCurrencyBalanceFetcher(userWalletId = userWalletId, currencies = listOf(tokenToAdd))
                refreshExpress(userWalletId = userWalletId, currencies = modifiedCurrencyList.total)
            }

            tokenToAdd
        }
    }

    private suspend fun Raise<Throwable>.getAccountStatus(accountId: AccountId): AccountStatus.CryptoPortfolio {
        val accountStatusList = singleAccountStatusListSupplier.getSyncOrNull(
            params = SingleAccountStatusListProducer.Params(userWalletId = accountId.userWalletId),
        ) ?: raise(IllegalStateException("No accounts for wallet ${accountId.userWalletId}"))

        return accountStatusList.accountStatuses
            .firstOrNull { it.accountId == accountId } as? AccountStatus.CryptoPortfolio
            ?: raise(IllegalStateException("No account with id $accountId"))
    }

    private fun List<CryptoCurrencyStatus>.modify(
        add: List<CryptoCurrency>,
        remove: List<CryptoCurrency> = emptyList(),
    ): ModifiedCurrencyList {
        val mutableCurrencies = this.map(CryptoCurrencyStatus::currency).toMutableList()
        val added = mutableListOf<CryptoCurrency>()
        val removed = mutableListOf<CryptoCurrency>()

        val existingCurrenciesById = this.associateBy(::TempID)

        add.groupByNetwork { tempID ->
            val found = existingCurrenciesById[tempID] ?: return@groupByNetwork true

            found.value is CryptoCurrencyStatus.MissedDerivation
        }
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

    private fun List<CryptoCurrency>.groupByNetwork(
        valuePredicate: (TempID) -> Boolean,
    ): LinkedHashMap<Network, MutableMap<TempID, CryptoCurrency>> {
        val destination = LinkedHashMap<Network, MutableMap<TempID, CryptoCurrency>>()

        for (currency in this) {
            val key = currency.network
            val mutableMap = destination.getOrPut(key) { mutableMapOf() }

            val id = TempID(currency)

            if (valuePredicate(id)) {
                mutableMap[id] = currency
            }
        }

        return destination
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

    private suspend fun syncTokens(userWalletId: UserWalletId, modifiedCurrencyList: ModifiedCurrencyList) {
        createWalletManagers(userWalletId = userWalletId, currencies = modifiedCurrencyList.added)

        runSuspendCatching { accountsCRUDRepository.syncTokens(userWalletId) }
            .onFailure { Timber.e(it, "Failed to sync tokens for wallet $userWalletId") }
    }

    /**
     * Creates wallet managers for the given [currencies] if they do not already exist.
     * The method will generate addresses for new networks to ensure the stability of the "Push notifications" feature.
     *
     * @param userWalletId The ID of the user's wallet.
     * @param currencies The list of cryptocurrencies for which to create wallet managers.
     */
    private suspend fun createWalletManagers(userWalletId: UserWalletId, currencies: List<CryptoCurrency>) {
        val networks = currencies.mapTo(hashSetOf(), CryptoCurrency::network)

        for (network in networks) {
            runSuspendCatching {
                walletManagersFacade.getOrCreateWalletManager(userWalletId = userWalletId, network = network)
            }
                .onFailure { Timber.e(it, "Failed to create wallet manager for network ${network.id}") }
        }
    }

    private suspend fun refreshExpress(userWalletId: UserWalletId, currencies: List<CryptoCurrency>) {
        if (currencies.isEmpty()) return

        coroutineScope {
            launch {
                val assetIds = currencies.mapTo(hashSetOf()) { currency ->
                    ExpressAsset.ID(
                        networkId = currency.network.backendId,
                        contractAddress = (currency as? CryptoCurrency.Token)?.contractAddress,
                    )
                }

                expressServiceFetcher.fetch(userWalletId = userWalletId, assetIds = assetIds)
            }
        }
    }

    private suspend fun clearMetadata(userWalletId: UserWalletId, currencies: List<CryptoCurrency>) {
        if (currencies.isEmpty()) return

        coroutineScope {
            launch {
                cryptoCurrencyMetadataCleaner(userWalletId = userWalletId, currencies = currencies)
            }
        }
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

        constructor(status: CryptoCurrencyStatus) : this(
            networkId = status.currency.network.backendId,
            derivationPath = status.currency.network.derivationPath,
            contractAddress = (status.currency as? CryptoCurrency.Token)?.contractAddress,
        )
    }

    private data class ModifiedCurrencyList(
        val added: List<CryptoCurrency>,
        val removed: List<CryptoCurrency>,
        val total: List<CryptoCurrency>,
    ) {

        val hasChanges get() = added.isNotEmpty() || removed.isNotEmpty()
    }
}