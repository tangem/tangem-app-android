package com.tangem.core.analytics.handlers.appsflyer

import com.tangem.core.analytics.api.AppsFlyerConversionDataHandler
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TangemAFConversionListenerTest {

    private val firstHandler: AppsFlyerConversionDataHandler = mockk(relaxUnitFun = true)
    private val secondHandler: AppsFlyerConversionDataHandler = mockk(relaxUnitFun = true)
    private val listener = TangemAFConversionListener(
        conversionDataHandlers = setOf(firstHandler, secondHandler),
    )

    @AfterEach
    fun tearDown() {
        clearMocks(firstHandler, secondHandler)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun onConversionDataSuccess(model: OnConversionDataSuccessModel) {
        listener.onConversionDataSuccess(p0 = model.params)

        if (model.shouldHandle) {
            verify { firstHandler.handle(conversionData = model.params!!) }
            verify { secondHandler.handle(conversionData = model.params!!) }
        } else {
            verify(inverse = true) { firstHandler.handle(conversionData = any()) }
            verify(inverse = true) { secondHandler.handle(conversionData = any()) }
        }
    }

    private fun provideTestModels(): List<OnConversionDataSuccessModel> {
        return listOf(
            OnConversionDataSuccessModel(
                params = null,
                shouldHandle = false,
            ),
            OnConversionDataSuccessModel(
                params = emptyMap(),
                shouldHandle = true,
            ),
            OnConversionDataSuccessModel(
                params = mapOf(
                    "deep_link_value" to "referral",
                ),
                shouldHandle = true,
            ),
        )
    }

    data class OnConversionDataSuccessModel(
        val params: Map<String?, Any?>?,
        val shouldHandle: Boolean,
    )
}