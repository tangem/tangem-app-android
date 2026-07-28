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
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.addressbook.error.AddressBookSyncError
import com.tangem.domain.addressbook.error.ContactNameValidationError
import com.tangem.domain.addressbook.error.SaveContactError
import com.tangem.domain.addressbook.interactor.SaveContactInteractor
import com.tangem.domain.addressbook.model.*
import com.tangem.domain.addressbook.usecase.DeleteContactUseCase
import com.tangem.domain.addressbook.usecase.GetContactByIdUseCase
import com.tangem.domain.addressbook.validation.ContactNameValidator
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.addressbook.common.AddressBookAnalyticsSender
import com.tangem.features.addressbook.common.AddressBookResultHolder
import com.tangem.features.addressbook.common.ConfirmedAddress
import com.tangem.features.addressbook.editcontact.DefaultEditContactComponent
import com.tangem.features.addressbook.editcontact.state.EditContactStateController
import com.tangem.features.addressbook.editcontact.ui.state.EditContactUM
import com.tangem.features.addressbook.editcontact.ui.state.ValidatedAddress
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioFetcher
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorController
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
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
    private val contactNameValidator: ContactNameValidator = mockk()
    private val saveContactInteractor: SaveContactInteractor = mockk()
    private val getContactByIdUseCase: GetContactByIdUseCase = mockk()
    private val deleteContactUseCase: DeleteContactUseCase = mockk()
    private val portfolioSelectorController: PortfolioSelectorController = mockk()
    private val portfolioFetcher: PortfolioFetcher = mockk(relaxed = true)
    private val portfolioFetcherFactory: PortfolioFetcher.Factory = mockk()
    private val analyticsSender: AddressBookAnalyticsSender = mockk(relaxed = true)

    // Drives the wallet picked in the reused portfolio selector; `first` of the pair is the chosen wallet.
    private val selectedWalletData =
        MutableSharedFlow<Pair<UserWallet, AccountStatus.CryptoPortfolio>?>(extraBufferCapacity = 1)

    private var model: EditContactModel? = null

    @BeforeEach
    fun setUp() {
        // Default: no wallets loaded, name always valid. Individual tests override as needed.
        setupWallets(wallets = emptyList(), selected = null)
        coEvery { contactNameValidator.validate(any(), any()) } returns ContactName("Satoshi").getOrNull()!!.right()
        every { portfolioFetcherFactory.create(any(), any()) } returns portfolioFetcher
        every { portfolioSelectorController.selectedAccountWithData(any()) } returns selectedWalletData
        // No existing contact by default; a StateFlow<null> never surfaces a contact and never completes.
        every { getContactByIdUseCase(any()) } returns MutableStateFlow<Contact?>(null)
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
            onAddressClick = state.onAddressClick,
            onDeleteClick = null,
        )
        assertThat(state).isEqualTo(expected)
    }

    @Test
    fun `GIVEN editor opened WHEN model created THEN AddContactTapped not sent from editor`() = runTest {
        // The Add-Contact-Tapped funnel event belongs to the "Add contact" button handlers, not the editor screen.
        createModel(testScope = this, params = createParams(contactId = null))
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { analyticsSender.sendAddContactTapped(any(), any()) }
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
        deliverConfirmed(validatedAddress)
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
        deliverConfirmed(validatedAddress)
        advanceUntilIdle()
        deliverConfirmed(validatedAddress)
        advanceUntilIdle()

        // Assert
        assertThat(model.state.value.addresses).containsExactly(validatedAddress)
    }

    @Test
    fun `GIVEN edit-address confirmed with replaces WHEN collected THEN old entry swapped for the new one`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()
        deliverConfirmed(ValidatedAddress(address = "0xOLD", networkIds = persistentListOf("ethereum")))
        advanceUntilIdle()

        // Act — the edit-address flow confirms a new address that supersedes 0xOLD.
        deliverConfirmed(
            address = ValidatedAddress(address = "0xNEW", networkIds = persistentListOf("bsc")),
            replaces = "0xOLD",
        )
        advanceUntilIdle()

        // Assert
        assertThat(model.state.value.addresses.map { it.address }).containsExactly("0xNEW")
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
        val walletA = createWallet(id = "aa", name = "Wallet A")
        setupWallets(wallets = listOf(walletA), selected = walletA)
        val model = createModel(
            testScope = this,
            params = createParams(onAddAddressClick = { _, _, _ -> addClicked = true }),
        )
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
            val model = createModel(
                testScope = this,
                params = createParams(onAddAddressClick = { _, _, _ -> addClicked = true }),
            )
            advanceUntilIdle()
            repeat(MAX_ADDRESSES) { index ->
                deliverConfirmed(
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
    fun `GIVEN changeable wallet block WHEN clicked THEN SaveToButtonClicked sent`() = runTest {
        // Arrange
        val walletA = createWallet(id = "aa", name = "Wallet A")
        val walletB = createWallet(id = "bb", name = "Wallet B")
        setupWallets(wallets = listOf(walletA, walletB), selected = walletA)
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.state.value.walletBlock.onClick()

        // Assert
        verify(exactly = 1) { analyticsSender.sendSaveToButtonClicked() }
    }

    @Test
    fun `GIVEN non-changeable wallet block WHEN clicked THEN SaveToButtonClicked not sent`() = runTest {
        // Arrange
        val walletA = createWallet(id = "aa", name = "Wallet A")
        setupWallets(wallets = listOf(walletA), selected = walletA)
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.state.value.walletBlock.onClick()

        // Assert
        verify(exactly = 0) { analyticsSender.sendSaveToButtonClicked() }
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
    fun `GIVEN cold wallet selected WHEN created THEN save button shows the Tangem logo`() = runTest {
        // Arrange — MockUserWalletFactory builds a cold (card) wallet.
        val coldWallet = createWallet(id = "aa", name = "Wallet A")
        setupWallets(wallets = listOf(coldWallet), selected = coldWallet)

        // Act
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Assert — a cold wallet signs via NFC, so the button carries the Tangem logo.
        assertThat(model.state.value.saveButton.tangemIconUM).isNotNull()
    }

    @Test
    fun `GIVEN duplicate name in selected wallet WHEN name entered THEN name error shown`() = runTest {
        // Arrange
        val walletA = createWallet(id = "aa", name = "Wallet A")
        setupWallets(wallets = listOf(walletA), selected = walletA)
        coEvery {
            contactNameValidator.validate(any(), any())
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
                contactNameValidator.validate(walletB.walletId, "Satoshi")
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
        deliverConfirmed(ValidatedAddress(address = "0xABC", networkIds = persistentListOf("ethereum")))
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
        coEvery { contactNameValidator.validate(any(), any()) } returns ContactNameValidationError.Duplicate.left()
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.state.value.onNameChange("Satoshi")
        deliverConfirmed(ValidatedAddress(address = "0xABC", networkIds = persistentListOf("ethereum")))
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
        val savedContact = mockk<Contact> { every { id } returns ContactId(value = "saved-1") }
        coEvery { saveContactInteractor.createContact(any(), any(), any(), any()) } returns savedContact.right()
        val model = createModel(testScope = this, params = createParams(onBackClick = { navigatedBack = true }))
        advanceUntilIdle()
        model.state.value.onNameChange("Satoshi")
        deliverConfirmed(ValidatedAddress(address = "0xABC", networkIds = persistentListOf("ethereum")))
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
                addresses = any(),
            )
        }
        verify(exactly = 1) {
            analyticsSender.sendContactSaved(walletId = walletA.walletId, contactId = "saved-1", isEdit = false)
        }
        assertThat(navigatedBack).isTrue()
    }

    @Test
    fun `GIVEN save fails WHEN save clicked THEN ContactSaved not sent`() = runTest {
        // Arrange
        val walletA = createWallet(id = "aa", name = "Wallet A")
        setupWallets(wallets = listOf(walletA), selected = walletA)
        coEvery { saveContactInteractor.createContact(any(), any(), any(), any()) } returns
            SaveContactError.Name(ContactNameValidationError.Duplicate).left()
        val model = createModel(testScope = this)
        advanceUntilIdle()
        model.state.value.onNameChange("Satoshi")
        deliverConfirmed(ValidatedAddress(address = "0xABC", networkIds = persistentListOf("ethereum")))
        advanceUntilIdle()

        // Act
        model.state.value.saveButton.onClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { analyticsSender.sendContactSaved(any(), any(), any()) }
    }

    @Test
    fun `GIVEN new contact AND save fails WHEN save clicked THEN SaveErrorShown sent with null contactId`() = runTest {
        // Arrange
        val walletA = createWallet(id = "aa", name = "Wallet A")
        setupWallets(wallets = listOf(walletA), selected = walletA)
        val error = SaveContactError.Backend(AddressBookSyncError.Network)
        coEvery { saveContactInteractor.createContact(any(), any(), any(), any()) } returns error.left()
        val model = createModel(testScope = this, params = createParams(contactId = null))
        advanceUntilIdle()
        model.state.value.onNameChange("Satoshi")
        deliverConfirmed(ValidatedAddress(address = "0xABC", networkIds = persistentListOf("ethereum")))
        advanceUntilIdle()

        // Act
        model.state.value.saveButton.onClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) {
            analyticsSender.sendSaveErrorShown(walletId = walletA.walletId, contactId = null, error = error)
        }
    }

    @Test
    fun `GIVEN existing contact AND save fails WHEN save clicked THEN SaveErrorShown sent with contactId`() = runTest {
        // Arrange
        val walletA = createWallet(id = "aa", name = "Wallet A")
        setupWallets(wallets = listOf(walletA), selected = walletA)
        val error = SaveContactError.Backend(AddressBookSyncError.Network)
        coEvery { saveContactInteractor.createContact(any(), any(), any(), any()) } returns error.left()
        val model = createModel(testScope = this, params = createParams(contactId = ContactId(value = "contact-id")))
        advanceUntilIdle()
        model.state.value.onNameChange("Satoshi")
        deliverConfirmed(ValidatedAddress(address = "0xABC", networkIds = persistentListOf("ethereum")))
        advanceUntilIdle()

        // Act
        model.state.value.saveButton.onClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) {
            analyticsSender.sendSaveErrorShown(walletId = walletA.walletId, contactId = "contact-id", error = error)
        }
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
        deliverConfirmed(ValidatedAddress(address = "0xABC", networkIds = persistentListOf("ethereum")))
        advanceUntilIdle()

        // Act
        model.state.value.saveButton.onClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.state.value.nameError)
            .isEqualTo(resourceReference(R.string.address_book_name_taken_error))
    }

    @Test
    fun `GIVEN save fails with backend error WHEN save clicked THEN button leaves loading AND editor stays`() = runTest {
        // Arrange
        var navigatedBack = false
        val walletA = createWallet(id = "aa", name = "Wallet A")
        setupWallets(wallets = listOf(walletA), selected = walletA)
        coEvery { saveContactInteractor.createContact(any(), any(), any(), any()) } returns
            SaveContactError.Backend(AddressBookSyncError.Network).left()
        val model = createModel(testScope = this, params = createParams(onBackClick = { navigatedBack = true }))
        advanceUntilIdle()
        model.state.value.onNameChange("Satoshi")
        deliverConfirmed(ValidatedAddress(address = "0xABC", networkIds = persistentListOf("ethereum")))
        advanceUntilIdle()

        // Act
        model.state.value.saveButton.onClick()
        advanceUntilIdle()

        // Assert — the failed save must not leave the button spinning, and the editor stays open for a retry.
        assertThat(model.state.value.saveButton.isLoading).isFalse()
        assertThat(navigatedBack).isFalse()
    }

    @Test
    fun `GIVEN existing contact WHEN model created THEN name and addresses prefilled`() = runTest {
        // Arrange
        val walletA = createWallet(id = "aa", name = "Wallet A")
        setupWallets(wallets = listOf(walletA), selected = walletA)
        val contact = existingContact(walletId = "aa", name = "Alice", address = "0xABC")
        every { getContactByIdUseCase(ContactId("c-1")) } returns MutableStateFlow(contact)

        // Act
        val model = createModel(testScope = this, params = createParams(contactId = ContactId("c-1")))
        advanceUntilIdle()

        // Assert
        val state = model.state.value
        assertThat(state.name).isEqualTo("Alice")
        assertThat(state.addresses.map { it.address }).containsExactly("0xABC")
    }

    @Test
    fun `GIVEN existing contact WHEN model created AND nothing changed THEN save button disabled`() = runTest {
        // Arrange
        val walletA = createWallet(id = "aa", name = "Wallet A")
        setupWallets(wallets = listOf(walletA), selected = walletA)
        val contact = existingContact(walletId = "aa", name = "Alice", address = "0xABC")
        every { getContactByIdUseCase(ContactId("c-1")) } returns MutableStateFlow(contact)

        // Act
        val model = createModel(testScope = this, params = createParams(contactId = ContactId("c-1")))
        advanceUntilIdle()

        // Assert — a valid contact that was only opened (not edited) must not be saveable.
        assertThat(model.state.value.saveButton.isEnabled).isFalse()
    }

    @Test
    fun `GIVEN existing contact WHEN name changed THEN save button enabled`() = runTest {
        // Arrange
        val walletA = createWallet(id = "aa", name = "Wallet A")
        setupWallets(wallets = listOf(walletA), selected = walletA)
        val contact = existingContact(walletId = "aa", name = "Alice", address = "0xABC")
        every { getContactByIdUseCase(ContactId("c-1")) } returns MutableStateFlow(contact)
        val model = createModel(testScope = this, params = createParams(contactId = ContactId("c-1")))
        advanceUntilIdle()

        // Act
        model.state.value.onNameChange("Alice edited")
        advanceUntilIdle()

        // Assert
        assertThat(model.state.value.saveButton.isEnabled).isTrue()
    }

    @Test
    fun `GIVEN existing contact WHEN save clicked THEN updateContact called instead of create`() = runTest {
        // Arrange
        val walletA = createWallet(id = "aa", name = "Wallet A")
        setupWallets(wallets = listOf(walletA), selected = walletA)
        val contact = existingContact(walletId = "aa", name = "Alice", address = "0xABC")
        every { getContactByIdUseCase(ContactId("c-1")) } returns MutableStateFlow(contact)
        coEvery { saveContactInteractor.updateContact(any(), any(), any(), any(), any()) } returns contact.right()
        val model = createModel(testScope = this, params = createParams(contactId = ContactId("c-1")))
        advanceUntilIdle()

        // Act
        model.state.value.saveButton.onClick()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { saveContactInteractor.updateContact(walletA, contact, "Alice", any(), any()) }
        coVerify(exactly = 0) { saveContactInteractor.createContact(any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN existing contact AND multiple unlocked wallets WHEN created THEN wallet block changeable`() = runTest {
        // Arrange
        val walletA = createWallet(id = "aa", name = "Wallet A")
        val walletB = createWallet(id = "bb", name = "Wallet B")
        setupWallets(wallets = listOf(walletA, walletB), selected = walletA)
        every { getContactByIdUseCase(ContactId("c-1")) } returns
            MutableStateFlow(existingContact(walletId = "aa", name = "Alice", address = "0xABC"))

        // Act
        val model = createModel(testScope = this, params = createParams(contactId = ContactId("c-1")))
        advanceUntilIdle()

        // Assert — an existing contact can now be moved, so its wallet block is changeable.
        assertThat(model.state.value.walletBlock.isChangeable).isTrue()
    }

    @Test
    fun `GIVEN existing contact AND wallet changed WHEN save clicked THEN moveContact called`() = runTest {
        // Arrange
        val walletA = createWallet(id = "aa", name = "Wallet A")
        val walletB = createWallet(id = "bb", name = "Wallet B")
        setupWallets(wallets = listOf(walletA, walletB), selected = walletA)
        val contact = existingContact(walletId = "aa", name = "Alice", address = "0xABC")
        every { getContactByIdUseCase(ContactId("c-1")) } returns MutableStateFlow(contact)
        val moved = mockk<Contact> { every { id } returns ContactId(value = "moved-1") }
        coEvery { saveContactInteractor.moveContact(any(), any(), any(), any(), any()) } returns moved.right()
        val model = createModel(testScope = this, params = createParams(contactId = ContactId("c-1")))
        advanceUntilIdle()

        // Act — pick wallet B in the selector, then save.
        selectedWalletData.tryEmit(walletB to mockk())
        advanceUntilIdle()
        model.state.value.saveButton.onClick()
        advanceUntilIdle()

        // Assert — the contact is moved to wallet B; plain update/create are not used.
        coVerify(exactly = 1) { saveContactInteractor.moveContact(walletB, contact, "Alice", any(), any()) }
        coVerify(exactly = 0) { saveContactInteractor.updateContact(any(), any(), any(), any(), any()) }
        coVerify(exactly = 0) { saveContactInteractor.createContact(any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN existing contact AND wallet unchanged WHEN save clicked THEN updateContact used not move`() = runTest {
        // Arrange
        val walletA = createWallet(id = "aa", name = "Wallet A")
        val walletB = createWallet(id = "bb", name = "Wallet B")
        setupWallets(wallets = listOf(walletA, walletB), selected = walletA)
        val contact = existingContact(walletId = "aa", name = "Alice", address = "0xABC")
        every { getContactByIdUseCase(ContactId("c-1")) } returns MutableStateFlow(contact)
        coEvery { saveContactInteractor.updateContact(any(), any(), any(), any(), any()) } returns contact.right()
        val model = createModel(testScope = this, params = createParams(contactId = ContactId("c-1")))
        advanceUntilIdle()

        // Act — no wallet pick, so it stays in its own wallet.
        model.state.value.saveButton.onClick()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { saveContactInteractor.updateContact(walletA, contact, "Alice", any(), any()) }
        coVerify(exactly = 0) { saveContactInteractor.moveContact(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `GIVEN new contact saved WHEN success THEN contact-added snackbar shown`() = runTest {
        // Arrange
        val walletA = createWallet(id = "aa", name = "Wallet A")
        setupWallets(wallets = listOf(walletA), selected = walletA)
        // relaxed so the merged analytics call (contact.id.value) doesn't throw before the snackbar is sent.
        coEvery { saveContactInteractor.createContact(any(), any(), any(), any()) } returns
            mockk<Contact>(relaxed = true).right()
        val model = createModel(testScope = this)
        advanceUntilIdle()
        model.state.value.onNameChange("Satoshi")
        deliverConfirmed(ValidatedAddress(address = "0xABC", networkIds = persistentListOf("ethereum")))
        advanceUntilIdle()

        // Act
        model.state.value.saveButton.onClick()
        advanceUntilIdle()

        // Assert
        verify { messageSender.send(any<SnackbarMessage>()) }
    }

    @Test
    fun `GIVEN existing contact WHEN delete confirmed THEN deleteContactUseCase called AND navigates back`() = runTest {
        // Arrange
        var navigatedBack = false
        val walletA = createWallet(id = "aa", name = "Wallet A")
        setupWallets(wallets = listOf(walletA), selected = walletA)
        val contact = existingContact(walletId = "aa", name = "Alice", address = "0xABC")
        every { getContactByIdUseCase(ContactId("c-1")) } returns MutableStateFlow(contact)
        coEvery { deleteContactUseCase(ContactId("c-1")) } returns Unit.right()
        val model = createModel(
            testScope = this,
            params = createParams(contactId = ContactId("c-1"), onBackClick = { navigatedBack = true }),
        )
        advanceUntilIdle()

        // Act — invoke the delete action, then confirm on the captured dialog.
        model.state.value.onDeleteClick?.invoke()
        val dialog = slot<DialogMessage>()
        verify { messageSender.send(capture(dialog)) }
        dialog.captured.firstAction.onClick()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) { deleteContactUseCase(ContactId("c-1")) }
        assertThat(navigatedBack).isTrue()
    }

    @Test
    fun `GIVEN delete fails WHEN confirmed THEN error dialog shown AND stays`() = runTest {
        // Arrange
        var navigatedBack = false
        val walletA = createWallet(id = "aa", name = "Wallet A")
        setupWallets(wallets = listOf(walletA), selected = walletA)
        val contact = existingContact(walletId = "aa", name = "Alice", address = "0xABC")
        every { getContactByIdUseCase(ContactId("c-1")) } returns MutableStateFlow(contact)
        coEvery { deleteContactUseCase(ContactId("c-1")) } returns AddressBookSyncError.Network.left()
        val model = createModel(
            testScope = this,
            params = createParams(contactId = ContactId("c-1"), onBackClick = { navigatedBack = true }),
        )
        advanceUntilIdle()

        // Act
        model.state.value.onDeleteClick?.invoke()
        val dialog = slot<DialogMessage>()
        verify { messageSender.send(capture(dialog)) }
        dialog.captured.firstAction.onClick()
        advanceUntilIdle()

        // Assert
        assertThat(navigatedBack).isFalse()
        // Two dialogs sent: the confirmation and the error.
        verify(atLeast = 2) { messageSender.send(any<DialogMessage>()) }
    }

    @Test
    fun `GIVEN unchanged new contact WHEN close clicked THEN navigates back without dialog`() = runTest {
        // Arrange
        val walletA = createWallet(id = "aa", name = "Wallet A")
        setupWallets(wallets = listOf(walletA), selected = walletA)
        var navigatedBack = false
        val model = createModel(testScope = this, params = createParams(onBackClick = { navigatedBack = true }))
        advanceUntilIdle()

        // Act
        model.state.value.onCloseClick()

        // Assert
        assertThat(navigatedBack).isTrue()
        verify(exactly = 0) { messageSender.send(any<DialogMessage>()) }
    }

    @Test
    fun `GIVEN edited name WHEN close clicked THEN discard dialog shown AND not navigated`() = runTest {
        // Arrange
        val walletA = createWallet(id = "aa", name = "Wallet A")
        setupWallets(wallets = listOf(walletA), selected = walletA)
        var navigatedBack = false
        val model = createModel(testScope = this, params = createParams(onBackClick = { navigatedBack = true }))
        advanceUntilIdle()
        model.state.value.onNameChange("Satoshi")
        advanceUntilIdle()

        // Act
        model.state.value.onCloseClick()

        // Assert
        assertThat(navigatedBack).isFalse()
        verify { messageSender.send(any<DialogMessage>()) }
    }

    @Test
    fun `GIVEN existing contact with one address WHEN it is deleted THEN contact deletion is offered not silent removal`() =
        runTest {
            // Arrange
            val walletA = createWallet(id = "aa", name = "Wallet A")
            setupWallets(wallets = listOf(walletA), selected = walletA)
            val contact = existingContact(walletId = "aa", name = "Alice", address = "0xABC")
            every { getContactByIdUseCase(ContactId("c-1")) } returns MutableStateFlow(contact)
            coEvery { deleteContactUseCase(ContactId("c-1")) } returns Unit.right()
            val model = createModel(testScope = this, params = createParams(contactId = ContactId("c-1")))
            advanceUntilIdle()

            // Act — delete the only address from the address-info sheet.
            model.createAddressInfoParams("0xABC").onDeleteAddress()
            advanceUntilIdle()

            // Assert — not silently removed; a confirmation is shown, and confirming deletes the whole contact.
            assertThat(model.state.value.addresses.map { it.address }).containsExactly("0xABC")
            val dialog = slot<DialogMessage>()
            verify { messageSender.send(capture(dialog)) }
            dialog.captured.firstAction.onClick()
            advanceUntilIdle()
            coVerify(exactly = 1) { deleteContactUseCase(ContactId("c-1")) }
        }

    @Test
    fun `GIVEN existing contact with several addresses WHEN one is deleted THEN it is removed and the rest kept`() =
        runTest {
            // Arrange
            val walletA = createWallet(id = "aa", name = "Wallet A")
            setupWallets(wallets = listOf(walletA), selected = walletA)
            val contact = existingContact(walletId = "aa", name = "Alice", address = "0xAAA").copy(
                addresses = listOf(
                    AddressEntry(
                        id = AddressEntryId("e-1"),
                        address = "0xAAA",
                        networkId = Network.RawID("ethereum"),
                        memo = null,
                        signature = "sig",
                    ),
                    AddressEntry(
                        id = AddressEntryId("e-2"),
                        address = "0xBBB",
                        networkId = Network.RawID("bsc"),
                        memo = null,
                        signature = "sig",
                    ),
                ),
            )
            every { getContactByIdUseCase(ContactId("c-1")) } returns MutableStateFlow(contact)
            val model = createModel(testScope = this, params = createParams(contactId = ContactId("c-1")))
            advanceUntilIdle()

            // Act
            model.createAddressInfoParams("0xAAA").onDeleteAddress()
            advanceUntilIdle()

            // Assert — plain removal, no contact-deletion prompt.
            assertThat(model.state.value.addresses.map { it.address }).containsExactly("0xBBB")
            verify(exactly = 0) { messageSender.send(any<DialogMessage>()) }
            coVerify(exactly = 0) { deleteContactUseCase(any()) }
        }

    @Test
    fun `GIVEN new contact with one address WHEN it is deleted THEN removed without a contact-deletion prompt`() =
        runTest {
            // Arrange
            val walletA = createWallet(id = "aa", name = "Wallet A")
            setupWallets(wallets = listOf(walletA), selected = walletA)
            val model = createModel(testScope = this)
            advanceUntilIdle()
            deliverConfirmed(ValidatedAddress(address = "0xABC", networkIds = persistentListOf("ethereum")))
            advanceUntilIdle()

            // Act
            model.createAddressInfoParams("0xABC").onDeleteAddress()
            advanceUntilIdle()

            // Assert — a new (unsaved) contact has nothing to delete, so the address is just removed.
            assertThat(model.state.value.addresses).isEmpty()
            verify(exactly = 0) { messageSender.send(any<DialogMessage>()) }
        }

    @Test
    fun `GIVEN new contact WHEN created THEN ContactScreenOpened sent with empty contactId`() = runTest {
        // Act
        createModel(testScope = this, params = createParams(contactId = null))
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { analyticsSender.sendContactScreenOpened(contactId = "", scope = any()) }
    }

    @Test
    fun `GIVEN existing contact WHEN created THEN ContactScreenOpened sent with contactId`() = runTest {
        // Arrange
        val walletA = createWallet(id = "aa", name = "Wallet A")
        setupWallets(wallets = listOf(walletA), selected = walletA)
        every { getContactByIdUseCase(ContactId("c-1")) } returns
            MutableStateFlow(existingContact(walletId = "aa", name = "Alice", address = "0xABC"))

        // Act
        createModel(testScope = this, params = createParams(contactId = ContactId("c-1")))
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { analyticsSender.sendContactScreenOpened(contactId = "c-1", scope = any()) }
    }

    @Test
    fun `GIVEN duplicate name in selected wallet WHEN name entered THEN DuplicateNameErrorShown sent`() = runTest {
        // Arrange
        val walletA = createWallet(id = "aa", name = "Wallet A")
        setupWallets(wallets = listOf(walletA), selected = walletA)
        coEvery { contactNameValidator.validate(any(), any()) } returns ContactNameValidationError.Duplicate.left()
        val model = createModel(testScope = this, params = createParams(contactId = null))
        advanceUntilIdle()

        // Act
        model.state.value.onNameChange("Satoshi")
        advanceUntilIdle()

        // Assert — create mode reports a null contact id.
        verify(exactly = 1) {
            analyticsSender.sendDuplicateNameErrorShown(walletId = walletA.walletId, contactId = null)
        }
    }

    @Test
    fun `GIVEN unique name WHEN name entered THEN DuplicateNameErrorShown not sent`() = runTest {
        // Arrange
        val walletA = createWallet(id = "aa", name = "Wallet A")
        setupWallets(wallets = listOf(walletA), selected = walletA)
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.state.value.onNameChange("Satoshi")
        advanceUntilIdle()

        // Assert
        verify(exactly = 0) { analyticsSender.sendDuplicateNameErrorShown(any(), any()) }
    }

    @Test
    fun `GIVEN several addresses WHEN one is deleted THEN AddressRemoved sent AND ContactDeleted not sent`() = runTest {
        // Arrange
        val walletA = createWallet(id = "aa", name = "Wallet A")
        setupWallets(wallets = listOf(walletA), selected = walletA)
        every { getContactByIdUseCase(ContactId("c-1")) } returns MutableStateFlow(twoAddressContact())
        val model = createModel(testScope = this, params = createParams(contactId = ContactId("c-1")))
        advanceUntilIdle()

        // Act
        model.createAddressInfoParams("0xAAA").onDeleteAddress()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { analyticsSender.sendAddressRemoved(walletId = walletA.walletId, contactId = "c-1") }
        verify(exactly = 0) { analyticsSender.sendContactDeleted(any(), any()) }
    }

    @Test
    fun `GIVEN last address WHEN deleted AND confirmed THEN AddressRemoved then ContactDeleted sent`() = runTest {
        // Arrange
        val walletA = createWallet(id = "aa", name = "Wallet A")
        setupWallets(wallets = listOf(walletA), selected = walletA)
        every { getContactByIdUseCase(ContactId("c-1")) } returns
            MutableStateFlow(existingContact(walletId = "aa", name = "Alice", address = "0xABC"))
        coEvery { deleteContactUseCase(ContactId("c-1")) } returns Unit.right()
        val model = createModel(testScope = this, params = createParams(contactId = ContactId("c-1")))
        advanceUntilIdle()

        // Act — deleting the only address prompts a contact deletion; confirming performs it.
        model.createAddressInfoParams("0xABC").onDeleteAddress()
        val dialog = slot<DialogMessage>()
        verify { messageSender.send(capture(dialog)) }
        dialog.captured.firstAction.onClick()
        advanceUntilIdle()

        // Assert — both events fire: the address removal, then the backend-confirmed deletion.
        verify(exactly = 1) { analyticsSender.sendAddressRemoved(walletId = walletA.walletId, contactId = "c-1") }
        verify(exactly = 1) { analyticsSender.sendContactDeleted(walletId = walletA.walletId, contactId = "c-1") }
    }

    @Test
    fun `GIVEN existing contact WHEN explicitly deleted THEN ContactDeleted sent AND AddressRemoved not sent`() =
        runTest {
            // Arrange
            val walletA = createWallet(id = "aa", name = "Wallet A")
            setupWallets(wallets = listOf(walletA), selected = walletA)
            every { getContactByIdUseCase(ContactId("c-1")) } returns
                MutableStateFlow(existingContact(walletId = "aa", name = "Alice", address = "0xABC"))
            coEvery { deleteContactUseCase(ContactId("c-1")) } returns Unit.right()
            val model = createModel(testScope = this, params = createParams(contactId = ContactId("c-1")))
            advanceUntilIdle()

            // Act
            model.state.value.onDeleteClick?.invoke()
            val dialog = slot<DialogMessage>()
            verify { messageSender.send(capture(dialog)) }
            dialog.captured.firstAction.onClick()
            advanceUntilIdle()

            // Assert — explicit deletion is not an address removal.
            verify(exactly = 1) { analyticsSender.sendContactDeleted(walletId = walletA.walletId, contactId = "c-1") }
            verify(exactly = 0) { analyticsSender.sendAddressRemoved(any(), any()) }
        }

    private fun twoAddressContact(): Contact = existingContact(walletId = "aa", name = "Alice", address = "0xAAA").copy(
        addresses = listOf(
            AddressEntry(
                id = AddressEntryId("e-1"),
                address = "0xAAA",
                networkId = Network.RawID("ethereum"),
                memo = null,
                signature = "sig",
            ),
            AddressEntry(
                id = AddressEntryId("e-2"),
                address = "0xBBB",
                networkId = Network.RawID("bsc"),
                memo = null,
                signature = "sig",
            ),
        ),
    )

    private fun existingContact(walletId: String, name: String, address: String): Contact = Contact(
        id = ContactId("c-1"),
        walletId = UserWalletId(walletId),
        name = requireNotNull(ContactName(name).getOrNull()),
        icon = "",
        iconColor = CryptoPortfolioIcon.Color.Azure.name,
        createdAt = "2026-01-01T00:00:00.000Z",
        updatedAt = "2026-01-01T00:00:00.000Z",
        addresses = listOf(
            AddressEntry(
                id = AddressEntryId("e-1"),
                address = address,
                networkId = Network.RawID("ethereum"),
                memo = null,
                signature = "sig",
            ),
        ),
    )

    private fun createParams(
        contactId: ContactId? = null,
        predefinedAddress: ValidatedAddress? = null,
        onAddAddressClick: (String, String?, ValidatedAddress?) -> Unit = { _, _, _ -> },
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
            contactNameValidator = contactNameValidator,
            saveContactInteractor = saveContactInteractor,
            getContactByIdUseCase = getContactByIdUseCase,
            deleteContactUseCase = deleteContactUseCase,
            analyticsSender = analyticsSender,
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

    /** Mirrors what the AddAddress screen delivers back through the result holder. */
    private fun deliverConfirmed(address: ValidatedAddress, replaces: String? = null) {
        resultHolder.setConfirmedAddress(ConfirmedAddress(address = address, replaces = replaces))
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
        const val MAX_ADDRESSES = 20
    }
}