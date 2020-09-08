package com.tangem.tap.features.send.redux

import android.view.View
import com.tangem.tap.common.entities.TapCurrency
import com.tangem.wallet.R
import org.rekotlin.StateType
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
data class SendState(
        val lastChangedStateType: StateType = NoneState(),
        val addressPayIdState: AddressPayIdState = AddressPayIdState(),
        val amountState: AmountState = AmountState(),
        val feeLayoutState: FeeLayoutState = FeeLayoutState(),
        val sendButtonIsEnabled: Boolean = false
) : StateType

class NoneState : StateType

data class AddressPayIdState(
        val etFieldValue: String? = null,
        val normalFieldValue: String? = null,
        val truncatedFieldValue: String? = null,
        val walletAddress: String? = null,
        val error: AddressPayIdVerifyAction.FailReason? = null,
        val truncateHandler: ((String) -> String)? = null
) : StateType {

    fun isPayIdState(): Boolean = walletAddress != null && walletAddress != normalFieldValue

    fun copyWalletAddress(address: String): AddressPayIdState {
        val truncated = truncateHandler?.invoke(address) ?: address
        return this.copy(
                etFieldValue = address,
                normalFieldValue = address,
                truncatedFieldValue = truncated,
                walletAddress = address,
                error = null
        )
    }

    fun copyError(address: String, error: AddressPayIdVerifyAction.FailReason): AddressPayIdState {
        val truncated = truncateHandler?.invoke(address) ?: address
        return this.copy(
                etFieldValue = address,
                normalFieldValue = address,
                truncatedFieldValue = truncated,
                error = error,
                walletAddress = null
        )
    }

    fun copyPayIdWalletAddress(payId: String, address: String): AddressPayIdState {
        val truncated = truncateHandler?.invoke(address) ?: address
        return this.copy(
                etFieldValue = payId,
                normalFieldValue = payId,
                truncatedFieldValue = truncated,
                walletAddress = address,
                error = null
        )
    }

    fun copyPaiIdError(payId: String, error: AddressPayIdVerifyAction.FailReason): AddressPayIdState {
        val truncated = truncateHandler?.invoke(payId) ?: payId
        return this.copy(
                etFieldValue = payId,
                normalFieldValue = payId,
                truncatedFieldValue = truncated,
                error = error,
                walletAddress = null
        )
    }
}

enum class MainCurrencyType {
    FIAT, CRYPTO
}

data class Value<T>(
        val value: T,
        val displayedValue: String
)

data class AmountState(
        val etAmountFieldValue: String = BigDecimal.ZERO.toPlainString(),
        val cursorAtTheSamePosition: Boolean = true,
        val amountToSendCrypto: BigDecimal = BigDecimal.ZERO,
        val balance: BigDecimal = BigDecimal.ZERO,
        val mainCurrency: Value<MainCurrencyType> = Value(MainCurrencyType.FIAT, TapCurrency.main),
        val amountIsOverBalance: Boolean = false
) : StateType

data class FeeLayoutState(
        val visibility: Int = View.GONE,
        val selectedFeeId: Int = R.id.chipNormal,
        val includeFeeIsChecked: Boolean = false
) : StateType