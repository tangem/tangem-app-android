package com.tangem.features.tangempay.card.details

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.pay.TangemPayCard
import com.tangem.domain.models.pay.TangemPayCardFrozenState
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.pay.repository.TangemPayCardDetailsRepository
import com.tangem.features.tangempay.tangemPayCard
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class TangemPayCardPageModelTest {

    private val userWalletId = UserWalletId("123")
    private val firstCard = tangemPayCard(id = "card_a")
    private val secondCard = tangemPayCard(id = "card_b")

    private val eventListener = DefaultCardDetailsEventListener()
    private val statusFlow = MutableSharedFlow<AccountStatus.Payment>(replay = 1)
    private val supplier: PaymentAccountStatusSupplier = mockk()
    private val cardDetailsRepository: TangemPayCardDetailsRepository = mockk(relaxed = true)
    private val controllerFactory: TangemPayCardDetailsController.Factory = mockk()

    private val loadedStatus: PaymentAccountStatusValue.Loaded = mockk(relaxed = true) {
        every { source } returns StatusSource.ACTUAL
        every { cards } returns listOf(firstCard, secondCard)
    }

    private val paymentStatus: AccountStatus.Payment = mockk(relaxed = true) {
        every { account } returns Account.Payment(userWalletId)
        every { value } returns loadedStatus
    }

    init {
        every { supplier.invoke(userWalletId) } returns statusFlow
        every { cardDetailsRepository.cardFrozenState(any()) } returns emptyFlow()
        coEvery { cardDetailsRepository.isAddToWalletDone(any()) } returns true.right()
        every {
            controllerFactory.create(any(), any(), any(), any(), any(), any())
        } answers {
            val card = secondArg<TangemPayCard>()
            mockk(relaxed = true) { every { cardId } returns card.id }
        }
    }

    @Test
    fun `GIVEN details shown WHEN pager moves to another card THEN details row is enabled`() = runTest {
        // Arrange
        val model = createModel()
        statusFlow.emit(paymentStatus)
        eventListener.send(CardDetailsEvent.Show(firstCard.id))
        runCurrent()

        // Act
        model.onCardPageSelected(index = 1)
        runCurrent()

        // Assert
        assertThat(model.isDetailsSettingEnabled()).isTrue()
    }

    @Test
    fun `GIVEN details shown WHEN pager returns to that card THEN details row stays disabled`() = runTest {
        // Arrange
        val model = createModel()
        statusFlow.emit(paymentStatus)
        eventListener.send(CardDetailsEvent.Show(firstCard.id))
        runCurrent()

        // Act
        model.onCardPageSelected(index = 1)
        runCurrent()
        model.onCardPageSelected(index = 0)
        runCurrent()

        // Assert
        assertThat(model.isDetailsSettingEnabled()).isFalse()
    }

    @Test
    fun `GIVEN details shown WHEN frozen state re-emitted THEN details row stays disabled`() = runTest {
        // Arrange
        val frozenStateFlow = MutableSharedFlow<TangemPayCardFrozenState>(replay = 1)
        every { cardDetailsRepository.cardFrozenState(firstCard.id) } returns frozenStateFlow
        val model = createModel()
        statusFlow.emit(paymentStatus)
        eventListener.send(CardDetailsEvent.Show(firstCard.id))
        runCurrent()

        // Act
        frozenStateFlow.emit(TangemPayCardFrozenState.Unfrozen)
        runCurrent()

        // Assert
        assertThat(model.isDetailsSettingEnabled()).isFalse()
    }

    @Test
    fun `GIVEN details hidden WHEN status emitted THEN details row is enabled`() = runTest {
        // Arrange
        val model = createModel()

        // Act
        statusFlow.emit(paymentStatus)
        runCurrent()

        // Assert
        assertThat(model.isDetailsSettingEnabled()).isTrue()
    }

    private fun TangemPayCardPageModel.isDetailsSettingEnabled(): Boolean =
        uiState.value.settings.first { it.id == TangemPayCardPageSetting.Id.Details }.isEnabled

    private fun createModel() = TangemPayCardPageModel(
        paramsContainer = MutableParamsContainer(
            TangemPayCardPageComponent.Params(initialStatus = paymentStatus, cardId = firstCard.id),
        ),
        tangemPayCurrencyFactory = mockk(relaxed = true),
        paymentAccountStatusSupplier = supplier,
        paymentAccountStatusFetcher = mockk(relaxed = true),
        dispatchers = TestingCoroutineDispatcherProvider(),
        router = mockk(relaxed = true),
        analytics = mockk(relaxed = true),
        sendFeedbackEmailUseCase = mockk(relaxed = true),
        cardDetailsRepository = cardDetailsRepository,
        uiMessageSender = mockk(relaxed = true),
        changeCardFrozenStateUseCase = mockk(relaxed = true),
        cardDetailsEventListener = eventListener,
        cardDetailsControllerFactory = controllerFactory,
        biometricAuthManager = mockk(relaxed = true),
        settingsManager = mockk(relaxed = true),
        tangemPayFeatureToggles = mockk(relaxed = true),
        onboardingRepository = mockk(relaxed = true),
    )
}