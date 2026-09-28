package com.tangem.spend.pushProvisioning.impl

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.android.gms.tapandpay.TapAndPayStatusCodes
import com.google.android.gms.tapandpay.issuer.TokenInfo
import com.meawallet.mpp.GooglePayTokenState
import com.meawallet.mpp.MppError
import com.meawallet.mpp.MppErrorCode
import com.meawallet.mpp.MppException
import com.tangem.spend.pushProvisioning.GoogleWalletProvisioner.AddResult
import com.tangem.spend.pushProvisioning.GoogleWalletProvisioner.CardStatus
import com.tangem.spend.pushProvisioning.GoogleWalletProvisioner.FailureReason

internal const val PUSH_PROVISIONING_LOG_TAG = "TangemPay: GoogleWallet"

internal data class TokenLookupFailure(
    val isWalletUnsupported: Boolean,
    val description: String,
    val sdkErrorCode: Int? = null,
    val walletErrorCode: Int? = null,
)

internal typealias TokenLookup = Either<TokenLookupFailure, List<TokenInfo>>

private const val NO_WALLET_ERROR_CODE = 0

private val WHITESPACE = Regex("\\s+")

private val IN_WALLET_TOKEN_STATES = setOf(
    GooglePayTokenState.TOKEN_STATE_ACTIVE,
    GooglePayTokenState.TOKEN_STATE_PENDING,
    GooglePayTokenState.TOKEN_STATE_SUSPENDED,
    GooglePayTokenState.TOKEN_STATE_FELICA_PENDING_PROVISIONING,
)

private val NETWORK_ERROR_CODES = setOf(
    MppErrorCode.NO_NETWORK_CONNECTION,
    MppErrorCode.GENERIC_NETWORK_PROBLEM,
    MppErrorCode.NETWORK_TIMEOUT,
    MppErrorCode.UNKNOWN_HOST,
    MppErrorCode.SSL_HANDSHAKE_FAILED,
    MppErrorCode.REMOTE_GATEWAY_TIMEOUT_ERROR,
)

internal val TokenInfo.state: GooglePayTokenState
    get() = GooglePayTokenState.getTokenState(tokenState)

internal val GooglePayTokenState.isInWallet: Boolean
    get() = this in IN_WALLET_TOKEN_STATES

internal val GooglePayTokenState.isAwaitingIdentityVerification: Boolean
    get() = this == GooglePayTokenState.TOKEN_STATE_NEEDS_IDENTITY_VERIFICATION

internal val MppError.isWalletUnsupported: Boolean
    get() = walletApiErrorCode == TapAndPayStatusCodes.TAP_AND_PAY_UNAVAILABLE

internal fun List<GooglePayTokenState>.toCardStatus(): CardStatus = when {
    any { it.isInWallet } -> CardStatus.Added
    any { it.isAwaitingIdentityVerification } -> CardStatus.Incomplete
    else -> CardStatus.NotAdded
}

internal fun MppError.toTokenLookup(): TokenLookup {
    val isCardMissingFromWallet = walletApiErrorCode == TapAndPayStatusCodes.TAP_AND_PAY_NO_ACTIVE_WALLET ||
        walletApiErrorCode == TapAndPayStatusCodes.TAP_AND_PAY_TOKEN_NOT_FOUND

    return if (isCardMissingFromWallet) {
        emptyList<TokenInfo>().right()
    } else {
        TokenLookupFailure(
            isWalletUnsupported = isWalletUnsupported,
            description = describe(),
            sdkErrorCode = code,
            walletErrorCode = walletErrorCodeOrNull,
        ).left()
    }
}

internal fun Throwable.toTokenLookup(): TokenLookup = (this as? MppException)?.mppError?.toTokenLookup()
    ?: TokenLookupFailure(isWalletUnsupported = false, description = describe()).left()

internal fun TokenLookupFailure.toAddResult(): AddResult.Failed = AddResult.Failed(
    reason = if (isWalletUnsupported) FailureReason.UNSUPPORTED else FailureReason.TOKEN_LOOKUP_FAILED,
    description = description,
    sdkErrorCode = sdkErrorCode,
    walletErrorCode = walletErrorCode,
)

internal fun MppError.toAddResult(): AddResult = when {
    code == MppErrorCode.OPERATION_CANCELLED_BY_USER ||
        walletApiErrorCode == TapAndPayStatusCodes.TAP_AND_PAY_USER_CANCELED_FLOW -> AddResult.Cancelled
    code == MppErrorCode.GOOGLE_PAY_TOKEN_ALREADY_EXISTS -> AddResult.AlreadyAdded
    else -> AddResult.Failed(
        reason = failureReason,
        description = describe(),
        sdkErrorCode = code,
        walletErrorCode = walletErrorCodeOrNull,
    )
}

internal fun Throwable.toAddResult(): AddResult = (this as? MppException)?.mppError?.toAddResult()
    ?: AddResult.Failed(reason = FailureReason.UNEXPECTED_ERROR, description = describe())

internal fun MppError.describe(): String = toString().replace(WHITESPACE, " ")

internal fun Throwable.describe(): String =
    (this as? MppException)?.mppError?.describe() ?: "${javaClass.simpleName}: $message"

private val MppError.walletErrorCodeOrNull: Int?
    get() = walletApiErrorCode.takeIf { it != NO_WALLET_ERROR_CODE }

private val MppError.failureReason: FailureReason
    get() = when {
        isWalletUnsupported -> FailureReason.UNSUPPORTED
        code in NETWORK_ERROR_CODES -> FailureReason.NETWORK_ERROR
        code == MppErrorCode.GOOGLE_PAY_ERROR || walletErrorCodeOrNull != null -> FailureReason.GOOGLE_WALLET_ERROR
        else -> FailureReason.MEAWALLET_ERROR
    }