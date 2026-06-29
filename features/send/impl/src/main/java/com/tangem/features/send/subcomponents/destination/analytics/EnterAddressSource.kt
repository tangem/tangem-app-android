package com.tangem.features.send.subcomponents.destination.analytics

internal enum class EnterAddressSource {
    QRCode,
    PasteButton,
    RecentAddress,
    InputField,
    MyWallets,
    Contact,
    ;

    val isPasted: Boolean
        get() = this != InputField

    val isAutoNext: Boolean
        get() = this == RecentAddress || this == MyWallets || this == Contact
}