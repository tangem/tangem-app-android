package com.tangem.features.onramp.alloffers.model

import arrow.core.left
import arrow.core.right
import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.domain.onramp.GetOnrampAllOffersUseCase
import com.tangem.domain.onramp.model.OnrampPaymentMethodGroup
import com.tangem.domain.onramp.model.error.OnrampError
import com.tangem.features.onramp.OnrampModelTestBase
import com.tangem.features.onramp.alloffers.AllOffersComponent
import com.tangem.features.onramp.alloffers.entity.AllOffersStateUM
import com.tangem.features.onramp.main.entity.OnrampOfferAdvantagesUM
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class AllOffersModelTest : OnrampModelTestBase() {
    @Test
    fun `GIVEN active failed source WHEN refreshed THEN old source cancelled and replacement content retained`() =
        runTest {
            // Arrange
            val old = MutableStateFlow<Either<OnrampError, List<OnrampPaymentMethodGroup>>>(
                OnrampError.DomainError("offline").left(),
            )
            val replacement = MutableStateFlow<Either<OnrampError, List<OnrampPaymentMethodGroup>>>(
                emptyList<OnrampPaymentMethodGroup>().right(),
            )
            val offers = mockk<GetOnrampAllOffersUseCase>()
            every { offers(any(), any()) } returnsMany listOf(old, replacement)
            val model = AllOffersModel(
                testDispatchers(), analyticsEventHandler, offers, mockk(), mockk(),
                MutableParamsContainer(mockk<AllOffersComponent.Params>(relaxed = true)),
            )
            advanceUntilIdle()
            assertThat(old.subscriptionCount.value).isEqualTo(1)

            // Act
            model.onRefresh()
            advanceUntilIdle()
            val content = model.state.value
            old.value = OnrampError.DomainError("late old error").left()
            advanceUntilIdle()

            // Assert
            assertThat(content).isInstanceOf(AllOffersStateUM.Content::class.java)
            assertThat(model.state.value).isEqualTo(content)
            assertThat(old.subscriptionCount.value).isEqualTo(0)
            assertThat(replacement.subscriptionCount.value).isEqualTo(1)
            model.onDestroy()
            advanceUntilIdle()
            assertThat(replacement.subscriptionCount.value).isEqualTo(0)
        }

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