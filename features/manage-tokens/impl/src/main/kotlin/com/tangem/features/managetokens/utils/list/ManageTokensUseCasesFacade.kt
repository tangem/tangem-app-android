package com.tangem.features.managetokens.utils.list

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.account.producer.SingleAccountProducer
import com.tangem.domain.account.status.usecase.ManageCryptoCurrenciesUseCase
import com.tangem.domain.account.supplier.SingleAccountSupplier
import com.tangem.domain.managetokens.CheckCurrencyUnsupportedUseCase
import com.tangem.domain.managetokens.GetDistinctManagedCurrenciesUseCase
import com.tangem.domain.managetokens.GetManagedTokensUseCase
import com.tangem.domain.managetokens.model.CurrencyUnsupportedState
import com.tangem.domain.managetokens.model.ManageTokensListConfig
import com.tangem.domain.managetokens.model.ManagedCryptoCurrency
import com.tangem.domain.managetokens.repository.CustomTokensRepository
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.usecase.ColdWalletAndHasMissedDerivationsUseCase
import com.tangem.features.managetokens.component.ManageTokensMode
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Suppress("LongParameterList")
internal class ManageTokensUseCasesFacade @AssistedInject constructor(
    val getManagedTokensUseCase: GetManagedTokensUseCase,
    val getDistinctManagedTokensUseCase: GetDistinctManagedCurrenciesUseCase,
    private val checkCurrencyUnsupportedUseCase: CheckCurrencyUnsupportedUseCase,
    private val coldWalletAndHasMissedDerivationsUseCase: ColdWalletAndHasMissedDerivationsUseCase,
    private val manageCryptoCurrenciesUseCase: ManageCryptoCurrenciesUseCase,
    private val customTokensRepository: CustomTokensRepository,
    private val singleAccountSupplier: SingleAccountSupplier,
    @Assisted private val mode: ManageTokensMode,
) {

    private val nonePortfolioError: IllegalStateException
        get() = IllegalStateException("Unsupported")

    fun manageTokensListConfig(searchText: String?): ManageTokensListConfig {
        return when (mode) {
            is ManageTokensMode.Account -> {
                ManageTokensListConfig(accountId = mode.accountId, searchText = searchText)
            }
            ManageTokensMode.None -> {
                ManageTokensListConfig(accountId = null, searchText = searchText)
            }
        }
    }

    suspend fun removeCustomCurrencyUseCase(customCurrency: ManagedCryptoCurrency.Custom): Either<Throwable, Unit> {
        return when (mode) {
            is ManageTokensMode.Account -> {
                val currency = customTokensRepository.convertToCryptoCurrency(
                    userWalletId = mode.accountId.userWalletId,
                    currency = customCurrency,
                )

                manageCryptoCurrenciesUseCase(accountId = mode.accountId, remove = currency)
            }
            ManageTokensMode.None -> nonePortfolioError.left()
        }
    }

    suspend fun checkHasLinkedTokensUseCase(
        network: Network,
        tempAddedTokens: Map<ManagedCryptoCurrency.Token, Set<Network>>,
        tempRemovedTokens: Map<ManagedCryptoCurrency.Token, Set<Network>>,
    ): Either<Throwable, Boolean> {
        return when (mode) {
            is ManageTokensMode.Account -> {
                val added = tempAddedTokens.mapToCryptoCurrencies(userWalletId = mode.accountId.userWalletId)
                val removed = tempRemovedTokens.mapToCryptoCurrencies(userWalletId = mode.accountId.userWalletId)

                val account = singleAccountSupplier.getSyncOrNull(
                    params = SingleAccountProducer.Params(accountId = mode.accountId),
                ) as? Account.CryptoPortfolio
                    ?: return IllegalStateException("Account not found").left()

                (account.cryptoCurrencies + added - removed).any { currency ->
                    currency is CryptoCurrency.Token && currency.network.backendId == network.backendId &&
                        currency.network.derivationPath == network.derivationPath
                }
                    .right()
            }
            ManageTokensMode.None -> nonePortfolioError.left()
        }
    }

    suspend fun checkCurrencyUnsupportedUseCase(
        sourceNetwork: ManagedCryptoCurrency.SourceNetwork,
    ): Either<Throwable, CurrencyUnsupportedState?> {
        return when (mode) {
            is ManageTokensMode.Account -> checkCurrencyUnsupportedUseCase.invoke(
                userWalletId = mode.accountId.userWalletId,
                sourceNetwork = sourceNetwork,
            )
            ManageTokensMode.None -> nonePortfolioError.left()
        }
    }

    suspend fun needColdWalletInteraction(network: Map<String, String?>): Boolean = when (mode) {
        is ManageTokensMode.Account -> coldWalletAndHasMissedDerivationsUseCase.invoke(
            userWalletId = mode.accountId.userWalletId,
            networksWithDerivationPath = network,
        )
        ManageTokensMode.None -> false
    }

    suspend fun saveManagedTokensUseCase(
        currenciesToAdd: Map<ManagedCryptoCurrency.Token, Set<Network>>,
        currenciesToRemove: Map<ManagedCryptoCurrency.Token, Set<Network>>,
    ): Either<Throwable, Unit> = when (mode) {
        is ManageTokensMode.Account -> {
            manageCryptoCurrenciesUseCase(
                accountId = mode.accountId,
                add = currenciesToAdd.mapToCryptoCurrencies(userWalletId = mode.accountId.userWalletId),
                remove = currenciesToRemove.mapToCryptoCurrencies(userWalletId = mode.accountId.userWalletId),
            )
        }
        ManageTokensMode.None -> nonePortfolioError.left()
    }

    private suspend fun Map<ManagedCryptoCurrency.Token, Set<Network>>.mapToCryptoCurrencies(
        userWalletId: UserWalletId,
    ): List<CryptoCurrency> {
        return flatMap { (token, networks) ->
            token.availableNetworks
                .filter { sourceNetwork -> networks.contains(sourceNetwork.network) }
                .map { sourceNetwork ->
                    when (sourceNetwork) {
                        is ManagedCryptoCurrency.SourceNetwork.Default -> customTokensRepository.createToken(
                            managedCryptoCurrency = token,
                            sourceNetwork = sourceNetwork,
                            rawId = CryptoCurrency.RawID(token.id.value),
                        )
                        is ManagedCryptoCurrency.SourceNetwork.Main -> customTokensRepository.createCoin(
                            userWalletId = userWalletId,
                            networkId = sourceNetwork.id,
                            derivationPath = sourceNetwork.network.derivationPath,
                        )
                    }
                }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(mode: ManageTokensMode): ManageTokensUseCasesFacade
    }
}