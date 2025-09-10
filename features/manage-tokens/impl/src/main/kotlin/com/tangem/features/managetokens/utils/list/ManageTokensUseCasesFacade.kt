package com.tangem.features.managetokens.utils.list

import arrow.core.Either
import arrow.core.left
import com.tangem.domain.managetokens.*
import com.tangem.domain.managetokens.model.CurrencyUnsupportedState
import com.tangem.domain.managetokens.model.ManageTokensListConfig
import com.tangem.domain.managetokens.model.ManagedCryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.usecase.HasMissedDerivationsUseCase
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
    private val hasMissedDerivationsUseCase: HasMissedDerivationsUseCase,
    private val saveManagedTokensUseCase: SaveManagedTokensUseCase,
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
            is ManageTokensMode.Account -> TODO("Account")
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

    suspend fun hasMissedDerivationsUseCase(network: Map<String, Nothing?>): Boolean = when (mode) {
        is ManageTokensMode.Account -> TODO("Account")
        is ManageTokensMode.Wallet -> hasMissedDerivationsUseCase.invoke(
            userWalletId = mode.userWalletId,
            networksWithDerivationPath = network,
        )
        ManageTokensMode.None -> false
    }

    suspend fun saveManagedTokensUseCase(
        currenciesToAdd: Map<ManagedCryptoCurrency.Token, Set<Network>>,
        currenciesToRemove: Map<ManagedCryptoCurrency.Token, Set<Network>>,
    ): Either<Throwable, Unit> = when (mode) {
        is ManageTokensMode.Account -> TODO("Account")
        is ManageTokensMode.Wallet -> saveManagedTokensUseCase.invoke(
            userWalletId = mode.userWalletId,
            currenciesToAdd = currenciesToAdd,
            currenciesToRemove = currenciesToRemove,
        )
        ManageTokensMode.None -> nonePortfolioError.left()
    }

    @AssistedFactory
    interface Factory {
        fun create(mode: ManageTokensMode): ManageTokensUseCasesFacade
    }
}