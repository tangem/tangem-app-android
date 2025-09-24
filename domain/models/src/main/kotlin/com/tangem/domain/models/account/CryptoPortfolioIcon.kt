package com.tangem.domain.models.account

import com.tangem.domain.models.account.CryptoPortfolioIcon.Companion.ofDefaultCustomAccount
import com.tangem.domain.models.account.CryptoPortfolioIcon.Companion.ofMainAccount
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.serialization.Serializable

/**
 * Represents an icon for an [Account.CryptoPortfolio] account
 *
 * @property value the type of the account icon
 * @property color the color of the account icon
 *
 * @constructor [ofMainAccount], [ofDefaultCustomAccount]
 *
[REDACTED_AUTHOR]
 */
@Serializable
data class CryptoPortfolioIcon private constructor(
    val value: Icon,
    val color: Color,
) {

    /**
     * Enum class representing the icons of accounts
     */
    @Serializable
    enum class Icon {
        Letter,
        Star,
        User,
        Family,
        Wallet,
        Money,
        Home,
        Safe,
        Beach,
        AirplaneMode,
        Shirt,
        ShoppingBasket,
        Favourite,
        Bookmark,
        StartUp,
        Clock,
        Package,
        Gift,
    }

    /**
     * Enum class representing the colors of account icons
     */
    @Serializable
    enum class Color {
        Azure,
        CaribbeanBlue,
        DullLavender,
        CandyGrapeFizz,
        SweetDesire,
        PalatinateBlue,
        FuchsiaNebula,
        MexicanPink,
        Pelati,
        Pattypan,
        UFOGreen,
        VitalGreen,
    }

    companion object {

        private val defaultMainAccountIcon: Icon = Icon.Star
        private val excludedCustomAccountIcons: Set<Icon> = setOf(Icon.Letter, Icon.Star)
        private const val HASH_MULTIPLIER = 31

        /**
         * Creating a [CryptoPortfolioIcon] for the Main account with default values.
         * The color is derived from the [UserWalletId].
         *
         * @param userWalletId the ID of the user wallet
         */
        fun ofMainAccount(userWalletId: UserWalletId): CryptoPortfolioIcon {
            val colors = Color.entries
            val hash = userWalletId.value.fold(0) { acc, byte -> acc * HASH_MULTIPLIER + byte }

            val index = (hash and Int.MAX_VALUE) % colors.size
            val color = colors[index]

            return CryptoPortfolioIcon(value = defaultMainAccountIcon, color = color)
        }

        /**
         * Creates an [CryptoPortfolioIcon] for a user account based on the account name
         */
        fun ofDefaultCustomAccount(): CryptoPortfolioIcon {
            val icon = (Icon.entries - excludedCustomAccountIcons).random()
            val color = Color.entries.random()

            return CryptoPortfolioIcon(value = icon, color = color)
        }

        /**
         * Creates a [CryptoPortfolioIcon] for a user account with a specific type and color
         *
         * @param value  the icon of the account
         * @param color the color of the account icon
         */
        fun ofCustomAccount(value: Icon, color: Color): CryptoPortfolioIcon {
            return CryptoPortfolioIcon(value = value, color = color)
        }
    }
}