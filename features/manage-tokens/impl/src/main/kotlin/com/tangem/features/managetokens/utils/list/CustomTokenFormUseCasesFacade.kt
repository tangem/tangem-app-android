package com.tangem.features.managetokens.utils.list

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.account.producer.SingleAccountProducer
import com.tangem.domain.account.status.usecase.ManageCryptoCurrenciesUseCase
import com.tangem.domain.account.supplier.SingleAccountSupplier
import com.tangem.domain.managetokens.CheckIsCurrencyNotAddedUseCase
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.tokens.AddCryptoCurrenciesUseCase
import com.tangem.domain.wallets.usecase.DerivePublicKeysUseCase
import com.tangem.features.managetokens.component.AddCustomTokenMode
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class CustomTokenFormUseCasesFacade @AssistedInject constructor(
    private val addCryptoCurrenciesUseCase: AddCryptoCurrenciesUseCase,
    private val derivePublicKeysUseCase: DerivePublicKeysUseCase,
    private val checkIsCurrencyNotAddedUseCase: CheckIsCurrencyNotAddedUseCase,
    private val manageCryptoCurrenciesUseCase: ManageCryptoCurrenciesUseCase,
    private val singleAccountSupplier: SingleAccountSupplier,
    @Assisted private val mode: AddCustomTokenMode,
) {

    suspend fun addCryptoCurrenciesUseCase(currency: CryptoCurrency): Either<Throwable, Unit> = when (mode) {
        is AddCustomTokenMode.Account -> {
            manageCryptoCurrenciesUseCase(
                accountId = AccountId.forCryptoPortfolio(
                    userWalletId = mode.userWalletId,
                    derivationIndex = DerivationIndex.Main,
                ),
                add = currency,
            )
        }
        is AddCustomTokenMode.Wallet -> {
            addCryptoCurrenciesUseCase.invoke(
                userWalletId = mode.userWalletId,
                currency = currency,
            )
        }
    }

    suspend fun derivePublicKeysUseCase(currencies: List<CryptoCurrency>): Either<Throwable, Unit> = when (mode) {
        is AddCustomTokenMode.Account -> Unit.right()
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
        is AddCustomTokenMode.Account -> {
            val account = singleAccountSupplier.getSyncOrNull(
                params = SingleAccountProducer.Params(accountId = mode.accountId),
            )
                ?: return IllegalStateException("Account not found").left()

            account.cryptoCurrencies.none { currency ->
                networkId == currency.network.id &&
                    derivationPath == currency.network.derivationPath &&
                    contractAddress.equals(currency.id.contractAddress, ignoreCase = true)
            }
                .right()
        }
        is AddCustomTokenMode.Wallet -> checkIsCurrencyNotAddedUseCase.invoke(
            userWalletId = mode.userWalletId,
            networkId = networkId,
            derivationPath = derivationPath,
            contractAddress = contractAddress,
        )
    }

    @AssistedFactory
    interface Factory {
        fun create(mode: AddCustomTokenMode): CustomTokenFormUseCasesFacade
    }
}