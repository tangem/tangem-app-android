package com.tangem.test.mock

import com.tangem.domain.account.models.AccountList
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.*
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
object MockAccounts {

    val userWalletId = UserWalletId("011")

    val onlyMainAccount = createAccountList(activeAccounts = 1)

    val fullAccountList = createAccountList(activeAccounts = 20)

    fun createAccountList(
        activeAccounts: Int,
        totalAccounts: Int = activeAccounts,
        totalArchivedAccounts: Int = 0,
        userWalletId: UserWalletId = this.userWalletId,
    ): AccountList {
        return AccountList(
            userWalletId = userWalletId,
            accounts = createAccounts(count = activeAccounts, userWalletId = userWalletId),
            totalAccounts = totalAccounts,
            totalArchivedAccounts = totalArchivedAccounts,
        ).getOrNull()!!
    }

    fun createAccounts(count: Int, userWalletId: UserWalletId = this.userWalletId): List<Account.CryptoPortfolio> {
        return buildList {
            add(Account.CryptoPortfolio.createMainAccount(userWalletId))

            repeat(count - 1) {
                val account = createAccount(derivationIndex = it + 1, userWalletId = userWalletId)

                add(account)
            }
        }
    }

    fun createJointAccount(
        derivationIndex: Int = 0,
        name: String = "Joint #$derivationIndex",
        icon: CryptoPortfolioIcon = CryptoPortfolioIcon.ofDefaultCustomAccount(),
        cryptoCurrencies: List<CryptoCurrency> = emptyList(),
        userWalletId: UserWalletId = this.userWalletId,
    ): Account.Joint {
        val backendId = derivationIndex.toString(radix = 16).padStart(length = 64, padChar = '0')

        return Account.Joint(
            accountId = AccountId.forJointAccount(userWalletId = userWalletId, value = backendId).getOrNull()!!,
            accountName = AccountName(name).getOrNull()!!,
            icon = icon,
            derivationIndex = DerivationIndex(derivationIndex).getOrNull()!!,
            cryptoCurrencies = cryptoCurrencies,
        )
    }

    fun createAccount(
        derivationIndex: Int,
        name: String = "Account #$derivationIndex",
        icon: CryptoPortfolioIcon = CryptoPortfolioIcon.ofDefaultCustomAccount(),
        cryptoCurrencies: List<CryptoCurrency> = emptyList(),
        userWalletId: UserWalletId = this.userWalletId,
    ): Account.CryptoPortfolio {
        val derivationIndex = DerivationIndex(derivationIndex).getOrNull()!!

        return Account.CryptoPortfolio(
            accountId = AccountId.forCryptoPortfolio(userWalletId = userWalletId, derivationIndex = derivationIndex),
            accountName = AccountName(name).getOrNull()!!,
            icon = icon,
            derivationIndex = derivationIndex,
            cryptoCurrencies = cryptoCurrencies,
        )
    }

    fun createPaymentAccountStatus(
        userWalletId: UserWalletId = this.userWalletId,
        cryptoCurrency: CryptoCurrency.Token = createPaymentAccountToken(),
        source: StatusSource = StatusSource.ACTUAL,
        customerId: String = "cust_1",
        depositAddress: String? = "0xdeposit",
        fiatRate: BigDecimal? = BigDecimal.ONE,
        balance: PaymentAccountStatusValue.Balance = createPaymentAccountBalance(),
    ): AccountStatus.Payment {
        return AccountStatus.Payment(
            account = Account.Payment(userWalletId),
            value = PaymentAccountStatusValue.Loaded(
                source = source,
                customerId = customerId,
                depositAddress = depositAddress,
                balance = balance,
                cryptoCurrency = cryptoCurrency,
                networks = emptyList(),
                cards = emptyList(),
                fiatRate = fiatRate,
                error = null,
                virtualAccount = null,
                tariffPlan = null,
            ),
        )
    }

    fun createPaymentAccountBalance(
        availableBalance: BigDecimal = BigDecimal.TEN,
        currency: String = "USD",
        availableForWithdrawal: BigDecimal = BigDecimal.TEN,
    ): PaymentAccountStatusValue.Balance {
        return PaymentAccountStatusValue.Balance(
            fiatBalance = PaymentAccountStatusValue.FiatBalance(
                availableBalance = availableBalance,
                currency = currency,
            ),
            cryptoBalance = PaymentAccountStatusValue.CryptoBalance(
                id = "usdc",
                chainId = 137L,
                depositAddress = "0xdeposit",
                tokenContractAddress = "0xcontract",
                balance = BigDecimal.TEN,
            ),
            availableForWithdrawal = availableForWithdrawal,
        )
    }

    fun createPaymentAccountToken(
        rawId: String = "usdc-polygon",
        contractAddress: String = "0xcontract",
        name: String = "USD Coin",
        symbol: String = "USDC",
    ): CryptoCurrency.Token {
        val network = Network(
            id = Network.ID(value = "polygon", derivationPath = Network.DerivationPath.None),
            name = "Polygon",
            currencySymbol = "MATIC",
            derivationPath = Network.DerivationPath.None,
            isTestnet = false,
            standardType = Network.StandardType.ERC20,
            hasFiatFeeRate = true,
            canHandleTokens = true,
            transactionExtrasType = Network.TransactionExtrasType.NONE,
            nameResolvingType = Network.NameResolvingType.NONE,
        )
        return CryptoCurrency.Token(
            id = CryptoCurrency.ID(
                prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
                body = CryptoCurrency.ID.Body.NetworkId(rawId = "polygon"),
                suffix = CryptoCurrency.ID.Suffix.RawID(rawId = rawId),
            ),
            network = network,
            name = name,
            symbol = symbol,
            decimals = 6,
            iconUrl = null,
            isCustom = false,
            contractAddress = contractAddress,
        )
    }
}