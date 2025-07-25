package com.tangem.domain.models.account

import com.tangem.domain.models.account.CryptoPortfolioIcon.Companion.ofCustomAccount
import com.tangem.domain.models.account.CryptoPortfolioIcon.Companion.ofMainAccount
import kotlinx.serialization.Serializable

/**
 * Represents an icon for an [Account.CryptoPortfolio] account
 *
 * @property type  the type of the account icon
 * @property color the color of the account icon
 *
 * @constructor [ofMainAccount], [ofCustomAccount]
 *
[REDACTED_AUTHOR]
 */
@Serializable
data class CryptoPortfolioIcon private constructor(
    val type: Type,
    val color: Color,
) {

    /**
     * Represents the type of an account icon. Can either be a specific [Icon] or a [Symbol]
     */
    @Serializable
    sealed interface Type {

        /**
         * Represents a specific predefined icon type
         *
         * @property value the predefined [Icon] of the icon
         */
        @Serializable
        data class Icon(val value: CryptoPortfolioIcon.Icon) : Type

        /**
         * Represents an icon with a letter
         *
         * @property value the letter used as the icon
         */
        @Serializable
        data class Symbol(val value: Char) : Type
    }

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
        ;
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
        Pelati,
        Pattypan,
        UFOGreen,
        SweetHoney,
        ;
    }

    companion object {

        private val defaultMainAccountType: Icon = Icon.Star
        private val defaultMainAccountColor: Color = Color.Azure

        /**
         * Creates an [CryptoPortfolioIcon] for the Main account, ensuring the color is not in the excluded set.
         *
         * @param exclude excluded colors that are already used for main accounts
         */
        fun ofMainAccount(exclude: Set<Color>): CryptoPortfolioIcon {
            val isDefaultColorBusy = defaultMainAccountColor in exclude

            val color = if (isDefaultColorBusy) {
                val colorsWithExcluded = Color.entries - exclude

                val availableColors = if (colorsWithExcluded.isNotEmpty()) {
                    colorsWithExcluded
                } else {
                    Color.entries
                }

                availableColors.random()
            } else {
                defaultMainAccountColor
            }

            return CryptoPortfolioIcon(
                type = Type.Icon(value = defaultMainAccountType),
                color = color,
            )
        }

        /**
         * Creates an [CryptoPortfolioIcon] for a user account based on the account name
         *
         * @param accountName the name of the account, used to determine the letter for the icon
         */
        fun ofCustomAccount(accountName: String): CryptoPortfolioIcon {
            val color = Color.entries.random()

            return CryptoPortfolioIcon(
                type = Type.Symbol(value = accountName.first()),
                color = color,
            )
        }

        /**
         * Creates a [CryptoPortfolioIcon] for a user account with a specific type and color
         *
         * @param type  the type of the account icon
         * @param color the color of the account icon
         */
        fun ofCustomAccount(type: Type, color: Color): CryptoPortfolioIcon {
            return CryptoPortfolioIcon(type = type, color = color)
        }
    }
}