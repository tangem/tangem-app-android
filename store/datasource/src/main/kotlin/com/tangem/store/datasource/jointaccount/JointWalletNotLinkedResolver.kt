package com.tangem.store.datasource.jointaccount

import com.squareup.moshi.Moshi
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.core.remote.response.ApiResponseError.HttpException.Code
import com.tangem.store.datasource.jointaccount.models.JointAccountErrorDto

class JointWalletNotLinkedResolver(moshi: Moshi) {

    private val errorAdapter = moshi.adapter(JointAccountErrorDto::class.java)

    fun isWalletNotLinked(error: Throwable): Boolean {
        if (error !is ApiResponseError.HttpException || error.code != Code.FORBIDDEN) return false

        val errorCode = error.errorBody
            ?.let { body -> runCatching { errorAdapter.fromJson(body) }.getOrNull() }
            ?.errorCode

        return errorCode == WALLET_NOT_AUTHORIZED
    }

    private companion object {
        const val WALLET_NOT_AUTHORIZED = "WALLET_NOT_AUTHORIZED"
    }
}