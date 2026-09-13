package com.tangem.domain.polymarket.usecase

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketOnboardedStore
import com.tangem.domain.polymarket.model.PolymarketWalletStatus
import com.tangem.test.core.ProvideTestModels
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RecordPolymarketConfirmationUseCaseTest {

    private val onboardedStore: PolymarketOnboardedStore = mockk()

    private val useCase = RecordPolymarketConfirmationUseCase(onboardedStore = onboardedStore)

    @BeforeEach
    fun resetMocks() {
        clearMocks(onboardedStore)
        coEvery { onboardedStore.markOnboarded(any()) } just Runs
        coEvery { onboardedStore.clear(any()) } just Runs
    }

    @ParameterizedTest
    @ProvideTestModels
    fun invoke(model: RecordModel) = runTest {
        // Act
        useCase(userWalletId = WALLET, status = model.status)

        // Assert
        coVerify(exactly = if (model.effect == Effect.MARKED) 1 else 0) { onboardedStore.markOnboarded(WALLET) }
        coVerify(exactly = if (model.effect == Effect.CLEARED) 1 else 0) { onboardedStore.clear(WALLET) }
    }

    internal data class RecordModel(val status: PolymarketWalletStatus, val effect: Effect)

    internal enum class Effect { MARKED, CLEARED, UNTOUCHED }

    private fun provideTestModels() = listOf(
        RecordModel(status = PolymarketWalletStatus.READY_TO_TRADE, effect = Effect.MARKED),
        RecordModel(status = PolymarketWalletStatus.NOT_CREATED, effect = Effect.CLEARED),
        // A setup in flight or a failed one says nothing about a readiness the backend has already confirmed
        RecordModel(status = PolymarketWalletStatus.DEPLOYMENT_IN_PROGRESS, effect = Effect.UNTOUCHED),
        RecordModel(status = PolymarketWalletStatus.DEPLOYED, effect = Effect.UNTOUCHED),
        RecordModel(status = PolymarketWalletStatus.APPROVALS_IN_PROGRESS, effect = Effect.UNTOUCHED),
        RecordModel(status = PolymarketWalletStatus.DEPLOYMENT_FAILED, effect = Effect.UNTOUCHED),
        RecordModel(status = PolymarketWalletStatus.APPROVALS_FAILED, effect = Effect.UNTOUCHED),
        RecordModel(status = PolymarketWalletStatus.UNKNOWN, effect = Effect.UNTOUCHED),
    )

    private companion object {
        val WALLET = UserWalletId("011")
    }
}