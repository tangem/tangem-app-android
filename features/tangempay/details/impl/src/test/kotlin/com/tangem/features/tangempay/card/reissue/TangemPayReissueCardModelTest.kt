package com.tangem.features.tangempay.card.reissue

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.error.UniversalError
import com.tangem.domain.models.pay.TangemPayReissueCardFee
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.TangemPayCardBalance
import com.tangem.domain.pay.repository.TangemPayCardDetailsRepository
import com.tangem.domain.pay.repository.TangemPayReissueCardRepository
import com.tangem.domain.pay.usecase.ReissueTangemPayCardUseCase
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayReissueCardModelTest {

    private val cardId = "test_card_id"
    private val userWalletId = UserWalletId("123")

    private val listener: ReissueCardListener = mockk(relaxed = true)
    private val cardDetailsRepository: TangemPayCardDetailsRepository = mockk()
    private val reissueCardRepository: TangemPayReissueCardRepository = mockk()
    private val reissueCardUseCase: ReissueTangemPayCardUseCase = mockk()
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)
    private val analytics: AnalyticsEventHandler = mockk(relaxed = true)

    private val params = TangemPayReissueCardComponent.Params(
        listener = listener,
        userWalletId = userWalletId,
        cardId = cardId,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(listener, cardDetailsRepository, reissueCardRepository, reissueCardUseCase, uiMessageSender)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN balance and fee WHEN data loaded THEN confirm button state matches`(model: LoadDataModel) {
        // Arrange
        coEvery { cardDetailsRepository.getCardBalance(userWalletId) } returns model.balance
        coEvery { reissueCardRepository.getReissueCardFee(userWalletId) } returns model.fee

        // Act
        val state = createModel().state.value

        // Assert
        assertThat(state.error).isEqualTo(model.expectedError)
        assertThat(state.isConfirmButtonEnabled).isEqualTo(model.expectedConfirmButtonEnabled)
        assertThat(state.isConfirmButtonLoading).isFalse()
    }

    @Test
    fun `GIVEN data is loading WHEN model created THEN confirm button is disabled`() {
        // Arrange
        coEvery { cardDetailsRepository.getCardBalance(userWalletId) } coAnswers { CompletableDeferred<Nothing>().await() }
        coEvery { reissueCardRepository.getReissueCardFee(userWalletId) } returns fee(amount = "1").right()

        // Act
        val state = createModel().state.value

        // Assert
        assertThat(state.isConfirmButtonEnabled).isFalse()
        assertThat(state.isConfirmButtonLoading).isFalse()
    }

    @Test
    fun `GIVEN sufficient balance WHEN confirm clicked THEN confirm button is loading and disabled`() {
        // Arrange
        coEvery { cardDetailsRepository.getCardBalance(userWalletId) } returns balance(amount = "10").right()
        coEvery { reissueCardRepository.getReissueCardFee(userWalletId) } returns fee(amount = "1").right()
        coEvery { reissueCardUseCase(userWalletId, cardId) } coAnswers { CompletableDeferred<Nothing>().await() }
        val model = createModel()

        // Act
        model.state.value.onConfirmClick()

        // Assert
        val state = model.state.value
        assertThat(state.isConfirmButtonLoading).isTrue()
        assertThat(state.isConfirmButtonEnabled).isFalse()
    }

    private fun createModel() = TangemPayReissueCardModel(
        paramsContainer = MutableParamsContainer(params),
        dispatchers = TestingCoroutineDispatcherProvider(),
        cardDetailsRepository = cardDetailsRepository,
        reissueCardRepository = reissueCardRepository,
        reissueTangemPayCardUseCase = reissueCardUseCase,
        uiMessageSender = uiMessageSender,
        analytics = analytics,
    )

    private fun provideTestModels() = listOf(
        LoadDataModel(
            balance = balance(amount = "10").right(),
            fee = fee(amount = "1").right(),
            expectedError = null,
            expectedConfirmButtonEnabled = true,
        ),
        LoadDataModel(
            balance = balance(amount = "1").right(),
            fee = fee(amount = "1").right(),
            expectedError = null,
            expectedConfirmButtonEnabled = true,
        ),
        LoadDataModel(
            balance = balance(amount = "0.5").right(),
            fee = fee(amount = "1").right(),
            expectedError = TangemPayReissueCardError.InsufficientFunds,
            expectedConfirmButtonEnabled = false,
        ),
        LoadDataModel(
            balance = ERROR.left(),
            fee = fee(amount = "1").right(),
            expectedError = TangemPayReissueCardError.InitialDataLoading,
            expectedConfirmButtonEnabled = true,
        ),
        LoadDataModel(
            balance = balance(amount = "10").right(),
            fee = VisaApiError.Unspecified.left(),
            expectedError = TangemPayReissueCardError.InitialDataLoading,
            expectedConfirmButtonEnabled = true,
        ),
    )

    private fun balance(amount: String) = TangemPayCardBalance(fiatBalance = BigDecimal(amount), currencyCode = "USD")

    private fun fee(amount: String) = TangemPayReissueCardFee(amount = BigDecimal(amount), currencyCode = "USD")

    data class LoadDataModel(
        val balance: Either<UniversalError, TangemPayCardBalance>,
        val fee: Either<VisaApiError, TangemPayReissueCardFee>,
        val expectedError: TangemPayReissueCardError?,
        val expectedConfirmButtonEnabled: Boolean,
    )

    private companion object {
        val ERROR = object : UniversalError {
            override val errorCode: Int = 0
        }
    }
}