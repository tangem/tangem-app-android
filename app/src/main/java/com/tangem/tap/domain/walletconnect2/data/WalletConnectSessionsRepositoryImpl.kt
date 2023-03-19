package com.tangem.tap.domain.walletconnect2.data

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.tangem.datasource.asset.AssetReader
import com.tangem.datasource.di.SdkMoshi
import com.tangem.tap.domain.walletconnect2.domain.WalletConnectSessionsRepository
import com.tangem.tap.domain.walletconnect2.domain.models.Session
import timber.log.Timber
import javax.inject.Inject

class WalletConnectSessionsRepositoryImpl @Inject constructor(
    @SdkMoshi private val moshi: Moshi,
    private val assetReader: AssetReader,
) :
    WalletConnectSessionsRepository {

    private val sessionsAdapter: JsonAdapter<List<Session>> = moshi.adapter(
        Types.newParameterizedType(List::class.java, Session::class.java),
    )

    override suspend fun loadSessions(userWallet: String): List<Session> {
        return try {
            val fileContent = assetReader.readJson(getFileNameForUserWallet(userWallet))
            sessionsAdapter.fromJson(fileContent) ?: emptyList()
        } catch (exception: Exception) {
            Timber.e(exception)
            emptyList()
        }
    }

    override suspend fun saveSession(userWallet: String, session: Session) {
        val updatedList = loadSessions(userWallet).plus(session)
        val serialized = sessionsAdapter.toJson(updatedList)
        assetReader.writeJson(serialized, getFileNameForUserWallet(userWallet))
    }

    override suspend fun removeSession(userWallet: String, topic: String) {
        val updatedList = loadSessions(userWallet).filterNot { it.topic == topic }
        val serialized = sessionsAdapter.toJson(updatedList)
        assetReader.writeJson(serialized, getFileNameForUserWallet(userWallet))
    }

    companion object {
        private const val FILE_NAME_PREFIX = "wc_2.0_topics"

        private fun getFileNameForUserWallet(userWallet: String): String {
            return "$FILE_NAME_PREFIX:$userWallet"
        }
    }
}