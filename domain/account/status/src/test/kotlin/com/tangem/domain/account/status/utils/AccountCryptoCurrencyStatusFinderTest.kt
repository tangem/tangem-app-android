package com.tangem.domain.account.status.utils

import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.model.AccountCryptoCurrency
import com.tangem.domain.account.status.model.AccountCryptoCurrencyStatus
import com.tangem.domain.core.utils.lceLoading
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TokensGroupType
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.JointAccountStatusValue
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.test.mock.MockAccounts
import org.junit.jupiter.api.Test

/**
 * A joint derivation path (m/44'/60'/888888'/0/N) must not be read as a personal account index: the currency of a
 * joint account is found in the joint account itself, never in the main or an indexed personal account.
 */
internal class AccountCryptoCurrencyStatusFinderTest {

    private val userWalletId = MockAccounts.userWalletId
    private val ethereum = MockCryptoCurrencyFactory().ethereum

    @Test
    fun `GIVEN a joint currency WHEN find status by id and network THEN the joint account status is found`() {
        // Arrange
        val jointCurrency = ethereum.onJointNetwork(ownerKeyIndex = 0)
        val jointStatus = createJointStatus(ownerKeyIndex = 0, currency = jointCurrency)
        val accountStatusList = createAccountStatusList(jointStatus)

        // Act
        val actual = AccountCryptoCurrencyStatusFinder(
            accountStatusList = accountStatusList,
            currencyId = jointCurrency.id,
            network = jointCurrency.network,
        )

        // Assert
        assertThat(actual).isEqualTo(
            AccountCryptoCurrencyStatus(
                account = jointStatus.account,
                status = jointStatus.flattenCurrencies().single(),
            ),
        )
    }

    @Test
    fun `GIVEN a joint currency of a non-zero owner key WHEN find status THEN the joint account status is found`() {
        // Arrange
        val jointCurrency = ethereum.onJointNetwork(ownerKeyIndex = 2)
        val jointStatus = createJointStatus(ownerKeyIndex = 2, currency = jointCurrency)
        val accountStatusList = createAccountStatusList(jointStatus)

        // Act
        val actual = AccountCryptoCurrencyStatusFinder(
            accountStatusList = accountStatusList,
            currencyId = jointCurrency.id,
            network = jointCurrency.network,
        )

        // Assert
        assertThat(actual).isEqualTo(
            AccountCryptoCurrencyStatus(
                account = jointStatus.account,
                status = jointStatus.flattenCurrencies().single(),
            ),
        )
    }

    @Test
    fun `GIVEN a personal currency WHEN find status by id and network THEN the main account status is found`() {
        // Arrange
        val jointStatus = createJointStatus(ownerKeyIndex = 0, currency = ethereum.onJointNetwork(ownerKeyIndex = 0))
        val accountStatusList = createAccountStatusList(jointStatus, mainCurrency = ethereum)

        // Act
        val actual = AccountCryptoCurrencyStatusFinder(
            accountStatusList = accountStatusList,
            currencyId = ethereum.id,
            network = ethereum.network,
        )

        // Assert
        assertThat(actual?.account).isInstanceOf(Account.Personal::class.java)
        assertThat(actual?.status?.currency).isEqualTo(ethereum)
    }

    @Test
    fun `GIVEN a joint currency WHEN find the account currency in the account list THEN the joint account is found`() {
        // Arrange
        val jointCurrency = ethereum.onJointNetwork(ownerKeyIndex = 0)
        val jointStatus = createJointStatus(ownerKeyIndex = 0, currency = jointCurrency)
        val accountList = createAccountStatusList(jointStatus).toAccountList().getOrNull()!!

        // Act
        val actual = AccountCryptoCurrencyStatusFinder(
            accountList = accountList,
            currencyId = jointCurrency.id,
            network = jointCurrency.network,
        )

        // Assert
        assertThat(actual).isEqualTo(
            AccountCryptoCurrency(account = jointStatus.account, cryptoCurrency = jointCurrency),
        )
    }

    private fun createJointStatus(ownerKeyIndex: Int, currency: CryptoCurrency): AccountStatus.Joint {
        val account = MockAccounts.createJointAccount(
            ownerKeyIndex = ownerKeyIndex,
            cryptoCurrencies = listOf(currency),
            userWalletId = userWalletId,
        )

        return AccountStatus.Joint(
            account = account,
            value = JointAccountStatusValue.Active(
                source = StatusSource.ACTUAL,
                totalMembers = 3,
                safeAddress = SAFE_ADDRESS,
            ),
            tokenList = TokenList.Ungrouped(
                currencies = listOf(CryptoCurrencyStatus(currency = currency, value = CryptoCurrencyStatus.Loading)),
                totalFiatBalance = TotalFiatBalance.Loading,
                sortedBy = TokensSortType.NONE,
            ),
            priceChangeLce = lceLoading(),
        )
    }

    private fun createAccountStatusList(
        jointStatus: AccountStatus.Joint,
        mainCurrency: CryptoCurrency? = null,
    ): AccountStatusList {
        val mainAccount = Account.Personal.createMainAccount(
            userWalletId = userWalletId,
            cryptoCurrencies = listOfNotNull(mainCurrency),
        )
        val mainStatus = AccountStatus.Personal(
            account = mainAccount,
            tokenList = if (mainCurrency == null) {
                TokenList.Empty
            } else {
                TokenList.Ungrouped(
                    currencies = listOf(
                        CryptoCurrencyStatus(currency = mainCurrency, value = CryptoCurrencyStatus.Loading),
                    ),
                    totalFiatBalance = TotalFiatBalance.Loading,
                    sortedBy = TokensSortType.NONE,
                )
            },
            priceChangeLce = lceLoading(),
        )

        return AccountStatusList(
            userWalletId = userWalletId,
            accountStatuses = listOf(mainStatus, jointStatus),
            totalAccounts = 1,
            totalJointAccounts = 1,
            totalArchivedAccounts = 0,
            totalFiatBalance = TotalFiatBalance.Loading,
            sortType = TokensSortType.NONE,
            groupType = TokensGroupType.NONE,
        )
    }

    private fun CryptoCurrency.Coin.onJointNetwork(ownerKeyIndex: Int): CryptoCurrency.Coin {
        val derivationPath = Network.DerivationPath.Joint(value = "m/44'/60'/888888'/0/$ownerKeyIndex")
        val jointNetwork = network.copy(
            id = Network.ID(rawId = network.id.rawId, derivationPath = derivationPath),
            derivationPath = derivationPath,
        )

        return copy(network = jointNetwork)
    }

    private companion object {
        const val SAFE_ADDRESS = "0x5afe000000000000000000000000000000000001"
    }
}