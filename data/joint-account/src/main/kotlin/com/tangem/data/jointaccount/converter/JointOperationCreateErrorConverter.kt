package com.tangem.data.jointaccount.converter

import com.squareup.moshi.Moshi
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.core.remote.response.ApiResponseError.HttpException.Code
import com.tangem.domain.jointaccount.model.JointOperationCreateError
import com.tangem.store.datasource.jointaccount.models.JointAccountErrorDto
import com.tangem.utils.converter.Converter
import com.tangem.utils.logging.TangemLogger

internal class JointOperationCreateErrorConverter(moshi: Moshi) : Converter<Throwable, JointOperationCreateError> {

    private val errorAdapter = moshi.adapter(JointAccountErrorDto::class.java)

    override fun convert(value: Throwable): JointOperationCreateError {
        if (value !is ApiResponseError.HttpException) return JointOperationCreateError.Failed(cause = value)

        val networkStateCode = value.networkStateCode()

        return when {
            value.code == Code.CONFLICT -> JointOperationCreateError.SlotBusy
            value.code == Code.BAD_REQUEST && networkStateCode != null -> {
                TangemLogger.e("The joint operation was refused by $networkStateCode")
                JointOperationCreateError.NetworkStateMoved
            }
            else -> JointOperationCreateError.Failed(cause = value)
        }
    }

    /** The chain moved between the reading the operation was built on and the backend's own check */
    private fun ApiResponseError.HttpException.networkStateCode(): String? {
        val code = errorBody
            ?.let { body -> runCatching { errorAdapter.fromJson(body) }.getOrNull() }
            ?.code
            ?: return null

        return code.takeIf { it in NETWORK_STATE_CODES }
    }

    private companion object {
        val NETWORK_STATE_CODES = setOf("NONCE_MISMATCH", "GAS_PRICE_TOO_LOW", "GAS_PRICE_TOO_HIGH")
    }
}