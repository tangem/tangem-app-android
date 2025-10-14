package com.tangem.features.managetokens.utils.list

import arrow.core.Either
import arrow.core.left
import com.tangem.domain.account.status.usecase.SaveCryptoCurrenciesUseCase
import com.tangem.domain.managetokens.*
import com.tangem.domain.managetokens.model.CurrencyUnsupportedState
import com.tangem.domain.managetokens.model.ManageTokensListConfig
import com.tangem.domain.managetokens.model.ManagedCryptoCurrency
import com.tangem.domain.managetokens.repository.CustomTokensRepository
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
    private val checkHasLinkedTokensUseCase: CheckHasLinkedTokensUseCase,
    private val removeCustomCurrencyUseCase: RemoveCustomManagedCryptoCurrencyUseCase,
    private val checkCurrencyUnsupportedUseCase: CheckCurrencyUnsupportedUseCase,
    private val coldWalletAndHasMissedDerivationsUseCase: ColdWalletAndHasMissedDerivationsUseCase,
    private val saveManagedTokensUseCase: SaveManagedTokensUseCase,
    private val saveCryptoCurrenciesUseCase: SaveCryptoCurrenciesUseCase,
    private val customTokensRepository: CustomTokensRepository,
    @Assisted private val mode: ManageTokensMode,
) {

    private val nonePortfolioError: IllegalStateException
        get() = IllegalStateException("Unsupported")

    fun manageTokensListConfig(searchText: String?): ManageTokensListConfig {
        val userWalletId: UserWalletId? = when (mode) {
            is ManageTokensMode.Account -> TODO("Account")
            ManageTokensMode.None -> null
            is ManageTokensMode.Wallet -> mode.userWalletId
        }
        return ManageTokensListConfig(userWalletId, searchText)
    }

    suspend fun removeCustomCurrencyUseCase(customCurrency: ManagedCryptoCurrency.Custom): Either<Throwable, Unit> {
        return when (mode) {
            is ManageTokensMode.Account -> {
                val currency = customTokensRepository.convertToCryptoCurrency(
                    userWalletId = mode.accountId.userWalletId,
                    currency = customCurrency,
                )

                saveCryptoCurrenciesUseCase(accountId = mode.accountId, remove = currency)
            }
            is ManageTokensMode.Wallet -> removeCustomCurrencyUseCase.invoke(
                userWalletId = mode.userWalletId,
                customCurrency = customCurrency,
            )
            ManageTokensMode.None -> nonePortfolioError.left()
        }
    }

    suspend fun checkHasLinkedTokensUseCase(
        network: Network,
        tempAddedTokens: Map<ManagedCryptoCurrency.Token, Set<Network>>,
        tempRemovedTokens: Map<ManagedCryptoCurrency.Token, Set<Network>>,
    ): Either<Throwable, Boolean> {
        return when (mode) {
            is ManageTokensMode.Account -> TODO("Account")
            is ManageTokensMode.Wallet -> checkHasLinkedTokensUseCase.invoke(
                userWalletId = mode.userWalletId,
                network = network,
                tempAddedTokens = tempAddedTokens,
                tempRemovedTokens = tempRemovedTokens,
            )
            ManageTokensMode.None -> nonePortfolioError.left()
        }
    }

    suspend fun checkCurrencyUnsupportedUseCase(
        sourceNetwork: ManagedCryptoCurrency.SourceNetwork,
    ): Either<Throwable, CurrencyUnsupportedState?> {
        return when (mode) {
            is ManageTokensMode.Account -> TODO("Account")
            is ManageTokensMode.Wallet -> checkCurrencyUnsupportedUseCase.invoke(
                userWalletId = mode.userWalletId,
                sourceNetwork = sourceNetwork,
            )
            ManageTokensMode.None -> nonePortfolioError.left()
        }
    }

    suspend fun needColdWalletInteraction(network: Map<String, Nothing?>): Boolean = when (mode) {
        is ManageTokensMode.Account -> TODO("Account")
        is ManageTokensMode.Wallet -> coldWalletAndHasMissedDerivationsUseCase.invoke(
            userWalletId = mode.userWalletId,
            networksWithDerivationPath = network,
        )
        ManageTokensMode.None -> false
    }

    suspend fun saveManagedTokensUseCase(
        currenciesToAdd: Map<ManagedCryptoCurrency.Token, Set<Network>>,
        currenciesToRemove: Map<ManagedCryptoCurrency.Token, Set<Network>>,
    ): Either<Throwable, Unit> = when (mode) {
        is ManageTokensMode.Account -> {
            saveCryptoCurrenciesUseCase(
                accountId = mode.accountId,
                add = currenciesToAdd.mapToCryptoCurrencies(userWalletId = mode.accountId.userWalletId),
                remove = currenciesToRemove.mapToCryptoCurrencies(userWalletId = mode.accountId.userWalletId),
            )
        }
        is ManageTokensMode.Wallet -> {
            saveManagedTokensUseCase.invoke(
                userWalletId = mode.userWalletId,
                currenciesToAdd = currenciesToAdd,
                currenciesToRemove = currenciesToRemove,
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