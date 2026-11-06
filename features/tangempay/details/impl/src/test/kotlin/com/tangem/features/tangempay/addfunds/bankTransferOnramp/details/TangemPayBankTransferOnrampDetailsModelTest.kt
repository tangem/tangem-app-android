package com.tangem.features.tangempay.addfunds.bankTransferOnramp.details

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItemUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.models.account.BankCredentials
import com.tangem.domain.models.tangempay.TangemPayProduct
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.model.AccountFee
import com.tangem.domain.tangempay.usecase.GetOnrampFeesUseCase
import com.tangem.domain.tangempay.error.TangemPayApiError
import com.tangem.features.tangempay.addfunds.bankTransferOnramp.details.TangemPayBankTransferOnrampDetailsScreenUM.DailyDepositLimitUM
import com.tangem.features.tangempay.addfunds.bankTransferOnramp.fees.TangemPayOnrampFeesUM
import com.tangem.features.tangempay.addfunds.bankTransferOnramp.fees.isEmpty
import com.tangem.features.tangempay.bankCredentials
import com.tangem.features.tangempay.components.TangemPayBankTransferOnrampDetailsComponent
import com.tangem.features.tangempay.components.TangemPayBankTransferOnrampDetailsComponent.FeeSource
import com.tangem.features.tangempay.components.TangemPayBankTransferOnrampDetailsComponent.RequisiteField
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.onrampFee
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import com.tangem.test.mock.tangempay.MockBankCredentials
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.util.Locale

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayBankTransferOnrampDetailsModelTest {

    private val userWalletId = UserWalletId("1234567890ABCDEF")
    private val product = TangemPayProduct.PAYMENT_ACCOUNT

    private val router: Router = mockk(relaxed = true)
    private val getOnrampFeesUseCase: GetOnrampFeesUseCase = mockk()
    private val clipboardManager: ClipboardManager = mockk(relaxed = true)
    private val shareManager: ShareManager = mockk(relaxed = true)
    private val analytics: TangemPayBankTransferOnrampDetailsComponent.Analytics = mockk(relaxed = true)

    private val defaultLocale = Locale.getDefault()

    @BeforeEach
    fun resetMocks() {
        Locale.setDefault(Locale.US)
        clearMocks(router, getOnrampFeesUseCase, clipboardManager, shareManager)
        clearMocks(analytics)
        coEvery { getOnrampFeesUseCase(userWalletId, product) } returns emptyList<AccountFee>().right()
    }

    @AfterEach
    fun restoreLocale() {
        Locale.setDefault(defaultLocale)
    }

    @Test
    fun `GIVEN bank credentials WHEN model created THEN requisites shown in the mock order and shown event sent`() =
        runTest {
            // Act
            val model = createModel()
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.requisites.map { it.key to it.value }).containsExactly(
                "Account number" to "123456",
                "Routing number" to "654321",
                "Beneficiary name" to "Rain Payments Inc",
                "Beneficiary address" to "123 Finance St",
                "Bank name" to "SSB Bank",
                "Bank address" to "8700 Perry Highway",
            ).inOrder()
            verify(exactly = 1) { analytics.onDetailsShown() }
        }

    @Test
    fun `GIVEN credentials with blank fields WHEN model created THEN those requisites are hidden`() = runTest {
        // Act
        val model = createModel(bankCredentials().copy(beneficiaryAddress = "", beneficiaryBankAddress = " "))
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.requisites.map { it.key }).containsExactly(
            "Account number",
            "Routing number",
            "Beneficiary name",
            "Bank name",
        ).inOrder()
    }

    @Test
    fun `GIVEN daily deposit limit WHEN model created THEN limitation shown`() = runTest {
        // Arrange
        val bankCredentials = bankCredentials(
            depositLimit = BankCredentials.DepositLimit(
                amount = BigDecimal("10000"),
                period = BankCredentials.DepositLimit.Period.DAY,
            ),
        )

        // Act
        val model = createModel(bankCredentials)
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.dailyDepositLimit).isEqualTo(
            DailyDepositLimitUM(
                value = stringReference("$10,000"),
                hint = resourceReference(
                    id = R.string.tangempay_bank_transfer_details_daily_deposits_hint,
                    formatArgs = wrappedList("$10,000"),
                ),
            ),
        )
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN no daily deposit limit WHEN model created THEN limitation hidden`(model: HiddenLimitModel) = runTest {
        // Act
        val detailsModel = createModel(bankCredentials(depositLimit = model.depositLimit))
        advanceUntilIdle()

        // Assert
        assertThat(detailsModel.uiState.value.dailyDepositLimit).isNull()
    }

    @Test
    fun `GIVEN onramp fees returned WHEN model created THEN fixed fee shown as amount and percentage as percent`() =
        runTest {
            // Arrange
            coEvery { getOnrampFeesUseCase(userWalletId, product) } returns listOf(
                onrampFee(amount = BigDecimal("1.00")),
                onrampFee(
                    type = "SPEI_ONRAMP",
                    name = "SPEI",
                    calculationType = AccountFee.CalculationType.PERCENTAGE,
                    amount = BigDecimal("1"),
                    currency = "MXN",
                ),
            ).right()

            // Act
            val model = createModel()
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.fees).isEqualTo(
                TangemPayOnrampFeesUM.Content(
                    rows = persistentListOf(
                        TangemPayOnrampFeesUM.FeeRow(type = "ACH_ONRAMP", title = stringReference("ACH"), value = "$1"),
                        TangemPayOnrampFeesUM.FeeRow(type = "SPEI_ONRAMP", title = stringReference("SPEI"), value = "1%"),
                    ),
                ),
            )
        }

    @ParameterizedTest
    @EnumSource(TangemPayProduct::class)
    fun `GIVEN catalogue fee source of a product WHEN model created THEN fees requested for that product`(
        catalogueProduct: TangemPayProduct,
    ) = runTest {
        // Arrange
        coEvery { getOnrampFeesUseCase(userWalletId, catalogueProduct) } returns emptyList<AccountFee>().right()

        // Act
        val model = createModel(feeSource = FeeSource.Catalogue(catalogueProduct))
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { getOnrampFeesUseCase(userWalletId, catalogueProduct) }
        model.onDestroy()
    }

    @ParameterizedTest
    @MethodSource("provideRouteFeeModels")
    fun `GIVEN route fee source WHEN model created THEN route fee shown and catalogue not requested`(
        model: RouteFeeModel,
    ) = runTest {
        // Act
        val detailsModel = createModel(
            bankCredentials = bankCredentials(rail = model.rail, depositFee = model.depositFee),
            feeSource = FeeSource.Route,
        )
        advanceUntilIdle()

        // Assert
        assertThat(detailsModel.uiState.value.fees).isEqualTo(TangemPayOnrampFeesUM.Content(rows = model.expectedRows))
        coVerify(exactly = 0) { getOnrampFeesUseCase(any(), any()) }
    }

    @Test
    fun `GIVEN fees request fails WHEN retry succeeds THEN fee rows shown`() = runTest {
        // Arrange
        coEvery { getOnrampFeesUseCase(userWalletId, product) } returnsMany listOf(
            TangemPayApiError.Unspecified.left(),
            listOf(onrampFee(amount = BigDecimal("1.00"))).right(),
        )
        val model = createModel()
        advanceUntilIdle()
        val error = model.uiState.value.fees as TangemPayOnrampFeesUM.Error

        // Act
        error.onRetryClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.fees).isInstanceOf(TangemPayOnrampFeesUM.Content::class.java)
        coVerify(exactly = 2) { getOnrampFeesUseCase(userWalletId, product) }
    }

    @Test
    fun `GIVEN fees retry in flight WHEN retry clicked again THEN shimmer shown and fees requested once more`() =
        runTest {
            // Arrange
            val pending = CompletableDeferred<Either<TangemPayApiError, List<AccountFee>>>()
            var calls = 0
            coEvery { getOnrampFeesUseCase(userWalletId, product) } coAnswers {
                calls++
                if (calls == 1) TangemPayApiError.Unspecified.left() else pending.await()
            }
            val model = createModel()
            advanceUntilIdle()
            val error = model.uiState.value.fees as TangemPayOnrampFeesUM.Error

            // Act
            error.onRetryClick()
            error.onRetryClick()
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.fees).isEqualTo(TangemPayOnrampFeesUM.Loading)
            coVerify(exactly = 2) { getOnrampFeesUseCase(userWalletId, product) }
            pending.complete(emptyList<AccountFee>().right())
            advanceUntilIdle()
            model.onDestroy()
        }

    @Test
    fun `GIVEN requisites shown WHEN copy clicked THEN value copied as sensitive`() = runTest {
        // Arrange
        val model = createModel()
        advanceUntilIdle()

        // Act
        model.uiState.value.requisites.first { it.key == "Account number" }.onCopyClick()

        // Assert
        verify(exactly = 1) { clipboardManager.setText(text = "123456", isSensitive = true) }
        verify(exactly = 1) { analytics.onFieldCopied(RequisiteField.ACCOUNT_NUMBER) }
    }

    @Test
    fun `GIVEN requisites shown WHEN share clicked THEN every requisite shared`() = runTest {
        // Arrange
        val model = createModel()
        advanceUntilIdle()

        // Act
        model.uiState.value.onShareClick()

        // Assert
        verify(exactly = 1) {
            shareManager.shareText(
                "Account number: 123456\n" +
                    "Routing number: 654321\n" +
                    "Beneficiary name: Rain Payments Inc\n" +
                    "Beneficiary address: 123 Finance St\n" +
                    "Bank name: SSB Bank\n" +
                    "Bank address: 8700 Perry Highway",
            )
        }
        verify(exactly = 1) { analytics.onShareClicked() }
    }

    @Test
    fun `GIVEN screen shown WHEN close clicked THEN screen popped`() = runTest {
        // Arrange
        val model = createModel()
        advanceUntilIdle()

        // Act
        model.uiState.value.onCloseClick()

        // Assert
        verify(exactly = 1) { router.pop(any()) }
    }

    @Test
    fun `GIVEN one rail WHEN model created THEN no tabs shown`() = runTest {
        // Act
        val model = createModel()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.tabs).isEmpty()
    }

    @Test
    fun `GIVEN USD and MXN rails WHEN model created THEN a tab per rail with the first one selected`() = runTest {
        // Act
        val model = createModelWithRails(listOf(USD_ROUTE, SPEI_CREDENTIALS))
        advanceUntilIdle()

        // Assert
        val state = model.uiState.value
        assertThat(state.tabs.map { it.toTabState() })
            .containsExactly(
                TabState(id = "0", label = stringReference("USD"), isSelected = true),
                TabState(id = "1", label = stringReference("MXN"), isSelected = false),
            )
            .inOrder()
        assertThat(state.requisites.map { it.key }).containsExactlyElementsIn(US_REQUISITE_KEYS).inOrder()
        assertThat(state.referenceWarning).isNull()
        assertThat(state.fees).isEqualTo(
            TangemPayOnrampFeesUM.Content(
                rows = persistentListOf(
                    TangemPayOnrampFeesUM.FeeRow(type = "ACH", title = stringReference("ACH"), value = "$1"),
                ),
            ),
        )
        assertThat(state.dailyDepositLimit).isEqualTo(
            DailyDepositLimitUM(
                value = stringReference("$10,000"),
                hint = resourceReference(
                    id = R.string.tangempay_bank_transfer_details_daily_deposits_hint,
                    formatArgs = wrappedList("$10,000"),
                ),
            ),
        )
    }

    @Test
    fun `GIVEN ACH route with a transfer message WHEN shown THEN no reference row or warning`() = runTest {
        // Act
        val model = createModel(
            bankCredentials = USD_ROUTE.copy(
                transferMessage = "REF-A1B2C3D4",
                exchangeRate = SPEI_CREDENTIALS.exchangeRate,
            ),
            feeSource = FeeSource.Route,
        )
        advanceUntilIdle()

        // Assert
        val state = model.uiState.value
        assertThat(state.requisites.map { it.key }).containsExactlyElementsIn(US_REQUISITE_KEYS).inOrder()
        assertThat(state.referenceWarning).isNull()
    }

    @Test
    fun `GIVEN lowercase spei rail WHEN shown THEN SPEI requisites shown`() = runTest {
        // Act
        val model = createModel(SPEI_CREDENTIALS.copy(rail = "spei"), FeeSource.Route)
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.requisites.map { it.key }).containsExactly("CLABE", "Reference").inOrder()
    }

    @ParameterizedTest
    @MethodSource("provideRouteFeesModels")
    fun `GIVEN route credentials WHEN shown THEN fees, rate and notes follow the rail`(model: RouteFeesModel) =
        runTest {
            // Act
            val detailsModel = createModel(bankCredentials = model.credentials, feeSource = FeeSource.Route)
            advanceUntilIdle()

            // Assert
            val fees = detailsModel.uiState.value.fees
            assertThat(fees).isEqualTo(model.expected)
            assertThat(fees.isEmpty).isFalse()
        }

    @ParameterizedTest
    @MethodSource("provideRateModels")
    fun `GIVEN SPEI exchange rate WHEN shown THEN rate rounded to two decimals`(model: RateModel) = runTest {
        // Act
        val detailsModel = createModel(
            bankCredentials = SPEI_CREDENTIALS.copy(
                exchangeRate = BankCredentials.ExchangeRate(rate = model.rate, base = "USD", quote = "MXN"),
            ),
            feeSource = FeeSource.Route,
        )
        advanceUntilIdle()

        // Assert
        val fees = detailsModel.uiState.value.fees as TangemPayOnrampFeesUM.Content
        assertThat(fees.rows.last()).isEqualTo(
            TangemPayOnrampFeesUM.FeeRow(
                type = "EXCHANGE_RATE",
                title = resourceReference(R.string.tangempay_bank_transfer_details_exchange_rate),
                value = model.expectedValue,
            ),
        )
    }

    @Test
    fun `GIVEN MXN tab WHEN clicked THEN SPEI details shown on the selected tab`() = runTest {
        // Arrange
        val model = createModelWithRails(listOf(bankCredentials(), SPEI_CREDENTIALS))
        advanceUntilIdle()

        // Act
        model.clickTab(index = 1)

        // Assert
        val state = model.uiState.value
        assertThat(state.tabs.map { it.toTabState() })
            .containsExactly(
                TabState(id = "0", label = stringReference("USD"), isSelected = false),
                TabState(id = "1", label = stringReference("MXN"), isSelected = true),
            )
            .inOrder()
        assertThat(state.requisites.map { it.key to it.value })
            .containsExactly("CLABE" to "646180157000000004", "Reference" to "TNG7K2Q9")
            .inOrder()
        assertThat(state.referenceWarning)
            .isEqualTo(resourceReference(R.string.tangempay_bank_transfer_details_reference_warning))
        assertThat(state.fees).isEqualTo(
            TangemPayOnrampFeesUM.Content(
                rows = persistentListOf(SPEI_FEE_ROW, RATE_ROW),
                notes = persistentListOf(CONVERSION_NOTICE, RATE_DISCLAIMER),
            ),
        )
        assertThat(state.dailyDepositLimit).isEqualTo(
            DailyDepositLimitUM(value = stringReference("$10,000"), hint = null),
        )
    }

    @Test
    fun `GIVEN MXN tab selected WHEN USD tab clicked THEN US details shown again`() = runTest {
        // Arrange
        val model = createModelWithRails(listOf(bankCredentials(), SPEI_CREDENTIALS))
        advanceUntilIdle()
        model.clickTab(index = 1)

        // Act
        model.clickTab(index = 0)

        // Assert
        assertThat(model.uiState.value.requisites.map { it.key })
            .containsExactlyElementsIn(US_REQUISITE_KEYS)
            .inOrder()
        assertThat(model.uiState.value.referenceWarning).isNull()
    }

    @Test
    fun `GIVEN SPEI rail without reference WHEN shown THEN only CLABE and no warning`() = runTest {
        // Act
        val model = createModel(SPEI_CREDENTIALS.copy(transferMessage = null), FeeSource.Route)
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.requisites.map { it.key }).containsExactly("CLABE")
        assertThat(model.uiState.value.referenceWarning).isNull()
    }

    @Test
    fun `GIVEN MXN tab selected WHEN share clicked THEN CLABE and reference shared`() = runTest {
        // Arrange
        val model = createModelWithRails(listOf(bankCredentials(), SPEI_CREDENTIALS))
        advanceUntilIdle()
        model.clickTab(index = 1)

        // Act
        model.uiState.value.onShareClick()

        // Assert
        verify(exactly = 1) { shareManager.shareText("CLABE: 646180157000000004\nReference: TNG7K2Q9") }
    }

    @ParameterizedTest
    @MethodSource("provideSpeiCopyModels")
    fun `GIVEN SPEI requisite WHEN copied THEN value copied as sensitive and its field reported`(
        model: SpeiCopyModel,
    ) = runTest {
        // Arrange
        val detailsModel = createModel(SPEI_CREDENTIALS, FeeSource.Route)
        advanceUntilIdle()

        // Act
        detailsModel.uiState.value.requisites.first { it.key == model.key }.onCopyClick()

        // Assert
        verify(exactly = 1) { clipboardManager.setText(text = model.value, isSensitive = true) }
        verify(exactly = 1) { analytics.onFieldCopied(model.field) }
    }

    private fun TestScope.createModelWithRails(
        rails: List<BankCredentials>,
        feeSource: FeeSource = FeeSource.Route,
    ) = TangemPayBankTransferOnrampDetailsModel(
        paramsContainer = MutableParamsContainer(
            TangemPayBankTransferOnrampDetailsComponent.Params(
                userWalletId = userWalletId,
                bankCredentials = rails,
                feeSource = feeSource,
                analytics = analytics,
            ),
        ),
        dispatchers = createTestingCoroutineDispatcherProvider(),
        router = router,
        getOnrampFeesUseCase = getOnrampFeesUseCase,
        clipboardManager = clipboardManager,
        shareManager = shareManager,
    )

    private fun TangemTabItemUM.toTabState() = (this as TangemTabItemUM.Content).let { tab ->
        TabState(id = tab.id, label = tab.label, isSelected = tab.isSelected)
    }

    data class TabState(val id: String, val label: TextReference, val isSelected: Boolean)

    private fun provideRouteFeesModels() = listOf(
        RouteFeesModel(
            credentials = SPEI_CREDENTIALS,
            expected = TangemPayOnrampFeesUM.Content(
                rows = persistentListOf(SPEI_FEE_ROW, RATE_ROW),
                notes = persistentListOf(CONVERSION_NOTICE, RATE_DISCLAIMER),
            ),
        ),
        RouteFeesModel(
            credentials = SPEI_CREDENTIALS.copy(exchangeRate = null),
            expected = TangemPayOnrampFeesUM.Content(
                rows = persistentListOf(SPEI_FEE_ROW),
                notes = persistentListOf(CONVERSION_NOTICE),
            ),
        ),
        RouteFeesModel(
            credentials = SPEI_CREDENTIALS.copy(
                depositFee = BankCredentials.DepositFee(BigDecimal("15"), BankCredentials.DepositFee.Type.FLAT),
                exchangeRate = null,
            ),
            expected = TangemPayOnrampFeesUM.Content(
                rows = persistentListOf(
                    TangemPayOnrampFeesUM.FeeRow(type = "SPEI", title = stringReference("SPEI"), value = "MX$15"),
                ),
                notes = persistentListOf(CONVERSION_NOTICE),
            ),
        ),
        RouteFeesModel(
            credentials = SPEI_CREDENTIALS.copy(depositFee = null, exchangeRate = null),
            expected = TangemPayOnrampFeesUM.Content(
                rows = persistentListOf(),
                notes = persistentListOf(CONVERSION_NOTICE),
            ),
        ),
        RouteFeesModel(
            credentials = USD_ROUTE.copy(exchangeRate = SPEI_CREDENTIALS.exchangeRate),
            expected = TangemPayOnrampFeesUM.Content(
                rows = persistentListOf(
                    TangemPayOnrampFeesUM.FeeRow(type = "ACH", title = stringReference("ACH"), value = "$1"),
                ),
            ),
        ),
    )

    data class RouteFeesModel(val credentials: BankCredentials, val expected: TangemPayOnrampFeesUM)

    private fun provideRateModels() = listOf(
        RateModel(rate = BigDecimal("17.25"), expectedValue = "1 USD ≈ 17.25 MXN"),
        RateModel(rate = BigDecimal("17.256"), expectedValue = "1 USD ≈ 17.26 MXN"),
        RateModel(rate = BigDecimal("1234.5"), expectedValue = "1 USD ≈ 1,234.5 MXN"),
    )

    data class RateModel(val rate: BigDecimal, val expectedValue: String)

    private fun TangemPayBankTransferOnrampDetailsModel.clickTab(index: Int) {
        (uiState.value.tabs[index] as TangemTabItemUM.Content).onClick()
    }

    private fun provideSpeiCopyModels() = listOf(
        SpeiCopyModel(key = "CLABE", value = "646180157000000004", field = RequisiteField.CLABE),
        SpeiCopyModel(key = "Reference", value = "TNG7K2Q9", field = RequisiteField.REFERENCE),
    )

    data class SpeiCopyModel(val key: String, val value: String, val field: RequisiteField)

    private fun TestScope.createModel(
        bankCredentials: BankCredentials = bankCredentials(),
        feeSource: FeeSource = FeeSource.Catalogue(product),
    ) = createModelWithRails(rails = listOf(bankCredentials), feeSource = feeSource)

    private fun provideRouteFeeModels(): List<RouteFeeModel> {
        val achRow = { value: String ->
            TangemPayOnrampFeesUM.FeeRow(type = "ACH", title = stringReference("ACH"), value = value)
        }
        val flatFee = BankCredentials.DepositFee(BigDecimal("1.00"), BankCredentials.DepositFee.Type.FLAT)
        return listOf(
            RouteFeeModel(rail = "ACH", depositFee = flatFee, expectedRows = persistentListOf(achRow("$1"))),
            RouteFeeModel(
                rail = "ACH",
                depositFee = BankCredentials.DepositFee(BigDecimal("1"), BankCredentials.DepositFee.Type.PERCENTAGE),
                expectedRows = persistentListOf(achRow("1%")),
            ),
            RouteFeeModel(
                rail = "ACH",
                depositFee = BankCredentials.DepositFee(BigDecimal("1.00"), BankCredentials.DepositFee.Type.UNKNOWN),
                expectedRows = persistentListOf(),
            ),
            RouteFeeModel(rail = "ACH", depositFee = null, expectedRows = persistentListOf()),
            RouteFeeModel(rail = null, depositFee = flatFee, expectedRows = persistentListOf()),
        )
    }

    private fun provideTestModels() = listOf(
        HiddenLimitModel(depositLimit = null),
        HiddenLimitModel(
            depositLimit = BankCredentials.DepositLimit(
                amount = BigDecimal("10000"),
                period = BankCredentials.DepositLimit.Period.UNKNOWN,
            ),
        ),
    )

    data class HiddenLimitModel(val depositLimit: BankCredentials.DepositLimit?)

    data class RouteFeeModel(
        val rail: String?,
        val depositFee: BankCredentials.DepositFee?,
        val expectedRows: ImmutableList<TangemPayOnrampFeesUM.FeeRow>,
    )

    private companion object {
        val SPEI_CREDENTIALS = MockBankCredentials.speiBankCredentials(transferMessage = "TNG7K2Q9")
        val USD_ROUTE = bankCredentials(
            depositFee = BankCredentials.DepositFee(BigDecimal("1.00"), BankCredentials.DepositFee.Type.FLAT),
            depositLimit = BankCredentials.DepositLimit(
                amount = BigDecimal("10000"),
                period = BankCredentials.DepositLimit.Period.DAY,
            ),
        )
        val CONVERSION_NOTICE = resourceReference(
            id = R.string.tangempay_bank_transfer_details_conversion_notice,
            formatArgs = wrappedList("MXN"),
        )
        val RATE_DISCLAIMER = resourceReference(R.string.tangempay_bank_transfer_details_rate_disclaimer)
        val SPEI_FEE_ROW = TangemPayOnrampFeesUM.FeeRow(type = "SPEI", title = stringReference("SPEI"), value = "1%")
        val RATE_ROW = TangemPayOnrampFeesUM.FeeRow(
            type = "EXCHANGE_RATE",
            title = resourceReference(R.string.tangempay_bank_transfer_details_exchange_rate),
            value = "1 USD ≈ 18.54 MXN",
        )
        val US_REQUISITE_KEYS = listOf(
            "Account number",
            "Routing number",
            "Beneficiary name",
            "Beneficiary address",
            "Bank name",
            "Bank address",
        )
    }
}