package com.tangem.data.collectibles.vault

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.blockchain.common.BlockchainError
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.common.core.TangemError
import com.tangem.common.core.TangemSdkError
import com.tangem.core.error.UniversalError
import com.tangem.core.error.ext.tangemError
import com.tangem.core.error.ext.universalError
import com.tangem.domain.collectibles.CollectiblesAccountError
import com.tangem.domain.collectibles.vault.GachaVaultError
import com.tangem.domain.models.wallet.UserWallet

internal fun <T> Result<T>.toEither(): Either<BlockchainError, T> = when (this) {
    is Result.Success -> data.right()
    is Result.Failure -> error.left()
}

internal fun SimpleResult.toEither(): Either<BlockchainError, Unit> = when (this) {
    is SimpleResult.Success -> Unit.right()
    is SimpleResult.Failure -> error.left()
}

/** A hot wallet has no NFC sheet, so its signer's failures are ours to report — only the cancellation stays silent. */
internal fun BlockchainError.toVaultFailure(userWallet: UserWallet): CollectiblesAccountError = when {
    this !is BlockchainSdkError.WrappedTangemError -> toUniversalError().asFailure()
    tangemError is TangemSdkError.UserCancelled -> CollectiblesAccountError.Cancelled
    userWallet is UserWallet.Cold -> tangemError.toVaultFailure()
    else -> toUniversalError().asFailure()
}

/** A card refusal keeps the SDK's own error, which the NFC sheet localises by type; the rest leave as a universal code. */
internal fun BlockchainError.toTangemError(): TangemError = when (this) {
    is BlockchainSdkError.WrappedTangemError -> tangemError
    else -> toUniversalError().tangemError
}

private fun BlockchainError.toUniversalError(): UniversalError = when (this) {
    is BlockchainSdkError.UnsupportedOperation -> GachaVaultError.NetworkUnsupported
    is BlockchainSdkError -> universalError
    else -> GachaVaultError.Unexpected(cause = this)
}

/** Anything but an SDK error is one of ours that left the card session as a universal code — see [toTangemError]. */
internal fun TangemError.toVaultFailure(): CollectiblesAccountError = when (this) {
    is TangemSdkError.UserCancelled -> CollectiblesAccountError.Cancelled
    is TangemSdkError -> CollectiblesAccountError.CardError(code = code, message = customMessage)
    else -> universalError.asFailure()
}

internal fun UniversalError.asFailure(): CollectiblesAccountError = CollectiblesAccountError.Vault(error = this)