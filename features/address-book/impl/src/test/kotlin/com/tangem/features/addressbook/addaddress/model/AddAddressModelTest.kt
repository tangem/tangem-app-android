package com.tangem.features.addressbook.addaddress.model

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.ui.extensions.iconResId
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.supplier.MultiAccountListSupplier
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.features.addressbook.addaddress.AddAddressComponent
import com.tangem.features.addressbook.addaddress.contract.AddAddressUM
import com.tangem.features.addressbook.editcontact.contract.ValidatedAddress
import com.tangem.test.mock.MockAccounts
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

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
            assertThat(state.addressField.isValuePasted).isFalse()
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
            val field = model.state.value.addressField
            assertThat(field.value).isEqualTo(address)
            assertThat(field.isValuePasted).isFalse()
        }

        @Test
        fun `GIVEN empty field WHEN onPasteClick THEN value marked as pasted`() = runTest {
            // Arrange
            val model = createModel(testScope = this)
            val address = "0xABC"
            every { clipboardManager.getText() } returns address

            // Act
            model.state.value.onPasteClick()

            // Assert
            val field = model.state.value.addressField
            assertThat(field.value).isEqualTo(address)
            assertThat(field.isValuePasted).isTrue()
        }

        // validateAndConfirm() is an unimplemented seam — the button click must NOT emit a result yet.
        // This guards the foundation and will fail (prompting an update) once validation is wired in.
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
    inner class AddressInput {

        @Test
        fun `GIVEN coins available WHEN valid address typed THEN matching network chosen`() = runTest {
            // Arrange
            every { multiAccountListSupplier.invoke() } returns
                flowOf(listOf(accountListWith(ethereum, bitcoin)))
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.state.value.onAddressChange(VALID_ETH_ADDRESS)
            advanceUntilIdle()

            // Assert
            val state = model.state.value
            assertThat(state.availableNetworks).containsExactly(ethereum.network)
            assertThat(state.chosenNetworkState)
                .isEqualTo(resultOf(ethereum.network))
        }

        @Test
        fun `GIVEN coins available WHEN address matches no network THEN empty state`() = runTest {
            // Arrange
            every { multiAccountListSupplier.invoke() } returns
                flowOf(listOf(accountListWith(ethereum, bitcoin)))
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.state.value.onAddressChange("not-an-address")
            advanceUntilIdle()

            // Assert
            val state = model.state.value
            assertThat(state.availableNetworks).isEmpty()
            assertThat(state.chosenNetworkState).isEqualTo(AddAddressUM.ChosenNetworkState.Empty)
        }

        @Test
        fun `GIVEN no coins available WHEN valid address typed THEN empty state`() = runTest {
            // Arrange — supplier emits no accounts.
            every { multiAccountListSupplier.invoke() } returns flowOf(emptyList())
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act
            model.state.value.onAddressChange(VALID_ETH_ADDRESS)
            advanceUntilIdle()

            // Assert
            val state = model.state.value
            assertThat(state.availableNetworks).isEmpty()
            assertThat(state.chosenNetworkState).isEqualTo(AddAddressUM.ChosenNetworkState.Empty)
        }

        // Covers the "not initialized yet" case: the address is typed before coins load, and the
        // chosen network must resolve reactively once the supplier emits them.
        @Test
        fun `GIVEN address typed before coins load WHEN coins emitted THEN network resolved reactively`() = runTest {
            // Arrange
            val accountsFlow = MutableStateFlow<List<AccountList>>(emptyList())
            every { multiAccountListSupplier.invoke() } returns accountsFlow
            val model = createModel(testScope = this)
            advanceUntilIdle()

            // Act — type while coins are still empty
            model.state.value.onAddressChange(VALID_ETH_ADDRESS)
            advanceUntilIdle()
            // Assert intermediate: nothing to match yet
            assertThat(model.state.value.chosenNetworkState).isEqualTo(AddAddressUM.ChosenNetworkState.Empty)

            // Act — coins arrive later
            accountsFlow.value = listOf(accountListWith(ethereum, bitcoin))
            advanceUntilIdle()

            // Assert
            assertThat(model.state.value.chosenNetworkState)
                .isEqualTo(resultOf(ethereum.network))
        }
    }

    private fun resultOf(vararg networks: Network) = AddAddressUM.ChosenNetworkState.Result(
        networkUMList = networks
            .map { network ->
                AddAddressUM.ChosenNetworkState.Result.NetworkUM(
                    networkName = network.name,
                    iconResId = network.iconResId,
                )
            }
            .toImmutableList(),
    )

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
        params: AddAddressComponent.Params = AddAddressComponent.Params(
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