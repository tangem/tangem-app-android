package com.tangem.tap.domain.walletconnect2.data

import android.app.Application
import android.content.Context
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Types
import com.tangem.datasource.api.common.MoshiConverter
import com.tangem.tap.domain.walletconnect2.domain.WalletConnectSessionsRepository
import com.tangem.tap.domain.walletconnect2.domain.models.Session
import timber.log.Timber

class WalletConnectSessionsRepositoryImpl(val context: Application) : WalletConnectSessionsRepository {

    private val sessionsAdapter: JsonAdapter<List<Session>> = MoshiConverter.sdkMoshi.adapter(
        Types.newParameterizedType(List::class.java, Session::class.java),
    )

    override suspend fun loadSessions(userWallet: String): List<Session> {
        return try {
            val fileContent = context.readFileText(getFileNameForUserWallet(userWallet))
            sessionsAdapter.fromJson(fileContent) ?: emptyList()
        } catch (exception: Exception) {
            Timber.e(exception)
            emptyList()
        }
    }

    override suspend fun saveSession(userWallet: String, session: Session) {
        val updatedList = loadSessions(userWallet).plus(session)
        val serialized = sessionsAdapter.toJson(updatedList)
        context.rewriteFile(serialized, getFileNameForUserWallet(userWallet))
    }

    override suspend fun removeSession(userWallet: String, topic: String) {
        val updatedList = loadSessions(userWallet).filterNot { it.topic == topic }
        val serialized = sessionsAdapter.toJson(updatedList)
        context.rewriteFile(serialized, getFileNameForUserWallet(userWallet))
    }

    private fun Context.readFileText(fileName: String): String =
        this.openFileInput(fileName).use { it.bufferedReader().readText() }

    private fun Context.rewriteFile(content: String, fileName: String) {
        this.openFileOutput(fileName, Context.MODE_PRIVATE).use {
            it.write(content.toByteArray(), 0, content.length)
        }
    }

    companion object {
        private const val FILE_NAME_PREFIX = "wc_2.0_topics"

        private fun getFileNameForUserWallet(userWallet: String): String {
            return "$FILE_NAME_PREFIX:$userWallet"
        }
    }
}