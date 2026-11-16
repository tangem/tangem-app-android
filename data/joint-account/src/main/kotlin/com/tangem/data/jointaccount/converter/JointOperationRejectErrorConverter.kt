package com.tangem.data.jointaccount.converter

import com.tangem.core.remote.response.ApiResponseError
import com.tangem.core.remote.response.ApiResponseError.HttpException.Code
import com.tangem.domain.jointaccount.model.JointOperationRejectError
import com.tangem.store.datasource.jointaccount.JointWalletNotLinkedResolver
import com.tangem.utils.converter.Converter

internal class JointOperationRejectErrorConverter(
    private val walletNotLinkedResolver: JointWalletNotLinkedResolver,
) : Converter<Throwable, JointOperationRejectError> {

    override fun convert(value: Throwable): JointOperationRejectError {
        if (value !is ApiResponseError.HttpException) return JointOperationRejectError.Failed(cause = value)

        return when {
            value.code == Code.NOT_FOUND -> JointOperationRejectError.OperationNotFound
            value.code == Code.CONFLICT -> JointOperationRejectError.ThresholdReached
            walletNotLinkedResolver.isWalletNotLinked(value) -> JointOperationRejectError.Failed(cause = value)
            value.code == Code.FORBIDDEN -> JointOperationRejectError.NotAMember
            else -> JointOperationRejectError.Failed(cause = value)
        }
    }
}