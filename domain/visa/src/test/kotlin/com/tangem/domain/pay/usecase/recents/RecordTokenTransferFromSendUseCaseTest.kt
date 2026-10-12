package com.tangem.domain.pay.usecase.recents

import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.account.PaymentNetworkStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.pay.repository.PaymentAccountRecentsStore
import com.tangem.test.mock.MockAccounts
import io.mockk.clearMocks
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class RecordTokenTransferFromSendUseCaseTest {

    private val recentsStore: PaymentAccountRecentsStore = mockk(relaxed = true)
    private val paymentAccountStatusSupplier: PaymentAccountStatusSupplier = mockk()

    private val useCase = RecordTokenTransferFromSendUseCase(
        recentsStore = recentsStore,
        paymentAccountStatusSupplier = paymentAccountStatusSupplier,
    )

    private val polygon = Network.RawID("polygon-pos")
    private val tron = Network.RawID("tron")

    @BeforeEach
    fun resetMocks() {
        clearMocks(recentsStore, paymentAccountStatusSupplier)
    }

    @Test
    fun `GIVEN the address and the network match WHEN invoke THEN the source is recorded`() = runTest {
        // Arrange
        val account = MockAccounts.createAccount(derivationIndex = 1, userWalletId = WALLET)
        val currency = currency(polygon)
        stubLoaded(listOf(availableNetwork(polygon, DEPOSIT_ADDRESS)))

        // Act
        useCase(
            userWalletId = WALLET,
            sourceAccount = account,
            currency = currency,
            destinationAddress = DEPOSIT_ADDRESS,
        )

        // Assert
        val expectedCurrencyId = currency.id
        coVerify(exactly = 1) {
            recentsStore.markUsed(userWalletId = WALLET, accountId = account.accountId, currencyId = expectedCurrencyId)
        }
    }

    @Test
    fun `GIVEN the address matches on another network WHEN invoke THEN nothing is recorded`() = runTest {
        // Arrange
        stubLoaded(listOf(availableNetwork(tron, DEPOSIT_ADDRESS)))

        // Act
        useCase(
            userWalletId = WALLET,
            sourceAccount = MockAccounts.createAccount(derivationIndex = 1, userWalletId = WALLET),
            currency = currency(polygon),
            destinationAddress = DEPOSIT_ADDRESS,
        )

        // Assert
        coVerify(exactly = 0) { recentsStore.markUsed(any(), any(), any()) }
    }

    @Test
    fun `GIVEN the address differs only in case WHEN invoke THEN the source is recorded`() = runTest {
        // Arrange
        val account = MockAccounts.createAccount(derivationIndex = 1, userWalletId = WALLET)
        val currency = currency(polygon)
        stubLoaded(listOf(availableNetwork(polygon, DEPOSIT_ADDRESS.uppercase())))

        // Act
        useCase(
            userWalletId = WALLET,
            sourceAccount = account,
            currency = currency,
            destinationAddress = DEPOSIT_ADDRESS,
        )

        // Assert
        val expectedCurrencyId = currency.id
        coVerify(exactly = 1) {
            recentsStore.markUsed(userWalletId = WALLET, accountId = account.accountId, currencyId = expectedCurrencyId)
        }
    }

    @Test
    fun `GIVEN the source is not a crypto portfolio WHEN invoke THEN nothing is recorded`() = runTest {
        // Arrange
        stubLoaded(listOf(availableNetwork(polygon, DEPOSIT_ADDRESS)))

        // Act
        useCase(
            userWalletId = WALLET,
            sourceAccount = Account.Payment(WALLET),
            currency = currency(polygon),
            destinationAddress = DEPOSIT_ADDRESS,
        )

        // Assert
        coVerify(exactly = 0) { recentsStore.markUsed(any(), any(), any()) }
    }

    @Test
    fun `GIVEN the payment account status is not loaded WHEN invoke THEN nothing is recorded`() = runTest {
        // Arrange
        val status = AccountStatus.Payment(
            account = Account.Payment(WALLET),
            value = PaymentAccountStatusValue.Loading,
        )
        every { paymentAccountStatusSupplier(WALLET) } returns flowOf(status)

        // Act
        useCase(
            userWalletId = WALLET,
            sourceAccount = MockAccounts.createAccount(derivationIndex = 1, userWalletId = WALLET),
            currency = currency(polygon),
            destinationAddress = DEPOSIT_ADDRESS,
        )

        // Assert
        coVerify(exactly = 0) { recentsStore.markUsed(any(), any(), any()) }
    }

    private fun stubLoaded(networks: List<PaymentNetworkStatus>) {
        val value: PaymentAccountStatusValue.Loaded = mockk(relaxed = true) {
            every { this@mockk.networks } returns networks
        }
        val status = AccountStatus.Payment(account = Account.Payment(WALLET), value = value)
        every { paymentAccountStatusSupplier(WALLET) } returns flowOf(status)
    }

    private fun availableNetwork(rawId: Network.RawID, depositAddress: String): PaymentNetworkStatus.Available =
        mockk(relaxed = true) {
            every { network } returns mockk(relaxed = true) {
                every { id } returns mockk(relaxed = true) { every { this@mockk.rawId } returns rawId }
            }
            every { this@mockk.depositAddress } returns depositAddress
        }

    private fun currency(rawId: Network.RawID): CryptoCurrency {
        val id: CryptoCurrency.ID = mockk(relaxed = true)
        return mockk(relaxed = true) {
            every { this@mockk.id } returns id
            every { network } returns mockk(relaxed = true) {
                every { this@mockk.id } returns mockk(relaxed = true) { every { this@mockk.rawId } returns rawId }
            }
        }
    }

    private companion object {
        val WALLET = UserWalletId("0123456789abcdef")
        const val DEPOSIT_ADDRESS = "0xdeposit"
    }
}