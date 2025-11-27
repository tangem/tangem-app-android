package com.tangem.features.onramp.hottokens.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.model.AccountCryptoCurrencyStatuses
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.usecase.GetAccountCurrencyStatusUseCase
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.onramp.GetHotCryptoUseCase
import com.tangem.domain.onramp.model.HotCryptoCurrency
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import javax.inject.Inject

@ModelScoped
internal class HotCryptoPortfolioDataLoader @Inject constructor(
    private val appCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getHotCryptoUseCase: GetHotCryptoUseCase,
    private val accountListSupplier: SingleAccountStatusListSupplier,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val getAccountCurrencyStatusUseCase: GetAccountCurrencyStatusUseCase,

) {

    fun loadPortfolioData(userWalletId: UserWalletId): Flow<HotCryptoPortfolioData> = combine(
        flow = appCurrencyUseCase.invokeOrDefault().distinctUntilChanged(),
        flow2 = getHotCryptoUseCase(userWalletId).distinctUntilChanged(),
        flow3 = accountListSupplier.invoke(userWalletId).distinctUntilChanged(),
        flow4 = getUserWalletUseCase.invokeFlow(userWalletId).mapNotNull { it.getOrNull() },
    ) { appCurrency, allHotCrypto, walletAccounts, userWallet ->

        val hotCryptoCurrencies = allHotCrypto.map { hotCrypto -> hotCrypto.cryptoCurrency }
        val mapOfAddedCurrencies: AccountCryptoCurrencyStatuses = getAccountCurrencyStatusUseCase
            .invokeSync(userWalletId, hotCryptoCurrencies)
            .getOrNull()
            .orEmpty()
        val accountsWithHotCrypto = walletAccounts.accountStatuses.map { accountStatus ->
            val account: AccountStatus.CryptoPortfolio = when (accountStatus) {
                is AccountStatus.CryptoPortfolio -> accountStatus
            }
            val addedHotCrypto = mapOfAddedCurrencies[account.account].orEmpty()
            HotCryptoPortfolioData.Account(
                account = account,
                addedHotCrypto = addedHotCrypto,
            )
        }

        HotCryptoPortfolioData(
            allHotCrypto = allHotCrypto,
            appCurrency = appCurrency,
            wallet = HotCryptoPortfolioData.Wallet(
                userWallet = userWallet,
                statusList = walletAccounts,
                accounts = accountsWithHotCrypto,
            ),
        )
    }
}

internal data class HotCryptoPortfolioData(
    val allHotCrypto: List<HotCryptoCurrency>,
    val wallet: Wallet,
    val appCurrency: AppCurrency,
) {

    data class Wallet(
        val userWallet: UserWallet,
        val statusList: AccountStatusList,
        val accounts: List<Account>,
    )

    data class Account(
        val account: AccountStatus.CryptoPortfolio,
        val addedHotCrypto: List<CryptoCurrencyStatus>,
    )
}