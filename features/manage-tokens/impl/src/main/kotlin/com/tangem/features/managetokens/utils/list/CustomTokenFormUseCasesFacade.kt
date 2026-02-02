package com.tangem.features.managetokens.utils.list

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import arrow.core.right
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.domain.account.featuretoggle.AccountsFeatureToggles
import com.tangem.domain.account.producer.SingleAccountListProducer
import com.tangem.domain.account.status.usecase.GetAccountCurrencyStatusUseCase
import com.tangem.domain.account.status.usecase.ManageCryptoCurrenciesUseCase
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.managetokens.CheckIsCurrencyNotAddedUseCase
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.tokens.AddCryptoCurrenciesUseCase
import com.tangem.domain.wallets.usecase.DerivePublicKeysUseCase
import com.tangem.features.managetokens.component.AddCustomTokenMode
import com.tangem.lib.crypto.derivation.AccountNodeRecognizer
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import timber.log.Timber

@Suppress("LongParameterList")
internal class CustomTokenFormUseCasesFacade @AssistedInject constructor(
    @Assisted private val mode: AddCustomTokenMode,
    private val addCryptoCurrenciesUseCase: AddCryptoCurrenciesUseCase,
    private val derivePublicKeysUseCase: DerivePublicKeysUseCase,
    private val checkIsCurrencyNotAddedUseCase: CheckIsCurrencyNotAddedUseCase,
    private val manageCryptoCurrenciesUseCase: ManageCryptoCurrenciesUseCase,
    private val singleAccountListSupplier: SingleAccountListSupplier,
    private val getAccountCurrencyStatusUseCase: GetAccountCurrencyStatusUseCase,
    private val accountsFeatureToggles: AccountsFeatureToggles,
) {

    suspend fun addCryptoCurrenciesUseCase(currency: CryptoCurrency): Either<Throwable, Unit> = when (mode) {
        is AddCustomTokenMode.Account -> either {
            val accountId = getAccountId(currency)

            manageCryptoCurrenciesUseCase(accountId = accountId, add = currency).bind()
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
    ): Either<Throwable, Boolean> = if (accountsFeatureToggles.isFeatureEnabled) {
        getAccountCurrencyStatusUseCase.invokeSync(
            userWalletId = mode.userWalletId,
            networkId = networkId,
            derivationPath = derivationPath,
            contractAddress = contractAddress,
        )
            .fold(ifEmpty = { true }, ifSome = { false })
            .right()
    } else {
        checkIsCurrencyNotAddedUseCase.invoke(
            userWalletId = mode.userWalletId,
            networkId = networkId,
            derivationPath = derivationPath,
            contractAddress = contractAddress,
        )
    }

    private suspend fun Raise<Throwable>.getAccountId(currency: CryptoCurrency): AccountId {
        val accountList = singleAccountListSupplier.getSyncOrNull(
            params = SingleAccountListProducer.Params(userWalletId = mode.userWalletId),
        )

        ensureNotNull(accountList) {
            IllegalStateException("Account list not found: ${mode.userWalletId}")
        }

        if (accountList.activeAccounts == 1) {
            return AccountId.forMainCryptoPortfolio(userWalletId = mode.userWalletId)
        }

        val currencyAccountIndex = currency.getAccountIndex().bind()

        val account = accountList.accounts.firstOrNull { account ->
            val cryptoPortfolioAccount = account as? Account.CryptoPortfolio

            cryptoPortfolioAccount?.derivationIndex?.value == currencyAccountIndex
        }

        return account?.accountId ?: AccountId.forMainCryptoPortfolio(userWalletId = mode.userWalletId)
    }

    private fun CryptoCurrency.getAccountIndex(): Either<Throwable, Int> = either {
        val currency = this@getAccountIndex
        val blockchain = Blockchain.fromNetworkId(networkId = currency.network.backendId)
        if (blockchain == null) {
            val exception = IllegalStateException("Token has unknown networkId: ${currency.id}")
            Timber.e(exception)
            raise(exception)
        }

        val derivationPathValue = currency.network.derivationPath.value
        if (derivationPathValue == null) {
            val exception = IllegalStateException("Token has no derivation path: ${currency.id}")
            Timber.e(exception)
            raise(exception)
        }

        val accountNodeRecognizer = AccountNodeRecognizer(blockchain)
        val index = accountNodeRecognizer.recognize(derivationPathValue)?.toInt()

        if (index == null) {
            Timber.e(
                "%s%s",
                "Unable to determine account index for derivation path: $derivationPathValue. ",
                "Use main account index instead.",
            )
            DerivationIndex.Main.value
        } else {
            index
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(mode: AddCustomTokenMode): CustomTokenFormUseCasesFacade
    }
}