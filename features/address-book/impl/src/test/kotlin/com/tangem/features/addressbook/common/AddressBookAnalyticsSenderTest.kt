package com.tangem.features.addressbook.common

import com.tangem.common.routing.entity.AddressBookOpenMode
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
        sender.sendContactListScreenOpened(mode = model.mode, scope = this)
        advanceUntilIdle()

        // Assert
        val expected = AddressBookEvents.ContactListScreenOpened(
            walletId = EXPECTED_WALLET_ID,
            source = model.expectedSource,
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
        val expected = AddressBookEvents.AddContactTapped(
            walletId = EXPECTED_WALLET_ID,
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
        val expected = AddressBookEvents.ContactSaved(
            walletId = EXPECTED_WALLET_ID,
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
                walletId = EXPECTED_WALLET_ID,
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

    internal data class ScreenOpenedModel(val mode: AddressBookOpenMode, val expectedSource: Source)

    internal data class AddContactModel(val fromSendSuccess: Boolean, val expectedSource: AddContactTapped.Source)

    internal data class ContactSavedModel(val isEdit: Boolean, val expectedMode: ContactSaved.Mode)

    internal data class SaveErrorModel(val error: SaveContactError, val expectedType: ErrorType?)

    private fun provideTestModels() = listOf(
        ScreenOpenedModel(mode = AddressBookOpenMode.Default, expectedSource = Source.Settings),
        ScreenOpenedModel(
            mode = AddressBookOpenMode.ContactSelection(networkId = "ethereum"),
            expectedSource = Source.SendFlow,
        ),
        ScreenOpenedModel(
            mode = AddressBookOpenMode.WithContactCreation(address = "0xABC", networkId = "ethereum"),
            expectedSource = Source.SendFlow,
        ),
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
        SaveErrorModel(error = SaveContactError.Backend(AddressBookSyncError.Network), expectedType = ErrorType.Network),
        // 412
        SaveErrorModel(error = SaveContactError.Backend(AddressBookSyncError.Conflict), expectedType = ErrorType.Server),
        // 5xx / unmapped
        SaveErrorModel(error = SaveContactError.Backend(AddressBookSyncError.Unknown), expectedType = ErrorType.Server),
        SaveErrorModel(
            error = SaveContactError.Backend(AddressBookSyncError.BadRequest),
            expectedType = ErrorType.Server,
        ),
        // Validation failures are shown inline, not as a save error.
        SaveErrorModel(error = SaveContactError.Name(mockk()), expectedType = null),
        SaveErrorModel(error = SaveContactError.Address(mockk()), expectedType = null),
    )

    private companion object {
        val EXPECTED_WALLET_ID = UserWalletId("0011223344")
        const val CONTACT_ID = "contact-42"
    }
}