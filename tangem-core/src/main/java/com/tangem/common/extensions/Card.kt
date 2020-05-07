package com.tangem.common.extensions

import com.tangem.commands.Card

fun Card.getType(): CardType {
    val firmware = this.firmwareVersion ?: return CardType.Unknown
    return when {
        firmware.endsWith("d SDK") -> {
            CardType.Sdk
        }
        firmware.endsWith("r") -> {
            CardType.Release
        }
        else -> {
            CardType.Unknown
        }
    }
}

enum class CardType {
    Sdk, Release, Unknown
}