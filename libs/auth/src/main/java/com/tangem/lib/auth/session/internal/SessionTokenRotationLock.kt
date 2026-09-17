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
 *
 * `/refresh` and `/authenticate` deliberately stay outside it: a request that already holds this lock
 * refreshes on a 401, and a non-reentrant mutex would deadlock there. Their writes are ordered by
 * `SessionTokensStore.save`, which drops a token set older than the stored one.
 */
@Singleton
internal class SessionTokenRotationLock @Inject constructor() : Mutex by Mutex()