package com.tangem.domain.transaction.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.ResolveAddressResult
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.CryptoCurrencyAddress
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.WalletAddressServiceRepository
import com.tangem.domain.transaction.error.AddressValidation
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.lib.crypto.BlockchainUtils
import com.tangem.lib.crypto.models.XrpTaggedAddress
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

internal class ValidateWalletAddressUseCaseTest {

    private val repository: WalletAddressServiceRepository = mockk()
    private val walletManagersFacade: WalletManagersFacade = mockk()
    private val useCase = ValidateWalletAddressUseCase(
        walletAddressServiceRepository = repository,
        walletManagersFacade = walletManagersFacade,
    )

    private val userWalletId: UserWalletId = mockk()
    private val network: Network = mockk()

    @Before
    fun setUp() {
        mockkObject(BlockchainUtils)
        every { BlockchainUtils.decodeRippleXAddress(any(), any()) } returns null
        every { network.rawId } returns "ethereum"
    }

    @After
    fun tearDown() {
        unmockkObject(BlockchainUtils)
    }

    @Test
    fun `GIVEN address not in sender list AND address is valid WHEN invoke THEN returns Valid`() = runTest {
        val address = "0xRecipient"
        val senderAddresses = listOf(senderAddress("0xSender"))

        coEvery { walletManagersFacade.checkSelfSendAvailability(userWalletId, network) } returns false
        coEvery { repository.validateAddress(userWalletId, network, address) } returns true

        val result = useCase(userWalletId, network, address, senderAddresses)

        assertThat(result.getOrNull()).isEqualTo(AddressValidation.Success.Valid)
    }

    @Test
    fun `GIVEN address in sender list AND self-send not available WHEN invoke THEN returns AddressInWallet`() = runTest {
        val address = "0xSender"
        val senderAddresses = listOf(senderAddress(address))

        coEvery { walletManagersFacade.checkSelfSendAvailability(userWalletId, network) } returns false
        coEvery { repository.validateAddress(userWalletId, network, address) } returns true

        val result = useCase(userWalletId, network, address, senderAddresses)

        assertThat(result.leftOrNull()).isEqualTo(AddressValidation.Error.AddressInWallet)
    }

    @Test
    fun `GIVEN address in sender list AND self-send available WHEN invoke THEN returns Valid`() = runTest {
        val address = "0xSender"
        val senderAddresses = listOf(senderAddress(address))

        coEvery { walletManagersFacade.checkSelfSendAvailability(userWalletId, network) } returns true
        coEvery { repository.validateAddress(userWalletId, network, address) } returns true

        val result = useCase(userWalletId, network, address, senderAddresses)

        assertThat(result.getOrNull()).isEqualTo(AddressValidation.Success.Valid)
    }

    @Test
    fun `GIVEN address in sender list AND self-send not available AND allowSelfSend is true WHEN invoke THEN returns Valid`() = runTest {
        val address = "0xSender"
        val senderAddresses = listOf(senderAddress(address))

        coEvery { walletManagersFacade.checkSelfSendAvailability(userWalletId, network) } returns false
        coEvery { repository.validateAddress(userWalletId, network, address) } returns true

        val result = useCase(userWalletId, network, address, senderAddresses, allowSelfSend = true)

        assertThat(result.getOrNull()).isEqualTo(AddressValidation.Success.Valid)
    }

    @Test
    fun `GIVEN invalid address AND resolved as named address WHEN invoke THEN returns ValidNamedAddress`() = runTest {
        val address = "vitalik.eth"
        val resolvedAddress = "0xResolved"
        val senderAddresses = listOf(senderAddress("0xSender"))

        coEvery { walletManagersFacade.checkSelfSendAvailability(userWalletId, network) } returns false
        coEvery { repository.validateAddress(userWalletId, network, address) } returns false
        coEvery { repository.resolveAddress(userWalletId, network, address) } returns
            ResolveAddressResult.Resolved(resolvedAddress)

        val result = useCase(userWalletId, network, address, senderAddresses)

        assertThat(result.getOrNull()).isEqualTo(AddressValidation.Success.ValidNamedAddress(resolvedAddress))
    }

