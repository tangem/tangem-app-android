package com.tangem.tap.domain.walletconnect

import android.app.Application
import android.content.Context
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Types
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import com.tangem.datasource.api.common.MoshiConverter
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectSession
import com.tangem.tap.features.details.redux.walletconnect.WalletForSession
import com.trustwallet.walletconnect.models.WCPeerMeta
import com.trustwallet.walletconnect.models.session.WCSession
import timber.log.Timber
import java.nio.charset.Charset

class WalletConnectRepository(val context: Application) {
    private val walletConnectAdapter: JsonAdapter<List<SessionDao>> = MoshiConverter.sdkMoshi.adapter(
        Types.newParameterizedType(List::class.java, SessionDao::class.java),
    )

    fun saveSession(session: WalletConnectSession) {
        val sessions = loadSavedSessions() + session
        saveSessions(sessions)
    }

    fun removeSession(session: WalletConnectSession) {
        val sessions = loadSavedSessions().filterNot { it == session }
        saveSessions(sessions)
    }

    fun removeSession(session: WCSession) {
        val sessions = loadSavedSessions().filterNot { it.session == session }
        saveSessions(sessions)
    }

    fun loadSavedSessions(): List<WalletConnectSession> {
        return try {
            val json = context.readFileText(FILE_NAME_PREFIX_SESSIONS)
                .hexToUtf8()
            walletConnectAdapter.fromJson(json)!!.map { it.toSession() }
        } catch (exception: Exception) {
            Timber.e(exception)
            emptyList()
        }
    }

    private fun saveSessions(sessions: List<WalletConnectSession>) {
        val json = walletConnectAdapter.toJson(sessions.map { SessionDao.fromSession(it) })
            .utf8ToHex() // convert to hex to solve problems with saving text with emojis
        Timber.e("WC sessions, saving following json: $json")
        context.rewriteFile(json, FILE_NAME_PREFIX_SESSIONS)
    }

    private fun String.utf8ToHex(): String {
        return this.toByteArray().toHexString()
    }

    private fun String.hexToUtf8(): String {
        return this.hexToBytes().toString(Charset.defaultCharset())
    }

    private fun Context.readFileText(fileName: String): String =
        this.openFileInput(fileName).bufferedReader().readText()

    private fun Context.rewriteFile(content: String, fileName: String) {
        this.openFileOutput(fileName, Context.MODE_PRIVATE).use {
            it.write(content.toByteArray(), 0, content.length)
        }
    }

    companion object {
        private const val FILE_NAME_PREFIX_SESSIONS = "wc_sessions"
    }
}

@JsonClass(generateAdapter = true)
data class SessionDao(
    val peerId: String,
    val remotePeerId: String?,
    val wallet: WalletForSession,
    val session: WCSession,
    val peerMeta: WCPeerMeta,
) {
    fun toSession(): WalletConnectSession {
        return WalletConnectSession(
            peerId = peerId,
            remotePeerId = remotePeerId,
            wallet = wallet,
            session = session,
            peerMeta = peerMeta,
        )
    }

    companion object {
        fun fromSession(session: WalletConnectSession): SessionDao {
            return SessionDao(
                peerId = session.peerId,
                remotePeerId = session.remotePeerId,
                wallet = session.wallet,
                session = session.session,
                peerMeta = session.peerMeta,
            )
        }
    }
}
