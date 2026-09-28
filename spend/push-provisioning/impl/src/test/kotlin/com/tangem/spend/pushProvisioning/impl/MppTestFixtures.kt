package com.tangem.spend.pushProvisioning.impl

import com.google.android.gms.tapandpay.TapAndPayStatusCodes
import com.google.android.gms.tapandpay.issuer.TokenInfo
import com.meawallet.mpp.MppError
import com.meawallet.mpp.MppErrorCode
import com.meawallet.mpp.MppException
import io.mockk.every
import io.mockk.mockk

internal data class FakeMppError(
    private val errorCode: Int,
    private val walletErrorCode: Int = 0,
    private val trace: String? = null,
) : MppError {

    override fun getCode(): Int = errorCode

    override fun getName(): String = "FAKE_ERROR"

    override fun getMessage(): String = "Fake error"

    override fun getTraceId(): String? = null

    @Deprecated("Deprecated in MPP SDK")
    override fun getGoogleApiErrorCode(): Int = walletErrorCode

    override fun getWalletApiErrorCode(): Int = walletErrorCode

    override fun getWalletApiExtraErrorCode(): Int = 0

    override fun toString(): String = """
        MppError[
            code: FAKE_ERROR ($errorCode)
            message: Fake error
            traceId: $trace
            Wallet error code: $walletErrorCode
        ]
    """.trimIndent()
}

internal fun createLookupError(walletErrorCode: Int): MppError =
    FakeMppError(errorCode = MppErrorCode.INTERNAL_ERROR, walletErrorCode = walletErrorCode)

internal fun createDeclinedError(trace: String? = null): MppError = FakeMppError(
    errorCode = MppErrorCode.GOOGLE_PAY_ERROR,
    walletErrorCode = TapAndPayStatusCodes.TAP_AND_PAY_TOKENIZATION_DECLINED,
    trace = trace,
)

internal fun createMppException(error: MppError): MppException = mockk(relaxed = true) {
    every { mppError } returns error
}

internal fun createTokenInfo(state: Int): TokenInfo = mockk {
    every { tokenState } returns state
}