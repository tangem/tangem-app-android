package com.tangem.features.tangempay.verified

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.tangempay.TangemPayProduct
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayVerifiedUMFactoryTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN wallet type and products WHEN create THEN tangem icon shown only before a card tap`(
        model: TangemIconModel,
    ) {
        // Arrange
        val factory = TangemPayVerifiedUMFactory(
            onChoosePlanClick = {},
            onOpenMoneyAccountClick = {},
            onCloseClick = {},
            isColdWallet = model.isColdWallet,
        )

        // Act
        val actual = factory.create(model.availableProducts).primaryButton.iconEnd

        // Assert
        assertThat(actual != null).isEqualTo(model.isIconShown)
    }

    private fun provideTestModels() = listOf(
        TangemIconModel(
            isColdWallet = true,
            availableProducts = setOf(TangemPayProduct.BANKING_ACCOUNT),
            isIconShown = true,
        ),
        TangemIconModel(
            isColdWallet = false,
            availableProducts = setOf(TangemPayProduct.BANKING_ACCOUNT),
            isIconShown = false,
        ),
        TangemIconModel(
            isColdWallet = true,
            availableProducts = setOf(TangemPayProduct.BANKING_ACCOUNT, TangemPayProduct.PAYMENT_ACCOUNT),
            isIconShown = false,
        ),
        TangemIconModel(
            isColdWallet = true,
            availableProducts = emptySet(),
            isIconShown = false,
        ),
    )

    data class TangemIconModel(
        val isColdWallet: Boolean,
        val availableProducts: Set<TangemPayProduct>,
        val isIconShown: Boolean,
    )
}