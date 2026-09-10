package com.tangem.data.jointaccount.converter

import com.tangem.core.remote.response.ApiResponseError
import com.tangem.core.remote.response.ApiResponseError.HttpException.Code
import com.tangem.domain.jointaccount.model.JointOperationCreateError
import com.tangem.utils.converter.Converter

internal class JointOperationCreateErrorConverter : Converter<Throwable, JointOperationCreateError> {

    override fun convert(value: Throwable): JointOperationCreateError {
        if (value !is ApiResponseError.HttpException) return JointOperationCreateError.Failed(cause = value)

        return when {
            value.code == Code.CONFLICT -> JointOperationCreateError.SlotBusy
            value.code == Code.BAD_REQUEST && value.mentionsMovedNetworkState() ->
                JointOperationCreateError.NetworkStateMoved
            else -> JointOperationCreateError.Failed(cause = value)
        }
    }

    private fun ApiResponseError.HttpException.mentionsMovedNetworkState(): Boolean {
        return NETWORK_STATE_MARKERS.any { marker ->
            errorBody?.contains(marker, ignoreCase = true) == true ||
                message?.contains(marker, ignoreCase = true) == true
        }
    }

    private companion object {
        val NETWORK_STATE_MARKERS = listOf("NONCE_MISMATCH", "GAS_PRICE_TOO_LOW")
    }
}