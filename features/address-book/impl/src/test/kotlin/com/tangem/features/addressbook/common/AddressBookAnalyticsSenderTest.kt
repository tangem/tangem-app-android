package com.tangem.features.addressbook.common

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.addressbook.error.AddressBookSyncError
import com.tangem.domain.addressbook.error.SaveContactError
import com.tangem.features.addressbook.analytics.AddressBookEvents
import com.tangem.features.addressbook.analytics.AddressBookEvents.AddContactTapped
import com.tangem.features.addressbook.analytics.AddressBookEvents.ContactListScreenOpened.Source
import com.tangem.features.addressbook.analytics.AddressBookEvents.ContactSaved
import com.tangem.features.addressbook.analytics.AddressBookEvents.SaveErrorShown.ErrorType
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AddressBookAnalyticsSenderTest {

    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)

    private val sender = AddressBookAnalyticsSender(analyticsEventHandler = analyticsEventHandler)

    @BeforeEach
    fun setup() {
        clearMocks(analyticsEventHandler)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun sendContactListScreenOpened(model: ScreenOpenedModel) {
        // Act
        sender.sendContactListScreenOpened(source = model.source, contactsCount = model.contactsCount)

        // Assert
        val expected = AddressBookEvents.ContactListScreenOpened(
            source = model.source,
            contactsCount = model.contactsCount,
        )
        verify(exactly = 1) { analyticsEventHandler.send(expected) }
    }

    @ParameterizedTest
    @EnumSource(AddContactTapped.Source::class)
    fun sendAddContactTapped(source: AddContactTapped.Source) {
        // Act
        sender.sendAddContactTapped(source = source)

        // Assert
        verify(exactly = 1) { analyticsEventHandler.send(AddContactTapped(source = source)) }
    }

    @ParameterizedTest
    @MethodSource("provideContactSavedModels")
    fun sendContactSaved(model: ContactSavedModel) {
        // Act
        sender.sendContactSaved(contactId = CONTACT_ID, isEdit = model.isEdit, source = model.source)

        // Assert
        val expected = ContactSaved(contactId = CONTACT_ID, mode = model.expectedMode, source = model.source)
        verify(exactly = 1) { analyticsEventHandler.send(expected) }
    }

    @ParameterizedTest
    @MethodSource("provideSaveErrorModels")
    fun sendSaveErrorShown(model: SaveErrorModel) {
        // Act
        sender.sendSaveErrorShown(contactId = CONTACT_ID, error = model.error)

        // Assert
        val expectedType = model.expectedType
        if (expectedType == null) {
            verify(exactly = 0) { analyticsEventHandler.send(any()) }
        } else {
            val expected = AddressBookEvents.SaveErrorShown(contactId = CONTACT_ID, errorType = expectedType)
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
    fun `WHEN sendContactScreenOpened THEN event sent`() {
        // Act
        sender.sendContactScreenOpened(contactId = CONTACT_ID)

        // Assert
        val expected = AddressBookEvents.ContactScreenOpened(contactId = CONTACT_ID)
        verify(exactly = 1) { analyticsEventHandler.send(expected) }
    }

    @Test
    fun `WHEN sendSendFlowWidgetShown THEN event sent`() {
        // Act
        sender.sendSendFlowWidgetShown()

        // Assert
        verify(exactly = 1) { analyticsEventHandler.send(AddressBookEvents.SendFlowWidgetShown) }
    }

    @Test
    fun `WHEN sendContactSelectedInSend THEN event sent`() {
        // Act
        sender.sendContactSelectedInSend(contactId = CONTACT_ID)

        // Assert
        val expected = AddressBookEvents.ContactSelectedInSend(contactId = CONTACT_ID)
        verify(exactly = 1) { analyticsEventHandler.send(expected) }
    }

    @Test
    fun `WHEN onAddressSubstitutedInSend THEN event sent`() {
        // Act
        sender.onAddressSubstitutedInSend(contactId = CONTACT_ID)

        // Assert
        val expected = AddressBookEvents.AddressSubstitutedInSend(contactId = CONTACT_ID)
        verify(exactly = 1) { analyticsEventHandler.send(expected) }
    }

    @ParameterizedTest
    @MethodSource("provideContactIdModels")
    fun sendAddressInvalid(contactId: String) {
        // Act
        sender.sendAddressInvalid(contactId = contactId)

        // Assert
        verify(exactly = 1) { analyticsEventHandler.send(AddressBookEvents.AddressInvalid(contactId = contactId)) }
    }

    @ParameterizedTest
    @MethodSource("provideNullableContactIdModels")
    fun sendDuplicateNameErrorShown(contactId: String?) {
        // Act
        sender.sendDuplicateNameErrorShown(contactId = contactId)

        // Assert
        val expected = AddressBookEvents.DuplicateNameErrorShown(contactId = contactId)
        verify(exactly = 1) { analyticsEventHandler.send(expected) }
    }

    @Test
    fun `WHEN sendAddressRemoved THEN event sent`() {
        // Act
        sender.sendAddressRemoved(contactId = CONTACT_ID)

        // Assert
        verify(exactly = 1) { analyticsEventHandler.send(AddressBookEvents.AddressRemoved(contactId = CONTACT_ID)) }
    }

    @Test
    fun `WHEN sendContactDeleted THEN event sent`() {
        // Act
        sender.sendContactDeleted(contactId = CONTACT_ID)

        // Assert
        verify(exactly = 1) { analyticsEventHandler.send(AddressBookEvents.ContactDeleted(contactId = CONTACT_ID)) }
    }

    internal data class ScreenOpenedModel(val source: Source, val contactsCount: Int)

    internal data class ContactSavedModel(
        val isEdit: Boolean,
        val expectedMode: ContactSaved.Mode,
        val source: AddContactTapped.Source? = null,
    )

    internal data class SaveErrorModel(val error: SaveContactError, val expectedType: ErrorType?)

    private fun provideTestModels() = listOf(
        ScreenOpenedModel(source = Source.Settings, contactsCount = 0),
        ScreenOpenedModel(source = Source.SendFlow, contactsCount = 3),
    )

    private fun provideContactSavedModels() = listOf(
        ContactSavedModel(isEdit = false, expectedMode = ContactSaved.Mode.Create),
        ContactSavedModel(isEdit = true, expectedMode = ContactSaved.Mode.Edit),
        ContactSavedModel(
            isEdit = false,
            expectedMode = ContactSaved.Mode.Create,
            source = AddContactTapped.Source.TransactionDetails,
        ),
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
        const val CONTACT_ID = "contact-42"
    }
}