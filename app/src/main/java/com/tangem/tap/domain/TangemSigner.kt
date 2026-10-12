package com.tangem.tap.domain

import com.tangem.Message
import com.tangem.TangemSdk
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.Wallet
import com.tangem.common.CompletionResult
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.run
import com.tangem.domain.card.models.TwinKey
import com.tangem.domain.models.scan.isRing
import com.tangem.lib.auth.AuthFeatureToggles
import com.tangem.operations.sign.SignData
import com.tangem.tap.domain.tasks.MultiSignHashTask
import com.tangem.tap.domain.tasks.SignHashesTask
import com.tangem.tap.domain.tasks.product.RegisterColdWalletRunnable
import com.tangem.tap.domain.walletregistration.WalletRegistrationLauncher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@Suppress("LongParameterList")
class TangemSigner(
    private val cardId: String?,
    private val tangemSdk: TangemSdk,
    private val initialMessage: Message,
    private val twinKey: TwinKey?,
    private val walletRegistrationLauncher: WalletRegistrationLauncher,
    private val authFeatureToggles: AuthFeatureToggles,
    private val signerCallback: (TangemSignerResponse) -> Unit,
    private val session: CardSession? = null,
) : TransactionSigner {

    // Cold registration piggybacks on the signing tap (best-effort — the launcher skips twin / Visa /
    // walletless cards and no-ops when backend auth is disabled). It runs on the card session's own
    // scope inside [RegisterColdWalletRunnable], so it's bound to the tap.
    private fun <T> withColdRegistration(task: CardSessionRunnable<T>): CardSessionRunnable<T> =
        if (authFeatureToggles.isBackendAuthenticationEnabled) {
            RegisterColdWalletRunnable(inner = task, launcher = walletRegistrationLauncher)
        } else {
            task
        }

    override suspend fun sign(
        hashes: List<ByteArray>,
        publicKey: Wallet.PublicKey,
    ): CompletionResult<List<ByteArray>> {
        val task = SignHashesTask(hashes, publicKey, twinKey?.getPairKey(publicKey.seedKey))

        return when (val result = runTask(task)) {
            is CompletionResult.Success -> {
                signerCallback(
                    TangemSignerResponse(
                        totalSignedHashes = result.data.totalSignedHashes,
                        remainingSignatures = result.data.remainingSignatures,
                        isRing = result.data.batchId?.let(::isRing) == true,
                        signedWalletPublicKey = publicKey.seedKey,
                    ),
                )
                CompletionResult.Success(result.data.signatures)
            }
            is CompletionResult.Failure -> CompletionResult.Failure(result.error)
        }
    }

    override suspend fun sign(hash: ByteArray, publicKey: Wallet.PublicKey): CompletionResult<ByteArray> {
        val result = sign(
            hashes = listOf(hash),
            publicKey = publicKey,
        )

        return when (result) {
            is CompletionResult.Success -> CompletionResult.Success(result.data.first())
            is CompletionResult.Failure -> CompletionResult.Failure(result.error)
        }
    }

    override suspend fun multiSign(
        dataToSign: List<SignData>,
        publicKey: Wallet.PublicKey,
    ): CompletionResult<Map<ByteArray, ByteArray>> {
        val result = runTask(MultiSignHashTask(dataToSign, publicKey, twinKey?.getPairKey(publicKey.seedKey)))

        return when (result) {
            is CompletionResult.Success -> {
                signerCallback(
                    TangemSignerResponse(
                        totalSignedHashes = result.data.totalSignedHashes,
                        remainingSignatures = result.data.remainingSignatures,
                        isRing = result.data.batchId?.let(::isRing) == true,
                        signedWalletPublicKey = publicKey.seedKey,
                    ),
                )
                CompletionResult.Success(result.data.signatures)
            }
            is CompletionResult.Failure -> CompletionResult.Failure(result.error)
        }
    }

    private suspend fun <T> runTask(task: CardSessionRunnable<T>): CompletionResult<T> {
        val runnable = withColdRegistration(task)
        return session?.let { runnable.run(session = it) }
            ?: suspendCancellableCoroutine { continuation ->
                tangemSdk.startSessionWithRunnable(
                    runnable = runnable,
                    cardId = cardId,
                    initialMessage = initialMessage,
                ) { result ->
                    if (continuation.isActive) continuation.resume(result)
                }
            }
    }
}

@Suppress("ArrayInDataClass")
data class TangemSignerResponse(
    val totalSignedHashes: Int?,
    val remainingSignatures: Int?,
    val isRing: Boolean,
    val signedWalletPublicKey: ByteArray,
)