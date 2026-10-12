package com.tangem.features.onramp.hottokens.model

import arrow.core.right
import arrow.core.some
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.usecase.GetAccountCurrencyStatusUseCase
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.core.utils.lceError
import com.tangem.domain.jointaccount.usecase.GetJointAccountSupportedNetworksUseCase
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
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.domain.onramp.GetHotCryptoUseCase
import com.tangem.domain.onramp.model.HotCryptoCurrency
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.test.mock.MockAccounts
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.math.BigDecimal
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/**
 * A joint account is offered as a target once it is active, and only for a token of a network it exists in; a
 * personal account is offered for every token, as before.
 */
internal class HotCryptoPortfolioDataLoaderTest {

    private val appCurrencyUseCase: GetSelectedAppCurrencyUseCase = mockk()
    private val getHotCryptoUseCase: GetHotCryptoUseCase = mockk()
    private val accountListSupplier: SingleAccountStatusListSupplier = mockk()
    private val getUserWalletUseCase: GetUserWalletUseCase = mockk()
    private val getAccountCurrencyStatusUseCase: GetAccountCurrencyStatusUseCase = mockk()
    private val getJointAccountSupportedNetworksUseCase: GetJointAccountSupportedNetworksUseCase = mockk()

    private val loader = HotCryptoPortfolioDataLoader(
        appCurrencyUseCase = appCurrencyUseCase,
        getHotCryptoUseCase = getHotCryptoUseCase,
        accountListSupplier = accountListSupplier,
        getUserWalletUseCase = getUserWalletUseCase,
        getAccountCurrencyStatusUseCase = getAccountCurrencyStatusUseCase,
        getJointAccountSupportedNetworksUseCase = getJointAccountSupportedNetworksUseCase,
    )

    private val userWallet = MockUserWalletFactory.create()
    private val ethereum: CryptoCurrency = MockCryptoCurrencyFactory().ethereum
    private val solana: CryptoCurrency = MockCryptoCurrencyFactory().createCoin(Blockchain.Solana)

    private val personal = AccountStatus.Personal(
        account = MockAccounts.createAccount(derivationIndex = 0),
        tokenList = TokenList.Empty,
        priceChangeLce = Unit.lceError(),
    )
    private val activeJoint = AccountStatus.Joint(
        account = MockAccounts.createJointAccount(ownerKeyIndex = 0),
        value = JointAccountStatusValue.Active(StatusSource.ACTUAL, totalMembers = 3, safeAddress = SAFE),
        tokenList = TokenList.Empty,
        priceChangeLce = Unit.lceError(),
    )
    private val pendingJoint = AccountStatus.Joint(
        account = MockAccounts.createJointAccount(ownerKeyIndex = 1),
        value = JointAccountStatusValue.Pending(StatusSource.ACTUAL, joinedMembers = 1, totalMembers = 3),
        tokenList = TokenList.Empty,
        priceChangeLce = Unit.lceError(),
    )

    @Test
    fun `GIVEN a personal and two joint accounts WHEN load THEN the pending joint account is left out`() = runTest {
        // Arrange
        arrange(accounts = listOf(personal, activeJoint, pendingJoint))

        // Act
        val actual = loader.loadPortfolioData(userWallet.walletId).first()

        // Assert
        assertThat(actual.wallet.accounts.map { it.account }).containsExactly(personal, activeJoint).inOrder()
    }

    @Test
    fun `GIVEN a token of a joint network WHEN accountsFor THEN both accounts are targets`() = runTest {
        // Arrange
        arrange(accounts = listOf(personal, activeJoint))
        val data = loader.loadPortfolioData(userWallet.walletId).first()

        // Act
        val actual = data.accountsFor(ethereum)

        // Assert
        assertThat(actual.map { it.account }).containsExactly(personal, activeJoint).inOrder()
    }

    @Test
    fun `GIVEN a token outside the joint networks WHEN accountsFor THEN only the personal account is a target`() =
        runTest {
            // Arrange
            arrange(accounts = listOf(personal, activeJoint))
            val data = loader.loadPortfolioData(userWallet.walletId).first()

            // Act
            val actual = data.accountsFor(solana)

            // Assert
            assertThat(actual.map { it.account }).containsExactly(personal)
        }

    private fun arrange(accounts: List<AccountStatus>) {
        every { appCurrencyUseCase.invokeOrDefault() } returns flowOf(AppCurrency.Default)
        every { getHotCryptoUseCase(userWallet.walletId) } returns flowOf(
            listOf(
                HotCryptoCurrency(cryptoCurrency = ethereum, quoteStatus = QuoteStatus(ethereum.id.rawCurrencyId!!)),
                HotCryptoCurrency(cryptoCurrency = solana, quoteStatus = QuoteStatus(solana.id.rawCurrencyId!!)),
            ),
        )
        every { accountListSupplier.invoke(userWallet.walletId) } returns flowOf(
            AccountStatusList(
                userWalletId = userWallet.walletId,
                accountStatuses = accounts,
                totalAccounts = accounts.size,
                totalArchivedAccounts = 0,
                totalFiatBalance = TotalFiatBalance.Loaded(BigDecimal.ONE, StatusSource.ACTUAL),
                sortType = TokensSortType.NONE,
                groupType = TokensGroupType.NONE,
            ),
        )
        every { getUserWalletUseCase.invokeFlow(userWallet.walletId) } returns flowOf(userWallet.right())
        coEvery {
            getAccountCurrencyStatusUseCase.invokeSync(userWallet.walletId, any<List<CryptoCurrency>>())
        } returns emptyMap<Account.CryptoPortfolio, List<CryptoCurrencyStatus>>().some()
        every { getJointAccountSupportedNetworksUseCase() } returns listOf(Network.RawID(ethereum.network.rawId))
    }

    private companion object {
        const val SAFE = "0x5afe00000000000000000000000000000000000001"
    }
}