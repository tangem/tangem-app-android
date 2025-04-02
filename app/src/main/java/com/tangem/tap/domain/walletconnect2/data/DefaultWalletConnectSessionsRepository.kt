package com.tangem.tap.domain.walletconnect2.data

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.tangem.datasource.files.FileReader
import com.tangem.domain.walletconnect.model.legacy.WalletConnectSessionsRepository
import com.tangem.domain.walletconnect.model.legacy.Session
import timber.log.Timber

internal class DefaultWalletConnectSessionsRepository(
    private val moshi: Moshi,
    private val fileReader: FileReader,
) :
    WalletConnectSessionsRepository {

    private val sessionsAdapter: JsonAdapter<List<Session>> = moshi.adapter(
        Types.newParameterizedType(List::class.java, Session::class.java),
    )

    override suspend fun loadSessions(userWallet: String): List<Session> {
        return try {
            val fileContent = fileReader.readFile(getFileNameForUserWallet(userWallet))
            sessionsAdapter.fromJson(fileContent) ?: emptyList()
        } catch (exception: Exception) {
            Timber.d(exception)
            emptyList()
        }
    }

    override suspend fun saveSession(userWallet: String, session: Session) {
        val updatedList = loadSessions(userWallet).plus(session)
        writeSessionToFile(sessions = updatedList, userWallet = userWallet)
    }

    override suspend fun removeSession(userWallet: String, topic: String) {
        val updatedList = loadSessions(userWallet).filterNot { it.topic == topic }
        writeSessionToFile(sessions = updatedList, userWallet = userWallet)
    }

    private fun writeSessionToFile(sessions: List<Session>, userWallet: String) {
        val serialized = sessionsAdapter.toJson(sessions)
        try {
            fileReader.rewriteFile(serialized, getFileNameForUserWallet(userWallet))
        } catch (exception: Exception) {
            Timber.e(exception)
        }
    }

    companion object {
        private const val FILE_NAME_PREFIX = "wc_2"

        private fun getFileNameForUserWallet(userWallet: String): String {
            return "$FILE_NAME_PREFIX-${userWallet.uppercase()}"
        }
    }
}