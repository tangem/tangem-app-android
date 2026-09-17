package com.tangem.lib.auth.session.internal

import kotlinx.coroutines.sync.Mutex
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The locks every caller of `/mobile/wallet/register` shares, whether it registers a plain hot wallet
 * or binds a social identity to one. Both go through the same endpoint, so their locks have to be the
 * same objects — a mutex per class would let the two run concurrently and burn two one-time wallet
 * nonces on the same wallet, the loser coming back with a 409.
 *
 * Take them in this order, never the other way round, so they cannot deadlock:
 *
 * 1. [forWallet] — one registration at a time per wallet, while different wallets still proceed in
 *    parallel. Held across the whole flow (idempotency check, nonce, signature, POST), so a second
 *    attempt cannot slip past the check and consume a nonce of its own.
 * 2. [sessionRotation] — one token-rotating request at a time, process-wide, held across the request
 *    and the write that follows it. The session tokens are a single global set, so registrations of
 *    *different* wallets race over it too.
 *
 * `/refresh` and `/authenticate` deliberately stay outside [sessionRotation]: a request that already
 * holds it refreshes on a 401, and a non-reentrant mutex would deadlock there. Their writes are
 * ordered by `SessionTokensStore.save` instead, which drops a token set issued before the stored one.
 */
@Singleton
internal class WalletRegistrationLocks @Inject constructor() {

    private val perWallet = ConcurrentHashMap<String, Mutex>()

    val sessionRotation: Mutex = Mutex()

    fun forWallet(walletId: String): Mutex = perWallet.computeIfAbsent(walletId) { Mutex() }
}