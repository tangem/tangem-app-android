package com.tangem.lib.auth.session.internal

import kotlinx.coroutines.sync.Mutex
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Serialises every request that makes the server rotate the session tokens.
 *
 * A per-wallet lock is not enough: the tokens are one global set, so two registrations for different
 * wallets — or a wallet registration racing a social binding — each get their own reissued pair back,
 * and whichever response is persisted last wins. The loser leaves `SessionTokensStore` holding
 * credentials the server has already superseded. Every such path therefore holds this lock across the
 * request *and* the save, so the store ends up with the reply that was actually last.
 *
 * Always taken INSIDE the per-wallet lock, never around it, so the acquisition order is the same
 * everywhere and the two locks cannot deadlock.
 */
@Singleton
internal class SessionTokenRotationLock @Inject constructor() : Mutex by Mutex()