package com.tangem.common.ui.amountScreen

/** Amount screen clicks */
interface AmountScreenClickIntents {

    /** On amount [value] changed */
    fun onAmountValueChange(value: String)

    /** Click triggered on value paste */
    fun onAmountPasteTriggerDismiss()

    /** On max amount click */
    fun onMaxValueClick()

    /**
     * On currency change from crypto currency to app currency clicked
     *
     * @param isFiat indicates currency to change
     */
    fun onCurrencyChangeClick(isFiat: Boolean)

    /** On next screen click */
    fun onAmountNext()
}