package com.tangem.tap.common.analytics.appsflyer

import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TangemAFConversionListenerTest {

    private val referralParamsHandler: AppsFlyerReferralParamsHandler = mockk(relaxUnitFun = true)
    private val listener = TangemAFConversionListener(
        referralParamsHandler = referralParamsHandler,
    )

    @AfterEach
    fun tearDown() {
        clearMocks(referralParamsHandler)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun onConversionDataSuccess(model: OnConversionDataSuccessModel) = runTest {
        listener.onConversionDataSuccess(p0 = model.params)

        if (model.shouldHandle) {
            coVerify { referralParamsHandler.handle(params = model.params!!) }
        } else {
            coVerify(inverse = true) { referralParamsHandler.handle(params = any()) }
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