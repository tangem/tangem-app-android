package com.tangem.features.polymarket.impl.main.addfunds.network

import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.TokenReceiveConfig
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PolymarketChooseNetworkModelTest {

    private val onNetworkClick: (TokenReceiveConfig) -> Unit = mockk(relaxed = true)
    private val onDismiss: () -> Unit = mockk(relaxed = true)

    @Test
    fun `GIVEN receive configs WHEN model created THEN each network is listed by name and token in order`() {
        // Arrange
        val configs = listOf(receiveConfig(rawId = "polygon-pos", name = "Polygon"), receiveConfig("solana", "Solana"))

        // Act
        val actual = createModel(configs).uiState.networks.map { it.name to it.symbol }

        // Assert
        assertThat(actual).containsExactly(
            stringReference("Polygon") to stringReference("USDC"),
            stringReference("Solana") to stringReference("USDC"),
        ).inOrder()
    }

    @Test
    fun `GIVEN a listed network WHEN it is clicked THEN its receive config is handed back`() {
        // Arrange
        val solana = receiveConfig(rawId = "solana", name = "Solana")
        val model = createModel(listOf(receiveConfig(rawId = "polygon-pos", name = "Polygon"), solana))

        // Act
        model.uiState.networks.last().onClick()

        // Assert
        verify(exactly = 1) { onNetworkClick(solana) }
    }

    @Test
    fun `GIVEN the sheet WHEN it is dismissed THEN the parent is told`() {
        // Arrange
        val model = createModel(listOf(receiveConfig(rawId = "polygon-pos", name = "Polygon")))

        // Act
        model.uiState.onDismiss()

        // Assert
        verify(exactly = 1) { onDismiss() }
    }

    private fun createModel(configs: List<TokenReceiveConfig>) = PolymarketChooseNetworkModel(
        paramsContainer = MutableParamsContainer(
            PolymarketChooseNetworkComponent.Params(
                receiveConfigs = configs,
                onNetworkClick = onNetworkClick,
                onDismiss = onDismiss,
            ),
        ),
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    private fun receiveConfig(rawId: String, name: String) = TokenReceiveConfig(
        shouldShowWarning = true,
        cryptoCurrency = usdc(rawId = rawId, networkName = name),
        userWalletId = UserWalletId(stringValue = "011"),
        shouldShowMemoDisclaimer = false,
        receiveAddress = emptyList(),
    )

    private fun usdc(rawId: String, networkName: String): CryptoCurrency.Token {
        val network = Network(
            id = Network.ID(value = rawId, derivationPath = Network.DerivationPath.None),
            name = networkName,
            currencySymbol = networkName,
            derivationPath = Network.DerivationPath.None,
            isTestnet = false,
            standardType = Network.StandardType.Unspecified(name = networkName),
            hasFiatFeeRate = true,
            canHandleTokens = true,
            transactionExtrasType = Network.TransactionExtrasType.NONE,
            nameResolvingType = Network.NameResolvingType.NONE,
        )
        return CryptoCurrency.Token(
            id = CryptoCurrency.ID(
                prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
                body = CryptoCurrency.ID.Body.NetworkId(rawId = rawId),
                suffix = CryptoCurrency.ID.Suffix.RawID(rawId = "usd-coin", contractAddress = "0x$rawId"),
            ),
            network = network,
            name = "USDC",
            symbol = "USDC",
            decimals = 6,
            iconUrl = null,
            isCustom = false,
            contractAddress = "0x$rawId",
        )
    }
}