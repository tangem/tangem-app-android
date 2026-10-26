package com.tangem.domain.yield.supply.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.yield.supply.YieldSupplyRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class YieldSupplyIsAvailableUseCaseTest {

    private val repository: YieldSupplyRepository = mockk()
    private val useCase = YieldSupplyIsAvailableUseCase(repository)

    @Test
    fun `GIVEN a personal token WHEN invoke THEN the repository answers`() = runTest {
        // Arrange
        val token = createToken(derivationPath = Network.DerivationPath.None)
        coEvery { repository.isYieldSupplySupported(WALLET_ID, token) } returns true

        // Act
        val actual = useCase(WALLET_ID, token)

        // Assert
        assertThat(actual).isTrue()
    }

    @Test
    fun `GIVEN a joint account token WHEN invoke THEN yield is unavailable without asking the repository`() = runTest {
        // Arrange
        val token = createToken(derivationPath = Network.DerivationPath.Joint(value = "m/44'/60'/888888'/0/0"))

        // Act
        val actual = useCase(WALLET_ID, token)

        // Assert
        assertThat(actual).isFalse()
        coVerify(exactly = 0) { repository.isYieldSupplySupported(any(), any()) }
    }

    private fun createToken(derivationPath: Network.DerivationPath): CryptoCurrency.Token {
        val network = Network(
            id = Network.ID(value = "ethereum", derivationPath = derivationPath),
            name = "Ethereum",
            currencySymbol = "ETH",
            derivationPath = derivationPath,
            isTestnet = false,
            standardType = Network.StandardType.ERC20,
            hasFiatFeeRate = true,
            canHandleTokens = true,
            transactionExtrasType = Network.TransactionExtrasType.NONE,
            nameResolvingType = Network.NameResolvingType.ENS,
        )

        return CryptoCurrency.Token(
            id = CryptoCurrency.ID(
                prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
                body = CryptoCurrency.ID.Body.NetworkId(network.rawId),
                suffix = CryptoCurrency.ID.Suffix.RawID("tether", "0xContract"),
            ),
            network = network,
            name = "Tether",
            symbol = "USDT",
            decimals = 6,
            iconUrl = null,
            isCustom = false,
            contractAddress = "0xContract",
        )
    }

    private companion object {
        val WALLET_ID = UserWalletId("011")
    }
}