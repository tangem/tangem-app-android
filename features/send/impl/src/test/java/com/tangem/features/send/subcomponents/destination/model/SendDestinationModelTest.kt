package com.tangem.features.send.subcomponents.destination.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.account.status.supplier.MultiAccountStatusListSupplier
import com.tangem.domain.account.status.usecase.GetBackupProblematicWalletForAddressUseCase
import com.tangem.domain.account.status.usecase.IsAccountsModeEnabledUseCase
import com.tangem.domain.addressbook.model.*
import com.tangem.domain.addressbook.usecase.GetContactsUseCase
import com.tangem.domain.feedback.SendBackupProblemEmailUseCase
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.CryptoCurrencyAddress
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.qrscanning.usecases.ListenToQrScanningUseCase
import com.tangem.domain.qrscanning.usecases.ParseQrCodeUseCase
import com.tangem.domain.tokens.GetNetworkAddressesUseCase
import com.tangem.domain.transaction.error.AddressValidation
import com.tangem.domain.transaction.error.AddressValidationResult
import com.tangem.domain.transaction.usecase.IsMemoRequiredUseCase
import com.tangem.domain.transaction.usecase.IsSelfSendAvailableUseCase
import com.tangem.domain.transaction.usecase.ValidateWalletAddressUseCase
import com.tangem.domain.transaction.usecase.ValidateWalletMemoUseCase
import com.tangem.domain.txhistory.usecase.GetFixedTxHistoryItemsUseCase
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.features.addressbook.ContactSelectionListener
import com.tangem.features.addressbook.MatchedContact
import com.tangem.features.addressbook.SelectedContact
import com.tangem.features.send.api.analytics.CommonSendAnalyticEvents
import com.tangem.features.send.api.entity.PredefinedValues
import com.tangem.features.send.api.subcomponents.destination.DestinationRoute
import com.tangem.features.send.api.subcomponents.destination.SendDestinationComponent
import com.tangem.features.send.api.subcomponents.destination.SendDestinationComponentParams
import com.tangem.features.send.api.subcomponents.destination.entity.DestinationUM
import com.tangem.features.send.common.CommonSendRoute
import com.tangem.features.send.subcomponents.destination.SendDestinationAlertFactory
import com.tangem.features.send.subcomponents.destination.analytics.EnterAddressSource
import com.tangem.features.send.subcomponents.destination.analytics.SendDestinationAnalyticEvents
import com.tangem.features.send.testDispatcherProvider
import com.tangem.test.core.ProvideTestModels
import io.mockk.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.tangem.features.send.api.subcomponents.destination.entity.DestinationTextFieldUM
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@OptIn(ExperimentalCoroutinesApi::class)
internal class SendDestinationModelTest {

    private val testUserWalletId = UserWalletId("1234567890ABCDEF")
    private val networkRawId = "eth"
    private val cryptoCurrency: CryptoCurrency = mockk(relaxed = true)
    private val contactIcon: AccountIconUM.CryptoPortfolio = mockk(relaxed = true)

