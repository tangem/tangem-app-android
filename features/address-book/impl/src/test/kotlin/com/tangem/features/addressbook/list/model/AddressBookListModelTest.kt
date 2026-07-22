package com.tangem.features.addressbook.list.model

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.addressbook.interactor.GetVerifiedContactsInteractor
import com.tangem.domain.addressbook.model.*
import com.tangem.domain.addressbook.usecase.IsAddressBookCompatibleUseCase
import com.tangem.domain.addressbook.usecase.SyncAddressBooksUseCase
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.features.addressbook.ContactSelectionTrigger
import com.tangem.features.addressbook.analytics.AddressBookEvents.ContactListScreenOpened.Source
import com.tangem.features.addressbook.common.AddressBookAnalyticsSender
import com.tangem.features.addressbook.list.DefaultAddressBookListComponent
import com.tangem.features.addressbook.list.state.AddressBookListStateController
import com.tangem.features.addressbook.list.ui.state.AddressBookListUM
import com.tangem.features.addressbook.list.ui.state.ContentMode
import com.tangem.features.addressbook.route.AddressBookRoute
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
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
internal class AddressBookListModelTest {

    private val router: Router = mockk(relaxed = true)
    private val contactSelectionTrigger: ContactSelectionTrigger = mockk(relaxed = true)
    private val analyticsSender: AddressBookAnalyticsSender = mockk(relaxed = true)
    private val getVerifiedContactsInteractor: GetVerifiedContactsInteractor = mockk()
    private val getWalletsUseCase: GetWalletsUseCase = mockk()
    private val syncAddressBooksUseCase: SyncAddressBooksUseCase = mockk(relaxed = true)
    private val isAddressBookCompatibleUseCase: IsAddressBookCompatibleUseCase = mockk()

