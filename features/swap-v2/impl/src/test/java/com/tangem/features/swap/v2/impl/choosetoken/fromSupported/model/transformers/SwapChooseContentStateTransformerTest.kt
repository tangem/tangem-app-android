package com.tangem.features.swap.v2.impl.choosetoken.fromSupported.model.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.swap.models.SwapCryptoCurrency
import com.tangem.domain.swap.models.SwapCurrencies
import com.tangem.domain.swap.models.SwapCurrenciesGroup
import com.tangem.features.swap.v2.impl.choosetoken.fromSupported.entity.SwapChooseTokenNetworkContentUM
import com.tangem.features.swap.v2.impl.choosetoken.fromSupported.entity.SwapChooseTokenNetworkUM
import com.tangem.features.swap.v2.impl.choosetoken.fromSupported.model.SwapChooseTokenFactory
import io.mockk.mockk
import org.junit.jupiter.api.Test

internal class SwapChooseContentStateTransformerTest {

    private val tokenName = "Shiba Inu"

    @Test
    fun `GIVEN no available but availableForSwap present WHEN transform THEN content is SwapAvailable`() {
        // Arrange
        val pairs = swapCurrencies(available = emptyList(), availableForSwap = listOf(swapCryptoCurrency()))

        // Act
        val result = transformer(pairs).transform(prevState())

        // Assert
        assertThat(result.bottomSheetConfig.content)
            .isInstanceOf(SwapChooseTokenNetworkContentUM.SwapAvailable::class.java)
    }

    @Test
    fun `GIVEN no available and no availableForSwap WHEN transform THEN content is Error`() {
        // Arrange
        val pairs = swapCurrencies(available = emptyList(), availableForSwap = emptyList())

        // Act
        val result = transformer(pairs).transform(prevState())

        // Assert
        assertThat(result.bottomSheetConfig.content)
            .isInstanceOf(SwapChooseTokenNetworkContentUM.Error::class.java)
    }

    private fun transformer(pairs: SwapCurrencies) = SwapChooseContentStateTransformer(
        pairs = pairs,
        tokenName = tokenName,
        onNetworkClick = { _, _ -> },
        onDismiss = {},
        onSwapClick = {},
    )

    private fun swapCurrencies(
        available: List<SwapCryptoCurrency>,
        availableForSwap: List<SwapCryptoCurrency>,
    ): SwapCurrencies = SwapCurrencies.EMPTY.copy(
        fromGroup = SwapCurrenciesGroup(
            available = available,
            unavailable = emptyList(),
            isAfterSearch = false,
            availableForSwap = availableForSwap,
        ),
    )

    private fun swapCryptoCurrency(): SwapCryptoCurrency = SwapCryptoCurrency(
        currencyStatus = CryptoCurrencyStatus(currency = mockk<CryptoCurrency>(relaxed = true), value = mockk(relaxed = true)),
        providers = emptyList(),
    )

    private fun prevState(): SwapChooseTokenNetworkUM = SwapChooseTokenNetworkUM(
        bottomSheetConfig = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = {},
            content = SwapChooseTokenNetworkContentUM.Loading(
                messageContent = SwapChooseTokenFactory.getErrorMessage(tokenName = tokenName, onDismiss = {}),
            ),
        ),
    )
}