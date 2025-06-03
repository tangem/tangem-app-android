package com.tangem.features.send.v2.subcomponents.destination.analytics

internal enum class EnterAddressSource {
    QRCode,
    PasteButton,
    RecentAddress,
    InputField,
    MyWallets,
    ;

    val isPasted: Boolean
        get() = this != InputField
}