    private val router: Router = mockk(relaxed = true)
    private val validateWalletAddressUseCase: ValidateWalletAddressUseCase = mockk(relaxed = true)
    private val validateWalletMemoUseCase: ValidateWalletMemoUseCase = mockk(relaxed = true)
    private val isMemoRequiredUseCase: IsMemoRequiredUseCase = mockk(relaxed = true)
    private val getWalletsUseCase: GetWalletsUseCase = mockk(relaxed = true)
    private val getNetworkAddressesUseCase: GetNetworkAddressesUseCase = mockk(relaxed = true)
    private val getFixedTxHistoryItemsUseCase: GetFixedTxHistoryItemsUseCase = mockk(relaxed = true)
    private val isSelfSendAvailableUseCase: IsSelfSendAvailableUseCase = mockk(relaxed = true)
    private val listenToQrScanningUseCase: ListenToQrScanningUseCase = mockk(relaxed = true)
    private val parseQrCodeUseCase: ParseQrCodeUseCase = mockk(relaxed = true)
    private val isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase = mockk(relaxed = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val multiAccountStatusListSupplier: MultiAccountStatusListSupplier = mockk(relaxed = true)
    private val getBackupProblematicWalletForAddressUseCase: GetBackupProblematicWalletForAddressUseCase =
        mockk(relaxed = true)
    private val sendDestinationAlertFactory: SendDestinationAlertFactory = mockk(relaxed = true)
    private val sendBackupProblemEmailUseCase: SendBackupProblemEmailUseCase = mockk(relaxed = true)
    private val getContactsUseCase: GetContactsUseCase = mockk(relaxed = true)
    private val contactSelectionListener: ContactSelectionListener = mockk(relaxed = true)
    private val callback: SendDestinationComponent.ModelCallback = mockk(relaxed = true)

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        // PER_CLASS parameterized nested classes reuse one instance — reset verified mocks between rows.
        clearMocks(callback, validateWalletAddressUseCase, answers = false, recordedCalls = true, childMocks = false)
        coEvery { getNetworkAddressesUseCase.invokeSync(any(), any<Network.RawID>()) } returns emptyList()
        every { getWalletsUseCase() } returns flowOf(emptyList())
        every { multiAccountStatusListSupplier() } returns flowOf(emptyList())
        every { getFixedTxHistoryItemsUseCase(any(), any(), any()) } returns flowOf(emptyList<TxInfo>()).right()
        every { isAccountsModeEnabledUseCase() } returns flowOf(false)
        coEvery { isSelfSendAvailableUseCase.invokeSync(any(), any()) } returns false
        every { listenToQrScanningUseCase(any()) } returns emptyFlow<String>().right()
        coEvery { validateWalletMemoUseCase(any(), any(), any()) } returns Unit.right()
        coEvery { isMemoRequiredUseCase(any(), any()) } returns false
        every { getContactsUseCase(any(), any()) } returns flowOf(emptyList())
        every { contactSelectionListener.resultFlow } returns MutableSharedFlow()
        coEvery { getBackupProblematicWalletForAddressUseCase(any()) } returns null
        every { cryptoCurrency.network.rawId } returns networkRawId
    }

    @Nested
    inner class Validate {

        @Test
        fun `GIVEN valid non-problematic address WHEN address entered THEN send valid analytics without backup alert`() =
            runTest {
                // Arrange
                coEvery {
                    validateWalletAddressUseCase(
                        any(),
                        any(),
                        any(),
                        any<List<CryptoCurrencyAddress>>(),
                        any()
                    )
                } returns
                    AddressValidation.Success.Valid.right()
                val sut = buildModel()
                advanceUntilIdle()

                // Act
                sut.onRecipientAddressValueChange("validAddr", EnterAddressSource.InputField)
                advanceUntilIdle()

                // Assert
                verify(exactly = 1) {
                    analyticsEventHandler.send(
                        match<SendDestinationAnalyticEvents.AddressEntered> { it.isValid },
                    )
                }
                verify(exactly = 0) { sendDestinationAlertFactory.showRecipientBackupErrorAlert(any()) }
                // InputField is not an auto-next source → no auto-advance even for a valid address
                verify(exactly = 0) { callback.onNextClick(CommonSendRoute.Destination(false)) }
            }

        @Test
        fun `GIVEN valid backup-problematic address WHEN address entered THEN show recipient backup error alert`() =
            runTest {
                // Arrange
                coEvery {
                    validateWalletAddressUseCase(
                        any(),
                        any(),
                        any(),
                        any<List<CryptoCurrencyAddress>>(),
                        any()
                    )
                } returns
                    AddressValidation.Success.Valid.right()
                coEvery { getBackupProblematicWalletForAddressUseCase(any()) } returns testUserWalletId
                val sut = buildModel()
                advanceUntilIdle()

                // Act
                sut.onRecipientAddressValueChange("problematicAddr", EnterAddressSource.InputField)
                advanceUntilIdle()

                // Assert
                verify(exactly = 1) { sendDestinationAlertFactory.showRecipientBackupErrorAlert(any()) }
                // backup override flips the (format-valid) result to error → analytics reports it as invalid
                verify(exactly = 1) {
                    analyticsEventHandler.send(match<SendDestinationAnalyticEvents.AddressEntered> { !it.isValid })
                }
            }

        @Test
        fun `GIVEN invalid address WHEN address entered THEN send invalid analytics`() = runTest {
            // Arrange
            coEvery {
                validateWalletAddressUseCase(
                    any(),
                    any(),
                    any(),
                    any<List<CryptoCurrencyAddress>>(),
                    any()
                )
            } returns
                AddressValidation.Error.InvalidAddress.left()
            val sut = buildModel()
            advanceUntilIdle()

            // Act
            sut.onRecipientAddressValueChange("badAddr", EnterAddressSource.InputField)
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) {
                analyticsEventHandler.send(
                    match<SendDestinationAnalyticEvents.AddressEntered> { !it.isValid },
                )
            }
        }

