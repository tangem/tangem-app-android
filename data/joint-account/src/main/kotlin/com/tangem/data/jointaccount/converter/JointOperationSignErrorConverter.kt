package com.tangem.data.jointaccount.converter

import com.tangem.core.remote.response.ApiResponseError
import com.tangem.core.remote.response.ApiResponseError.HttpException.Code
import com.tangem.domain.jointaccount.model.JointOperationSignError
import com.tangem.utils.converter.Converter

internal class JointOperationSignErrorConverter : Converter<Throwable, JointOperationSignError> {

    override fun convert(value: Throwable): JointOperationSignError {
        if (value !is ApiResponseError.HttpException) return JointOperationSignError.Failed(cause = value)

        return when (value.code) {
            Code.NOT_FOUND -> JointOperationSignError.OperationNotFound
            Code.CONFLICT -> JointOperationSignError.ThresholdReached
            else -> JointOperationSignError.Failed(cause = value)
        }
    }
}