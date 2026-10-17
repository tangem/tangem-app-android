package com.tangem.tap.common.analytics

import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.core.analytics.api.AnalyticsUserIdHandler
import com.tangem.core.analytics.api.ParamsInterceptor
import com.tangem.core.analytics.api.ParamsInterceptorHolder
import com.tangem.core.analytics.utils.TrackingContextProxy
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.tap.common.analytics.paramsInterceptor.HotWalletContextInterceptor
import com.tangem.tap.common.analytics.paramsInterceptor.LinkedCardContextInterceptor
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

internal class DefaultTrackingContextProxyTest {

    private val userIdHandler: AnalyticsUserIdHandler = mockk(relaxed = true)
    private val observer: TrackingContextProxy.Observer = mockk(relaxed = true)
    private val interceptorHolder = InMemoryParamsInterceptorHolder()

    private val proxy = DefaultTrackingContextProxy(
        paramsInterceptorHolder = interceptorHolder,
        userIdHandler = userIdHandler,
        observers = setOf(observer),
    )

    private val coldWallet = MockUserWalletFactory.create()
    private val hotWallet = UserWallet.Hot(
        name = "Hot",
        walletId = UserWalletId("00aa"),
        hotWalletId = mockk(relaxed = true),
        wallets = null,
        backedUp = false,
    )

    @BeforeEach
    fun reset() {
        clearMocks(userIdHandler, observer)
        interceptorHolder.interceptors.clear()
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SetContext {

        @Test
        fun `GIVEN hot context WHEN setContext with cold wallet THEN only card context remains`() {
            // Arrange
            proxy.setContext(hotWallet)

            // Act
            proxy.setContext(coldWallet)

            // Assert
            assertThat(interceptorHolder.interceptors.values.single())
                .isInstanceOf(LinkedCardContextInterceptor::class.java)
            verify(exactly = 1) { userIdHandler.setUserId(coldWallet.walletId.stringValue) }
        }

        @Test
        fun `GIVEN card context WHEN setContext with hot wallet THEN only hot context remains`() {
            // Arrange
            proxy.setContext(coldWallet)

            // Act
            proxy.setContext(hotWallet)

            // Assert
            assertThat(interceptorHolder.interceptors.values.single())
                .isInstanceOf(HotWalletContextInterceptor::class.java)
            verify(exactly = 1) { userIdHandler.setUserId(hotWallet.walletId.stringValue) }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class AddAndRemoveContext {

        @Test
        fun `GIVEN card context WHEN addContext with hot wallet and removeContext THEN card context restored`() {
            // Arrange
            proxy.setContext(coldWallet)
            val cardContext = interceptorHolder.interceptors.getValue(LinkedCardContextInterceptor.id())

            // Act
            proxy.addContext(hotWallet)
            proxy.removeContext()

            // Assert
            assertThat(interceptorHolder.interceptors).containsExactly(LinkedCardContextInterceptor.id(), cardContext)
        }

        @Test
        fun `GIVEN card context WHEN addHotWalletContext and removeContext THEN card context restored`() {
            // Arrange
            proxy.setContext(coldWallet)
            val cardContext = interceptorHolder.interceptors.getValue(LinkedCardContextInterceptor.id())

            // Act
            proxy.addHotWalletContext()
            proxy.removeContext()

            // Assert
            assertThat(interceptorHolder.interceptors).containsExactly(LinkedCardContextInterceptor.id(), cardContext)
        }

        @Test
        fun `GIVEN hot context WHEN addHotWalletContext and removeContext THEN hot context restored`() {
            // Arrange
            proxy.setContext(hotWallet)
            val hotContext = interceptorHolder.interceptors.getValue(HotWalletContextInterceptor.id())

            // Act
            proxy.addHotWalletContext()
            proxy.removeContext()

            // Assert
            assertThat(interceptorHolder.interceptors).containsExactly(HotWalletContextInterceptor.id(), hotContext)
        }

        @Test
        fun `GIVEN no previous context WHEN addHotWalletContext and removeContext THEN no context remains`() {
            // Arrange
            proxy.addHotWalletContext()

            // Act
            proxy.removeContext()

            // Assert
            assertThat(interceptorHolder.interceptors).isEmpty()
        }

        @Test
        fun `GIVEN no previous context WHEN addContext and removeContext THEN no context remains`() {
            // Arrange
            proxy.addContext(coldWallet.scanResponse)

            // Act
            proxy.removeContext()

            // Assert
            assertThat(interceptorHolder.interceptors).isEmpty()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class EraseContext {

        @Test
        fun `GIVEN card context WHEN eraseContext THEN context and user id cleared and observers notified`() {
            // Arrange
            proxy.setContext(coldWallet)

            // Act
            proxy.eraseContext()

            // Assert
            assertThat(interceptorHolder.interceptors).isEmpty()
            verify(exactly = 1) { userIdHandler.clearUserId() }
            verify(exactly = 1) { observer.eraseContext() }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Observers {

        @Test
        fun `GIVEN observer WHEN every proxy call made THEN each is forwarded once`() {
            // Act
            proxy.setContext(coldWallet.scanResponse)
            proxy.setContext(coldWallet)
            proxy.addContext(hotWallet)
            proxy.setHotWalletContext()
            proxy.addContext(coldWallet.scanResponse)
            proxy.addHotWalletContext()
            proxy.removeContext()
            proxy.eraseContext()

            // Assert
            verifyOrder {
                observer.setContext(coldWallet.scanResponse)
                observer.setContext(coldWallet)
                observer.addContext(hotWallet)
                observer.setHotWalletContext()
                observer.addContext(coldWallet.scanResponse)
                observer.addHotWalletContext()
                observer.removeContext()
                observer.eraseContext()
            }
        }

        @Test
        fun `GIVEN observer WHEN setContext with wallet THEN analytics context is set before observer is notified`() {
            // Arrange
            var interceptorsOnNotify: Set<String>? = null
            every { observer.setContext(coldWallet) } answers {
                interceptorsOnNotify = interceptorHolder.interceptors.keys.toSet()
            }

            // Act
            proxy.setContext(coldWallet)

            // Assert
            assertThat(interceptorsOnNotify).containsExactly(LinkedCardContextInterceptor.id())
        }
    }

    private class InMemoryParamsInterceptorHolder : ParamsInterceptorHolder {

        val interceptors = mutableMapOf<String, ParamsInterceptor>()

        override fun addParamsInterceptor(interceptor: ParamsInterceptor) {
            interceptors[interceptor.id()] = interceptor
        }

        override fun removeParamsInterceptor(interceptorId: String): ParamsInterceptor? {
            return interceptors.remove(interceptorId)
        }
    }
}