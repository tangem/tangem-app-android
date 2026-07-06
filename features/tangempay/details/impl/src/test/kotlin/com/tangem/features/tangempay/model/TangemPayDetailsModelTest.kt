package com.tangem.features.tangempay.model

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.pay.TangemPayCardFrozenState
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.pay.repository.TangemPayCardDetailsRepository
import com.tangem.features.tangempay.addFundsButton
import com.tangem.features.tangempay.components.TangemPayDetailsContainerComponent
import com.tangem.features.tangempay.entity.TangemPayDetailsBalanceBlockState
import com.tangem.features.tangempay.tangemPayCard
import com.tangem.features.tangempay.withdrawButton
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
internal class TangemPayDetailsModelTest {

    private val userWalletId = UserWalletId("123")

    private val paymentAccountStatusSupplier: PaymentAccountStatusSupplier = mockk()
    private val cardDetailsRepository: TangemPayCardDetailsRepository = mockk(relaxed = true)

    @ParameterizedTest
    @MethodSource("provideFreezeCases")
    fun `GIVEN frozen state and balance WHEN status loaded THEN action buttons gated accordingly`(
        case: FreezeCase,
    ) = runTest {
        // Arrange + Act
        val model = createModel(
            testScope = this,
            statusSource = case.statusSource,
            frozenState = case.frozenState,
            availableForWithdrawal = case.availableForWithdrawal,
        )
        advanceUntilIdle()

        // Assert
        val state = model.uiState.value
        assertThat(state.addFundsButton.isEnabled).isEqualTo(case.expectedAddFundsEnabled)
        assertThat(state.withdrawButton.isEnabled).isEqualTo(case.expectedWithdrawEnabled)
        model.onDestroy()
    }

    @ParameterizedTest
    @MethodSource("provideMutedCases")
    fun `GIVEN status source WHEN status loaded THEN balance is muted only when cached`(case: MutedCase) = runTest {
        // Arrange + Act
        val model = createModel(
            testScope = this,
            statusSource = case.statusSource,
            frozenState = TangemPayCardFrozenState.Unfrozen,
            availableForWithdrawal = BigDecimal.ZERO,
            accountError = case.accountError,
        )
        advanceUntilIdle()

        // Assert
        val balanceState = model.uiState.value.balanceBlockState
        assertThat(balanceState).isInstanceOf(TangemPayDetailsBalanceBlockState.Content::class.java)
        assertThat((balanceState as TangemPayDetailsBalanceBlockState.Content).isMuted).isEqualTo(case.expectedMuted)
        model.onDestroy()
    }

    private fun createModel(
        testScope: TestScope,
        statusSource: StatusSource,
        frozenState: TangemPayCardFrozenState,
        availableForWithdrawal: BigDecimal,
        accountError: PaymentAccountStatusValue.Error? = null,
    ): TangemPayDetailsModel {
        val loaded: PaymentAccountStatusValue.Loaded = mockk(relaxed = true) {
            every { source } returns statusSource
            every { error } returns accountError
            every { cards } returns listOf(tangemPayCard())
            every { balance } returns PaymentAccountStatusValue.Balance(
                fiatBalance = PaymentAccountStatusValue.FiatBalance(
                    availableBalance = BigDecimal.ZERO,
                    currency = "USD",
                ),
                cryptoBalance = PaymentAccountStatusValue.CryptoBalance(
                    id = "id",
                    chainId = 1L,
                    depositAddress = "address",
                    tokenContractAddress = "contract",
                    balance = BigDecimal.ZERO,
                ),
                availableForWithdrawal = availableForWithdrawal,
            )
        }
        val paymentStatus: AccountStatus.Payment = mockk(relaxed = true) {
            every { value } returns loaded
            every { account } returns mockk(relaxed = true) {
                every { userWalletId } returns this@TangemPayDetailsModelTest.userWalletId
            }
        }
        val params = TangemPayDetailsContainerComponent.Params(initialStatus = paymentStatus)

        every { paymentAccountStatusSupplier.invoke(any<UserWalletId>()) } returns flowOf(paymentStatus)
        every { cardDetailsRepository.cardFrozenState(any()) } returns flowOf(frozenState)
        coEvery { cardDetailsRepository.isAddToWalletDone(any()) } returns false.right()

        return TangemPayDetailsModel(
            paramsContainer = MutableParamsContainer(params),
            paymentAccountStatusSupplier = paymentAccountStatusSupplier,
            dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
            analytics = mockk(relaxed = true),
            router = mockk(relaxed = true),
            urlOpener = mockk(relaxed = true),
            cardDetailsRepository = cardDetailsRepository,
            getBalanceHidingSettingsUseCase = mockk(relaxed = true),
            uiMessageSender = mockk(relaxed = true),
            txHistoryUpdateListener = mockk(relaxed = true),
            tangemPayWithdrawRepository = mockk(relaxed = true),
            sendFeedbackEmailUseCase = mockk(relaxed = true),
            expressTransactionsEventListener = mockk(relaxed = true),
            tangemPayFeatureToggles = mockk(relaxed = true),
            paymentAccountStatusFetcher = mockk(relaxed = true),
            produceTangemPayInitialDataUseCase = mockk(relaxed = true),
            onboardingRepository = mockk(relaxed = true),
            getCustomerOffers = mockk(relaxed = true),
        )
    }

