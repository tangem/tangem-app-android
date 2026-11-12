package com.tangem.feature.referral.deeplink

import android.net.Uri
import arrow.core.left
import arrow.core.right
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.routing.deeplink.DeepLinkRequest
import com.tangem.common.test.domain.card.MockScanResponseFactory
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.domain.card.configs.GenericCardConfig
import com.tangem.domain.card.configs.MultiWalletCardConfig
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.models.errors.GetUserWalletError
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ReferralDeepLinkHandlerTest {

    private val appRouter: AppRouter = mockk(relaxed = true)
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase = mockk()

    private val handler = ReferralDeepLinkHandler(
        appRouter = appRouter,
        getSelectedWalletSyncUseCase = getSelectedWalletSyncUseCase,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(appRouter, getSelectedWalletSyncUseCase)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN selected wallet WHEN handled THEN referral program opens only for wallets that support it`(
        model: WalletModel,
    ) {
        // Arrange
        every { getSelectedWalletSyncUseCase() } returns model.userWallet.right()

        // Act
        handler.handle(createRequest())

        // Assert
        val expectedRoute = AppRoute.ReferralProgram(userWalletId = model.userWallet.walletId)
        verify(exactly = if (model.isReferralOpened) 1 else 0) { appRouter.push(expectedRoute, any()) }
    }

    @Test
    fun `GIVEN no selected wallet WHEN handled THEN nothing opens`() {
        // Arrange
        every { getSelectedWalletSyncUseCase() } returns GetUserWalletError.UserWalletNotFound.left()

        // Act
        handler.handle(createRequest())

        // Assert
        verify(exactly = 0) { appRouter.push(any(), any()) }
    }

    internal data class WalletModel(val userWallet: UserWallet, val isReferralOpened: Boolean)

    private fun provideTestModels() = listOf(
        WalletModel(
            userWallet = mockk<UserWallet.Hot> { every { walletId } returns UserWalletId("011") },
            isReferralOpened = true,
        ),
        WalletModel(
            userWallet = MockUserWalletFactory.create(
                scanResponse = MockScanResponseFactory.create(
                    cardConfig = MultiWalletCardConfig,
                    derivedKeys = emptyMap(),
                ),
            ),
            isReferralOpened = true,
        ),
        // A card that is not a Tangem Wallet has no referral program
        WalletModel(
            userWallet = MockUserWalletFactory.create(
                scanResponse = MockScanResponseFactory.create(
                    cardConfig = GenericCardConfig(maxWalletCount = 2),
                    derivedKeys = emptyMap(),
                ),
            ),
            isReferralOpened = false,
        ),
    )

    private fun createRequest() = DeepLinkRequest(
        uri = mockk<Uri>(),
        queryParams = emptyMap(),
        scope = mockk<CoroutineScope>(),
        isFromOnNewIntent = false,
    )
}