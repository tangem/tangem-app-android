package com.tangem.feature.wallet.presentation.wallet.state.transformers

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.state.model.PromoCardsCheck
import com.tangem.feature.wallet.presentation.wallet.state.model.createWalletContentUM
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SetRefreshStateTransformerTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN refresh state WHEN it changes THEN only a finished refresh makes the promo cards check due`(
        model: RefreshModel,
    ) {
        // Arrange
        val walletUM = createWalletContentUM(isRefreshing = model.wasRefreshing, promoCardsCheck = model.check)
        val transformer = SetRefreshStateTransformer(
            userWalletId = UserWalletId("01"),
            isRefreshing = model.isRefreshing,
        )

        // Act
        val actual = transformer.transform(walletUM)

        // Assert
        val expected = createWalletContentUM(isRefreshing = model.isRefreshing, promoCardsCheck = model.expectedCheck)
        assertThat(actual).isEqualTo(expected)
    }

    private fun provideTestModels() = listOf(
        RefreshModel(wasRefreshing = true, isRefreshing = false, expectedCheck = PromoCardsCheck.AfterRefresh),
        RefreshModel(wasRefreshing = false, isRefreshing = true, expectedCheck = PromoCardsCheck.None),
        RefreshModel(wasRefreshing = false, isRefreshing = false, expectedCheck = PromoCardsCheck.None),
        RefreshModel(wasRefreshing = true, isRefreshing = true, expectedCheck = PromoCardsCheck.None),
        RefreshModel(
            wasRefreshing = true,
            isRefreshing = false,
            check = PromoCardsCheck.OnOpen,
            expectedCheck = PromoCardsCheck.AfterRefresh,
        ),
        RefreshModel(
            wasRefreshing = false,
            isRefreshing = true,
            check = PromoCardsCheck.OnOpen,
            expectedCheck = PromoCardsCheck.OnOpen,
        ),
    )

    data class RefreshModel(
        val wasRefreshing: Boolean,
        val isRefreshing: Boolean,
        val expectedCheck: PromoCardsCheck,
        val check: PromoCardsCheck = PromoCardsCheck.None,
    )
}