        @Test
        fun `GIVEN memo change with null type WHEN handled THEN no address-entered analytics and no auto-next`() =
            runTest {
                // Arrange
                coEvery {
                    validateWalletAddressUseCase(
                        any(),
                        any(),
                        any(),
                        any<List<CryptoCurrencyAddress>>(),
                        any()
                    )
                } returns
                    AddressValidation.Success.Valid.right()
                val sut = buildModel()
                advanceUntilIdle()

                // Act — onRecipientMemoValueChange calls validate(type = null)
                sut.onRecipientMemoValueChange("memo", isValuePasted = false)
                advanceUntilIdle()

                // Assert
                verify(exactly = 0) {
                    analyticsEventHandler.send(any<SendDestinationAnalyticEvents.AddressEntered>())
                }
                verify(exactly = 0) { callback.onNextClick(CommonSendRoute.Destination(false)) }
            }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class AutoNext {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN auto-next source WHEN address entered THEN advance only when address valid`(model: AutoNextModel) =
            runTest {
                // Arrange
                coEvery {
                    validateWalletAddressUseCase(any(), any(), any(), any<List<CryptoCurrencyAddress>>(), any())
                } returns model.addressValidation

                val sut = buildModel()
                advanceUntilIdle()

                // Act — RecentAddress is an auto-next source
                sut.onRecipientAddressValueChange("addr", EnterAddressSource.RecentAddress)
                advanceUntilIdle()

                // Assert
                verify(exactly = model.expectedNextClicks) { callback.onNextClick(CommonSendRoute.Destination(false)) }
            }

        private fun provideTestModels() = listOf(
            AutoNextModel(addressValidation = AddressValidation.Success.Valid.right(), expectedNextClicks = 1),
            AutoNextModel(addressValidation = AddressValidation.Error.InvalidAddress.left(), expectedNextClicks = 0),
        )
    }

    @Nested
    inner class QrScan {

        @Test
        fun `GIVEN unparseable QR WHEN scanned THEN do NOT validate`() = runTest {
            // Arrange
            val qrFlow = MutableStateFlow("rawQr")
            every { listenToQrScanningUseCase(any()) } returns qrFlow.right()
            every { parseQrCodeUseCase("rawQr", cryptoCurrency) } returns
                IllegalStateException("bad qr").left()
            buildModel()

            // Act
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 0) {
                validateWalletAddressUseCase(
                    any(),
                    any(),
                    any(),
                    any<List<CryptoCurrencyAddress>>(),
                    any()
                )
            }
        }
    }

    @Nested
    inner class Contacts {

        @Test
        fun `GIVEN a selected contact WHEN applySelectedContact THEN address filled validated and contact set`() =
            runTest {
                // Arrange
                coEvery {
                    validateWalletAddressUseCase(any(), any(), any(), any<List<CryptoCurrencyAddress>>(), any())
                } returns AddressValidation.Success.Valid.right()
                val sut = buildModel()
                advanceUntilIdle()

                // Act
                sut.applySelectedContact(selectedContact(name = "Bob", address = "0xBob"))
                advanceUntilIdle()

                // Assert — the contact's address is filled in and validated, and the contact name is shown
                coVerify {
                    validateWalletAddressUseCase(any(), any(), eq("0xBob"), any<List<CryptoCurrencyAddress>>(), any())
                }
                assertThat(content(sut).addressTextField.value).isEqualTo("0xBob")
                assertThat(content(sut).addressTextField.contactName).isEqualTo("Bob")
            }

        @Test
        fun `GIVEN a contact is pre-set in state AND route is edit mode WHEN model initializes THEN the contact is reset`() =
            runTest {
                // Arrange — build initial state with a contact name already set and isInitialized = true
                // so the InitialStateTransformer does NOT overwrite it, leaving resetContactOnEdit() to clear it.
                val stateWithContact = DestinationUM.Content(
                    isPrimaryButtonEnabled = false,
                    isInitialized = true,
                    addressTextField = DestinationTextFieldUM.RecipientAddress(
                        value = "0xDave",
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next, keyboardType = KeyboardType.Text),
                        placeholder = com.tangem.core.ui.extensions.stringReference(""),
                        label = com.tangem.core.ui.extensions.stringReference(""),
                        isValuePasted = false,
                        contactName = "Dave",
                    ),
                    memoTextField = null,
                    recent = persistentListOf(),
                    wallets = persistentListOf(),
                    networkName = "Ethereum",
                    isRecentHidden = false,
                )

                // Act — init with isEditMode = true triggers resetContactOnEdit()
                val sut = buildModel(
                    currentRoute = CommonSendRoute.Destination(isEditMode = true),
                    initialState = stateWithContact,
                )
                advanceUntilIdle()

                // Assert
                assertThat(content(sut).addressTextField.contactName).isNull()
            }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ContactRecognition {

        @ParameterizedTest
        @ProvideTestModels
        fun `WHEN address entered THEN recognize matching saved contact case-insensitively`(
            model: ContactRecognitionModel,
        ) = runTest {
            // Arrange
            coEvery {
                validateWalletAddressUseCase(any(), any(), any(), any<List<CryptoCurrencyAddress>>(), any())
            } returns AddressValidation.Success.Valid.right()
            every { getContactsUseCase(any(), any()) } returns
                flowOf(listOf(buildContact(name = model.savedName, address = model.savedAddress)))
            val sut = buildModel()
            advanceUntilIdle()

            // Act
            sut.onRecipientAddressValueChange(model.enteredAddress, EnterAddressSource.InputField)
            advanceUntilIdle()

            // Assert
            assertThat(content(sut).addressTextField.contactName).isEqualTo(model.expectedContactName)
        }

        private fun provideTestModels() = listOf(
            // saved "0xAddr", entered "0xaddr" → case-insensitive match
            ContactRecognitionModel(
                savedName = "Alice",
                savedAddress = "0xAddr",
                enteredAddress = "0xaddr",
                expectedContactName = "Alice"
            ),
            // entered address not among saved contacts → no recognition
            ContactRecognitionModel(
                savedName = "Alice",
                savedAddress = "0xOther",
                enteredAddress = "0xAddr",
                expectedContactName = null
            ),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class OnContactClick {

        @ParameterizedTest
        @ProvideTestModels
        fun `WHEN onContactClick THEN apply single-address contact directly else open selector`(
            model: ContactClickModel,
        ) = runTest {
            // Arrange
            coEvery {
                validateWalletAddressUseCase(any(), any(), any(), any<List<CryptoCurrencyAddress>>(), any())
            } returns AddressValidation.Success.Valid.right()
            val sut = buildModel()
            advanceUntilIdle()

            // Act
            sut.onContactClick(matchedContact(addresses = model.addresses))
            advanceUntilIdle()

            // Assert
            if (model.expectedValidatedAddress != null) {
                // single entry → applied directly → that address gets validated
                coVerify {
                    validateWalletAddressUseCase(
                        any(), any(), eq(model.expectedValidatedAddress), any<List<CryptoCurrencyAddress>>(), any(),
                    )
                }
            } else {
                // multiple entries → selector opened, nothing applied/validated yet
                coVerify(exactly = 0) {
                    validateWalletAddressUseCase(any(), any(), any(), any<List<CryptoCurrencyAddress>>(), any())
                }
            }
        }

        private fun provideTestModels() = listOf(
            ContactClickModel(addresses = listOf("0xSingle"), expectedValidatedAddress = "0xSingle"),
            ContactClickModel(addresses = listOf("0xA", "0xB"), expectedValidatedAddress = null),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ShowAddContact {

        @ParameterizedTest
        @ProvideTestModels
        fun `WHEN address entered THEN show add-contact only when available and not already saved`(
            model: AddContactModel,
        ) = runTest {
            // Arrange
            coEvery {
                validateWalletAddressUseCase(any(), any(), any(), any<List<CryptoCurrencyAddress>>(), any())
            } returns AddressValidation.Success.Valid.right()
            every { getContactsUseCase(any(), any()) } returns
                flowOf(model.savedAddresses.map { buildContact(address = it) })
            val sut = buildBlockModel(isAddContactAvailable = model.isAddContactAvailable)
            advanceUntilIdle()

            // Act
            sut.onRecipientAddressValueChange(model.enteredAddress, EnterAddressSource.InputField)
            advanceUntilIdle()

            // Assert
            assertThat(sut.showAddContact.value).isEqualTo(model.expectedShown)
        }

        private fun provideTestModels() = listOf(
            // not available -> never shown, even for a fresh valid address
            AddContactModel(
                isAddContactAvailable = false,
                savedAddresses = emptyList(),
                enteredAddress = "0xFresh",
                expectedShown = false
            ),
            // available + address not in the book -> shown
            AddContactModel(
                isAddContactAvailable = true,
                savedAddresses = emptyList(),
                enteredAddress = "0xFresh",
                expectedShown = true
            ),
            // available but address already saved -> hidden
            AddContactModel(
                isAddContactAvailable = true,
                savedAddresses = listOf("0xSaved"),
                enteredAddress = "0xSaved",
                expectedShown = false
            ),
        )
    }

    // region fixtures

    private fun TestScope.buildModel(
        currentRoute: DestinationRoute = CommonSendRoute.Destination(isEditMode = false),
        initialState: DestinationUM = DestinationUM.Empty(),
    ): SendDestinationModel {
        val params = SendDestinationComponentParams.DestinationParams(
            state = initialState,
            analyticsCategoryName = "test_send",
            analyticsSendSource = CommonSendAnalyticEvents.CommonSendSource.Send,
            cryptoCurrency = cryptoCurrency,
            userWalletId = testUserWalletId,
            title = stringReference("Send to"),
            isBalanceHidingFlow = MutableStateFlow(false),
            route = currentRoute,
            callback = callback,
            isAllowSelfSend = false,
        )
        return createModel(params)
    }

    /** Builds the model with the success-screen block flavor ([DestinationBlockParams]) used by `showAddContact`. */
    private fun TestScope.buildBlockModel(isAddContactAvailable: Boolean): SendDestinationModel {
        val params = SendDestinationComponentParams.DestinationBlockParams(
            state = DestinationUM.Empty(),
            analyticsCategoryName = "test_send",
            analyticsSendSource = CommonSendAnalyticEvents.CommonSendSource.Send,
            userWalletId = testUserWalletId,
            cryptoCurrency = cryptoCurrency,
            blockClickEnableFlow = MutableStateFlow(true),
            predefinedValues = PredefinedValues.Empty,
            isAllowSelfSend = false,
            isAddContactAvailable = isAddContactAvailable,
        )
        return createModel(params)
    }

    private fun TestScope.createModel(params: SendDestinationComponentParams): SendDestinationModel {
        return SendDestinationModel(
            paramsContainer = MutableParamsContainer(params),
            dispatchers = testDispatcherProvider(),
            router = router,
            validateWalletAddressUseCase = validateWalletAddressUseCase,
            validateWalletMemoUseCase = validateWalletMemoUseCase,
            isMemoRequiredUseCase = isMemoRequiredUseCase,
            getWalletsUseCase = getWalletsUseCase,
            getNetworkAddressesUseCase = getNetworkAddressesUseCase,
            getFixedTxHistoryItemsUseCase = getFixedTxHistoryItemsUseCase,
            isSelfSendAvailableUseCase = isSelfSendAvailableUseCase,
            listenToQrScanningUseCase = listenToQrScanningUseCase,
            parseQrCodeUseCase = parseQrCodeUseCase,
            isAccountsModeEnabledUseCase = isAccountsModeEnabledUseCase,
            analyticsEventHandler = analyticsEventHandler,
            multiAccountStatusListSupplier = multiAccountStatusListSupplier,
            getBackupProblematicWalletForAddressUseCase = getBackupProblematicWalletForAddressUseCase,
            sendDestinationAlertFactory = sendDestinationAlertFactory,
            sendBackupProblemEmailUseCase = sendBackupProblemEmailUseCase,
            getContactsUseCase = getContactsUseCase,
            contactSelectionListener = contactSelectionListener,
        )
    }

    private fun buildContact(name: String = "Alice", address: String = "0xAddr"): Contact = Contact(
        id = ContactId("c1"),
        walletId = testUserWalletId,
        name = ContactName(name).getOrNull()!!,
        icon = "icon",
        iconColor = "#FFFFFF",
        createdAt = "2026-01-01T00:00:00.000Z",
        updatedAt = "2026-01-01T00:00:00.000Z",
        addressEntries = listOf(
            AddressEntry(
                id = AddressEntryId("e1"),
                address = address,
                networkId = Network.RawID(networkRawId),
                networkName = "Ethereum",
                memo = null,
                signature = "",
            ),
        ),
    )

    private fun matchedContact(name: String = "Alice", addresses: List<String> = listOf("0xAddr")): MatchedContact =
        MatchedContact(
            contactId = "c1",
            walletId = testUserWalletId.stringValue,
            name = name,
            icon = contactIcon,
            networkId = networkRawId,
            entries = addresses
                .map { MatchedContact.ContactAddress(address = it, memo = null, networkName = "Ethereum") }
                .toImmutableList(),
        )

    private fun selectedContact(
        name: String = "Alice",
        address: String = "0xAddr",
        memo: String? = null,
    ): SelectedContact = SelectedContact(
        contactId = "c1",
        name = name,
        icon = contactIcon,
        address = address,
        networkId = networkRawId,
        memo = memo,
    )

    private fun content(model: SendDestinationModel): DestinationUM.Content =
        model.uiState.value as DestinationUM.Content

    data class AutoNextModel(val addressValidation: AddressValidationResult, val expectedNextClicks: Int)

    data class AddContactModel(
        val isAddContactAvailable: Boolean,
        val savedAddresses: List<String>,
        val enteredAddress: String,
        val expectedShown: Boolean,
    )

    data class ContactClickModel(val addresses: List<String>, val expectedValidatedAddress: String?)

    data class ContactRecognitionModel(
        val savedName: String,
        val savedAddress: String,
        val enteredAddress: String,
        val expectedContactName: String?,
    )

    // endregion
}