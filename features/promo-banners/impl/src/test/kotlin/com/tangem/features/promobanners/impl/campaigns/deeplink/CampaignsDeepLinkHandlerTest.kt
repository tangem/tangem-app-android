package com.tangem.features.promobanners.impl.campaigns.deeplink

import android.net.Uri
import arrow.core.left
import arrow.core.right
import com.tangem.common.routing.deeplink.DeepLinkRequest
import com.tangem.common.routing.deeplink.DeeplinkConst.CAMPAIGN_ID_KEY
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.domain.wallets.models.errors.GetUserWalletError
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.features.promobanners.impl.campaigns.service.CampaignsService
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class CampaignsDeepLinkHandlerTest {

    private val campaignsService: CampaignsService = mockk(relaxed = true)
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase = mockk()

    private val handler = CampaignsDeepLinkHandler(
        campaignsService = campaignsService,
        getSelectedWalletSyncUseCase = getSelectedWalletSyncUseCase,
    )

    private val userWallet = MockUserWalletFactory.create()

    @BeforeEach
    fun resetMocks() {
        clearMocks(campaignsService, getSelectedWalletSyncUseCase)
        every { getSelectedWalletSyncUseCase() } returns userWallet.right()
    }

    @Test
    fun `GIVEN campaign id and selected wallet WHEN handled THEN campaign is shown for that wallet`() {
        // Act
        handler.handle(createRequest(queryParams = mapOf(CAMPAIGN_ID_KEY to "1")))

        // Assert
        verify(exactly = 1) { campaignsService.show(campaignId = "1", userWalletId = userWallet.walletId) }
    }

    @Test
    fun `GIVEN no campaign id WHEN handled THEN an empty campaign id is passed on`() {
        // Act
        handler.handle(createRequest(queryParams = emptyMap()))

        // Assert
        verify(exactly = 1) { campaignsService.show(campaignId = "", userWalletId = userWallet.walletId) }
    }

    @Test
    fun `GIVEN no selected wallet WHEN handled THEN no campaign is shown`() {
        // Arrange
        every { getSelectedWalletSyncUseCase() } returns GetUserWalletError.UserWalletNotFound.left()

        // Act
        handler.handle(createRequest(queryParams = mapOf(CAMPAIGN_ID_KEY to "1")))

        // Assert
        verify(exactly = 0) { campaignsService.show(any(), any()) }
    }

    private fun createRequest(queryParams: Map<String, String>) = DeepLinkRequest(
        uri = mockk<Uri>(),
        queryParams = queryParams,
        scope = mockk<CoroutineScope>(),
        isFromOnNewIntent = false,
    )
}