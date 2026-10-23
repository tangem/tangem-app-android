package com.tangem.common.ui.tokens

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.ds2.tokenicon.TangemTokenIcon
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class TokenRowStateExtTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ToTokenIconState {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN currency icon state WHEN toTokenIconState THEN matching token icon`(model: TokenIconModel) {
            // Act
            val actual = model.input.toTokenIconState()

            // Assert
            assertThat(actual.withoutIndicator()).isEqualTo(model.expected)
            assertThat(actual.hasIndicator()).isEqualTo(model.hasIndicator)
        }

        private fun provideTestModels() = listOf(
            TokenIconModel(
                input = CurrencyIconState.CoinIcon(
                    url = COIN_URL,
                    fallbackResId = FALLBACK_RES_ID,
                    isGrayscale = false,
                    shouldShowCustomBadge = false,
                ),
                expected = token(url = COIN_URL),
            ),
            TokenIconModel(
                input = CurrencyIconState.TokenIcon(
                    url = TOKEN_URL,
                    topBadgeIconResId = NETWORK_RES_ID,
                    isGrayscale = true,
                    shouldShowCustomBadge = false,
                    fallbackTint = Color.White,
                    fallbackBackground = Color.Black,
                ),
                expected = token(url = TOKEN_URL, topIcon = NETWORK_RES_ID, isGrayscale = true),
            ),
            TokenIconModel(
                input = CurrencyIconState.CustomTokenIcon(
                    tint = Color.White,
                    background = Color.Black,
                    topBadgeIconResId = NETWORK_RES_ID,
                    isGrayscale = false,
                ),
                expected = token(url = null, topIcon = NETWORK_RES_ID),
                hasIndicator = true,
            ),
            TokenIconModel(input = CurrencyIconState.Loading, expected = TangemTokenIcon.UiState.Shimmer),
            TokenIconModel(input = CurrencyIconState.Locked, expected = TangemTokenIcon.UiState.Error),
        )
    }

    private fun token(url: String?, topIcon: Int? = null, isGrayscale: Boolean = false) =
        TangemTokenIcon.UiState.Token(TangemTokenIcon.State(url = url, topIcon = topIcon, isGrayscale = isGrayscale))

    private fun TangemTokenIcon.UiState.withoutIndicator(): TangemTokenIcon.UiState =
        if (this is TangemTokenIcon.UiState.Token) copy(tokenState = tokenState.copy(indicator = null)) else this

    private fun TangemTokenIcon.UiState.hasIndicator(): Boolean =
        (this as? TangemTokenIcon.UiState.Token)?.tokenState?.indicator != null

    data class TokenIconModel(
        val input: CurrencyIconState,
        val expected: TangemTokenIcon.UiState,
        val hasIndicator: Boolean = false,
    )

    private companion object {
        const val COIN_URL = "https://coin.png"
        const val TOKEN_URL = "https://token.png"
        const val FALLBACK_RES_ID = 1
        const val NETWORK_RES_ID = 2
    }
}