package com.tangem.tap.domain

import androidx.annotation.StringRes
import com.tangem.common.core.TangemError
import com.tangem.wallet.R

interface TapErrors

interface ArgError {
    val args: List<Any>?
}

interface MultiMessageError : TapErrors {
    val errorList: List<TapError>
    val builder: (List<String>) -> String
}

sealed class TapError(
    @StringRes val messageResource: Int,
    override val args: List<Any>? = null,
) : Throwable(), TapErrors, ArgError {

    object UnknownError : TapError(R.string.send_error_unknown)
    open class CustomError(val customMessage: String) : TapError(R.string.common_custom_string, listOf(customMessage))
    object ScanCardError : TapError(R.string.scan_card_error)
    object UnknownBlockchain : TapError(R.string.wallet_error_unsupported_blockchain_subtitle)
    object NoInternetConnection : TapError(R.string.wallet_notification_no_internet)
    object BlockchainInternalError : TapError(R.string.send_error_blockchain_internal)
    object AmountExceedsBalance : TapError(R.string.send_validation_amount_exceeds_balance)
    data class AmountLowerExistentialDeposit(
        override val args: List<Any>,
    ) : TapError(R.string.send_error_minimum_balance_format)

    object FeeExceedsBalance : TapError(R.string.send_validation_invalid_fee)
    object TotalExceedsBalance : TapError(R.string.send_validation_invalid_total)
    object InvalidAmountValue : TapError(R.string.send_validation_invalid_amount)
    object InvalidFeeValue : TapError(R.string.send_error_invalid_fee_value)
    data class DustAmount(override val args: List<Any>) : TapError(R.string.send_error_dust_amount_format)
    object DustChange : TapError(R.string.send_error_dust_change)

    data class UnsupportedState(
        val stateError: String,
        val customMessage: String = "Unsupported state:",
    ) : TapError(R.string.common_custom_string, listOf("$customMessage $stateError"))

    sealed class WalletManager {
        object CreationError : CustomError("Can't create wallet manager")
        class NoAccountError(amountToCreateAccount: String) : CustomError(amountToCreateAccount)
        class InternalError(message: String) : CustomError(message)
        object BlockchainIsUnreachableTryLater : TapError(R.string.wallet_balance_blockchain_unreachable_try_later)
    }

    sealed class WalletConnect {
        object UnsupportedDapp : TapError(R.string.wallet_connect_error_unsupported_dapp)
        object UnsupportedLink : TapError(R.string.wallet_connect_error_failed_to_connect)
    }

    data class ValidateTransactionErrors(
        override val errorList: List<TapError>,
        override val builder: (List<String>) -> String,
    ) : TapError(-1), MultiMessageError
}

sealed class TapSdkError(override val messageResId: Int?) : TangemError(code = 50100) {
    override var customMessage: String = code.toString()

    object CardForDifferentApp : TapSdkError(R.string.alert_unsupported_card)
    object CardNotSupportedByRelease : TapSdkError(R.string.error_update_app)
}

fun TapErrors.assembleErrors(): MutableList<Pair<Int, List<Any>?>> {
    val idList = mutableListOf<Pair<Int, List<Any>?>>()
    when (this) {
        is MultiMessageError -> this.errorList.forEach { idList.addAll(it.assembleErrors()) }
        is TapError -> idList.add(Pair(this.messageResource, this.args))
    }
    return idList
}
