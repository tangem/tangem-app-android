package com.tangem.features.managetokens.utils.list

import arrow.core.Either
import arrow.core.NonEmptyList
import com.tangem.domain.managetokens.CheckIsCurrencyNotAddedUseCase
import com.tangem.domain.managetokens.CreateCryptoCurrencyUseCase
import com.tangem.domain.managetokens.FindTokenUseCase
import com.tangem.domain.managetokens.ValidateTokenFormUseCase
import com.tangem.domain.managetokens.model.AddCustomTokenForm
import com.tangem.domain.managetokens.model.exceptoin.CustomTokenFormValidationException
import com.tangem.domain.managetokens.model.exceptoin.FindTokenException
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.tokens.AddCryptoCurrenciesUseCase
import com.tangem.domain.wallets.usecase.BackendId
import com.tangem.domain.wallets.usecase.DerivePublicKeysUseCase
import com.tangem.domain.wallets.usecase.HasMissedDerivationsUseCase
import com.tangem.features.managetokens.component.AddCustomTokenMode
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

@Suppress("LongParameterList")
internal class CustomTokenFormUseCasesFacade @AssistedInject constructor(
    private val hasMissedDerivationsUseCase: HasMissedDerivationsUseCase,
    private val addCryptoCurrenciesUseCase: AddCryptoCurrenciesUseCase,
    private val derivePublicKeysUseCase: DerivePublicKeysUseCase,
    private val validateTokenFormUseCase: ValidateTokenFormUseCase,
    private val createCryptoCurrencyUseCase: CreateCryptoCurrencyUseCase,
    private val findTokenUseCase: FindTokenUseCase,
    private val checkIsCurrencyNotAddedUseCase: CheckIsCurrencyNotAddedUseCase,
    @Assisted private val mode: AddCustomTokenMode,
) {

    suspend fun hasMissedDerivationsUseCase(networksWithDerivationPath: Map<BackendId, String?>): Boolean =
        when (mode) {
            is AddCustomTokenMode.Account -> TODO("Account")
            is AddCustomTokenMode.Wallet -> hasMissedDerivationsUseCase.invoke(
                userWalletId = mode.userWalletId,
                networksWithDerivationPath = networksWithDerivationPath,
            )
        }

    suspend fun addCryptoCurrenciesUseCase(currency: CryptoCurrency): Either<Throwable, Unit> = when (mode) {
        is AddCustomTokenMode.Account -> TODO("Account")
        is AddCustomTokenMode.Wallet -> addCryptoCurrenciesUseCase.invoke(
            userWalletId = mode.userWalletId,
            currency = currency,
        )
    }

    suspend fun derivePublicKeysUseCase(currencies: List<CryptoCurrency>): Either<Throwable, Unit> = when (mode) {
        is AddCustomTokenMode.Account -> TODO("Account")
        is AddCustomTokenMode.Wallet -> derivePublicKeysUseCase.invoke(
            userWalletId = mode.userWalletId,
            currencies = currencies,
        )
    }

    suspend fun checkIsCurrencyNotAddedUseCase(
        networkId: Network.ID,
        derivationPath: Network.DerivationPath,
        contractAddress: String?,
    ): Either<Throwable, Boolean> = when (mode) {
        is AddCustomTokenMode.Account -> TODO("Account")
        is AddCustomTokenMode.Wallet -> checkIsCurrencyNotAddedUseCase.invoke(
            userWalletId = mode.userWalletId,
            networkId = networkId,
            derivationPath = derivationPath,
            contractAddress = contractAddress,
        )
    }

    suspend fun createCryptoCurrencyUseCase(
        networkId: Network.ID,
        derivationPath: Network.DerivationPath,
        formValues: AddCustomTokenForm.Validated.All?,
    ): Either<Throwable, CryptoCurrency> = when (mode) {
        is AddCustomTokenMode.Account -> TODO("Account")
        is AddCustomTokenMode.Wallet -> createCryptoCurrencyUseCase.invoke(
            userWalletId = mode.userWalletId,
            networkId = networkId,
            derivationPath = derivationPath,
            formValues = formValues,
        )
    }

    suspend fun findTokenUseCase(
        contractAddress: String,
        networkId: Network.ID,
        derivationPath: Network.DerivationPath,
    ): Either<FindTokenException, CryptoCurrency.Token> = when (mode) {
        is AddCustomTokenMode.Account -> TODO("Account")
        is AddCustomTokenMode.Wallet -> findTokenUseCase.invoke(
            userWalletId = mode.userWalletId,
            contractAddress = contractAddress,
            networkId = networkId,
            derivationPath = derivationPath,
        )
    }

    suspend fun validateTokenFormUseCase(
        networkId: Network.ID,
        formValues: AddCustomTokenForm.Raw,
    ): Either<NonEmptyList<CustomTokenFormValidationException>, AddCustomTokenForm.Validated> = when (mode) {
        is AddCustomTokenMode.Account -> TODO("Account")
        is AddCustomTokenMode.Wallet -> validateTokenFormUseCase.invoke(
            networkId = networkId,
            formValues = formValues,
        )
    }

    @AssistedFactory
    interface Factory {
        fun create(mode: AddCustomTokenMode): CustomTokenFormUseCasesFacade
    }
}