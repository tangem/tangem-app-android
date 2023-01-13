package com.tangem.tap.domain.tokens

import com.squareup.moshi.JsonAdapter
import com.tangem.Log
import com.tangem.datasource.api.common.MoshiConverter
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.domain.common.CardDTO
import com.tangem.tap.common.FileReader
import com.tangem.tap.features.wallet.models.Currency
import com.tangem.tap.features.wallet.models.toCurrencies

class UserTokensStorageService(
    private val oldUserTokensRepository: OldUserTokensRepository,
    private val fileReader: FileReader,
) {
    private val userTokensAdapter: JsonAdapter<UserTokensResponse> =
        MoshiConverter.networkMoshi.adapter(UserTokensResponse::class.java)

    fun getUserTokens(userId: String): List<Currency>? {
        return try {
            val json = fileReader.readFile(getFileNameForUserTokens(userId))
            userTokensAdapter.fromJson(json)?.tokens?.mapNotNull { Currency.fromTokenResponse(it) }
        } catch (exception: Exception) {
            Log.error { exception.stackTraceToString() }
            null
        }
    }

    @Deprecated("")
    suspend fun getUserTokens(card: CardDTO): List<Currency> {
        val blockchainNetworks =
            oldUserTokensRepository.loadSavedCurrencies(card.cardId, card.settings.isHDWalletAllowed)
        return blockchainNetworks.flatMap { it.toCurrencies() }
    }

    fun saveUserTokens(userId: String, tokens: UserTokensResponse) {
        val json = userTokensAdapter.toJson(tokens)
        fileReader.rewriteFile(json, getFileNameForUserTokens(userId))
    }

    companion object {
        private const val FILE_NAME_PREFIX_USER_TOKENS = "user_tokens"
        private fun getFileNameForUserTokens(userId: String): String = "${FILE_NAME_PREFIX_USER_TOKENS}_$userId"
    }
}