    @Test
    fun `GIVEN invalid address AND not resolved WHEN invoke THEN returns InvalidAddress`() = runTest {
        val address = "invalid_address"
        val senderAddresses = listOf(senderAddress("0xSender"))

        coEvery { walletManagersFacade.checkSelfSendAvailability(userWalletId, network) } returns false
        coEvery { repository.validateAddress(userWalletId, network, address) } returns false
        coEvery { repository.resolveAddress(userWalletId, network, address) } returns
            ResolveAddressResult.NotSupported

        val result = useCase(userWalletId, network, address, senderAddresses)

        assertThat(result.leftOrNull()).isEqualTo(AddressValidation.Error.InvalidAddress)
    }

    @Test
    fun `GIVEN valid XRP X-address WHEN invoke THEN returns ValidXAddress`() = runTest {
        val xAddress = "X7AcgcsBL4L51nv2theWPZRMcGF37HeMBCFMDcaVEEF8Y3q"
        val decodedAddress = "rHb9CJAWyB4rj91VRWn96DkukG4bwdtyTh"
        val senderAddresses = listOf(senderAddress("0xSender"))

        every { BlockchainUtils.decodeRippleXAddress(xAddress, any()) } returns
            XrpTaggedAddress(address = decodedAddress, destinationTag = null)
        coEvery { walletManagersFacade.checkSelfSendAvailability(userWalletId, network) } returns false
        coEvery { repository.validateAddress(userWalletId, network, decodedAddress) } returns true

        val result = useCase(userWalletId, network, xAddress, senderAddresses)

        assertThat(result.getOrNull()).isEqualTo(AddressValidation.Success.ValidXAddress)
    }

    @Test
    fun `GIVEN valid XRP X-address that decodes to sender address AND self-send not available WHEN invoke THEN returns AddressInWallet`() = runTest {
        val xAddress = "X7AcgcsBL4L51nv2theWPZRMcGF37HeMBCFMDcaVEEF8Y3q"
        val decodedAddress = "rSenderAddress"
        val senderAddresses = listOf(senderAddress(decodedAddress))

        every { BlockchainUtils.decodeRippleXAddress(xAddress, any()) } returns
            XrpTaggedAddress(address = decodedAddress, destinationTag = null)
        coEvery { walletManagersFacade.checkSelfSendAvailability(userWalletId, network) } returns false
        coEvery { repository.validateAddress(userWalletId, network, decodedAddress) } returns true

        val result = useCase(userWalletId, network, xAddress, senderAddresses)

        assertThat(result.leftOrNull()).isEqualTo(AddressValidation.Error.AddressInWallet)
    }

    @Test
    fun `GIVEN null currencyAddresses AND self-send not available WHEN invoke THEN returns AddressInWallet`() = runTest {
        val address = "0xSender"

        coEvery { walletManagersFacade.checkSelfSendAvailability(userWalletId, network) } returns false
        coEvery { repository.validateAddress(userWalletId, network, address) } returns true

        val result = useCase(userWalletId, network, address, currencyAddresses = null)

        assertThat(result.leftOrNull()).isEqualTo(AddressValidation.Error.AddressInWallet)
    }

    @Test
    fun `GIVEN currencyAddresses not containing address AND address is valid WHEN invoke THEN returns Valid`() = runTest {
        val address = "0xRecipient"
        val currencyAddresses = setOf(networkAddress("0xSender"))

        coEvery { walletManagersFacade.checkSelfSendAvailability(userWalletId, network) } returns false
        coEvery { repository.validateAddress(userWalletId, network, address) } returns true

        val result = useCase(userWalletId, network, address, currencyAddresses)

        assertThat(result.getOrNull()).isEqualTo(AddressValidation.Success.Valid)
    }

    @Test
    fun `GIVEN currencyAddresses containing address AND self-send not available AND allowSelfSend is true WHEN invoke THEN returns Valid`() = runTest {
        val address = "0xSender"
        val currencyAddresses = setOf(networkAddress(address))

        coEvery { walletManagersFacade.checkSelfSendAvailability(userWalletId, network) } returns false
        coEvery { repository.validateAddress(userWalletId, network, address) } returns true

        val result = useCase(userWalletId, network, address, currencyAddresses, allowSelfSend = true)

        assertThat(result.getOrNull()).isEqualTo(AddressValidation.Success.Valid)
    }

    private fun senderAddress(address: String): CryptoCurrencyAddress =
        CryptoCurrencyAddress(cryptoCurrency = mockk<CryptoCurrency.Coin>(relaxed = true), address = address)

    private fun networkAddress(value: String): NetworkAddress.Address =
        NetworkAddress.Address(value = value, type = NetworkAddress.Address.Type.Primary)
}