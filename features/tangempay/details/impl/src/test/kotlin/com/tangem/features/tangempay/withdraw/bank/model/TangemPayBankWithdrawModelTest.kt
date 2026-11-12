package com.tangem.features.tangempay.withdraw.bank.model

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.domain.models.tangempay.TangemPayCountry
import com.tangem.domain.models.tangempay.TangemPayRegion
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.MoneyAccountFeatureToggles
import com.tangem.domain.tangempay.model.bankofframp.BankAccount
import com.tangem.domain.tangempay.model.bankofframp.BankAccountType
import com.tangem.domain.tangempay.model.bankofframp.BankOfframpError
import com.tangem.domain.tangempay.model.bankofframp.BankOfframpOrderRequest
import com.tangem.domain.tangempay.model.bankofframp.BankOfframpOrderResult
import com.tangem.domain.tangempay.model.bankofframp.BankOfframpRecipient
import com.tangem.domain.tangempay.model.bankofframp.BankRail
import com.tangem.domain.tangempay.model.bankofframp.CustomerName
import com.tangem.domain.tangempay.repository.BankOfframpRepository
import com.tangem.domain.tangempay.usecase.GetTangemPayCountriesUseCase
import com.tangem.domain.tangempay.usecase.GetTangemPayRegionsUseCase
import com.tangem.domain.tangempay.usecase.bankofframp.GetBankAccountsUseCase
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.domain.tangempay.model.bankofframp.BankOfframpQuote
import com.tangem.features.tangempay.TangemPayModelTestBase
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawEntry
import com.tangem.features.tangempay.withdraw.bank.BankWithdrawSourceAccount
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
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import org.joda.time.DateTime
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayBankWithdrawModelTest : TangemPayModelTestBase() {

    private val userWalletId = UserWalletId("011")
    private val getBankAccountsUseCase: GetBankAccountsUseCase = mockk()
    private val getTangemPayCountriesUseCase: GetTangemPayCountriesUseCase = mockk()
    private val getTangemPayRegionsUseCase: GetTangemPayRegionsUseCase = mockk()
    private val moneyAccountFeatureToggles: MoneyAccountFeatureToggles = mockk()
    private val speiAvailability = BankWithdrawSpeiAvailability(moneyAccountFeatureToggles)
    private val onWithdrawSent: () -> Unit = mockk(relaxed = true)
    private val bankOfframpRepository: BankOfframpRepository = mockk()
    private val dispatchers = TestingCoroutineDispatcherProvider()

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            getBankAccountsUseCase,
            getTangemPayCountriesUseCase,
            getTangemPayRegionsUseCase,
            analytics,
            onWithdrawSent,
            bankOfframpRepository,
        )
        coEvery { getBankAccountsUseCase(userWalletId, any()) } returns emptyList<BankAccount>().right()
        coEvery { getTangemPayCountriesUseCase() } returns emptyList<TangemPayCountry>().right()
        coEvery { getTangemPayRegionsUseCase(any()) } returns emptyList<TangemPayRegion>().right()
        every { moneyAccountFeatureToggles.isMxnEnabled } returns false
        coEvery { bankOfframpRepository.getCustomerName(userWalletId) } returns PROFILE_NAME.right()
    }

    @ParameterizedTest
    @MethodSource("provideNameNeedModels")
    fun `GIVEN entry and offered rails WHEN model created THEN only a new personal SPEI recipient loads the name`(
        model: NameNeedModel,
    ) {
        // Act
        val withdrawModel = createModel(model.entry, options = model.options)

        // Assert
        assertThat(withdrawModel.customerName.value).isEqualTo(model.expected)
        coVerify(exactly = model.expectedRequests) { bankOfframpRepository.getCustomerName(any()) }
        withdrawModel.onDestroy()
    }

    @ParameterizedTest
    @MethodSource("provideNameAnswerModels")
    fun `GIVEN name answer WHEN loaded THEN a full name is trimmed and anything less is unreachable`(
        model: NameAnswerModel,
    ) {
        // Arrange
        coEvery { bankOfframpRepository.getCustomerName(userWalletId) } returns model.answer

        // Act
        val withdrawModel = createModel(BankWithdrawEntry.NewPersonal, options = SPEI_OPTIONS)

        // Assert
        assertThat(withdrawModel.customerName.value).isEqualTo(model.expected)
        withdrawModel.onDestroy()
    }

    @Test
    fun `GIVEN unreachable name WHEN retried THEN it is requested again and loads`() {
        // Arrange
        coEvery { bankOfframpRepository.getCustomerName(userWalletId) } returnsMany listOf(
            BankOfframpError.Unknown(cause = null).left(),
            PROFILE_NAME.right(),
        )
        val withdrawModel = createModel(BankWithdrawEntry.NewPersonal, options = SPEI_OPTIONS)

        // Act
        withdrawModel.onRetryCustomerName()

        // Assert
        assertThat(withdrawModel.customerName.value).isEqualTo(LOADED_NAME)
        coVerify(exactly = 2) { bankOfframpRepository.getCustomerName(userWalletId) }
        withdrawModel.onDestroy()
    }

    @Test
    fun `GIVEN name request pending WHEN opened and when retried THEN the name is loading until it answers`() {
        // Arrange
        val firstAnswer = CompletableDeferred<Either<BankOfframpError, CustomerName>>()
        val secondAnswer = CompletableDeferred<Either<BankOfframpError, CustomerName>>()
        val answers = ArrayDeque(listOf(firstAnswer, secondAnswer))
        coEvery { bankOfframpRepository.getCustomerName(userWalletId) } coAnswers { answers.removeFirst().await() }
        val withdrawModel = createModel(BankWithdrawEntry.NewPersonal, options = SPEI_OPTIONS)
        val whileOpening = withdrawModel.customerName.value
        firstAnswer.complete(BankOfframpError.Unknown(cause = null).left())

        // Act
        withdrawModel.onRetryCustomerName()
        val whileRetrying = withdrawModel.customerName.value
        secondAnswer.complete(PROFILE_NAME.right())

        // Assert
        assertThat(whileOpening).isEqualTo(CustomerNameState.Loading)
        assertThat(whileRetrying).isEqualTo(CustomerNameState.Loading)
        assertThat(withdrawModel.customerName.value).isEqualTo(LOADED_NAME)
        withdrawModel.onDestroy()
    }

    @Test
    fun `GIVEN loaded name WHEN retried THEN nothing is requested again`() {
        // Arrange
        val withdrawModel = createModel(BankWithdrawEntry.NewPersonal, options = SPEI_OPTIONS)

        // Act
        withdrawModel.onRetryCustomerName()

        // Assert
        coVerify(exactly = 1) { bankOfframpRepository.getCustomerName(userWalletId) }
        withdrawModel.onDestroy()
    }

    @ParameterizedTest
    @MethodSource("provideLockedRecipientModels")
    fun `GIVEN rail and name WHEN recipient values locked THEN only a SPEI recipient gets the loaded name`(
        model: LockedRecipientModel,
    ) {
        // Arrange
        coEvery { bankOfframpRepository.getCustomerName(userWalletId) } returns model.answer
        val withdrawModel = createModel(BankWithdrawEntry.NewPersonal, options = SPEI_OPTIONS)
        withdrawModel.onAmountConfirmed(achAmountResult().copy(destination = offrampDestination(rail = model.rail)))

        // Act
        val actual = withdrawModel.lockedRecipientValues()

        // Assert
        assertThat(actual).isEqualTo(model.expected)
        withdrawModel.onDestroy()
    }

    @Test
    fun `GIVEN signed order WHEN reported THEN withdraw reported as sent`() {
        // Arrange
        val model = createModel(entry = BankWithdrawEntry.NewPersonal)
        model.onAmountConfirmed(
            BankWithdrawAmountResult(
                amount = BigDecimal("500"),
                source = bankWithdrawSource(),
                destination = offrampDestination(),
                networkFee = BigDecimal.ZERO,
            ),
        )
        model.onOrderCreated(BankOfframpOrderResult(orderId = "order-1", bankAccountId = "acc-7"))

        // Act
        model.onOrderSigned()

        // Assert
        verify(exactly = 1) { onWithdrawSent() }
        model.onDestroy()
    }

    @Test
    fun `GIVEN saved entry WHEN model created THEN recipient loaded into draft`() {
        // Arrange
        val saved = bankAccount(id = "acc-2")
        coEvery { getBankAccountsUseCase(userWalletId, any()) } returns listOf(bankAccount(id = "acc-1"), saved).right()

        // Act
        val model = createModel(BankWithdrawEntry.Saved("acc-2"))

        // Assert
        assertThat(model.draft.value).isEqualTo(BankWithdrawDraft(entry = BankWithdrawEntry.Saved("acc-2"), recipient = saved))
        model.onDestroy()
    }

    @Test
    fun `GIVEN options without SPEI WHEN saved entry is a CLABE recipient THEN neither selected nor suggested`() {
        // Arrange
        val clabe = bankAccount(id = "acc-2", rail = BankRail.SPEI)
        coEvery { getBankAccountsUseCase(userWalletId, any()) } returns listOf(bankAccount(id = "acc-1"), clabe).right()

        // Act
        val model = createModel(BankWithdrawEntry.Saved("acc-2"))

        // Assert
        assertThat(model.draft.value.recipient).isNull()
        assertThat(model.savedAccounts.value.map { it.id }).containsExactly("acc-1")
        model.onDestroy()
    }

    @Test
    fun `GIVEN saved entry WHEN accounts fail THEN recipient stays null`() {
        // Arrange
        coEvery { getBankAccountsUseCase(userWalletId, any()) } returns BankOfframpError.Unknown(null).left()

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
        coEvery { getBankAccountsUseCase(userWalletId, any()) } returns accounts.right()

        // Act
        val model = createModel(BankWithdrawEntry.NewPersonal)

        // Assert
        coVerify(exactly = 1) { getBankAccountsUseCase(userWalletId, any()) }
        assertThat(model.savedAccounts.value).containsExactlyElementsIn(accounts).inOrder()
        assertThat(model.draft.value).isEqualTo(BankWithdrawDraft(entry = BankWithdrawEntry.NewPersonal))
        model.onDestroy()
    }

    @Test
    fun `GIVEN accounts fail WHEN model created THEN saved accounts stay empty`() {
        // Arrange
        coEvery { getBankAccountsUseCase(userWalletId, any()) } returns BankOfframpError.Unknown(null).left()

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
                networkFee = BigDecimal.ZERO,
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
                tokenDecimals = 6,
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
    fun `GIVEN draft WHEN amount confirmed THEN draft keeps amount source destination and network fee`() {
        // Arrange
        val model = createModel(BankWithdrawEntry.NewBusiness)
        val source = bankWithdrawSource()
        val destination = offrampDestination()

        // Act
        model.onAmountConfirmed(
            BankWithdrawAmountResult(
                amount = BigDecimal("500"),
                source = source,
                destination = destination,
                networkFee = BigDecimal("0.03"),
            ),
        )

        // Assert
        assertThat(model.draft.value).isEqualTo(
            BankWithdrawDraft(
                entry = BankWithdrawEntry.NewBusiness,
                amount = BigDecimal("500"),
                source = source,
                destination = destination,
                networkFee = BigDecimal("0.03"),
            ),
        )
        model.onDestroy()
    }

    @Test
    fun `GIVEN amount confirmed WHEN quote changed THEN draft keeps the quote until the amount is confirmed again`() {
        // Arrange
        val model = createModel(BankWithdrawEntry.NewPersonal)
        model.onAmountConfirmed(achAmountResult())

        // Act
        model.onQuoteChanged(speiQuote())
        val quoted = model.draft.value.quote
        model.onAmountConfirmed(achAmountResult())

        // Assert
        assertThat(quoted).isEqualTo(speiQuote())
        assertThat(model.draft.value.quote).isNull()
        model.onDestroy()
    }

    @ParameterizedTest
    @MethodSource("provideSpeiRecipientsModels")
    fun `GIVEN source account and MXN toggle WHEN model created THEN CLABE recipients loaded only with SPEI`(
        model: SpeiRecipientsModel,
    ) {
        // Arrange
        every { moneyAccountFeatureToggles.isMxnEnabled } returns model.isMxnEnabled

        // Act
        val wizard = createModel(BankWithdrawEntry.NewPersonal, sourceAccount = model.sourceAccount)

        // Assert
        coVerify(exactly = 1) { getBankAccountsUseCase(userWalletId, model.expectedIncludeSpei) }
        wizard.onDestroy()
    }

    @Test
    fun `GIVEN amount confirmed WHEN network fee changed THEN draft keeps everything else and the new fee`() {
        // Arrange
        val model = createModel(BankWithdrawEntry.NewPersonal)
        model.onAmountConfirmed(achAmountResult())

        // Act
        model.onNetworkFeeChanged(BigDecimal("0.05"))

        // Assert
        assertThat(model.draft.value).isEqualTo(
            BankWithdrawDraft(
                entry = BankWithdrawEntry.NewPersonal,
                amount = BigDecimal("500"),
                source = bankWithdrawSource(),
                destination = offrampDestination(),
                networkFee = BigDecimal("0.05"),
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
    fun `GIVEN amount confirmed WHEN destination changed THEN draft keeps amount source and the new destination`() {
        // Arrange
        val model = createModel(BankWithdrawEntry.NewPersonal)
        model.onAmountConfirmed(achAmountResult())
        val repriced = offrampDestination(fee = BigDecimal("1.50"), minDays = 2, maxDays = 4)

        // Act
        model.onDestinationChanged(repriced)

        // Assert
        assertThat(model.draft.value).isEqualTo(
            BankWithdrawDraft(
                entry = BankWithdrawEntry.NewPersonal,
                amount = BigDecimal("500"),
                source = bankWithdrawSource(),
                destination = repriced,
            ),
        )
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
        coEvery { getBankAccountsUseCase(userWalletId, any()) } returns listOf(bankAccount(id = "acc-2")).right()

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
                    networkFee = BigDecimal.ZERO,
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

    private fun provideNameNeedModels() = listOf(
        NameNeedModel(
            entry = BankWithdrawEntry.NewPersonal,
            options = SPEI_OPTIONS,
            expected = LOADED_NAME,
            expectedRequests = 1,
        ),
        NameNeedModel(
            entry = BankWithdrawEntry.NewPersonal,
            options = bankWithdrawOptions(),
            expected = CustomerNameState.NotNeeded,
            expectedRequests = 0,
        ),
        NameNeedModel(
            entry = BankWithdrawEntry.NewBusiness,
            options = SPEI_OPTIONS,
            expected = CustomerNameState.NotNeeded,
            expectedRequests = 0,
        ),
        NameNeedModel(
            entry = BankWithdrawEntry.Saved(bankAccountId = "acc-1"),
            options = SPEI_OPTIONS,
            expected = CustomerNameState.NotNeeded,
            expectedRequests = 0,
        ),
        NameNeedModel(
            entry = BankWithdrawEntry.SeeAll,
            options = SPEI_OPTIONS,
            expected = CustomerNameState.NotNeeded,
            expectedRequests = 0,
        ),
    )

    data class NameNeedModel(
        val entry: BankWithdrawEntry,
        val options: BankWithdrawOptions,
        val expected: CustomerNameState,
        val expectedRequests: Int,
    )

    private fun provideNameAnswerModels() = listOf(
        NameAnswerModel(
            answer = CustomerName(firstName = " José ", lastName = "Hernández ").right(),
            expected = LOADED_NAME,
        ),
        NameAnswerModel(
            answer = CustomerName(firstName = "José", lastName = " ").right(),
            expected = CustomerNameState.Unreachable,
        ),
        NameAnswerModel(
            answer = CustomerName(firstName = null, lastName = "Hernández").right(),
            expected = CustomerNameState.Unreachable,
        ),
        NameAnswerModel(
            answer = BankOfframpError.Unknown(cause = null).left(),
            expected = CustomerNameState.Unreachable,
        ),
    )

    data class NameAnswerModel(
        val answer: Either<BankOfframpError, CustomerName>,
        val expected: CustomerNameState,
    )

    private fun provideLockedRecipientModels() = listOf(
        LockedRecipientModel(
            rail = BankRail.SPEI,
            answer = PROFILE_NAME.right(),
            expected = mapOf(
                BankWithdrawFormField.FirstName to "José",
                BankWithdrawFormField.LastName to "Hernández",
            ),
        ),
        LockedRecipientModel(rail = BankRail.ACH, answer = PROFILE_NAME.right(), expected = emptyMap()),
        LockedRecipientModel(
            rail = BankRail.SPEI,
            answer = BankOfframpError.Unknown(cause = null).left(),
            expected = emptyMap(),
        ),
    )

    data class LockedRecipientModel(
        val rail: BankRail,
        val answer: Either<BankOfframpError, CustomerName>,
        val expected: Map<BankWithdrawFormField, String>,
    )

    private fun achAmountResult() = BankWithdrawAmountResult(
        amount = BigDecimal("500"),
        source = bankWithdrawSource(),
        destination = offrampDestination(),
        networkFee = BigDecimal.ZERO,
    )

    private fun wireDestination() =
        offrampDestination(rail = BankRail.WIRE, fee = BigDecimal("11.00"), minDays = 0, maxDays = 0)

    private fun provideSpeiRecipientsModels() = listOf(
        SpeiRecipientsModel(
            sourceAccount = BankWithdrawSourceAccount.Money(userWalletId, productInstanceId = "instance-1"),
            isMxnEnabled = true,
            expectedIncludeSpei = true,
        ),
        SpeiRecipientsModel(
            sourceAccount = BankWithdrawSourceAccount.Money(userWalletId, productInstanceId = "instance-1"),
            isMxnEnabled = false,
            expectedIncludeSpei = false,
        ),
        SpeiRecipientsModel(
            sourceAccount = BankWithdrawSourceAccount.Payment(userWalletId),
            isMxnEnabled = true,
            expectedIncludeSpei = false,
        ),
    )

    data class SpeiRecipientsModel(
        val sourceAccount: BankWithdrawSourceAccount,
        val isMxnEnabled: Boolean,
        val expectedIncludeSpei: Boolean,
    )

    private fun speiQuote() = BankOfframpQuote(
        quoteId = "quote-1",
        bankAccountId = "acc-1",
        feeAmount = BigDecimal("5.50"),
        sourceAmount = BigDecimal("500.00"),
        destinationAmount = BigDecimal("8530.13"),
        destinationCurrency = "MXN",
        exchangeRate = BigDecimal("17.25"),
        fee = offrampDestination().fee,
        eta = offrampDestination().eta,
        expiresAt = DateTime.parse("2099-01-01T00:00:00Z"),
    )

    private fun createModel(
        entry: BankWithdrawEntry,
        sourceAccount: BankWithdrawSourceAccount = BankWithdrawSourceAccount.Payment(userWalletId),
        options: BankWithdrawOptions = bankWithdrawOptions(),
    ): TangemPayBankWithdrawModel = TangemPayBankWithdrawModel(
        paramsContainer = MutableParamsContainer(
            TangemPayBankWithdrawComponent.Params(
                sourceAccount = sourceAccount,
                entry = entry,
                options = MutableStateFlow(options),
                onCloseFlow = {},
                onWithdrawSent = onWithdrawSent,
            ),
        ),
        dispatchers = dispatchers,
        getBankAccountsUseCase = getBankAccountsUseCase,
        getTangemPayCountriesUseCase = getTangemPayCountriesUseCase,
        getTangemPayRegionsUseCase = getTangemPayRegionsUseCase,
        speiAvailability = speiAvailability,
        bankOfframpRepository = bankOfframpRepository,
        analytics = analytics,
    )

    private companion object {
        val PROFILE_NAME = CustomerName(firstName = "José", lastName = "Hernández")
        val LOADED_NAME = CustomerNameState.Loaded(firstName = "José", lastName = "Hernández")
        val SPEI_OPTIONS = bankWithdrawOptions(
            destinations = listOf(offrampDestination(), offrampDestination(rail = BankRail.SPEI)),
        )
    }
}