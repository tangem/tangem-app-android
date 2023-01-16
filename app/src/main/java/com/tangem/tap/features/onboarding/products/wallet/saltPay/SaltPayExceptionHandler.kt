package com.tangem.tap.features.onboarding.products.wallet.saltPay

import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.common.core.TangemSdkError
import com.tangem.tap.common.extensions.dispatchDialogShow
import com.tangem.tap.common.redux.AppDialog
import com.tangem.tap.features.onboarding.products.wallet.saltPay.dialog.SaltPayDialog
import com.tangem.tap.features.onboarding.products.wallet.saltPay.message.SaltPayActivationError
import com.tangem.tap.store

/**
 * Created by Anton Zhilenkov on 22.10.2022.
 */
object SaltPayExceptionHandler {
    fun handle(throwable: Throwable) {
        when (throwable) {
            is SaltPayActivationError -> {
                val dialog = when (throwable) {
                    is SaltPayActivationError.NoGas -> SaltPayDialog.Activation.NoGas
                    else -> SaltPayDialog.Activation.OnError(throwable)
                }
                store.dispatchDialogShow(dialog)
            }
            is TangemSdkError -> {
                when (throwable) {
                    is TangemSdkError.NetworkError -> {
                        val message = throwable.customMessage
                        store.dispatchDialogShow(AppDialog.SimpleOkErrorDialog(message))
                    }
                    else -> {
                        // do nothing
                    }
                }
            }
            is BlockchainSdkError -> {
                store.dispatchDialogShow(AppDialog.SimpleOkErrorDialog(throwable.customMessage))
            }
            else -> {
                val message = throwable.localizedMessage ?: "SaltPay unknown error"
                store.dispatchDialogShow(AppDialog.SimpleOkErrorDialog(message))
            }
        }
    }
}