    private fun TestScope.createTestingCoroutineDispatcherProvider(): TestingCoroutineDispatcherProvider {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        return TestingCoroutineDispatcherProvider(
            main = testDispatcher,
            mainImmediate = testDispatcher,
            io = testDispatcher,
            default = testDispatcher,
            single = testDispatcher,
        )
    }

    internal data class FreezeCase(
        val statusSource: StatusSource,
        val frozenState: TangemPayCardFrozenState,
        val availableForWithdrawal: BigDecimal,
        val expectedAddFundsEnabled: Boolean,
        val expectedWithdrawEnabled: Boolean,
    )

    internal data class MutedCase(
        val statusSource: StatusSource,
        val expectedMuted: Boolean,
        val accountError: PaymentAccountStatusValue.Error? = null,
    )

    private companion object {
        @JvmStatic
        fun provideFreezeCases() = listOf(
            FreezeCase(
                statusSource = StatusSource.ACTUAL,
                frozenState = TangemPayCardFrozenState.Unfrozen,
                availableForWithdrawal = BigDecimal.ZERO,
                expectedAddFundsEnabled = true,
                expectedWithdrawEnabled = false,
            ),
            FreezeCase(
                statusSource = StatusSource.ACTUAL,
                frozenState = TangemPayCardFrozenState.Unfrozen,
                availableForWithdrawal = BigDecimal.TEN,
                expectedAddFundsEnabled = true,
                expectedWithdrawEnabled = true,
            ),
            FreezeCase(
                statusSource = StatusSource.ACTUAL,
                frozenState = TangemPayCardFrozenState.Frozen,
                availableForWithdrawal = BigDecimal.TEN,
                expectedAddFundsEnabled = false,
                expectedWithdrawEnabled = false,
            ),
            FreezeCase(
                statusSource = StatusSource.CACHE,
                frozenState = TangemPayCardFrozenState.Unfrozen,
                availableForWithdrawal = BigDecimal.TEN,
                expectedAddFundsEnabled = false,
                expectedWithdrawEnabled = false,
            ),
        )

        @JvmStatic
        fun provideMutedCases() = listOf(
            MutedCase(statusSource = StatusSource.ACTUAL, expectedMuted = false),
            MutedCase(statusSource = StatusSource.CACHE, expectedMuted = true),
            MutedCase(statusSource = StatusSource.ONLY_CACHE, expectedMuted = true),
            // ACTUAL but errored is still not fresh -> muted (guards !isFresh, not just source != ACTUAL)
            MutedCase(
                statusSource = StatusSource.ACTUAL,
                accountError = PaymentAccountStatusValue.Error.Unavailable,
                expectedMuted = true,
            ),
        )
    }
}