package com.tangem.features.onramp.alloffers.model

import arrow.core.left
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.domain.onramp.GetOnrampAllOffersUseCase
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.features.onramp.OnrampModelTestBase
import com.tangem.features.onramp.alloffers.AllOffersComponent
import com.tangem.features.onramp.alloffers.entity.AllOffersStateUM
import com.tangem.features.onramp.main.entity.OnrampOfferAdvantagesUM
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class AllOffersModelTest : OnrampModelTestBase() {
    @Test
    fun `GIVEN failed offers WHEN refreshed THEN subscription restarts and restricted buy stays inert`() = runTest {
        // Arrange
        val params = mockk<AllOffersComponent.Params>(relaxed = true)
        val offers = mockk<GetOnrampAllOffersUseCase>()
        var subscriptions = 0
        every { offers(any(), any()) } returns flow {
            subscriptions++
            emit(OnrampError.DomainError("offline").left())
        }
        val model = AllOffersModel(
            testDispatchers(), analyticsEventHandler, offers, mockk(), mockk(),
            MutableParamsContainer(params),
        )
        advanceUntilIdle()
        val error = model.state.value

        // Act
        model.onRefresh()
        model.onBuyClick(mockk(), OnrampOfferAdvantagesUM.Restricted)
        advanceUntilIdle()

        // Assert
        assertThat(subscriptions).isEqualTo(2)
        assertThat(error).isInstanceOf(AllOffersStateUM.Error::class.java)
        verify(exactly = 0) { params.openRedirectPage(any()) }
        verify(exactly = 0) { params.onDismiss() }
        model.onDestroy()
    }
}