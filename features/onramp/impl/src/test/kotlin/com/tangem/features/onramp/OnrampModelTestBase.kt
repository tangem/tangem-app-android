package com.tangem.features.onramp

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope

/** Shared fixtures for Onramp model tests. */
@OptIn(ExperimentalCoroutinesApi::class)
internal abstract class OnrampModelTestBase {

    protected val userWalletId = UserWalletId("0011")
    protected val cryptoCurrency: CryptoCurrency = mockk(relaxed = true)
    protected val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxUnitFun = true)
    protected val router: Router = mockk(relaxed = true)

    protected fun TestScope.testDispatchers(): CoroutineDispatcherProvider =
        createTestingCoroutineDispatcherProvider()
}