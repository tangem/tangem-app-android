package com.tangem.tap.domain.tokens

import com.squareup.moshi.JsonAdapter
import com.tangem.Log
import com.tangem.datasource.api.common.MoshiConverter
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.files.FileReader
import com.tangem.tap.features.wallet.models.Currency

@Deprecated("Use [com.tangem.datasource.local.token.UserTokensStore] instead.")
class UserTokensStorageService(private val fileReader: FileReader) {
    private val userTokensAdapter: JsonAdapter<UserTokensResponse> =
        MoshiConverter.networkMoshi.adapter(UserTokensResponse::class.java)

    fun getUserTokens(userWalletId: String): List<Currency>? {
        return try {
            val json = fileReader.readFile(getFileNameForUserTokens(userWalletId))
            userTokensAdapter.fromJson(json)?.tokens?.mapNotNull { Currency.fromTokenResponse(it) }
        } catch (exception: Exception) {
            Log.error { exception.stackTraceToString() }
            null
        }
    }

    fun saveUserTokens(userWalletId: String, tokens: UserTokensResponse) {
        val json = userTokensAdapter.toJson(tokens)
        fileReader.rewriteFile(json, getFileNameForUserTokens(userWalletId))
    }

    companion object {
        private const val FILE_NAME_PREFIX_USER_TOKENS = "user_tokens"
        private fun getFileNameForUserTokens(userId: String): String = "${FILE_NAME_PREFIX_USER_TOKENS}_$userId"
    }
}
