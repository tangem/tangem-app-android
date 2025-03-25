package com.tangem.features.send.v2.subcomponents.destination.model

import com.tangem.features.send.v2.subcomponents.destination.analytics.EnterAddressSource

internal interface SendDestinationClickIntents {

    fun onRecipientAddressValueChange(value: String, type: EnterAddressSource)

    fun onRecipientMemoValueChange(value: String, isValuePasted: Boolean = false)

    fun onQrCodeScanClick()
}