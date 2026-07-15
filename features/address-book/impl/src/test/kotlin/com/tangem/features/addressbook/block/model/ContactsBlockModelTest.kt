package com.tangem.features.addressbook.block.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.domain.addressbook.interactor.GetVerifiedContactsInteractor
import com.tangem.domain.addressbook.model.*
import com.tangem.domain.addressbook.usecase.SyncAddressBooksUseCase
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.features.addressbook.AddressBookContactsBlockComponent
import com.tangem.features.addressbook.MatchedContact
import com.tangem.features.addressbook.block.state.ContactsBlockStateController
import com.tangem.features.addressbook.block.ui.state.ContactsBlockUM
import com.tangem.features.addressbook.common.AddressBookAnalyticsSender
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ContactsBlockModelTest {

    private val getVerifiedContactsInteractor: GetVerifiedContactsInteractor = mockk()
    private val getWalletsUseCase: GetWalletsUseCase = mockk()
    private val syncAddressBooksUseCase: SyncAddressBooksUseCase = mockk(relaxed = true)
    private val analyticsSender: AddressBookAnalyticsSender = mockk(relaxed = true)
    private val network: Network = mockk { every { rawId } returns ETHEREUM }

    private var model: ContactsBlockModel? = null

    @BeforeEach
    fun resetMocks() {
        clearMocks(getVerifiedContactsInteractor, getWalletsUseCase, analyticsSender)
        every { getWalletsUseCase.invokeAsMap(isOnlyMultiCurrency = false, filterLocked = true) } returns
            flowOf(linkedMapOf())
    }

    @AfterEach
    fun tearDown() {
        model?.onDestroy()
        model = null
    }

    @Test
    fun `GIVEN matching contacts WHEN block populated THEN SendFlowWidgetShown sent once`() = runTest {
        // Arrange
        every { getVerifiedContactsInteractor.getVerifiedContacts(query = any(), userWalletId = null) } returns
            flowOf(listOf(contact(id = "1"), contact(id = "2")))

        // Act
        createModel(testScope = this)
        advanceUntilIdle()

        // Assert — reported once even though two contacts populate the block.
        verify(exactly = 1) { analyticsSender.sendSendFlowWidgetShown(scope = any()) }
    }

    @Test
    fun `GIVEN no matching contacts WHEN block empty THEN SendFlowWidgetShown not sent`() = runTest {
        // Arrange
        every { getVerifiedContactsInteractor.getVerifiedContacts(query = any(), userWalletId = null) } returns
            flowOf(emptyList())

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert — the widget is hidden, so nothing is reported.
        assertThat(model.state.value).isEqualTo(ContactsBlockUM.Hidden)
        verify(exactly = 0) { analyticsSender.sendSendFlowWidgetShown(any()) }
    }

    @Test
    fun `GIVEN populated block WHEN contact tapped THEN ContactSelectedInSend sent AND click propagated`() = runTest {
        // Arrange
        var clicked: MatchedContact? = null
        every { getVerifiedContactsInteractor.getVerifiedContacts(query = any(), userWalletId = null) } returns
            flowOf(listOf(contact(id = "42")))
        val model = createModel(testScope = this, onContactClick = { clicked = it })
        advanceUntilIdle()

        // Act
        (model.state.value as ContactsBlockUM.Content).contacts.first().onClick()

        // Assert
        verify(exactly = 1) { analyticsSender.sendContactSelectedInSend(contactId = "42", scope = any()) }
        assertThat(clicked?.contactId).isEqualTo("42")
    }

    private fun contact(id: String): Contact = Contact(
        id = ContactId(id),
        walletId = UserWalletId("a"),
        name = ContactName("Contact $id").getOrNull()!!,
        icon = "",
        iconColor = CryptoPortfolioIcon.Color.Azure.name,
        createdAt = TIMESTAMP,
        updatedAt = TIMESTAMP,
        addresses = listOf(
            AddressEntry(
                id = AddressEntryId("e-$id"),
                address = "0x$id",
                networkId = Network.RawID(ETHEREUM),
                memo = null,
                signature = "sig",
            ),
        ),
    )

    private fun createModel(
        testScope: TestScope,
        onContactClick: (MatchedContact) -> Unit = {},
        onSeeAllClick: () -> Unit = {},
    ): ContactsBlockModel {
        val params = AddressBookContactsBlockComponent.Params(
            network = network,
            queryFlow = MutableStateFlow(""),
            onContactClick = onContactClick,
            onSeeAllClick = onSeeAllClick,
        )
        return ContactsBlockModel(
            paramsContainer = MutableParamsContainer(value = params),
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            stateController = ContactsBlockStateController(),
            analyticsSender = analyticsSender,
            syncAddressBooksUseCase = syncAddressBooksUseCase,
            getVerifiedContactsInteractor = getVerifiedContactsInteractor,
            getWalletsUseCase = getWalletsUseCase,
        ).also { model = it }
    }

    private fun TestScope.createTestingCoroutineDispatcherProvider(): TestingCoroutineDispatcherProvider {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        return TestingCoroutineDispatcherProvider(
            main = testDispatcher,
            mainImmediate = testDispatcher,
            io = testDispatcher,
            default = testDispatcher,
            single = testDispatcher,
        )
    }

    private companion object {
        const val ETHEREUM = "ethereum"
        const val TIMESTAMP = "2026-06-10T14:30:00.000Z"
    }
}