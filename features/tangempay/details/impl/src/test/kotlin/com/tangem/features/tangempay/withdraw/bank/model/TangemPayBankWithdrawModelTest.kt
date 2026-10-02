package com.tangem.features.tangempay.withdraw.bank.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.domain.models.pay.TangemPayCountry
import com.tangem.domain.models.pay.TangemPayRegion
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.bankofframp.BankAccount
import com.tangem.domain.pay.model.bankofframp.BankAccountType
import com.tangem.domain.pay.model.bankofframp.BankOfframpError
import com.tangem.domain.pay.model.bankofframp.BankOfframpOrderRequest
import com.tangem.domain.pay.model.bankofframp.BankOfframpOrderResult
import com.tangem.domain.pay.model.bankofframp.BankOfframpRecipient
import com.tangem.domain.pay.model.bankofframp.BankRail
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.usecase.GetTangemPayCountriesUseCase
import com.tangem.domain.pay.usecase.GetTangemPayRegionsUseCase
import com.tangem.domain.pay.usecase.bankofframp.GetBankAccountsUseCase
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawEntry
import com.tangem.features.tangempay.withdraw.bank.POLYGON_CHAIN_ID
import com.tangem.features.tangempay.withdraw.bank.USDC_POLYGON
import com.tangem.features.tangempay.withdraw.bank.bankWithdrawOptions
import com.tangem.features.tangempay.withdraw.bank.TangemPayBankWithdrawComponent
import com.tangem.features.tangempay.withdraw.bank.bankWithdrawSource
import com.tangem.features.tangempay.withdraw.bank.form.model.BankWithdrawFormField
import com.tangem.features.tangempay.withdraw.bank.form.model.BankWithdrawFormFields
import com.tangem.features.tangempay.withdraw.bank.form.model.BankWithdrawFormResult
import com.tangem.features.tangempay.withdraw.bank.offrampDestination
import com.tangem.features.tangempay.withdraw.bank.preview.model.toOrderRequest
import com.tangem.features.tangempay.withdraw.hub.bankAccount
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayBankWithdrawModelTest {

    private val userWalletId = UserWalletId("011")
    private val getBankAccountsUseCase: GetBankAccountsUseCase = mockk()
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher = mockk(relaxed = true)
    private val getTangemPayCountriesUseCase: GetTangemPayCountriesUseCase = mockk()
    private val getTangemPayRegionsUseCase: GetTangemPayRegionsUseCase = mockk()
    private val analytics: AnalyticsEventHandler = mockk(relaxed = true)
    private val dispatchers = TestingCoroutineDispatcherProvider()

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            getBankAccountsUseCase,
            paymentAccountStatusFetcher,
            getTangemPayCountriesUseCase,
            getTangemPayRegionsUseCase,
            analytics,
        )
        coEvery { paymentAccountStatusFetcher(userWalletId) } returns Unit.right()
        coEvery { getBankAccountsUseCase(userWalletId) } returns emptyList<BankAccount>().right()
        coEvery { getTangemPayCountriesUseCase() } returns emptyList<TangemPayCountry>().right()
        coEvery { getTangemPayRegionsUseCase(any()) } returns emptyList<TangemPayRegion>().right()
    }

    @Test
    fun `GIVEN signed order WHEN reported THEN payment account refreshed`() {
        // Arrange
        val model = createModel(entry = BankWithdrawEntry.NewPersonal)
        model.onAmountConfirmed(
            BankWithdrawAmountResult(
                amount = BigDecimal("500"),
                source = bankWithdrawSource(),
                destination = offrampDestination(),
            ),
        )
        model.onOrderCreated(BankOfframpOrderResult(orderId = "order-1", bankAccountId = "acc-7"))

        // Act
        model.onOrderSigned()

        // Assert
        coVerify(exactly = 1) { paymentAccountStatusFetcher(userWalletId) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN saved entry WHEN model created THEN recipient loaded into draft`() {
        // Arrange
        val saved = bankAccount(id = "acc-2")
        coEvery { getBankAccountsUseCase(userWalletId) } returns listOf(bankAccount(id = "acc-1"), saved).right()

        // Act
        val model = createModel(BankWithdrawEntry.Saved("acc-2"))

        // Assert
        assertThat(model.draft.value).isEqualTo(BankWithdrawDraft(entry = BankWithdrawEntry.Saved("acc-2"), recipient = saved))
        model.onDestroy()
    }

    @Test
    fun `GIVEN saved entry WHEN accounts fail THEN recipient stays null`() {
        // Arrange
        coEvery { getBankAccountsUseCase(userWalletId) } returns BankOfframpError.Unknown(null).left()

        // Act
        val model = createModel(BankWithdrawEntry.Saved("acc-2"))

        // Assert
        assertThat(model.draft.value.recipient).isNull()
        model.onDestroy()
    }

    @Test
    fun `GIVEN new personal entry WHEN model created THEN saved accounts loaded once`() {
        // Arrange
        val accounts = listOf(bankAccount(id = "acc-1"), bankAccount(id = "acc-2"))
        coEvery { getBankAccountsUseCase(userWalletId) } returns accounts.right()

        // Act
        val model = createModel(BankWithdrawEntry.NewPersonal)

        // Assert
        coVerify(exactly = 1) { getBankAccountsUseCase(userWalletId) }
        assertThat(model.savedAccounts.value).containsExactlyElementsIn(accounts).inOrder()
        assertThat(model.draft.value).isEqualTo(BankWithdrawDraft(entry = BankWithdrawEntry.NewPersonal))
        model.onDestroy()
    }

    @Test
    fun `GIVEN accounts fail WHEN model created THEN saved accounts stay empty`() {
        // Arrange
        coEvery { getBankAccountsUseCase(userWalletId) } returns BankOfframpError.Unknown(null).left()

        // Act
        val model = createModel(BankWithdrawEntry.NewPersonal)

        // Assert
        assertThat(model.savedAccounts.value).isEmpty()
        assertThat(model.draft.value).isEqualTo(BankWithdrawDraft(entry = BankWithdrawEntry.NewPersonal))
        model.onDestroy()
    }

    @Test
    fun `GIVEN new business entry WHEN model created THEN draft is third party`() {
        // Act
        val model = createModel(BankWithdrawEntry.NewBusiness)

        // Assert
        assertThat(model.draft.value.newRecipient)
            .isEqualTo(NewRecipientDraft(isThirdParty = true, accountType = null))
        model.onDestroy()
    }

    @Test
    fun `GIVEN new personal entry WHEN model created THEN draft has no transfer type yet`() {
        // Act
        val model = createModel(BankWithdrawEntry.NewPersonal)

        // Assert
        assertThat(model.draft.value.newRecipient).isEqualTo(NewRecipientDraft())
        model.onDestroy()
    }

    @Test
    fun `GIVEN see all entry WHEN recipient selected THEN draft keeps recipient`() {
        // Arrange
        val model = createModel(BankWithdrawEntry.SeeAll)
        val recipient = bankAccount(id = "acc-3")

        // Act
        model.onRecipientSelected(recipient)

        // Assert
        assertThat(model.draft.value)
            .isEqualTo(BankWithdrawDraft(entry = BankWithdrawEntry.SeeAll, recipient = recipient))
        model.onDestroy()
    }

    @Test
    fun `GIVEN typed new recipient WHEN saved recipient selected THEN draft recipient set and order uses account id`() {
        // Arrange
        val model = createModel(BankWithdrawEntry.NewPersonal)
        model.onAmountConfirmed(
            BankWithdrawAmountResult(
                amount = BigDecimal("500"),
                source = bankWithdrawSource(),
                destination = offrampDestination(),
            ),
        )
        model.onDetailsFilled(formResult(BankWithdrawFormField.AccountNumber to "4830019271"))
        val suggested = bankAccount(id = "acc-5", accountNumber = "4830019271")

        // Act
        model.onSavedRecipientSelected(suggested)

        // Assert
        assertThat(model.draft.value.recipient).isEqualTo(suggested)
        assertThat(model.draft.value.toOrderRequest()).isEqualTo(
            BankOfframpOrderRequest(
                amountInCents = 50_000L,
                chainId = POLYGON_CHAIN_ID,
                tokenContractAddress = USDC_POLYGON,
                rail = BankRail.ACH,
                recipient = BankOfframpRecipient.Saved(bankAccountId = "acc-5"),
            ),
        )
        model.onDestroy()
    }

    @Test
    fun `GIVEN typed new recipient WHEN saved recipient selected THEN new recipient draft reset`() {
        // Arrange
        val model = createModel(BankWithdrawEntry.NewPersonal)
        model.onDetailsFilled(formResult(BankWithdrawFormField.AccountNumber to "4830019271"))

        // Act
        model.onSavedRecipientSelected(bankAccount(id = "acc-5", accountNumber = "4830019271"))

        // Assert
        assertThat(model.draft.value.newRecipient).isEqualTo(NewRecipientDraft.forEntry(BankWithdrawEntry.NewPersonal))
    }

    @Test
    fun `GIVEN ach confirmed WHEN saved recipient on wire suggested THEN draft destination switches to wire`() {
        // Arrange
        val model = createModel(BankWithdrawEntry.NewPersonal)
        model.onAmountConfirmed(achAmountResult())

        // Act
        model.onSavedRecipientSelected(bankAccount(id = "acc-5", rail = BankRail.WIRE))

        // Assert
        assertThat(model.draft.value.destination).isEqualTo(wireDestination())
        model.onDestroy()
    }

    @Test
    fun `GIVEN ach confirmed WHEN saved recipient on wire picked from all THEN draft destination switches to wire`() {
        // Arrange
        val model = createModel(BankWithdrawEntry.SeeAll)
        model.onAmountConfirmed(achAmountResult())

        // Act
        model.onRecipientSelected(bankAccount(id = "acc-5", rail = BankRail.WIRE))

        // Assert
        assertThat(model.draft.value.destination).isEqualTo(wireDestination())
        model.onDestroy()
    }

    @Test
    fun `GIVEN draft WHEN amount confirmed THEN draft keeps amount source and destination`() {
        // Arrange
        val model = createModel(BankWithdrawEntry.NewBusiness)
        val source = bankWithdrawSource()
        val destination = offrampDestination()

        // Act
        model.onAmountConfirmed(
            BankWithdrawAmountResult(amount = BigDecimal("500"), source = source, destination = destination),
        )

        // Assert
        assertThat(model.draft.value).isEqualTo(
            BankWithdrawDraft(
                entry = BankWithdrawEntry.NewBusiness,
                amount = BigDecimal("500"),
                source = source,
                destination = destination,
            ),
        )
        model.onDestroy()
    }

    @Test
    fun `GIVEN new personal entry WHEN transfer type selected THEN draft keeps third party flag`() {
        // Arrange
        val model = createModel(BankWithdrawEntry.NewPersonal)

        // Act
        model.onTransferTypeSelected(isThirdParty = true)

        // Assert
        assertThat(model.draft.value.newRecipient).isEqualTo(NewRecipientDraft(isThirdParty = true))
        model.onDestroy()
    }

    @Test
    fun `GIVEN new personal entry WHEN account type selected THEN draft keeps account type`() {
        // Arrange
        val model = createModel(BankWithdrawEntry.NewPersonal)

        // Act
        model.onAccountTypeSelected(BankAccountType.SAVINGS)

        // Assert
        assertThat(model.draft.value.newRecipient).isEqualTo(NewRecipientDraft(accountType = BankAccountType.SAVINGS))
        model.onDestroy()
    }

    @Test
    fun `GIVEN transfer type selected WHEN account type selected THEN draft keeps both answers`() {
        // Arrange
        val model = createModel(BankWithdrawEntry.NewPersonal)
        model.onTransferTypeSelected(isThirdParty = true)

        // Act
        model.onAccountTypeSelected(BankAccountType.SAVINGS)

        // Assert
        assertThat(model.draft.value.newRecipient)
            .isEqualTo(NewRecipientDraft(isThirdParty = true, accountType = BankAccountType.SAVINGS))
        model.onDestroy()
    }

    @Test
    fun `GIVEN filled recipient details WHEN bank details filled THEN draft merges both forms`() {
        // Arrange
        val model = createModel(BankWithdrawEntry.NewPersonal)
        model.onDetailsFilled(formResult(BankWithdrawFormField.AccountNumber to "4830019271"))

        // Act
        model.onDetailsFilled(formResult(BankWithdrawFormField.BankName to "Test Bank"))

        // Assert
        assertThat(model.draft.value.newRecipient.fields).containsExactly(
            BankWithdrawFormField.AccountNumber, "4830019271",
            BankWithdrawFormField.BankName, "Test Bank",
        )
        model.onDestroy()
    }

    @Test
    fun `GIVEN two form steps WHEN details filled THEN draft merges codes and names`() {
        // Arrange
        val model = createModel(BankWithdrawEntry.NewPersonal)
        model.onDetailsFilled(
            BankWithdrawFormResult(
                values = mapOf(
                    BankWithdrawFormField.Country to "US",
                    BankWithdrawFormField.Region to "CA",
                ),
                displayNames = mapOf(
                    BankWithdrawFormField.Country to "United States",
                    BankWithdrawFormField.Region to "California",
                ),
            ),
        )

        // Act
        model.onDetailsFilled(
            BankWithdrawFormResult(
                values = mapOf(BankWithdrawFormField.BankRegion to "NY"),
                displayNames = mapOf(BankWithdrawFormField.BankRegion to "New York"),
            ),
        )

        // Assert
        assertThat(model.draft.value.newRecipient).isEqualTo(
            NewRecipientDraft(
                fields = mapOf(
                    BankWithdrawFormField.Country to "US",
                    BankWithdrawFormField.Region to "CA",
                    BankWithdrawFormField.BankRegion to "NY",
                ),
                displayNames = mapOf(
                    BankWithdrawFormField.Country to "United States",
                    BankWithdrawFormField.Region to "California",
                    BankWithdrawFormField.BankRegion to "New York",
                ),
            ),
        )
        model.onDestroy()
    }

    @Test
    fun `GIVEN named field WHEN the same step reports it typed THEN the stale name is dropped`() {
        // Arrange
        val model = createModel(BankWithdrawEntry.NewPersonal)
        model.onDetailsFilled(
            BankWithdrawFormResult(
                values = mapOf(BankWithdrawFormField.Region to "CA"),
                displayNames = mapOf(BankWithdrawFormField.Region to "California"),
            ),
        )

        // Act
        model.onDetailsFilled(formResult(BankWithdrawFormField.Region to "Bavaria"))

        // Assert
        assertThat(model.draft.value.newRecipient).isEqualTo(
            NewRecipientDraft(
                fields = mapOf(BankWithdrawFormField.Region to "Bavaria"),
                displayNames = mapOf(BankWithdrawFormField.Region to ""),
            ),
        )
        model.onDestroy()
    }

    @Test
    fun `GIVEN draft WHEN order created THEN draft keeps order`() {
        // Arrange
        val model = createModel(BankWithdrawEntry.NewPersonal)
        val order = BankOfframpOrderResult(orderId = "order-1", bankAccountId = "acc-9")

        // Act
        model.onOrderCreated(order)

        // Assert
        assertThat(model.draft.value.order).isEqualTo(order)
        model.onDestroy()
    }

    @Test
    fun `GIVEN new personal entry WHEN model created THEN countries and us regions prefetched`() {
        // Act
        val model = createModel(BankWithdrawEntry.NewPersonal)

        // Assert
        coVerify(exactly = 1) { getTangemPayCountriesUseCase() }
        coVerify(exactly = 1) { getTangemPayRegionsUseCase(BankWithdrawFormFields.US_COUNTRY_CODE) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN new business entry WHEN model created THEN countries and us regions prefetched`() {
        // Act
        val model = createModel(BankWithdrawEntry.NewBusiness)

        // Assert
        coVerify(exactly = 1) { getTangemPayCountriesUseCase() }
        coVerify(exactly = 1) { getTangemPayRegionsUseCase(BankWithdrawFormFields.US_COUNTRY_CODE) }
        model.onDestroy()
    }

    @Test
    fun `GIVEN saved entry WHEN model created THEN nothing prefetched`() {
        // Arrange
        coEvery { getBankAccountsUseCase(userWalletId) } returns listOf(bankAccount(id = "acc-2")).right()

        // Act
        val model = createModel(BankWithdrawEntry.Saved("acc-2"))

        // Assert
        coVerify(exactly = 0) { getTangemPayCountriesUseCase() }
        coVerify(exactly = 0) { getTangemPayRegionsUseCase(any()) }
        model.onDestroy()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN chosen rail WHEN order signed THEN SendSuccessScreenShowed is sent with that rail`(
        testModel: SuccessModel,
    ) {
        // Arrange
        val model = createModel(entry = BankWithdrawEntry.NewPersonal)
        testModel.rail?.let { rail ->
            model.onAmountConfirmed(
                BankWithdrawAmountResult(
                    amount = BigDecimal("500"),
                    source = bankWithdrawSource(),
                    destination = offrampDestination(rail = rail),
                ),
            )
        }

        // Act
        model.onOrderSigned()

        // Assert
        verify(exactly = 1) {
            analytics.send(TangemPayAnalyticsEvents.Offramp.SendSuccessScreenShowed(rail = testModel.rail))
        }
        model.onDestroy()
    }

    private fun provideTestModels() = listOf(
        SuccessModel(rail = BankRail.ACH),
        SuccessModel(rail = BankRail.WIRE),
        SuccessModel(rail = null),
    )

    internal data class SuccessModel(val rail: BankRail?)

    /** A step that reports typed values: every field carries a code and a blank name, as the form model does. */
    private fun formResult(vararg values: Pair<BankWithdrawFormField, String>): BankWithdrawFormResult =
        BankWithdrawFormResult(
            values = values.toMap(),
            displayNames = values.associate { (field, _) -> field to "" },
        )

    private fun achAmountResult() = BankWithdrawAmountResult(
        amount = BigDecimal("500"),
        source = bankWithdrawSource(),
        destination = offrampDestination(),
    )

    private fun wireDestination() =
        offrampDestination(rail = BankRail.WIRE, fee = BigDecimal("11.00"), minDays = 0, maxDays = 0)

    private fun createModel(entry: BankWithdrawEntry): TangemPayBankWithdrawModel = TangemPayBankWithdrawModel(
        paramsContainer = MutableParamsContainer(
            TangemPayBankWithdrawComponent.Params(
                userWalletId = userWalletId,
                entry = entry,
                options = MutableStateFlow(bankWithdrawOptions()),
            ),
        ),
        dispatchers = dispatchers,
        getBankAccountsUseCase = getBankAccountsUseCase,
        paymentAccountStatusFetcher = paymentAccountStatusFetcher,
        getTangemPayCountriesUseCase = getTangemPayCountriesUseCase,
        getTangemPayRegionsUseCase = getTangemPayRegionsUseCase,
        analytics = analytics,
    )
}