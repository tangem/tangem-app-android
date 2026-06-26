package com.tangem.features.addressbook.list.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.addressbook.interactor.GetVerifiedContactsInteractor
import com.tangem.domain.addressbook.model.AddressEntry
import com.tangem.domain.addressbook.model.AddressEntryId
import com.tangem.domain.addressbook.model.Contact
import com.tangem.domain.addressbook.model.ContactId
import com.tangem.domain.addressbook.model.ContactName
import com.tangem.domain.addressbook.model.VerifiedContact
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.features.addressbook.ContactSelectionTrigger
import com.tangem.features.addressbook.list.DefaultAddressBookListComponent
import com.tangem.features.addressbook.list.state.AddressBookListStateController
import com.tangem.features.addressbook.list.ui.state.AddressBookListUM
import com.tangem.features.addressbook.list.ui.state.ContentMode
import com.tangem.features.addressbook.route.AddressBookRoute
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.*

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AddressBookListModelTest {

    private val router: Router = mockk(relaxed = true)
    private val contactSelectionTrigger: ContactSelectionTrigger = mockk(relaxed = true)
    private val getVerifiedContactsInteractor: GetVerifiedContactsInteractor = mockk()
    private val getWalletsUseCase: GetWalletsUseCase = mockk()

    private var model: AddressBookListModel? = null

    @BeforeEach
    fun resetMocks() {
        clearMocks(getVerifiedContactsInteractor, getWalletsUseCase)
        every { getWalletsUseCase.invokeAsMap(isOnlyMultiCurrency = false, filterLocked = true) } returns
            flowOf(linkedMapOf<UserWalletId, UserWallet>())
    }

    @AfterEach
    fun tearDown() {
        model?.onDestroy()
        model = null
    }

    @Test
    fun `GIVEN default mode AND verified contacts WHEN created THEN content shown`() = runTest {
        // Arrange
        every { getVerifiedContactsInteractor(query = "", userWalletId = null) } returns
            flowOf(listOf(verifiedContact(id = "1", name = "Alice"), verifiedContact(id = "2", name = "Bob")))

        // Act
        val model = createModel(testScope = this, mode = AddressBookRoute.ListMode.Default)
        advanceUntilIdle()

        // Assert
        val state = model.state.value as AddressBookListUM.Content
        assertThat(state.contentMode).isInstanceOf(ContentMode.Default::class.java)
        assertThat(state.contacts.map { it.name }).containsExactly("Alice", "Bob")
    }

    @Test
    fun `GIVEN default mode AND no contacts WHEN created THEN empty state`() = runTest {
        // Arrange
        every { getVerifiedContactsInteractor(query = "", userWalletId = null) } returns flowOf(emptyList())

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
        every { getVerifiedContactsInteractor(query = "", userWalletId = null) } returns
            flowOf(listOf(verifiedContact(id = "42", name = "Alice")))
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

    private fun verifiedContact(id: String, name: String): VerifiedContact = VerifiedContact(
        contact = Contact(
            id = ContactId(id),
            walletId = UserWalletId("a"),
            name = ContactName(name).getOrNull()!!,
            icon = "",
            iconColor = CryptoPortfolioIcon.Color.Azure.name,
            createdAt = TIMESTAMP,
            updatedAt = TIMESTAMP,
            addressEntries = listOf(
                AddressEntry(
                    id = AddressEntryId("e-$id"),
                    address = "0xABC",
                    networkId = Network.RawID("ethereum"),
                    networkName = "Ethereum",
                    memo = null,
                    signature = "sig",
                ),
            ),
        ),
        invalidEntries = emptyList(),
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