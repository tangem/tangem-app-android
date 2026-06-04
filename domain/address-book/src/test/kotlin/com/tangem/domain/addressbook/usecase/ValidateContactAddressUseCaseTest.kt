package com.tangem.domain.addressbook.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.network.CryptoCurrencyAddress
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.GetNetworkAddressesUseCase
import com.tangem.domain.transaction.error.AddressValidation
import com.tangem.domain.transaction.usecase.ValidateWalletAddressUseCase
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ValidateContactAddressUseCaseTest {

    private val validateWalletAddressUseCase: ValidateWalletAddressUseCase = mockk()
    private val getNetworkAddressesUseCase: GetNetworkAddressesUseCase = mockk()
    private val useCase = ValidateContactAddressUseCase(
        validateWalletAddressUseCase = validateWalletAddressUseCase,
        getNetworkAddressesUseCase = getNetworkAddressesUseCase,
    )

    private val walletId = UserWalletId("011")
    private val networkRawId = Network.RawID("ethereum")
    private val networkId = Network.ID(value = "ethereum", derivationPath = Network.DerivationPath.None)
    private val network: Network = mockk { every { id } returns networkId }

    @BeforeEach
    fun resetMocks() {
        clearMocks(validateWalletAddressUseCase, getNetworkAddressesUseCase)
    }

    @Test
    fun `valid address forwards sender addresses and allows self-send`() = runTest {
        val senderAddresses = listOf<CryptoCurrencyAddress>(mockk())
        coEvery { getNetworkAddressesUseCase.invokeSync(walletId, networkRawId) } returns senderAddresses
        coEvery {
            validateWalletAddressUseCase(any(), any(), any(), any<List<CryptoCurrencyAddress>>(), any())
        } returns AddressValidation.Success.Valid.right()

        val result = useCase(walletId, network, "0xabc")

        assertThat(result.isRight()).isTrue()
        coVerify {
            validateWalletAddressUseCase(
                userWalletId = walletId,
                network = network,
                address = "0xabc",
                senderAddresses = senderAddresses,
                allowSelfSend = true,
            )
        }
    }

    @Test
    fun `invalid address propagates the validation error`() = runTest {
        coEvery { getNetworkAddressesUseCase.invokeSync(walletId, networkRawId) } returns emptyList()
        coEvery {
            validateWalletAddressUseCase(any(), any(), any(), any<List<CryptoCurrencyAddress>>(), any())
        } returns AddressValidation.Error.InvalidAddress.left()

        val result = useCase(walletId, network, "bad")

        assertThat(result.leftOrNull()).isEqualTo(AddressValidation.Error.InvalidAddress)
    }
}