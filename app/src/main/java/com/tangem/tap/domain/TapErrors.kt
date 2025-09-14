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

    class UnknownError : TapError(R.string.send_error_unknown)
    open class CustomError(val customMessage: String) : TapError(R.string.common_custom_string, listOf(customMessage))

    class NoInternetConnection : TapError(R.string.wallet_notification_no_internet)

    sealed class WalletManager {
        class NoAccountError(amountToCreateAccount: String) : CustomError(amountToCreateAccount)
        class InternalError(message: String) : CustomError(message)
        class BlockchainIsUnreachableTryLater : TapError(R.string.wallet_balance_blockchain_unreachable_try_later)
    }
}

sealed class TapSdkError(override val messageResId: Int?) : TangemError(code = 50100) {
    override var customMessage: String = code.toString()

    class CardForDifferentApp : TapSdkError(R.string.alert_unsupported_card)
    class CardNotSupportedByRelease : TapSdkError(R.string.error_wrong_card_type)
}

fun TapErrors.assembleErrors(): MutableList<Pair<Int, List<Any>?>> {
    val idList = mutableListOf<Pair<Int, List<Any>?>>()
    when (this) {
        is MultiMessageError -> this.errorList.forEach { idList.addAll(it.assembleErrors()) }
        is TapError -> idList.add(Pair(this.messageResource, this.args))
    }
    return idList
}