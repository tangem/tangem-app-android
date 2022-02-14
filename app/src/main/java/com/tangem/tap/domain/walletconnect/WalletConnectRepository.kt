package com.tangem.tap.domain.walletconnect

import android.app.Application
import android.content.Context
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Types
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectSession
import com.tangem.tap.features.details.redux.walletconnect.WalletForSession
import com.tangem.tap.network.createMoshi
import com.trustwallet.walletconnect.models.WCPeerMeta
import com.trustwallet.walletconnect.models.session.WCSession

class WalletConnectRepository(val context: Application) {
    private val moshi = createMoshi()
    private val walletConnectAdapter: JsonAdapter<List<SessionDao>> = moshi.adapter(
        Types.newParameterizedType(List::class.java, SessionDao::class.java)
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
            walletConnectAdapter.fromJson(json)!!.map { it.toSession() }
        } catch (exception: Exception) {
            emptyList()
        }
    }

    private fun saveSessions(sessions: List<WalletConnectSession>) {
        val json = walletConnectAdapter.toJson(sessions.map { SessionDao.fromSession(it) })
        context.rewriteFile(json, FILE_NAME_PREFIX_SESSIONS)
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
            peerMeta = peerMeta
        )
    }

    companion object {
        fun fromSession(session: WalletConnectSession): SessionDao {
            return SessionDao(
                peerId = session.peerId,
                remotePeerId = session.remotePeerId,
                wallet = session.wallet,
                session = session.session,
                peerMeta = session.peerMeta
            )
        }
    }
}