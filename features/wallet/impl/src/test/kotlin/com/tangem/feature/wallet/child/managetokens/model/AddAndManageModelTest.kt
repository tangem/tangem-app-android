package com.tangem.feature.wallet.child.managetokens.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.wallet.child.managetokens.AddAndManageBottomSheetComponent
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioFetcher
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorController
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class AddAndManageModelTest {

    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val portfolioFetcher: PortfolioFetcher = mockk(relaxed = true) {
        every { data } returns flowOf(
            PortfolioFetcher.Data(
                appCurrency = mockk(relaxed = true),
                isBalanceHidden = false,
                balances = emptyMap(),
            ),
        )
    }
    private val portfolioFetcherFactory: PortfolioFetcher.Factory = mockk(relaxed = true) {
        every { create(any(), any()) } returns portfolioFetcher
    }
    private val portfolioSelectorController: PortfolioSelectorController = mockk(relaxed = true) {
        every { selectedAccount } returns flowOf(null)
    }
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier = mockk(relaxed = true) {
        coEvery { getSyncOrNull(any<UserWalletId>()) } returns null
    }

    private val onDismiss: () -> Unit = mockk(relaxed = true)
    private val onOrganizeTokensClick: () -> Unit = mockk(relaxed = true)
    private val onManageTokensClick: (AccountId) -> Unit = mockk(relaxed = true)

    private val userWalletId = UserWalletId(stringValue = "0123456789ABCDEF")

    private val params = AddAndManageBottomSheetComponent.Params(
        userWalletId = userWalletId,
        onDismiss = onDismiss,
        onOrganizeTokensClick = onOrganizeTokensClick,
        onManageTokensClick = onManageTokensClick,
    )

    private fun createModel(): AddAndManageModel = AddAndManageModel(
        paramsContainer = MutableParamsContainer(params),
        dispatchers = TestingCoroutineDispatcherProvider(),
        portfolioFetcherFactory = portfolioFetcherFactory,
        analyticsEventHandler = analyticsEventHandler,
        portfolioSelectorController = portfolioSelectorController,
        singleAccountStatusListSupplier = singleAccountStatusListSupplier,
    )

    @Test
    fun `GIVEN bottom sheet model WHEN onAddTokensClick THEN sends ButtonAddTokens event with correct payload`() =
        runTest {
            val model = createModel()
            val captured = slot<AnalyticsEvent>()

            model.onAddTokensClick()

            verify(exactly = 1) { analyticsEventHandler.send(capture(captured)) }
            assertThat(captured.captured.category).isEqualTo("Portfolio")
            assertThat(captured.captured.event).isEqualTo("Button - Add tokens")
            assertThat(captured.captured.params).isEmpty()
        }

    @Test
    fun `GIVEN bottom sheet model WHEN onOrganizeTokensClick THEN sends ButtonOrganizeTokens event with correct payload`() =
        runTest {
            val model = createModel()
            val captured = slot<AnalyticsEvent>()

            model.onOrganizeTokensClick()

            verify(exactly = 1) { analyticsEventHandler.send(capture(captured)) }
            assertThat(captured.captured.category).isEqualTo("Portfolio")
            assertThat(captured.captured.event).isEqualTo("Button - Organize Tokens")
            assertThat(captured.captured.params).isEmpty()
        }

    @Test
    fun `GIVEN bottom sheet model WHEN onOrganizeTokensClick THEN dismisses bottom sheet and forwards to params callback`() =
        runTest {
            val model = createModel()

            model.onOrganizeTokensClick()

            verify(exactly = 1) { onDismiss() }
            verify(exactly = 1) { onOrganizeTokensClick() }
        }

    @Test
    fun `GIVEN wallet has multi currency account WHEN model is created THEN shouldShowOrganize is true`() = runTest {
        coEvery { singleAccountStatusListSupplier.getSyncOrNull(userWalletId) } returns
            accountStatusListWithCurrencyCounts(2)

        val model = createModel()

        assertThat(model.state.value.shouldShowOrganize).isTrue()
    }

    @Test
    fun `GIVEN wallet has no multi currency account WHEN model is created THEN shouldShowOrganize is false`() = runTest {
        coEvery { singleAccountStatusListSupplier.getSyncOrNull(userWalletId) } returns
            accountStatusListWithCurrencyCounts(1)

        val model = createModel()

        assertThat(model.state.value.shouldShowOrganize).isFalse()
    }

    private fun accountStatusListWithCurrencyCounts(vararg currencyCounts: Int): AccountStatusList {
        val statuses: List<AccountStatus> = currencyCounts.map { count ->
            val tokenList = mockk<TokenList> {
                every { flattenCurrencies() } returns List(count) { mockk<CryptoCurrencyStatus>() }
            }
            mockk<AccountStatus.CryptoPortfolio> {
                every { this@mockk.tokenList } returns tokenList
            }
        }
        return mockk {
            every { accountStatuses } returns statuses
        }
    }
}