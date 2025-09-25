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
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import timber.log.Timber

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
        val wallet = userWallet.wallets.orEmpty().firstOrNull { it.publicKey.contentEquals(publicKey.seedKey) }
            ?: return CompletionResult.Failure(
                TangemSdkError.ExceptionError(IllegalStateException("wallet is locked")),
            )

        val result = runCatching {
            hotWalletAccessor.signHashes(
                hotWalletId = userWallet.hotWalletId,
                dataToSign = listOf(
                    DataToSign(
                        curve = wallet.curve,
                        hashes = hashes,
                        derivationPath = publicKey.derivationPath,
                    ),
                ),
            )
        }.getOrElse {
            Timber.e(it)
            return if (it is TangemSdkError) {
                CompletionResult.Failure(it)
            } else {
                CompletionResult.Failure(TangemSdkError.ExceptionError(it))
            }
        }

        return CompletionResult.Success(result.map { it.signatures }.flatten())
    }

    override suspend fun multiSign(
        dataToSign: List<SignData>,
        publicKey: Wallet.PublicKey,
    ): CompletionResult<Map<ByteArray, ByteArray>> {
        val result = runCatching {
            hotWalletAccessor.signHashes(
                hotWalletId = userWallet.hotWalletId,
                dataToSign = dataToSign.map { signData ->
                    val wallet =
                        userWallet.wallets.orEmpty().firstOrNull { it.publicKey.contentEquals(signData.publicKey) }
                            ?: return CompletionResult.Failure(
                                TangemSdkError.ExceptionError(IllegalStateException("wallet is locked")),
                            )

                    DataToSign(
                        curve = wallet.curve,
                        hashes = listOf(signData.hash),
                        derivationPath = signData.derivationPath,
                    )
                },
            )
        }.getOrElse {
            Timber.e(it)
            return if (it is TangemSdkError) {
                CompletionResult.Failure(it)
            } else {
                CompletionResult.Failure(TangemSdkError.ExceptionError(it))
            }
        }

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