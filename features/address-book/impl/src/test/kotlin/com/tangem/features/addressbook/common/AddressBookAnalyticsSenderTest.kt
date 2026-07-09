package com.tangem.features.addressbook.common

import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.toHexString
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.addressbook.error.AddressBookSyncError
import com.tangem.domain.addressbook.error.SaveContactError
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.addressbook.analytics.AddressBookEvents
import com.tangem.features.addressbook.analytics.AddressBookEvents.AddContactTapped
import com.tangem.features.addressbook.analytics.AddressBookEvents.ContactListScreenOpened.Source
import com.tangem.features.addressbook.analytics.AddressBookEvents.ContactSaved
import com.tangem.features.addressbook.analytics.AddressBookEvents.SaveErrorShown.ErrorType
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AddressBookAnalyticsSenderTest {

    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val userWalletsListRepository: UserWalletsListRepository = mockk()

    private val sender = AddressBookAnalyticsSender(
        analyticsEventHandler = analyticsEventHandler,
        userWalletsListRepository = userWalletsListRepository,
        dispatcherProvider = TestingCoroutineDispatcherProvider(),
    )

    @BeforeEach
    fun setup() {
        clearMocks(analyticsEventHandler)
        val wallet = mockk<UserWallet> { every { walletId } returns EXPECTED_WALLET_ID }
        every { userWalletsListRepository.selectedUserWallet } returns MutableStateFlow(wallet)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun sendContactListScreenOpened(model: ScreenOpenedModel) = runTest {
        // Act
        sender.sendContactListScreenOpened(source = model.source, contactsCount = model.contactsCount, scope = this)
        advanceUntilIdle()

        // Assert
        val expected = AddressBookEvents.ContactListScreenOpened(
            walletId = EXPECTED_WALLET_ID_HASH,
            source = model.source,
            contactsCount = model.contactsCount,
        )
        verify(exactly = 1) { analyticsEventHandler.send(expected) }
    }

    @ParameterizedTest
    @MethodSource("provideAddContactModels")
    fun sendAddContactTapped(model: AddContactModel) = runTest {
        // Act
        sender.sendAddContactTapped(fromSendSuccess = model.fromSendSuccess, scope = this)
        advanceUntilIdle()

        // Assert
        val expected = AddContactTapped(
            walletId = EXPECTED_WALLET_ID_HASH,
            source = model.expectedSource,
        )
        verify(exactly = 1) { analyticsEventHandler.send(expected) }
    }

    @ParameterizedTest
    @MethodSource("provideContactSavedModels")
    fun sendContactSaved(model: ContactSavedModel) = runTest {
        // Act
        sender.sendContactSaved(walletId = EXPECTED_WALLET_ID, contactId = CONTACT_ID, isEdit = model.isEdit)

        // Assert
        val expected = ContactSaved(
            walletId = EXPECTED_WALLET_ID_HASH,
            contactId = CONTACT_ID,
            mode = model.expectedMode,
        )
        verify(exactly = 1) { analyticsEventHandler.send(expected) }
    }

    @ParameterizedTest
    @MethodSource("provideSaveErrorModels")
    fun sendSaveErrorShown(model: SaveErrorModel) = runTest {
        // Act
        sender.sendSaveErrorShown(walletId = EXPECTED_WALLET_ID, contactId = CONTACT_ID, error = model.error)

        // Assert
        val expectedType = model.expectedType
        if (expectedType == null) {
            verify(exactly = 0) { analyticsEventHandler.send(any()) }
        } else {
            val expected = AddressBookEvents.SaveErrorShown(
                walletId = EXPECTED_WALLET_ID_HASH,
                contactId = CONTACT_ID,
                errorType = expectedType,
            )
            verify(exactly = 1) { analyticsEventHandler.send(expected) }
        }
    }

    @Test
    fun `WHEN sendSaveToButtonClicked THEN event sent`() {
        // Act
        sender.sendSaveToButtonClicked()

        // Assert
        verify(exactly = 1) { analyticsEventHandler.send(AddressBookEvents.SaveToButtonClicked) }
    }

    @Test
    fun `WHEN sendAddressScreenOpened THEN event sent`() {
        // Act
        sender.sendAddressScreenOpened()

        // Assert
        verify(exactly = 1) { analyticsEventHandler.send(AddressBookEvents.AddressScreenOpened) }
    }

    @Test
    fun `WHEN sendContactScreenOpened THEN event sent with selected wallet`() = runTest {
        // Act
        sender.sendContactScreenOpened(contactId = CONTACT_ID, scope = this)
        advanceUntilIdle()

        // Assert
        val expected = AddressBookEvents.ContactScreenOpened(walletId = EXPECTED_WALLET_ID_HASH, contactId = CONTACT_ID)
        verify(exactly = 1) { analyticsEventHandler.send(expected) }
    }

    @Test
    fun `WHEN sendSendFlowWidgetShown THEN event sent with selected wallet`() = runTest {
        // Act
        sender.sendSendFlowWidgetShown(scope = this)
        advanceUntilIdle()

        // Assert
        val expected = AddressBookEvents.SendFlowWidgetShown(walletId = EXPECTED_WALLET_ID_HASH)
        verify(exactly = 1) { analyticsEventHandler.send(expected) }
    }

    @Test
    fun `WHEN sendContactSelectedInSend THEN event sent with selected wallet`() = runTest {
        // Act
        sender.sendContactSelectedInSend(contactId = CONTACT_ID, scope = this)
        advanceUntilIdle()

        // Assert
        val expected = AddressBookEvents.ContactSelectedInSend(
            walletId = EXPECTED_WALLET_ID_HASH,
            contactId = CONTACT_ID,
        )
        verify(exactly = 1) { analyticsEventHandler.send(expected) }
    }

    @Test
    fun `WHEN onAddressSubstitutedInSend THEN event sent`() {
        // Act
        sender.onAddressSubstitutedInSend(walletId = EXPECTED_WALLET_ID, contactId = CONTACT_ID)

        // Assert
        val expected =
            AddressBookEvents.AddressSubstitutedInSend(walletId = EXPECTED_WALLET_ID_HASH, contactId = CONTACT_ID)
        verify(exactly = 1) { analyticsEventHandler.send(expected) }
    }

    @ParameterizedTest
    @MethodSource("provideContactIdModels")
    fun sendAddressInvalid(contactId: String) {
        // Act
        sender.sendAddressInvalid(walletId = EXPECTED_WALLET_ID, contactId = contactId)

        // Assert
        val expected = AddressBookEvents.AddressInvalid(walletId = EXPECTED_WALLET_ID_HASH, contactId = contactId)
        verify(exactly = 1) { analyticsEventHandler.send(expected) }
    }

    @ParameterizedTest
    @MethodSource("provideNullableContactIdModels")
    fun sendDuplicateNameErrorShown(contactId: String?) {
        // Act
        sender.sendDuplicateNameErrorShown(walletId = EXPECTED_WALLET_ID, contactId = contactId)

        // Assert
        val expected = AddressBookEvents.DuplicateNameErrorShown(
            walletId = EXPECTED_WALLET_ID_HASH,
            contactId = contactId,
        )
        verify(exactly = 1) { analyticsEventHandler.send(expected) }
    }

    @Test
    fun `WHEN sendAddressRemoved THEN event sent`() {
        // Act
        sender.sendAddressRemoved(walletId = EXPECTED_WALLET_ID, contactId = CONTACT_ID)

        // Assert
        val expected = AddressBookEvents.AddressRemoved(walletId = EXPECTED_WALLET_ID_HASH, contactId = CONTACT_ID)
        verify(exactly = 1) { analyticsEventHandler.send(expected) }
    }

    @Test
    fun `WHEN sendContactDeleted THEN event sent`() {
        // Act
        sender.sendContactDeleted(walletId = EXPECTED_WALLET_ID, contactId = CONTACT_ID)

        // Assert
        val expected = AddressBookEvents.ContactDeleted(walletId = EXPECTED_WALLET_ID_HASH, contactId = CONTACT_ID)
        verify(exactly = 1) { analyticsEventHandler.send(expected) }
    }

    internal data class ScreenOpenedModel(val source: Source, val contactsCount: Int)

    internal data class AddContactModel(val fromSendSuccess: Boolean, val expectedSource: AddContactTapped.Source)

    internal data class ContactSavedModel(val isEdit: Boolean, val expectedMode: ContactSaved.Mode)

    internal data class SaveErrorModel(val error: SaveContactError, val expectedType: ErrorType?)

    private fun provideTestModels() = listOf(
        ScreenOpenedModel(source = Source.Settings, contactsCount = 0),
        ScreenOpenedModel(source = Source.SendFlow, contactsCount = 3),
    )

    private fun provideAddContactModels() = listOf(
        AddContactModel(fromSendSuccess = false, expectedSource = AddContactTapped.Source.Settings),
        AddContactModel(fromSendSuccess = true, expectedSource = AddContactTapped.Source.SendSuccess),
    )

    private fun provideContactSavedModels() = listOf(
        ContactSavedModel(isEdit = false, expectedMode = ContactSaved.Mode.Create),
        ContactSavedModel(isEdit = true, expectedMode = ContactSaved.Mode.Edit),
    )

    private fun provideSaveErrorModels() = listOf(
        SaveErrorModel(error = SaveContactError.Signing(mockk()), expectedType = ErrorType.Signing),
        SaveErrorModel(
            error = SaveContactError.Backend(AddressBookSyncError.Network),
            expectedType = ErrorType.Network,
        ),
        // 412
        SaveErrorModel(
            error = SaveContactError.Backend(AddressBookSyncError.Conflict),
            expectedType = ErrorType.Server,
        ),
        // 5xx / unmapped
        SaveErrorModel(error = SaveContactError.Backend(AddressBookSyncError.Unknown), expectedType = ErrorType.Server),
        SaveErrorModel(
            error = SaveContactError.Backend(AddressBookSyncError.BadRequest),
            expectedType = ErrorType.Server,
        ),
        // Validation failures are shown inline, not as a save error.
        SaveErrorModel(error = SaveContactError.Name(mockk()), expectedType = null),
    )

    // Create sends an empty contact id, edit sends the contact id.
    private fun provideContactIdModels() = listOf("", CONTACT_ID)

    // Duplicate-name allows a null contact id (create) as well as an edited contact's id.
    private fun provideNullableContactIdModels() = listOf(null, CONTACT_ID)

    private companion object {
        val EXPECTED_WALLET_ID = UserWalletId("0011223344")

        // Analytics receives the SHA-256 (uppercase hex) of the wallet id, never the raw value.
        val EXPECTED_WALLET_ID_HASH = EXPECTED_WALLET_ID.value.calculateSha256().toHexString()

        const val CONTACT_ID = "contact-42"
    }
}