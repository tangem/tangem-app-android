package com.tangem.features.earn.impl.sheet.model

import arrow.core.Option
import arrow.core.none
import arrow.core.some
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.utils.CryptoCurrencyStatusOperations
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.features.earn.impl.fake.FakeEarnPosition
import com.tangem.features.earn.impl.fake.FakeEarnProductPreset
import com.tangem.features.earn.impl.fake.FakeForeignRoute
import com.tangem.features.earn.impl.fake.createFakeEarnProduct
import com.tangem.features.earn.impl.fake.fakeSummary
import com.tangem.features.earn.impl.overview.EarnProductSummaries
import com.tangem.features.earn.impl.overview.EarnProductSummarySupplier
import com.tangem.features.earn.impl.overview.EarnProductsRegistry
import com.tangem.features.earn.impl.overview.TestAction
import com.tangem.features.earn.impl.overview.USER_WALLET_ID
import com.tangem.features.earn.impl.overview.createSummary
import com.tangem.features.earn.impl.sheet.EarnYieldSheetComponent
import com.tangem.features.earn.product.api.navigation.EarnNavigator
import com.tangem.features.earn.product.api.product.EarnActionHandler
import com.tangem.features.earn.product.api.product.EarnPositionSummary
import com.tangem.features.earn.product.api.product.EarnProduct
import com.tangem.features.earn.product.api.product.EarnProductSummary
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.Called
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EarnYieldSheetModelTest {

    private val router: Router = mockk(relaxed = true)
    private val callback: EarnYieldSheetComponent.ModelCallback = mockk(relaxed = true)
    private val summarySupplier: EarnProductSummarySupplier = mockk()
    private val accountStatusListSupplier: SingleAccountStatusListSupplier = mockk()
    private val accountStatusList: AccountStatusList = mockk()
    private val getSelectedAppCurrency: GetSelectedAppCurrencyUseCase = mockk()
    private val currency: CryptoCurrency = MockCryptoCurrencyFactory().ethereum

    private val lending = createFakeEarnProduct(FakeEarnProductPreset.Lending)
    private val staking = createFakeEarnProduct(FakeEarnProductPreset.Staking)
    private val vault = createFakeEarnProduct(FakeEarnProductPreset.Vault)

    private lateinit var originalLocale: Locale

    @BeforeEach
    fun setUp() {
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
        clearMocks(
            router,
            callback,
            summarySupplier,
            accountStatusListSupplier,
            accountStatusList,
            getSelectedAppCurrency,
        )
        mockkObject(CryptoCurrencyStatusOperations)
        every { accountStatusListSupplier(USER_WALLET_ID) } returns flowOf(accountStatusList)
        every { getSelectedAppCurrency.invokeOrDefault() } returns flowOf(AppCurrency.Default)
        stubStatus(accountStatusList, none())
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(CryptoCurrencyStatusOperations)
        Locale.setDefault(originalLocale)
    }

    @Test
    fun `GIVEN summaries frame WHEN model created THEN uiState lists the product rows`() = runTest {
        // Arrange
        val summaries = listOf(
            fakeSummary(FakeEarnProductPreset.Lending, FakeEarnPosition.Known),
            fakeSummary(FakeEarnProductPreset.Staking, FakeEarnPosition.None),
            fakeSummary(FakeEarnProductPreset.Vault, FakeEarnPosition.None),
        )

        // Act
        val model = createModel(summaries)
        advanceUntilIdle()

        // Assert
        val actual = model.uiState.value
        assertThat(actual.active).hasSize(1)
        assertThat(actual.inactive).hasSize(2)
        assertThat(actual.active.map { it.id }).containsExactly(lending.id)
        assertThat(actual.inactive.map { it.id }).containsExactly(staking.id, vault.id).inOrder()
        model.onDestroy()
    }

    @Test
    fun `GIVEN registered inactive Lending WHEN row onClick THEN product handler navigates through the host navigator and then callback dismisses`() =
        runTest {
            // Arrange
            val model = createModel(listOf(fakeSummary(FakeEarnProductPreset.Lending, FakeEarnPosition.None)))
            advanceUntilIdle()

            // Act
            model.uiState.value.inactive.single().onClick()

            // Assert
            verifyOrder {
                router.push(
                    AppRoute.EarnEntry(
                        userWalletId = USER_WALLET_ID,
                        cryptoCurrency = currency,
                        source = AppRoute.EarnEntry.Source.YieldSheet,
                        start = AppRoute.EarnEntry.Start.Picker(preselected = lending.id),
                    ),
                    any(),
                )
                callback.onDismiss()
            }
            model.onDestroy()
        }

    @Test
    fun `GIVEN registered active Lending WHEN row onClick THEN Info is opened and the callback dismisses`() = runTest {
        // Arrange
        val model = createModel(listOf(fakeSummary(FakeEarnProductPreset.Lending, FakeEarnPosition.Known)))
        advanceUntilIdle()

        // Act
        model.uiState.value.active.single().onClick()

        // Assert
        verifyOrder {
            router.push(
                AppRoute.EarnEntry(
                    userWalletId = USER_WALLET_ID,
                    cryptoCurrency = currency,
                    source = AppRoute.EarnEntry.Source.YieldSheet,
                    start = AppRoute.EarnEntry.Start.Info(productId = lending.id),
                ),
                any(),
            )
            callback.onDismiss()
        }
        model.onDestroy()
    }

    @Test
    fun `GIVEN staking product WHEN row onClick THEN a foreign route goes to the router unchanged and the callback dismisses`() =
        runTest {
            // Arrange
            val model = createModel(listOf(fakeSummary(FakeEarnProductPreset.Staking, FakeEarnPosition.None)))
            advanceUntilIdle()

            // Act
            model.uiState.value.inactive.single().onClick()

            // Assert
            verifyOrder {
                router.push(FakeForeignRoute, any())
                callback.onDismiss()
            }
            model.onDestroy()
        }

    @Test
    fun `GIVEN row of a product that left the registry WHEN row onClick THEN router and callback are not touched`() =
        runTest {
            // Arrange
            val model = createModel(listOf(fakeSummary(FakeEarnProductPreset.Lending, FakeEarnPosition.Known)))
            advanceUntilIdle()
            val rowsWhileRegistered = model.uiState.value.active

            // Act
            model.onActionClick(EarnProductId("gone"), TestAction)

            // Assert
            assertThat(rowsWhileRegistered).hasSize(1)
            verify { router wasNot Called }
            verify { callback wasNot Called }
            model.onDestroy()
        }

    @Test
    fun `GIVEN one row WHEN tapped twice THEN the product handler is created once`() = runTest {
        // Arrange
        val countingLending = CountingProduct(delegate = lending)
        val model = createModel(
            summaries = listOf(fakeSummary(FakeEarnProductPreset.Lending, FakeEarnPosition.None)),
            products = listOf(countingLending, staking, vault),
        )
        advanceUntilIdle()
        val row = model.uiState.value.inactive.single()

        // Act
        row.onClick()
        row.onClick()

        // Assert
        assertThat(countingLending.actionHandlerCalls).isEqualTo(1)
        verify(exactly = 2) {
            router.push(
                AppRoute.EarnEntry(
                    userWalletId = USER_WALLET_ID,
                    cryptoCurrency = currency,
                    source = AppRoute.EarnEntry.Source.YieldSheet,
                    start = AppRoute.EarnEntry.Start.Picker(preselected = lending.id),
                ),
                any(),
            )
        }
        verify(exactly = 2) { callback.onDismiss() }
        model.onDestroy()
    }

    @Test
    fun `GIVEN model WHEN onDismissClick THEN callback dismisses`() = runTest {
        // Arrange
        val model = createModel(emptyList())
        advanceUntilIdle()

        // Act
        model.onDismissClick()

        // Assert
        verify(exactly = 1) { callback.onDismiss() }
        model.onDestroy()
    }

    @Test
    fun `GIVEN status with fiat rate WHEN model created THEN the active row shows the fiat amount`() = runTest {
        // Arrange
        val amount = BigDecimal("0.5031")
        val fiatRate = BigDecimal("4000")
        stubFiatRate(accountStatusList, fiatRate)
        val summary = createSummary(productId = lending.id, position = EarnPositionSummary(amount))

        // Act
        val model = createModel(listOf(summary))
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.active.single().amountFiat)
            .isEqualTo(fiatText(amount, fiatRate, AppCurrency.Default))
        model.onDestroy()
    }

    @Test
    fun `GIVEN account status not emitted yet WHEN model created THEN the rows are already listed without fiat`() =
        runTest {
            // Arrange
            every { accountStatusListSupplier(USER_WALLET_ID) } returns flow<AccountStatusList> { awaitCancellation() }
            val summaries = listOf(
                createSummary(productId = lending.id, position = EarnPositionSummary(BigDecimal("0.5031"))),
                createSummary(productId = vault.id),
            )

            // Act
            val model = createModel(summaries)
            advanceUntilIdle()

            // Assert
            val actual = model.uiState.value
            assertThat(actual.active.map { it.id }).containsExactly(lending.id)
            assertThat(actual.inactive.map { it.id }).containsExactly(vault.id)
            assertThat(actual.active.single().amountFiat).isNull()
            model.onDestroy()
        }

    @Test
    fun `GIVEN rows shown WHEN the selected app currency changes THEN the fiat amount is recomputed in the new currency`() =
        runTest {
            // Arrange
            val amount = BigDecimal("0.5031")
            val fiatRate = BigDecimal("4000")
            val euro = AppCurrency(code = "EUR", name = "Euro", symbol = "€")
            val selectedCurrency = MutableStateFlow(AppCurrency.Default)
            every { getSelectedAppCurrency.invokeOrDefault() } returns selectedCurrency
            stubFiatRate(accountStatusList, fiatRate)
            val model = createModel(listOf(createSummary(productId = lending.id, position = EarnPositionSummary(amount))))
            advanceUntilIdle()
            val fiatInDefaultCurrency = model.uiState.value.active.single().amountFiat

            // Act
            selectedCurrency.value = euro
            advanceUntilIdle()

            // Assert
            assertThat(fiatInDefaultCurrency).isEqualTo(fiatText(amount, fiatRate, AppCurrency.Default))
            assertThat(model.uiState.value.active.single().amountFiat).isEqualTo(fiatText(amount, fiatRate, euro))
            model.onDestroy()
        }

    @Test
    fun `GIVEN rows shown WHEN the wallet reports a new fiat rate THEN the fiat amount is recomputed with it`() =
        runTest {
            // Arrange
            val amount = BigDecimal("0.5031")
            val firstRate = BigDecimal("4000")
            val nextRate = BigDecimal("4500")
            val updatedAccountStatusList: AccountStatusList = mockk()
            val accountStatusLists = MutableStateFlow(accountStatusList)
            every { accountStatusListSupplier(USER_WALLET_ID) } returns accountStatusLists
            stubFiatRate(accountStatusList, firstRate)
            stubFiatRate(updatedAccountStatusList, nextRate)
            val model = createModel(listOf(createSummary(productId = lending.id, position = EarnPositionSummary(amount))))
            advanceUntilIdle()
            val fiatWithFirstRate = model.uiState.value.active.single().amountFiat

            // Act
            accountStatusLists.value = updatedAccountStatusList
            advanceUntilIdle()

            // Assert
            assertThat(fiatWithFirstRate).isEqualTo(fiatText(amount, firstRate, AppCurrency.Default))
            assertThat(model.uiState.value.active.single().amountFiat)
                .isEqualTo(fiatText(amount, nextRate, AppCurrency.Default))
            model.onDestroy()
        }

    @Test
    fun `GIVEN no status for the currency WHEN model created THEN the active row has no fiat amount`() = runTest {
        // Arrange
        val summary = createSummary(productId = lending.id, position = EarnPositionSummary(BigDecimal("0.5031")))

        // Act
        val model = createModel(listOf(summary))
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.active.single().amountFiat).isNull()
        model.onDestroy()
    }

    private fun TestScope.createModel(
        summaries: List<EarnProductSummary>,
        products: List<EarnProduct> = listOf(lending, staking, vault),
    ): EarnYieldSheetModel {
        every { summarySupplier(USER_WALLET_ID) } returns flowOf(
            EarnProductSummaries(
                products = mapOf(currency.id to summaries),
                rateSources = emptyMap(),
                source = StatusSource.ACTUAL,
            ),
        )
        val registry = EarnProductsRegistry(products = products.toSet(), rateSources = emptySet())

        return EarnYieldSheetModel(
            dispatchers = createTestingCoroutineDispatcherProvider(),
            paramsContainer = MutableParamsContainer(
                EarnYieldSheetComponent.Params(
                    userWalletId = USER_WALLET_ID,
                    cryptoCurrency = currency,
                    callback = callback,
                ),
            ),
            registry = registry,
            summarySupplier = summarySupplier,
            singleAccountStatusListSupplier = accountStatusListSupplier,
            getSelectedAppCurrency = getSelectedAppCurrency,
            converter = EarnYieldSheetConverter(registry = registry),
            router = router,
        )
    }

    private fun stubFiatRate(list: AccountStatusList, fiatRate: BigDecimal) {
        val value: CryptoCurrencyStatus.Value = mockk()
        every { value.fiatRate } returns fiatRate
        val status: CryptoCurrencyStatus = mockk()
        every { status.value } returns value
        stubStatus(list, status.some())
    }

    private fun stubStatus(list: AccountStatusList, result: Option<CryptoCurrencyStatus>) {
        every {
            with(CryptoCurrencyStatusOperations) {
                list.getCryptoCurrencyStatus(any<CryptoCurrency>())
            }
        } returns result
    }

    private fun fiatText(amount: BigDecimal, fiatRate: BigDecimal, appCurrency: AppCurrency) =
        stringReference((amount * fiatRate).format { fiat(appCurrency.code, appCurrency.symbol) })

    private class CountingProduct(private val delegate: EarnProduct) : EarnProduct by delegate {

        var actionHandlerCalls: Int = 0
            private set

        override fun actionHandler(params: EarnProduct.Params, navigator: EarnNavigator): EarnActionHandler {
            actionHandlerCalls++
            return delegate.actionHandler(params, navigator)
        }
    }
}