package com.tangem.features.commonfeatures.impl.portfolioselector.fetcher

import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.core.utils.lceError
import com.tangem.domain.models.TokensGroupType
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.JointAccountStatusValue
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioFetcher
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.MockAccounts
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultPortfolioSelectorBridgeTest {

    private val userWallet = MockUserWalletFactory.create()

    private val personal = AccountStatus.Personal(
        account = MockAccounts.createAccount(derivationIndex = 0),
        tokenList = TokenList.Empty,
        priceChangeLce = Unit.lceError(),
    )

    private val joint = AccountStatus.Joint(
        account = MockAccounts.createJointAccount(),
        value = JointAccountStatusValue.Loading,
        tokenList = TokenList.Empty,
        priceChangeLce = Unit.lceError(),
    )

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a personal and a joint account WHEN data collected THEN joint accounts follow the flag`(
        model: JointModel,
    ) = runTest {
        // Arrange
        val fetcher: DefaultPortfolioFetcher = mockk {
            every { data } returns flowOf(portfolioData(listOf(personal, joint)))
        }
        val fetcherFactory: DefaultPortfolioFetcher.Factory = mockk {
            every { create(any(), any()) } returns fetcher
        }
        val bridge = DefaultPortfolioSelectorBridge(
            portfolioFetcherFactory = fetcherFactory,
            mode = PortfolioFetcher.Mode.All(isOnlyMultiCurrency = true),
            scope = backgroundScope,
            isJointAccountsIncluded = model.isJointAccountsIncluded,
        )

        // Act
        val actual = bridge.data.first()

        // Assert
        assertThat(actual).isEqualTo(portfolioData(model.expectedAccounts))
    }

    private fun provideTestModels() = listOf(
        JointModel(isJointAccountsIncluded = true, expectedAccounts = listOf(personal, joint)),
        JointModel(isJointAccountsIncluded = false, expectedAccounts = listOf(personal)),
    )

    private fun portfolioData(accountStatuses: List<AccountStatus>) = PortfolioFetcher.Data(
        appCurrency = AppCurrency.Default,
        isBalanceHidden = false,
        balances = mapOf(
            userWallet.walletId to PortfolioFetcher.PortfolioBalance(
                userWallet = userWallet,
                accountsBalance = AccountStatusList(
                    userWalletId = userWallet.walletId,
                    accountStatuses = accountStatuses,
                    totalAccounts = 2,
                    totalArchivedAccounts = 0,
                    totalJointAccounts = 1,
                    totalFiatBalance = TotalFiatBalance.Loading,
                    sortType = TokensSortType.NONE,
                    groupType = TokensGroupType.NONE,
                ),
            ),
        ),
    )

    data class JointModel(
        val isJointAccountsIncluded: Boolean,
        val expectedAccounts: List<AccountStatus>,
    )
}