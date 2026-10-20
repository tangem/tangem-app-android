package com.tangem.tap.common.analytics

import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.core.abtests.manager.ABTestsManager
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.hashedStringValue
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class ABTestsTrackingContextObserverTest {

    private val abTestsManager: ABTestsManager = mockk(relaxed = true)
    private val observer = ABTestsTrackingContextObserver(abTestsManager)

    private val coldWallet = MockUserWalletFactory.create()
    private val hotWallet = UserWallet.Hot(
        name = "Hot",
        walletId = UserWalletId("00aa"),
        hotWalletId = mockk(relaxed = true),
        wallets = null,
        isBackedUp = false,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(abTestsManager)
    }

    @Test
    fun `GIVEN cold wallet WHEN setContext THEN card properties are set`() {
        // Act
        observer.setContext(coldWallet)

        // Assert
        val card = coldWallet.scanResponse.card
        verify(exactly = 1) {
            abTestsManager.setUserProperties(
                userId = coldWallet.walletId.hashedStringValue,
                batch = card.batchId,
                productType = coldWallet.scanResponse.productType.name,
                firmware = card.firmwareVersion.stringValue,
            )
        }
    }

    @Test
    fun `GIVEN hot wallet WHEN setContext THEN mobile wallet properties are set`() {
        // Act
        observer.setContext(hotWallet)

        // Assert
        verify(exactly = 1) {
            abTestsManager.setUserProperties(
                userId = hotWallet.walletId.hashedStringValue,
                batch = null,
                productType = AnalyticsParam.ProductType.MobileWallet.value,
                firmware = null,
            )
        }
    }

    @Test
    fun `GIVEN scan response WHEN setContext THEN properties of its wallet are set`() {
        // Act
        observer.setContext(coldWallet.scanResponse)

        // Assert
        val card = coldWallet.scanResponse.card
        verify(exactly = 1) {
            abTestsManager.setUserProperties(
                userId = coldWallet.walletId.hashedStringValue,
                batch = card.batchId,
                productType = coldWallet.scanResponse.productType.name,
                firmware = card.firmwareVersion.stringValue,
            )
        }
    }

    @Test
    fun `GIVEN properties set WHEN eraseContext THEN properties are removed`() {
        // Act
        observer.eraseContext()

        // Assert
        verify(exactly = 1) { abTestsManager.removeUserProperties() }
    }
}