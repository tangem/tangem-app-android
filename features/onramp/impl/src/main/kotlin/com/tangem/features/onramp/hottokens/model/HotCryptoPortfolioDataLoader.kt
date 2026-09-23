package com.tangem.features.onramp.hottokens.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.model.AccountCryptoCurrencyStatuses
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.usecase.GetAccountCurrencyStatusUseCase
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.jointaccount.usecase.GetJointAccountSupportedNetworksUseCase
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.JointAccountStatusValue
import com.tangem.domain.models.account.filterCryptoPortfolio
import com.tangem.domain.models.currency.CryptoCurrency
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
    private val getJointAccountSupportedNetworksUseCase: GetJointAccountSupportedNetworksUseCase,
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
        val portfolioStatuses = walletAccounts.accountStatuses.filterCryptoPortfolio().filter { status ->
            status !is AccountStatus.Joint || status.value is JointAccountStatusValue.Active
        }
        val accountsWithHotCrypto = portfolioStatuses.map { accountStatus ->
            val addedHotCrypto = mapOfAddedCurrencies[accountStatus.account].orEmpty()
            HotCryptoPortfolioData.Account(
                account = accountStatus,
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
            jointSupportedNetworkIds = getJointAccountSupportedNetworksUseCase().mapTo(hashSetOf()) { it.value },
        )
    }
}

/**
 * @property jointSupportedNetworkIds the networks a joint account exists in: a joint account is offered for a
 * hot token only when the token lives in one of them
 */
internal data class HotCryptoPortfolioData(
    val allHotCrypto: List<HotCryptoCurrency>,
    val wallet: Wallet,
    val appCurrency: AppCurrency,
    val jointSupportedNetworkIds: Set<String>,
) {

    /** The accounts [currency] can be added to */
    fun accountsFor(currency: CryptoCurrency): List<Account> = wallet.accounts.filter { account ->
        account.account !is AccountStatus.Joint || currency.network.rawId in jointSupportedNetworkIds
    }

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