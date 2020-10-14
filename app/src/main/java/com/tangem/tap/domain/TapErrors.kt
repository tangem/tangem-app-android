package com.tangem.tap.domain

import androidx.annotation.StringRes
import com.tangem.TangemError
import com.tangem.wallet.R

interface TapErrors

interface ArgError{
    val args: List<Any>?
}

interface MultiMessageError : TapErrors {
    val errorList: List<TapError>
    val builder: (List<String>) -> String
}

sealed class TapError(
        @StringRes val localizedMessage: Int,
        override val args: List<Any>? = null
) : Throwable(), TapErrors, ArgError {

    object UnknownError : TapError(R.string.send_error_unknown)
    object PayIdAlreadyCreated : TapError(R.string.wallet_create_payid_error_already_created)
    object PayIdCreatingError : TapError(R.string.wallet_create_payid_error_message)
    object PayIdEmptyField : TapError(R.string.wallet_create_payid_empty)
    object UnknownBlockchain : TapError(R.string.wallet_error_unsupported_blockchain_subtitle)
    object NoInternetConnection : TapError(R.string.wallet_notification_no_internet)
    object InsufficientBalance : TapError(R.string.send_error_insufficient_balance)
    object BlockchainInternalError : TapError(R.string.send_error_blockchain_internal)
    object AmountExceedsBalance : TapError(R.string.send_validation_amount_exceeds_balance)
    object FeeExceedsBalance : TapError(R.string.send_validation_invalid_fee)
    object TotalExceedsBalance : TapError(R.string.send_validation_invalid_total)
    object InvalidAmountValue : TapError(R.string.send_validation_invalid_amount)
    object InvalidFeeValue : TapError(R.string.send_error_invalid_fee_value)
    data class DustAmount(override val args: List<Any>) : TapError(R.string.send_error_dust_amount_format)
    object DustChange : TapError(R.string.send_error_dust_change)
    data class CreateAccountUnderfunded(override val args: List<Any>) : TapError(R.string.send_error_no_target_account)

    data class ValidateTransactionErrors(
            override val errorList: List<TapError>,
            override val builder: (List<String>) -> String
    ) : TapError(-1), MultiMessageError
}

sealed class TapSdkError(override val messageResId: Int?) : Throwable(), TangemError {
    final override val code: Int = 1
    override var customMessage: String = code.toString()

    object CardForDifferentApp : TapSdkError(R.string.alert_unsupported_card)
}


fun TapErrors.assembleErrors(): MutableList<Pair<Int, List<Any>?>> {
    val idList = mutableListOf<Pair<Int, List<Any>?>>()
    when (this) {
        is MultiMessageError -> this.errorList.forEach { idList.addAll(it.assembleErrors()) }
        is TapError -> idList.add(Pair(this.localizedMessage, this.args))
    }
    return idList
}