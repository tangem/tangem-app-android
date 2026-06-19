package com.tangem.features.addressbook.addaddress.model

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.R
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.supplier.MultiAccountListSupplier
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.addressbook.addaddress.DefaultAddAddressComponent
import com.tangem.features.addressbook.addaddress.state.AddAddressStateController
import com.tangem.features.addressbook.editcontact.ui.state.ValidatedAddress
import com.tangem.test.mock.MockAccounts
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.*

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AddAddressModelTest {

    private val multiAccountListSupplier: MultiAccountListSupplier = mockk()
    private val clipboardManager: ClipboardManager = mockk()

    private val cryptoCurrencyFactory = MockCryptoCurrencyFactory()
    private val ethereum = cryptoCurrencyFactory.createCoin(Blockchain.Ethereum)
    private val bitcoin = cryptoCurrencyFactory.createCoin(Blockchain.Bitcoin)

    private var model: AddAddressModel? = null

    @BeforeEach
    fun resetMocks() {
        clearMocks(multiAccountListSupplier, clipboardManager)
        // Default: no accounts, so no coins are available unless a test overrides it.
        every { multiAccountListSupplier.invoke() } returns flowOf(emptyList())
    }

    @AfterEach
    fun tearDown() {
        // Cancels modelScope, stopping the long-lived availableCoins / address-input collectors.
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

        // validateAndConfirm() is an unimplemented seam — the button click must NOT emit a result yet.
        @Test
        fun `GIVEN typed address WHEN button clicked THEN onConfirm not called yet`() = runTest {
            // Arrange
            var confirmed: ValidatedAddress? = null
            val model = createModel(testScope = this, onConfirm = { confirmed = it })
            model.state.value.onAddressChange("0xABC")

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
        fun `GIVEN coins available WHEN valid address typed THEN no error AND button enabled`() = runTest {
            // Arrange
            every { multiAccountListSupplier.invoke() } returns flowOf(listOf(accountListWith(ethereum, bitcoin)))
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.state.value.onAddressChange(VALID_ETH_ADDRESS)
            advanceUntilIdle()

            // Assert
            val state = model.state.value
            assertThat(state.addressField.isError).isFalse()
            assertThat(state.buttonUM.isEnabled).isTrue()
        }

        @Test
        fun `GIVEN coins available WHEN address matches no network THEN error AND button disabled`() = runTest {
            // Arrange
            every { multiAccountListSupplier.invoke() } returns flowOf(listOf(accountListWith(ethereum, bitcoin)))
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.state.value.onAddressChange("not-an-address")
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
            every { multiAccountListSupplier.invoke() } returns flowOf(listOf(accountListWith(ethereum)))
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

        // The address is typed before coins load; validity must resolve reactively once the supplier emits them.
        @Test
        fun `GIVEN address typed before coins load WHEN coins emitted THEN validated reactively`() = runTest {
            // Arrange
            val accountsFlow = MutableStateFlow<List<AccountList>>(emptyList())
            every { multiAccountListSupplier.invoke() } returns accountsFlow
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act — type while coins are still empty
            model.state.value.onAddressChange(VALID_ETH_ADDRESS)
            advanceUntilIdle()
            // Assert intermediate: nothing to match yet
            assertThat(model.state.value.buttonUM.isEnabled).isFalse()

            // Act — coins arrive later
            accountsFlow.value = listOf(accountListWith(ethereum, bitcoin))
            advanceUntilIdle()

            // Assert
            val state = model.state.value
            assertThat(state.buttonUM.isEnabled).isTrue()
            assertThat(state.addressField.isError).isFalse()
        }
    }

    private fun accountListWith(vararg currencies: CryptoCurrency): AccountList {
        val walletId = MockAccounts.userWalletId
        val accounts = listOf(
            Account.CryptoPortfolio.createMainAccount(
                userWalletId = walletId,
                cryptoCurrencies = currencies.toList(),
            ),
        )
        return AccountList(
            userWalletId = walletId,
            accounts = accounts,
            totalAccounts = accounts.size,
            totalArchivedAccounts = 0,
        ).getOrNull()!!
    }

    private fun createModel(
        testScope: TestScope,
        onConfirm: (ValidatedAddress) -> Unit = {},
        params: DefaultAddAddressComponent.Params = DefaultAddAddressComponent.Params(
            onBackClick = {},
            onConfirm = onConfirm,
        ),
        paramsContainer: ParamsContainer = MutableParamsContainer(value = params),
    ): AddAddressModel {
        return AddAddressModel(
            paramsContainer = paramsContainer,
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            multiAccountListSupplier = multiAccountListSupplier,
            clipboardManager = clipboardManager,
            stateController = AddAddressStateController(),
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
        // EIP-55 checksummed address from the spec — guaranteed to pass Ethereum validation.
        const val VALID_ETH_ADDRESS = "0x5aAeb6053F3E94C9b9A09f33669435E7Ef1BeAed"
    }
}