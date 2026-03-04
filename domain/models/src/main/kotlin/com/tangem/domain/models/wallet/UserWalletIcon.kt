package com.tangem.domain.models.wallet

/**
 * Represents the icon of a user wallet, which can be of different types such as hot, stub, default, or colored.
 */
sealed class UserWalletIcon {
    data object Hot : UserWalletIcon()
    data class Stub(val cardsCount: Int) : UserWalletIcon()

    data class Default(
        val isRing: Boolean,
        val cardsCount: Int,
    ) : UserWalletIcon()

    data class Colored(
        val isRing: Boolean,
        val mainColor: String,
        val secondColor: String? = null,
        val thirdColor: String? = null,
    ) : UserWalletIcon()
}