    private var model: AddressBookListModel? = null

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            getVerifiedContactsInteractor,
            getWalletsUseCase,
            analyticsSender,
            contactSelectionTrigger,
            syncAddressBooksUseCase,
            isAddressBookCompatibleUseCase,
        )
        every { getWalletsUseCase.invokeAsMap(isOnlyMultiCurrency = false, filterLocked = true) } returns
            flowOf(linkedMapOf())
        every { isAddressBookCompatibleUseCase() } returns flowOf(true)
    }

    @AfterEach
    fun tearDown() {
        model?.onDestroy()
        model = null
    }

    @Test
    fun `GIVEN feature just opened WHEN contacts not yet loaded THEN Loading state`() = runTest {
        // Arrange — the interactor has not emitted yet (books still syncing).
        every { getVerifiedContactsInteractor.getVerifiedContacts(query = "", userWalletId = null) } returns emptyFlow()

        // Act
        val model = createModel(testScope = this, mode = AddressBookRoute.ListMode.Default)

        // Assert — shimmer placeholder until the first emission arrives.
        assertThat(model.state.value).isEqualTo(AddressBookListUM.Loading)
    }

    @Test
    fun `GIVEN cached contacts AND sync in progress WHEN created THEN stays Loading until sync completes`() = runTest {
        // Arrange — contacts are already cached locally, but the open-time sync has not returned yet.
        val syncGate = CompletableDeferred<Unit>()
        coEvery { syncAddressBooksUseCase() } coAnswers { syncGate.await(); Unit.right() }
        every { getVerifiedContactsInteractor.getVerifiedContacts(query = "", userWalletId = null) } returns
            flowOf(listOf(contact(id = "1", name = "Alice")))

        // Act
        val model = createModel(testScope = this, mode = AddressBookRoute.ListMode.Default)
        advanceUntilIdle()

        // Assert — the possibly-stale cache is not revealed while the sync is still running.
        assertThat(model.state.value).isEqualTo(AddressBookListUM.Loading)

        // Act — the sync finishes.
        syncGate.complete(Unit)
        advanceUntilIdle()

        // Assert — the list is revealed only after the sync completed.
        assertThat(model.state.value).isInstanceOf(AddressBookListUM.Content::class.java)
    }

    @Test
    fun `GIVEN default mode AND verified contacts WHEN created THEN content shown`() = runTest {
        // Arrange
        every { getVerifiedContactsInteractor.getVerifiedContacts(query = "", userWalletId = null) } returns
            flowOf(listOf(contact(id = "1", name = "Alice"), contact(id = "2", name = "Bob")))

        // Act
        val model = createModel(testScope = this, mode = AddressBookRoute.ListMode.Default)
        advanceUntilIdle()

        // Assert
        val state = model.state.value as AddressBookListUM.Content
        assertThat(state.contentMode).isInstanceOf(ContentMode.Default::class.java)
        assertThat(state.contacts.map { it.name }).containsExactly("Alice", "Bob")
    }

    @Test
    fun `GIVEN a book newer than supported WHEN created THEN incompatible state shown`() = runTest {
        // Arrange
        every { isAddressBookCompatibleUseCase() } returns flowOf(false)
        every { getVerifiedContactsInteractor.getVerifiedContacts(query = "", userWalletId = null) } returns
            flowOf(listOf(contact(id = "1", name = "Alice")))

        // Act
        val model = createModel(testScope = this, mode = AddressBookRoute.ListMode.Default)
        advanceUntilIdle()

        // Assert — the "update the app" stub replaces the list even though contacts are cached.
        assertThat(model.state.value).isEqualTo(AddressBookListUM.Incompatible)
    }

    @Test
    fun `GIVEN default mode AND no contacts WHEN created THEN empty state`() = runTest {
        // Arrange
        every { getVerifiedContactsInteractor.getVerifiedContacts(query = "", userWalletId = null) } returns flowOf(emptyList())

        // Act
        val model = createModel(testScope = this, mode = AddressBookRoute.ListMode.Default)
        advanceUntilIdle()

        // Assert
        assertThat(model.state.value).isInstanceOf(AddressBookListUM.Empty::class.java)
    }

    @Test
    fun `GIVEN default mode WHEN contact clicked THEN editor opened with contact id`() = runTest {
        // Arrange
        var clickedId: String? = null
        every { getVerifiedContactsInteractor.getVerifiedContacts(query = "", userWalletId = null) } returns
            flowOf(listOf(contact(id = "42", name = "Alice")))
        val model = createModel(
            testScope = this,
            mode = AddressBookRoute.ListMode.Default,
            onContactClick = { clickedId = it },
        )
        advanceUntilIdle()

        // Act
        (model.state.value as AddressBookListUM.Content).contacts.first().onClick()

        // Assert
        assertThat(clickedId).isEqualTo("42")
    }

    @Test
    fun `GIVEN default mode WHEN created THEN ContactListScreenOpened sent with settings source and all-tab count`() =
        runTest {
            // Arrange
            every { getVerifiedContactsInteractor.getVerifiedContacts(query = "", userWalletId = null) } returns
                flowOf(listOf(contact(id = "1", name = "Alice"), contact(id = "2", name = "Bob")))

            // Act
            createModel(testScope = this, mode = AddressBookRoute.ListMode.Default)
            advanceUntilIdle()

            // Assert — count comes from the list's own contacts subscription.
            verify(exactly = 1) {
                analyticsSender.sendContactListScreenOpened(source = Source.Settings, contactsCount = 2, scope = any())
            }
        }

    @Test
    fun `GIVEN selector mode WHEN created THEN ContactListScreenOpened sent with send_flow source`() = runTest {
        // Arrange
        every { getVerifiedContactsInteractor.getVerifiedContacts(query = "", userWalletId = null) } returns flowOf(emptyList())

        // Act
        createModel(testScope = this, mode = AddressBookRoute.ListMode.Selector(networkId = "ethereum"))
        advanceUntilIdle()

        // Assert — the "See all" list opened from Send reports send_flow, even with zero contacts.
        verify(exactly = 1) {
            analyticsSender.sendContactListScreenOpened(source = Source.SendFlow, contactsCount = 0, scope = any())
        }
    }

    @Test
    fun `GIVEN selector mode WHEN contact picked THEN ContactSelectedInSend sent`() = runTest {
        // Arrange — a contact with a single ethereum address matches the selection network.
        every { getVerifiedContactsInteractor.getVerifiedContacts(query = "", userWalletId = null) } returns
            flowOf(listOf(contact(id = "42", name = "Alice")))
        val model = createModel(
            testScope = this,
            mode = AddressBookRoute.ListMode.Selector(networkId = "ethereum"),
        )
        advanceUntilIdle()

        // Act — tapping a contact in selector mode picks it.
        (model.state.value as AddressBookListUM.Content).contacts.first().onClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { analyticsSender.sendContactSelectedInSend(contactId = "42", scope = any()) }
    }

    private fun contact(id: String, name: String): Contact = Contact(
        id = ContactId(id),
        walletId = UserWalletId("a"),
        name = ContactName(name).getOrNull()!!,
        icon = "",
        iconColor = CryptoPortfolioIcon.Color.Azure.name,
        createdAt = TIMESTAMP,
        updatedAt = TIMESTAMP,
        addresses = listOf(
            AddressEntry(
                id = AddressEntryId("e-$id"),
                address = "0xABC",
                networkId = Network.RawID("ethereum"),
                memo = null,
                signature = "sig",
            ),
        ),
    )

    private fun createModel(
        testScope: TestScope,
        mode: AddressBookRoute.ListMode,
        onContactClick: (String) -> Unit = {},
        onAddContactClick: () -> Unit = {},
    ): AddressBookListModel {
        val params = DefaultAddressBookListComponent.Params(
            mode = mode,
            onContactClick = onContactClick,
            onAddContactClick = onAddContactClick,
        )
        return AddressBookListModel(
            paramsContainer = MutableParamsContainer(value = params),
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            stateController = AddressBookListStateController(),
            router = router,
            contactSelectionTrigger = contactSelectionTrigger,
            analyticsSender = analyticsSender,
            syncAddressBooksUseCase = syncAddressBooksUseCase,
            isAddressBookCompatibleUseCase = isAddressBookCompatibleUseCase,
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
        const val TIMESTAMP = "2026-06-10T14:30:00.000Z"
    }
}