package com.tangem.features.addressbook.editcontact.model

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.addressbook.error.ContactNameValidationError
import com.tangem.domain.addressbook.error.SaveContactError
import com.tangem.domain.addressbook.interactor.SaveContactInteractor
import com.tangem.domain.addressbook.model.Contact
import com.tangem.domain.addressbook.model.ContactId
import com.tangem.domain.addressbook.model.ContactName
import com.tangem.domain.addressbook.usecase.ValidateContactNameUseCase
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.addressbook.common.AddressBookResultHolder
import com.tangem.features.addressbook.editcontact.DefaultEditContactComponent
import com.tangem.features.addressbook.editcontact.state.EditContactStateController
import com.tangem.features.addressbook.editcontact.ui.state.EditContactUM
import com.tangem.features.addressbook.editcontact.ui.state.ValidatedAddress
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioFetcher
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorController
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class EditContactModelTest {

    private val resultHolder = AddressBookResultHolder()
    private val messageSender: UiMessageSender = mockk(relaxed = true)
    private val userWalletsListRepository: UserWalletsListRepository = mockk(relaxed = true)
    private val validateContactNameUseCase: ValidateContactNameUseCase = mockk()
    private val saveContactInteractor: SaveContactInteractor = mockk()
    private val portfolioSelectorController: PortfolioSelectorController = mockk()
    private val portfolioFetcher: PortfolioFetcher = mockk(relaxed = true)
    private val portfolioFetcherFactory: PortfolioFetcher.Factory = mockk()

    // Drives the wallet picked in the reused portfolio selector; `first` of the pair is the chosen wallet.
    private val selectedWalletData =
        MutableSharedFlow<Pair<UserWallet, AccountStatus.CryptoPortfolio>?>(extraBufferCapacity = 1)

    private var model: EditContactModel? = null

    @BeforeEach
    fun setUp() {
        // Default: no wallets loaded, name always valid. Individual tests override as needed.
        setupWallets(wallets = emptyList(), selected = null)
        coEvery { validateContactNameUseCase(any(), any()) } returns ContactName("Satoshi").getOrNull()!!.right()
        every { portfolioFetcherFactory.create(any(), any()) } returns portfolioFetcher
        every { portfolioSelectorController.selectedAccountWithData(any()) } returns selectedWalletData
    }

    @AfterEach
    fun tearDown() {
        // Cancels modelScope, stopping the confirmed-addresses collector.
        model?.onDestroy()
        model = null
    }

    @Test
    fun `WHEN model created THEN initial state is correct`() = runTest {
        val expectedColors = CryptoPortfolioIcon.Color.entries.toImmutableList()
        val expectedSelectedColor = expectedColors.first()

        val model = createModel(testScope = this)
        advanceUntilIdle()
        val state = model.state.value

        val expected = EditContactUM(
            title = resourceReference(R.string.address_book_new_contact),
            name = "",
            namePlaceholder = resourceReference(R.string.address_book_new_contact),
            nameError = null,
            portfolioIcon = AccountIconUM.CryptoPortfolio(
                value = CryptoPortfolioIcon.Icon.Letter,
                color = expectedSelectedColor,
            ),
            colors = EditContactUM.Colors(
                selected = expectedSelectedColor,
                list = expectedColors,
                onColorSelect = state.colors.onColorSelect,
            ),
            addresses = persistentListOf(),
            walletBlock = EditContactUM.WalletBlockUM(
                walletName = "",
                isChangeable = false,
                onClick = state.walletBlock.onClick,
            ),
            isAddAddressEnabled = true,
            saveButton = state.saveButton,
            onNameChange = state.onNameChange,
            onCloseClick = state.onCloseClick,
            onAddAddressClick = state.onAddAddressClick,
        )
        assertThat(state).isEqualTo(expected)
    }

    @Test
    fun `GIVEN existing contactId WHEN model created THEN title is contact`() = runTest {
        // Arrange
        val params = createParams(contactId = ContactId(value = "contact-id"))

        // Act
        val model = createModel(testScope = this, params = params)
        val state = model.state.value

        // Assert
        assertThat(state.title).isEqualTo(resourceReference(R.string.address_book_contact))
    }

    @Test
    fun `GIVEN initial state WHEN onNameChange THEN name updated`() = runTest {
        val model = createModel(testScope = this)
        val newName = "Satoshi"

        model.state.value.onNameChange(newName)

        assertThat(model.state.value.name).isEqualTo(newName)
    }

    @Test
    fun `GIVEN initial state WHEN onColorSelect THEN selected color and portfolio icon updated`() = runTest {
        val model = createModel(testScope = this)
        val newColor = CryptoPortfolioIcon.Color.entries.last()

        model.state.value.colors.onColorSelect(newColor)

        val state = model.state.value
        assertThat(state.colors.selected).isEqualTo(newColor)
        assertThat(state.portfolioIcon.color).isEqualTo(newColor)
    }

    @Test
    fun `GIVEN confirmed address set on holder WHEN collected THEN address appended to state`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()
        val validatedAddress = ValidatedAddress(address = "0xABC", networkIds = persistentListOf("ethereum"))

        // Act
        resultHolder.setConfirmedAddress(validatedAddress)
        advanceUntilIdle()

        // Assert
        assertThat(model.state.value.addresses).containsExactly(validatedAddress)
        // The value must be consumed so it is not re-applied on resubscription.
        assertThat(resultHolder.confirmedAddress.value).isNull()
    }

    @Test
    fun `GIVEN same address confirmed twice WHEN collected THEN added only once`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()
        val validatedAddress = ValidatedAddress(address = "0xABC", networkIds = persistentListOf("ethereum"))

        // Act
        resultHolder.setConfirmedAddress(validatedAddress)
        advanceUntilIdle()
        resultHolder.setConfirmedAddress(validatedAddress)
        advanceUntilIdle()

        // Assert
        assertThat(model.state.value.addresses).containsExactly(validatedAddress)
    }

    @Test
    fun `GIVEN predefined address WHEN model created THEN address attached`() = runTest {
        // Arrange
        val predefined = ValidatedAddress(address = "0xABC", networkIds = persistentListOf("ethereum"))

        // Act
        val model = createModel(testScope = this, params = createParams(predefinedAddress = predefined))
        advanceUntilIdle()

        // Assert
        assertThat(model.state.value.addresses).containsExactly(predefined)
    }

    @Test
    fun `GIVEN below address limit WHEN onAddAddressClick THEN click propagated AND no dialog`() = runTest {
        // Arrange
        var addClicked = false
        val model = createModel(testScope = this, params = createParams(onAddAddressClick = { addClicked = true }))
        advanceUntilIdle()

        // Act
        model.state.value.onAddAddressClick()

        // Assert
        assertThat(addClicked).isTrue()
        verify(exactly = 0) { messageSender.send(any<DialogMessage>()) }
    }

    @Test
    fun `GIVEN max addresses reached WHEN onAddAddressClick THEN limit dialog shown AND click not propagated`() =
        runTest {
            // Arrange
            var addClicked = false
            val model = createModel(testScope = this, params = createParams(onAddAddressClick = { addClicked = true }))
            advanceUntilIdle()
            repeat(MAX_ADDRESSES) { index ->
                resultHolder.setConfirmedAddress(
                    ValidatedAddress(address = "0x$index", networkIds = persistentListOf("ethereum")),
                )
                advanceUntilIdle()
            }

            // Act
            model.state.value.onAddAddressClick()

            // Assert
            assertThat(model.state.value.addresses).hasSize(MAX_ADDRESSES)
            assertThat(model.state.value.isAddAddressEnabled).isFalse()
            assertThat(addClicked).isFalse()
            verify { messageSender.send(any<DialogMessage>()) }
        }

    @Test
    fun `GIVEN new contact AND multiple unlocked wallets WHEN created THEN wallet block changeable`() = runTest {
        // Arrange
        val walletA = createWallet(id = "aa", name = "Wallet A")
        val walletB = createWallet(id = "bb", name = "Wallet B")
        setupWallets(wallets = listOf(walletA, walletB), selected = walletA)

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        val block = model.state.value.walletBlock
        assertThat(block.isChangeable).isTrue()
        assertThat(block.walletName).isEqualTo("Wallet A")
    }

    @Test
    fun `GIVEN new contact AND single unlocked wallet WHEN created THEN wallet block not changeable`() = runTest {
        // Arrange
        val walletA = createWallet(id = "aa", name = "Wallet A")
        setupWallets(wallets = listOf(walletA), selected = walletA)

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert
        assertThat(model.state.value.walletBlock.isChangeable).isFalse()
    }

    @Test
    fun `GIVEN duplicate name in selected wallet WHEN name entered THEN name error shown`() = runTest {
        // Arrange
        val walletA = createWallet(id = "aa", name = "Wallet A")
        setupWallets(wallets = listOf(walletA), selected = walletA)
        coEvery {
            validateContactNameUseCase(any(), any())
        } returns ContactNameValidationError.Duplicate.left()
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.state.value.onNameChange("Satoshi")
        advanceUntilIdle()

        // Assert
        assertThat(model.state.value.nameError)
            .isEqualTo(resourceReference(R.string.address_book_name_taken_error))
    }

    @Test
    fun `GIVEN unique name in selected wallet WHEN name entered THEN no name error`() = runTest {
        // Arrange
        val walletA = createWallet(id = "aa", name = "Wallet A")
        setupWallets(wallets = listOf(walletA), selected = walletA)
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.state.value.onNameChange("Satoshi")
        advanceUntilIdle()

        // Assert
        assertThat(model.state.value.nameError).isNull()
    }

    @Test
    fun `GIVEN multiple wallets WHEN wallet picked in selector THEN block reflects chosen wallet AND name re-validated`() =
        runTest {
            // Arrange
            val walletA = createWallet(id = "aa", name = "Wallet A")
            val walletB = createWallet(id = "bb", name = "Wallet B")
            setupWallets(wallets = listOf(walletA, walletB), selected = walletA)
            coEvery {
                validateContactNameUseCase(walletB.walletId, "Satoshi")
            } returns ContactNameValidationError.Duplicate.left()
            val model = createModel(testScope = this)
            advanceUntilIdle()
            model.state.value.onNameChange("Satoshi")
            advanceUntilIdle()

            // Act — the reused portfolio selector reports wallet B (wallet-only mode maps to its main account).
            selectedWalletData.tryEmit(walletB to mockk())
            advanceUntilIdle()

            // Assert
            assertThat(model.state.value.walletBlock.walletName).isEqualTo("Wallet B")
            assertThat(model.state.value.nameError)
                .isEqualTo(resourceReference(R.string.address_book_name_taken_error))
        }

    @Test
    fun `GIVEN valid name address and wallet WHEN observed THEN save button enabled`() = runTest {
        // Arrange
        val walletA = createWallet(id = "aa", name = "Wallet A")
        setupWallets(wallets = listOf(walletA), selected = walletA)
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.state.value.onNameChange("Satoshi")
        resultHolder.setConfirmedAddress(ValidatedAddress(address = "0xABC", networkIds = persistentListOf("ethereum")))
        advanceUntilIdle()

        // Assert
        assertThat(model.state.value.saveButton.isEnabled).isTrue()
    }

    @Test
    fun `GIVEN name but no address WHEN observed THEN save button disabled`() = runTest {
        // Arrange
        val walletA = createWallet(id = "aa", name = "Wallet A")
        setupWallets(wallets = listOf(walletA), selected = walletA)
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.state.value.onNameChange("Satoshi")
        advanceUntilIdle()

        // Assert
        assertThat(model.state.value.saveButton.isEnabled).isFalse()
    }

    @Test
    fun `GIVEN name error WHEN observed THEN save button disabled`() = runTest {
        // Arrange
        val walletA = createWallet(id = "aa", name = "Wallet A")
        setupWallets(wallets = listOf(walletA), selected = walletA)
        coEvery { validateContactNameUseCase(any(), any()) } returns ContactNameValidationError.Duplicate.left()
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.state.value.onNameChange("Satoshi")
        resultHolder.setConfirmedAddress(ValidatedAddress(address = "0xABC", networkIds = persistentListOf("ethereum")))
        advanceUntilIdle()

        // Assert
        assertThat(model.state.value.saveButton.isEnabled).isFalse()
    }

    @Test
    fun `GIVEN valid contact WHEN save clicked THEN createContact called AND navigates back`() = runTest {
        // Arrange
        var navigatedBack = false
        val walletA = createWallet(id = "aa", name = "Wallet A")
        setupWallets(wallets = listOf(walletA), selected = walletA)
        coEvery { saveContactInteractor.createContact(any(), any(), any(), any()) } returns mockk<Contact>().right()
        val model = createModel(testScope = this, params = createParams(onBackClick = { navigatedBack = true }))
        advanceUntilIdle()
        model.state.value.onNameChange("Satoshi")
        resultHolder.setConfirmedAddress(ValidatedAddress(address = "0xABC", networkIds = persistentListOf("ethereum")))
        advanceUntilIdle()

        // Act
        model.state.value.saveButton.onClick()
        advanceUntilIdle()

        // Assert
        coVerify {
            saveContactInteractor.createContact(
                userWallet = walletA,
                name = "Satoshi",
                iconColor = any(),
                addressEntries = any(),
            )
        }
        assertThat(navigatedBack).isTrue()
    }

    @Test
    fun `GIVEN save returns name error WHEN save clicked THEN inline name error shown`() = runTest {
        // Arrange
        val walletA = createWallet(id = "aa", name = "Wallet A")
        setupWallets(wallets = listOf(walletA), selected = walletA)
        coEvery { saveContactInteractor.createContact(any(), any(), any(), any()) } returns
            SaveContactError.Name(ContactNameValidationError.Duplicate).left()
        val model = createModel(testScope = this)
        advanceUntilIdle()
        model.state.value.onNameChange("Satoshi")
        resultHolder.setConfirmedAddress(ValidatedAddress(address = "0xABC", networkIds = persistentListOf("ethereum")))
        advanceUntilIdle()

        // Act
        model.state.value.saveButton.onClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.state.value.nameError)
            .isEqualTo(resourceReference(R.string.address_book_name_taken_error))
    }

    private fun createParams(
        contactId: ContactId? = null,
        predefinedAddress: ValidatedAddress? = null,
        onAddAddressClick: () -> Unit = {},
        onBackClick: () -> Unit = {},
    ): DefaultEditContactComponent.Params = DefaultEditContactComponent.Params(
        contactId = contactId,
        predefinedAddress = predefinedAddress,
        onBackClick = onBackClick,
        onAddAddressClick = onAddAddressClick,
    )

    private fun createModel(
        testScope: TestScope,
        params: DefaultEditContactComponent.Params = createParams(),
        paramsContainer: ParamsContainer = MutableParamsContainer(value = params),
    ): EditContactModel {
        return EditContactModel(
            paramsContainer = paramsContainer,
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            stateController = EditContactStateController(),
            resultHolder = resultHolder,
            messageSender = messageSender,
            userWalletsListRepository = userWalletsListRepository,
            validateContactNameUseCase = validateContactNameUseCase,
            saveContactInteractor = saveContactInteractor,
            portfolioSelectorController = portfolioSelectorController,
            portfolioFetcherFactory = portfolioFetcherFactory,
        ).also { model = it }
    }

    private fun setupWallets(wallets: List<UserWallet>, selected: UserWallet?) {
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(wallets)
        every { userWalletsListRepository.selectedUserWallet } returns MutableStateFlow(selected)
    }

    private fun createWallet(id: String, name: String): UserWallet =
        MockUserWalletFactory.create().copy(walletId = UserWalletId(id), name = name)

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
        const val MAX_ADDRESSES = 20
    }
}