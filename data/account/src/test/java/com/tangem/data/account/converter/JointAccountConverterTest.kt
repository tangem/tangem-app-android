package com.tangem.data.account.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.data.common.currency.ResponseCryptoCurrenciesFactory
import com.tangem.data.common.currency.UserTokensResponseFactory
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.api.tangemTech.models.account.WalletAccountDTO
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class JointAccountConverterTest {

    private val userWallet = mockk<UserWallet> {
        every { walletId } returns UserWalletId("011")
    }

    private val responseCryptoCurrenciesFactory: ResponseCryptoCurrenciesFactory = mockk()
    private val userTokensResponseFactory: UserTokensResponseFactory = mockk()
    private val converter = JointAccountConverter(
        userWallet = userWallet,
        responseCryptoCurrenciesFactory = responseCryptoCurrenciesFactory,
        userTokensResponseFactory = userTokensResponseFactory,
    )

    private val token = UserTokensResponse.Token(
        networkId = "ethereum",
        derivationPath = "m/44'/60'/888888'/0/2",
        name = "Ethereum",
        symbol = "ETH",
        decimals = 18,
        contractAddress = null,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(responseCryptoCurrenciesFactory, userTokensResponseFactory)
    }

    @Test
    fun `GIVEN joint row with tokens WHEN convert THEN currencies sit on the network at the owner key path`() {
        // Arrange
        val currency = MockCryptoCurrencyFactory().ethereum
        val ownerKeyPath = Network.DerivationPath.Joint(value = "m/44'/60'/888888'/0/2")
        every {
            responseCryptoCurrenciesFactory.createCurrencies(
                tokens = listOf(token),
                userWallet = userWallet,
                derivationPath = ownerKeyPath,
            )
        } returns listOf(currency)

        // Act
        val actual = converter.convert(value = jointRow(derivationIndex = 2, tokens = listOf(token)))

        // Assert
        assertThat(actual.ownerKeyIndex.value).isEqualTo(2)
        assertThat(actual.cryptoCurrencies).containsExactly(currency)
        verify(exactly = 0) {
            responseCryptoCurrenciesFactory.createCurrencies(
                tokens = any(),
                userWallet = any(),
                accountIndex = any<DerivationIndex>(),
            )
        }
    }

    @Test
    fun `GIVEN joint row without tokens WHEN convert THEN account is empty and no network is built`() {
        // Act
        val actual = converter.convert(value = jointRow(derivationIndex = 0, tokens = null))

        // Assert
        assertThat(actual.cryptoCurrencies).isEmpty()
        verify(exactly = 0) {
            responseCryptoCurrenciesFactory.createCurrencies(
                tokens = any(),
                userWallet = any(),
                derivationPath = any<Network.DerivationPath>(),
            )
        }
    }

    private fun jointRow(derivationIndex: Int, tokens: List<UserTokensResponse.Token>?): WalletAccountDTO {
        return createWalletAccountDTO(
            userWalletId = userWallet.walletId,
            accountName = "Family",
            derivationIndex = derivationIndex,
            tokens = tokens,
            type = WalletAccountDTO.Type.JOINT.value,
        )
    }
}