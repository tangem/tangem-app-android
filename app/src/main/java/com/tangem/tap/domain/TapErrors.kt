package com.tangem.tap.domain

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
    code: Int,
    override val messageResId: Int,
    override val args: List<Any>? = null,
) : TangemError(code), TapErrors, ArgError {

    override var customMessage: String = ""

    object UnknownError : TapError(code = 70001, R.string.send_error_unknown)
    open class CustomError(override var customMessage: String) : TapError(
        code = 70002,
        messageResId = R.string.common_custom_string,
        args = listOf(customMessage),
    )

    object ScanCardError : TapError(code = 70003, R.string.scan_card_error)
    object UnknownBlockchain : TapError(code = 70004, R.string.wallet_error_unsupported_blockchain_subtitle)
    object NoInternetConnection : TapError(code = 70005, R.string.wallet_notification_no_internet)
    object BlockchainInternalError : TapError(code = 70006, R.string.send_error_blockchain_internal)
    object AmountExceedsBalance : TapError(code = 70007, R.string.send_validation_amount_exceeds_balance)
    data class AmountLowerExistentialDeposit(
        override val args: List<Any>,
    ) : TapError(code = 70008, R.string.send_error_minimum_balance_format)

    object FeeExceedsBalance : TapError(code = 70009, R.string.send_validation_invalid_fee)
    object TotalExceedsBalance : TapError(code = 70010, R.string.send_validation_invalid_total)
    object InvalidAmountValue : TapError(code = 70011, R.string.send_validation_invalid_amount)
    object InvalidFeeValue : TapError(code = 70012, R.string.send_error_invalid_fee_value)
    data class DustAmount(override val args: List<Any>) : TapError(code = 70013, R.string.send_error_dust_amount_format)
    object DustChange : TapError(code = 70014, R.string.send_error_dust_change)

    data class UnsupportedState(
        val stateError: String,
        override var customMessage: String = "Unsupported state:",
    ) : TapError(code = 70015, R.string.common_custom_string, listOf("$customMessage $stateError"))

    sealed class WalletManager {
        object CreationError : CustomError("Can't create wallet manager")
        class NoAccountError(amountToCreateAccount: String) : CustomError(amountToCreateAccount)
        class InternalError(message: String) : CustomError(message)
        object BlockchainIsUnreachableTryLater : TapError(
            code = 70016,
            messageResId = R.string.wallet_balance_blockchain_unreachable_try_later,
        )
    }

    sealed class WalletConnect {
        object UnsupportedDapp : TapError(code = 70017, R.string.wallet_connect_error_unsupported_dapp)
        object UnsupportedLink : TapError(code = 70018, R.string.wallet_connect_error_failed_to_connect)
    }

    data class ValidateTransactionErrors(
        override val errorList: List<TapError>,
        override val builder: (List<String>) -> String,
    ) : TapError(code = 70019, messageResId = -1), MultiMessageError
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
        is TapError -> idList.add(Pair(this.messageResId, this.args))
    }
    return idList
}
