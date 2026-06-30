package com.tangem.features.addressbook.addaddress.model

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.R
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.qrscanning.models.SourceType
import com.tangem.domain.qrscanning.usecases.ListenToQrScanningUseCase
import com.tangem.features.addressbook.addaddress.DefaultAddAddressComponent
import com.tangem.features.addressbook.addaddress.state.AddAddressStateController
import com.tangem.features.addressbook.addaddress.ui.state.AddAddressUM.ChosenNetworkStateUM
import com.tangem.features.addressbook.common.AddressMemoValidator
import com.tangem.features.addressbook.common.SelectNetworksResultHolder
import com.tangem.features.addressbook.common.SupportedNetworksMatcher
import com.tangem.features.addressbook.editcontact.ui.state.ValidatedAddress
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.*

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AddAddressModelTest {

    private val supportedNetworksMatcher: SupportedNetworksMatcher = mockk()
    private val memoValidator: AddressMemoValidator = mockk()
    private val clipboardManager: ClipboardManager = mockk()
    private val listenToQrScanningUseCase: ListenToQrScanningUseCase = mockk()
    private val router: Router = mockk(relaxed = true)
    private val selectNetworksResultHolder = SelectNetworksResultHolder()

    private var model: AddAddressModel? = null

    @BeforeEach
    fun resetMocks() {
        clearMocks(supportedNetworksMatcher, memoValidator, clipboardManager, listenToQrScanningUseCase, router)
        selectNetworksResultHolder.clear()
        // Default: an address matches nothing unless a test stubs a specific value.
        every { supportedNetworksMatcher.match(any()) } returns emptyList()
        // Default: any memo passes unless a test stubs an invalid one.
        coEvery { memoValidator.isValid(any(), any()) } returns true
        // Default: no QR results unless a test overrides it.
        every { listenToQrScanningUseCase(SourceType.ADDRESS_BOOK) } returns flowOf<String>().right()
    }

    @AfterEach
    fun tearDown() {
        // Cancels modelScope, stopping the long-lived validation / address-input collectors.
        model?.onDestroy()
        model = null
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class AddressField {

        @Test
        fun `WHEN model created THEN field is empty AND button disabled`() = runTest {
            // Act
            val model = createModel(testScope = this)
            val state = model.state.value

            // Assert
            assertThat(state.addressField.value).isEmpty()
            assertThat(state.buttonUM.isEnabled).isFalse()
        }

        @Test
        fun `GIVEN empty field WHEN onAddressChange THEN value updated`() = runTest {
            // Arrange
            val model = createModel(testScope = this)
            val address = "0xABC"

            // Act
            model.state.value.onAddressChange(address)

            // Assert
            assertThat(model.state.value.addressField.value).isEqualTo(address)
        }

        @Test
        fun `GIVEN empty field WHEN onPasteClick THEN value taken from clipboard`() = runTest {
            // Arrange
            val model = createModel(testScope = this)
            val address = "0xABC"
            every { clipboardManager.getText() } returns address

            // Act
            model.state.value.onPasteClick()

            // Assert
            assertThat(model.state.value.addressField.value).isEqualTo(address)
        }

        // No network matches, so the button is disabled; clicking it must not emit a result.
        @Test
        fun `GIVEN no matching network WHEN button clicked THEN onConfirm not called`() = runTest {
            // Arrange
            var confirmed: ValidatedAddress? = null
            val model = createModel(testScope = this, onConfirm = { confirmed = it })
            model.state.value.onAddressChange("0xABC")
            advanceUntilIdle()

            // Act
            model.state.value.buttonUM.onClick()

            // Assert
            assertThat(confirmed).isNull()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Validation {

        @Test
        fun `GIVEN single matching network WHEN typed THEN no error AND button enabled`() = runTest {
            // Arrange — a single matched network is auto-selected, so the button is enabled right away.
            every { supportedNetworksMatcher.match(ADDRESS) } returns listOf(Blockchain.Ethereum)
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.state.value.onAddressChange(ADDRESS)
            advanceUntilIdle()

            // Assert
            val state = model.state.value
            assertThat(state.addressField.isError).isFalse()
            assertThat(state.buttonUM.isEnabled).isTrue()
        }

        @Test
        fun `GIVEN several matching networks WHEN typed THEN no error but button disabled until selection`() = runTest {
            // Arrange — several matches are shown for context, but none is selected until the user picks explicitly.
            every { supportedNetworksMatcher.match(ADDRESS) } returns listOf(Blockchain.Ethereum, Blockchain.BSC)
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.state.value.onAddressChange(ADDRESS)
            advanceUntilIdle()

            // Assert
            val state = model.state.value
            assertThat(state.addressField.isError).isFalse()
            assertThat(state.buttonUM.isEnabled).isFalse()
        }

        @Test
        fun `GIVEN address matching no network WHEN typed THEN error AND button disabled`() = runTest {
            // Arrange
            every { supportedNetworksMatcher.match(ADDRESS) } returns emptyList()
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.state.value.onAddressChange(ADDRESS)
            advanceUntilIdle()

            // Assert
            val state = model.state.value
            assertThat(state.addressField.isError).isTrue()
            assertThat(state.addressField.label)
                .isEqualTo(resourceReference(R.string.address_book_invalid_address_error))
            assertThat(state.buttonUM.isEnabled).isFalse()
        }

        @Test
        fun `GIVEN empty address WHEN validated THEN no error AND button disabled`() = runTest {
            // Arrange
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.state.value.onAddressChange("")
            advanceUntilIdle()

            // Assert
            val state = model.state.value
            assertThat(state.addressField.isError).isFalse()
            assertThat(state.buttonUM.isEnabled).isFalse()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class NetworkSelector {

        @Test
        fun `GIVEN blank address WHEN validated THEN selector hidden`() = runTest {
            // Act
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Assert
            assertThat(model.state.value.chosenNetworkStateUM).isEqualTo(ChosenNetworkStateUM.Hidden)
        }

        @Test
        fun `GIVEN invalid address WHEN validated THEN selector hidden`() = runTest {
            // Arrange
            every { supportedNetworksMatcher.match(ADDRESS) } returns emptyList()
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.state.value.onAddressChange(ADDRESS)
            advanceUntilIdle()

            // Assert
            assertThat(model.state.value.chosenNetworkStateUM).isEqualTo(ChosenNetworkStateUM.Hidden)
        }

        @Test
        fun `GIVEN address matching several networks WHEN validated THEN all shown AND clickable`() = runTest {
            // Arrange
            every { supportedNetworksMatcher.match(ADDRESS) } returns listOf(Blockchain.Ethereum, Blockchain.BSC)
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.state.value.onAddressChange(ADDRESS)
            advanceUntilIdle()

            // Assert — all matched networks are shown by default; the block opens the selection screen to narrow them.
            val result = model.state.value.chosenNetworkStateUM as ChosenNetworkStateUM.Result
            assertThat(result.networkUMList.map { it.networkName })
                .containsExactly(Blockchain.Ethereum.fullName, Blockchain.BSC.fullName)
            assertThat(result.isClickable).isTrue()
        }

        @Test
        fun `GIVEN address matching a single network WHEN validated THEN selector is not clickable`() = runTest {
            // Arrange
            every { supportedNetworksMatcher.match(ADDRESS) } returns listOf(Blockchain.Bitcoin)
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.state.value.onAddressChange(ADDRESS)
            advanceUntilIdle()

            // Assert
            val result = model.state.value.chosenNetworkStateUM as ChosenNetworkStateUM.Result
            assertThat(result.networkUMList.map { it.networkName }).containsExactly(Blockchain.Bitcoin.fullName)
            assertThat(result.isClickable).isFalse()
        }

        @Test
        fun `GIVEN valid address WHEN onNetworkClick THEN opens selector with address and default selection`() =
            runTest {
                // Arrange
                var openedAddress: String? = null
                var openedSelection: List<String> = listOf("sentinel")
                every { supportedNetworksMatcher.match(ADDRESS) } returns listOf(Blockchain.Ethereum, Blockchain.BSC)
                val model = createModel(
                    testScope = this,
                    onSelectNetworksClick = { address, selection ->
                        openedAddress = address
                        openedSelection = selection
                    },
                )
                advanceUntilIdle()
                model.state.value.onAddressChange(ADDRESS)
                advanceUntilIdle()

                // Act
                model.state.value.onNetworkClick()

                // Assert — empty selection means "nothing selected yet" on the selection screen.
                assertThat(openedAddress).isEqualTo(ADDRESS)
                assertThat(openedSelection).isEmpty()
            }

        @Test
        fun `GIVEN networks chosen via holder WHEN applied THEN selector reflects subset AND confirm uses it`() =
            runTest {
                // Arrange
                var confirmed: ValidatedAddress? = null
                every { supportedNetworksMatcher.match(ADDRESS) } returns listOf(Blockchain.Ethereum, Blockchain.BSC)
                val model = createModel(testScope = this, onConfirm = { confirmed = it })
                advanceUntilIdle()
                model.state.value.onAddressChange(ADDRESS)
                advanceUntilIdle()

                // Act — the user keeps only Ethereum on the network-selection screen.
                selectNetworksResultHolder.setSelectedNetworkIds(setOf(Blockchain.Ethereum.toNetworkId()))
                advanceUntilIdle()

                // Assert — selector shows the subset and the result is consumed.
                val chosen = model.state.value.chosenNetworkStateUM as ChosenNetworkStateUM.Result
                assertThat(chosen.networkUMList.map { it.networkName }).containsExactly(Blockchain.Ethereum.fullName)
                assertThat(selectNetworksResultHolder.selectedNetworkIds.value).isNull()

                // And confirm persists only the kept network.
                model.state.value.buttonUM.onClick()
                assertThat(confirmed).isEqualTo(
                    ValidatedAddress(
                        address = ADDRESS,
                        networkIds = persistentListOf(Blockchain.Ethereum.toNetworkId()),
                    ),
                )
            }

        @Test
        fun `GIVEN non-blank address WHEN typed THEN loading shown AND button blocked until validated`() = runTest {
            // Arrange
            every { supportedNetworksMatcher.match(ADDRESS) } returns listOf(Blockchain.Ethereum, Blockchain.BSC)
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act — typed, but validation is still debounced.
            model.state.value.onAddressChange(ADDRESS)

            // Assert
            assertThat(model.state.value.chosenNetworkStateUM).isEqualTo(ChosenNetworkStateUM.Loading)
            assertThat(model.state.value.buttonUM.isEnabled).isFalse()
        }

        @Test
        fun `GIVEN resolved networks WHEN address edited THEN keeps result without flashing loading`() = runTest {
            // Arrange
            every { supportedNetworksMatcher.match(any()) } returns listOf(Blockchain.Ethereum, Blockchain.BSC)
            val model = createModel(testScope = this)
            model.state.value.onAddressChange(ADDRESS)
            advanceUntilIdle()
            assertThat(model.state.value.chosenNetworkStateUM).isInstanceOf(ChosenNetworkStateUM.Result::class.java)

            // Act — keep typing; validation is pending again.
            model.state.value.onAddressChange(ADDRESS + "00")

            // Assert — the resolved networks stay on screen (no spinner), but the button is blocked while validating.
            assertThat(model.state.value.chosenNetworkStateUM).isInstanceOf(ChosenNetworkStateUM.Result::class.java)
            assertThat(model.state.value.buttonUM.isEnabled).isFalse()
        }

        @Test
        fun `GIVEN single matched network WHEN confirmed THEN it is persisted`() = runTest {
            // Arrange — a single match is auto-selected, so confirm works without opening the selection screen.
            var confirmed: ValidatedAddress? = null
            every { supportedNetworksMatcher.match(ADDRESS) } returns listOf(Blockchain.Ethereum)
            val model = createModel(testScope = this, onConfirm = { confirmed = it })
            advanceUntilIdle()
            model.state.value.onAddressChange(ADDRESS)
            advanceUntilIdle()

            // Act
            model.state.value.buttonUM.onClick()

            // Assert
            assertThat(confirmed).isEqualTo(
                ValidatedAddress(
                    address = ADDRESS,
                    networkIds = persistentListOf(Blockchain.Ethereum.toNetworkId()),
                ),
            )
        }

        @Test
        fun `GIVEN several matched networks AND none selected WHEN confirmed THEN nothing persisted`() = runTest {
            // Arrange
            var confirmed: ValidatedAddress? = null
            every { supportedNetworksMatcher.match(ADDRESS) } returns listOf(Blockchain.Ethereum, Blockchain.BSC)
            val model = createModel(testScope = this, onConfirm = { confirmed = it })
            advanceUntilIdle()
            model.state.value.onAddressChange(ADDRESS)
            advanceUntilIdle()

            // Act — networks are shown but not selected, so confirming is a no-op.
            model.state.value.buttonUM.onClick()

            // Assert
            assertThat(confirmed).isNull()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Memo {

        @Test
        fun `GIVEN address matching an extras network WHEN validated THEN memo field shown`() = runTest {
            // Arrange — XRP supports a destination tag.
            every { supportedNetworksMatcher.match(ADDRESS) } returns listOf(Blockchain.XRP)
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.state.value.onAddressChange(ADDRESS)
            advanceUntilIdle()

            // Assert
            val memoField = model.state.value.memoField
            assertThat(memoField.isVisible).isTrue()
            assertThat(memoField.label).isEqualTo(resourceReference(R.string.send_destination_tag_field))
        }

        @Test
        fun `GIVEN non-extras networks WHEN validated THEN memo field hidden`() = runTest {
            // Arrange
            every { supportedNetworksMatcher.match(ADDRESS) } returns listOf(Blockchain.Ethereum, Blockchain.BSC)
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.state.value.onAddressChange(ADDRESS)
            advanceUntilIdle()

            // Assert
            assertThat(model.state.value.memoField.isVisible).isFalse()
        }

        @Test
        fun `GIVEN extras network and memo entered WHEN confirmed THEN memo included`() = runTest {
            // Arrange
            var confirmed: ValidatedAddress? = null
            every { supportedNetworksMatcher.match(ADDRESS) } returns listOf(Blockchain.XRP)
            val model = createModel(testScope = this, onConfirm = { confirmed = it })
            advanceUntilIdle()
            model.state.value.onAddressChange(ADDRESS)
            advanceUntilIdle()

            // Act
            model.state.value.memoField.onValueChange("123456")
            model.state.value.buttonUM.onClick()

            // Assert
            assertThat(confirmed).isEqualTo(
                ValidatedAddress(
                    address = ADDRESS,
                    networkIds = persistentListOf(Blockchain.XRP.toNetworkId()),
                    memo = "123456",
                ),
            )
        }

        @Test
        fun `GIVEN invalid memo WHEN entered THEN memo error shown AND button blocked`() = runTest {
            // Arrange
            every { supportedNetworksMatcher.match(ADDRESS) } returns listOf(Blockchain.XRP)
            coEvery { memoValidator.isValid(Blockchain.XRP, "bad-tag") } returns false
            val model = createModel(testScope = this)
            advanceUntilIdle()
            model.state.value.onAddressChange(ADDRESS)
            advanceUntilIdle()

            // Act — type a malformed destination tag.
            model.state.value.memoField.onValueChange("bad-tag")
            advanceUntilIdle()

            // Assert
            assertThat(model.state.value.memoField.isError).isTrue()
            assertThat(model.state.value.buttonUM.isEnabled).isFalse()
        }

        @Test
        fun `GIVEN non-extras network WHEN confirmed THEN memo is null`() = runTest {
            // Arrange
            var confirmed: ValidatedAddress? = null
            every { supportedNetworksMatcher.match(ADDRESS) } returns listOf(Blockchain.Ethereum)
            val model = createModel(testScope = this, onConfirm = { confirmed = it })
            advanceUntilIdle()
            model.state.value.onAddressChange(ADDRESS)
            advanceUntilIdle()

            // Act
            model.state.value.buttonUM.onClick()

            // Assert
            assertThat(confirmed?.memo).isNull()
        }

        @Test
        fun `WHEN memo paste clicked THEN clipboard goes into memo and not address`() = runTest {
            // Arrange
            every { clipboardManager.getText() } returns "TAG-123"
            val model = createModel(testScope = this)

            // Act
            model.state.value.memoField.onPasteClick()

            // Assert
            assertThat(model.state.value.memoField.value).isEqualTo("TAG-123")
            assertThat(model.state.value.addressField.value).isEmpty()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class QrScan {

        @Test
        fun `WHEN onQrClick THEN navigates to address-book QR scanning`() = runTest {
            // Arrange
            val model = createModel(testScope = this)

            // Act
            model.state.value.onQrClick()

            // Assert
            verify { router.push(AppRoute.QrScanning(source = AppRoute.QrScanning.Source.AddressBook)) }
        }

        @Test
        fun `GIVEN scanned address WHEN emitted THEN address field updated`() = runTest {
            // Arrange
            every { listenToQrScanningUseCase(SourceType.ADDRESS_BOOK) } returns flowOf(ADDRESS).right()
            val model = createModel(testScope = this)

            // Act
            advanceUntilIdle()

            // Assert
            assertThat(model.state.value.addressField.value).isEqualTo(ADDRESS)
        }

        @Test
        fun `GIVEN scanned payment URI WHEN emitted THEN scheme and query stripped`() = runTest {
            // Arrange
            every { listenToQrScanningUseCase(SourceType.ADDRESS_BOOK) } returns
                flowOf("ethereum:$ADDRESS?amount=1.5").right()
            val model = createModel(testScope = this)

            // Act
            advanceUntilIdle()

            // Assert
            assertThat(model.state.value.addressField.value).isEqualTo(ADDRESS)
        }
    }

    private fun createModel(
        testScope: TestScope,
        onConfirm: (ValidatedAddress) -> Unit = {},
        onSelectNetworksClick: (String, List<String>) -> Unit = { _, _ -> },
        params: DefaultAddAddressComponent.Params = DefaultAddAddressComponent.Params(
            onBackClick = {},
            onSelectNetworksClick = onSelectNetworksClick,
            onConfirm = onConfirm,
        ),
        paramsContainer: ParamsContainer = MutableParamsContainer(value = params),
    ): AddAddressModel {
        return AddAddressModel(
            paramsContainer = paramsContainer,
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            supportedNetworksMatcher = supportedNetworksMatcher,
            memoValidator = memoValidator,
            listenToQrScanningUseCase = listenToQrScanningUseCase,
            clipboardManager = clipboardManager,
            stateController = AddAddressStateController(),
            selectNetworksResultHolder = selectNetworksResultHolder,
            router = router,
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
        const val ADDRESS = "0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed"
    }
}