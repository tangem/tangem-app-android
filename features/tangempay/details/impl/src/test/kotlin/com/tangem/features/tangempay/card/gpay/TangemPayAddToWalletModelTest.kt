package com.tangem.features.tangempay.card.gpay

import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.tangempay.card.details.CardDetailsEvent
import com.tangem.features.tangempay.card.details.CardDetailsEventListener
import com.tangem.features.tangempay.card.details.TangemPayCardDetailsController
import com.tangem.features.tangempay.tangemPayCard
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class TangemPayAddToWalletModelTest {

    private val card = tangemPayCard(id = "card_a")
    private val cardDetailsEventListener = slot<CardDetailsEventListener>()
    private val cardDetailsControllerFactory: TangemPayCardDetailsController.Factory = mockk {
        every {
            create(any(), any(), any(), any(), capture(cardDetailsEventListener), any())
        } returns mockk(relaxed = true)
    }

    @Test
    fun `GIVEN details shown WHEN screen stopped THEN details hidden`() = runTest {
        // Arrange
        val model = createModel()
        cardDetailsEventListener.captured.send(CardDetailsEvent.Show(card.id))

        // Act
        model.onStop()

        // Assert
        assertThat(cardDetailsEventListener.captured.event.first()).isEqualTo(CardDetailsEvent.HideAll)
    }

    private fun createModel() = TangemPayAddToWalletModel(
        paramsContainer = MutableParamsContainer(
            TangemPayAddToWalletComponent.Params(card = card, userWalletId = UserWalletId("123")),
        ),
        dispatchers = TestingCoroutineDispatcherProvider(),
        router = mockk(relaxed = true),
        googleWalletUtil = mockk(relaxed = true),
        cardDetailsControllerFactory = cardDetailsControllerFactory,
    )
}