package com.tangem.data.wallets.hot

import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.Wallet
import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemSdkError
import com.tangem.common.map
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.hot.HotWalletAccessor
import com.tangem.hot.sdk.model.DataToSign
import com.tangem.operations.sign.SignData
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class TangemHotWalletSigner @AssistedInject constructor(
    @Assisted private val userWallet: UserWallet.Hot,
    private val hotWalletAccessor: HotWalletAccessor,
) : TransactionSigner {

    override suspend fun sign(hash: ByteArray, publicKey: Wallet.PublicKey): CompletionResult<ByteArray> {
        return sign(listOf(hash), publicKey).map { it.first() }
    }

    override suspend fun sign(
        hashes: List<ByteArray>,
        publicKey: Wallet.PublicKey,
    ): CompletionResult<List<ByteArray>> {
        val wallet = findWallet(publicKey) ?: return walletLockedFailure()

        val result = runSuspendCatching {
            hotWalletAccessor.signHashes(
                hotWalletId = userWallet.hotWalletId,
                dataToSign = listOf(
                    DataToSign(curve = wallet.curve, derivationPath = publicKey.derivationPath, hashes = hashes),
                ),
            )
        }.getOrElse { return it.toSignFailure() }

        return CompletionResult.Success(result.map { it.signatures }.flatten())
    }

    override suspend fun sign(
        publicKey: Wallet.PublicKey,
        hashesProvider: suspend () -> List<ByteArray>,
    ): CompletionResult<List<ByteArray>> {
        val wallet = findWallet(publicKey) ?: return walletLockedFailure()

        val result = runSuspendCatching {
            hotWalletAccessor.signHashesPreparing(hotWalletId = userWallet.hotWalletId) {
                listOf(
                    DataToSign(
                        curve = wallet.curve,
                        derivationPath = publicKey.derivationPath,
                        hashes = hashesProvider(),
                    ),
                )
            }
        }.getOrElse { return it.toSignFailure() }

        return CompletionResult.Success(result.map { it.signatures }.flatten())
    }

    private fun findWallet(publicKey: Wallet.PublicKey) =
        userWallet.wallets.orEmpty().firstOrNull { it.publicKey.contentEquals(publicKey.seedKey) }

    private fun <T> walletLockedFailure(): CompletionResult<T> =
        CompletionResult.Failure(TangemSdkError.ExceptionError(IllegalStateException("wallet is locked")))

    private fun <T> Throwable.toSignFailure(): CompletionResult<T> {
        TangemLogger.e("Error", this)
        return CompletionResult.Failure(this as? TangemSdkError ?: TangemSdkError.ExceptionError(this))
    }

    override suspend fun multiSign(
        dataToSign: List<SignData>,
        publicKey: Wallet.PublicKey,
    ): CompletionResult<Map<ByteArray, ByteArray>> {
        val result = runSuspendCatching {
            hotWalletAccessor.signHashes(
                hotWalletId = userWallet.hotWalletId,
                dataToSign = dataToSign.map { signData ->
                    val wallet = findWallet(publicKey) ?: return walletLockedFailure()

                    DataToSign(
                        curve = wallet.curve,
                        hashes = listOf(signData.hash),
                        derivationPath = signData.derivationPath,
                    )
                },
            )
        }.getOrElse { return it.toSignFailure() }

        return CompletionResult.Success(
            result.mapIndexed { index, data ->
                dataToSign[index].publicKey to data.signatures.first()
            }.toMap(),
        )
    }

    @AssistedFactory
    interface Factory {
        fun create(@Assisted userWallet: UserWallet.Hot): TangemHotWalletSigner
    }
}