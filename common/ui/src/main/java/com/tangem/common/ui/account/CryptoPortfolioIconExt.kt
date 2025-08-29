package com.tangem.common.ui.account

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.tangem.core.ui.R
import com.tangem.domain.models.account.CryptoPortfolioIcon

/** Extension function to get the UI color associated with an [CryptoPortfolioIcon.Color] */
fun CryptoPortfolioIcon.Color.getUiColor(): Color {
    return when (this) {
        CryptoPortfolioIcon.Color.Azure -> Color(color = 0xFF007FFF)
        CryptoPortfolioIcon.Color.CaribbeanBlue -> Color(color = 0xFF0FBED6)
        CryptoPortfolioIcon.Color.DullLavender -> Color(color = 0xFFA28BF2)
        CryptoPortfolioIcon.Color.CandyGrapeFizz -> Color(color = 0xFF704AF1)
        CryptoPortfolioIcon.Color.SweetDesire -> Color(color = 0xFFAF34E3)
        CryptoPortfolioIcon.Color.PalatinateBlue -> Color(color = 0xFF2D30EE)
        CryptoPortfolioIcon.Color.FuchsiaNebula -> Color(color = 0xFF7E28A3)
        CryptoPortfolioIcon.Color.MexicanPink -> Color(color = 0xFFE6007A)
        CryptoPortfolioIcon.Color.Pelati -> Color(color = 0xFFFF2E32)
        CryptoPortfolioIcon.Color.Pattypan -> Color(color = 0xFFF0B90B)
        CryptoPortfolioIcon.Color.UFOGreen -> Color(color = 0xFF32CB77)
        CryptoPortfolioIcon.Color.VitalGreen -> Color(color = 0xFF15865B)
    }
}

/** Extension function to get the drawable resource ID associated with an [CryptoPortfolioIcon.Icon] */
@Suppress("CyclomaticComplexMethod")
@DrawableRes
fun CryptoPortfolioIcon.Icon.getResId(): Int {
    return when (this) {
        CryptoPortfolioIcon.Icon.Letter -> R.drawable.ic_letter_24
        CryptoPortfolioIcon.Icon.Star -> R.drawable.ic_rounded_star_24
        CryptoPortfolioIcon.Icon.User -> R.drawable.ic_user_24
        CryptoPortfolioIcon.Icon.Family -> R.drawable.ic_family_24
        CryptoPortfolioIcon.Icon.Wallet -> R.drawable.ic_wallet_24
        CryptoPortfolioIcon.Icon.Money -> R.drawable.ic_money_24
        CryptoPortfolioIcon.Icon.Home -> R.drawable.ic_home_24
        CryptoPortfolioIcon.Icon.Safe -> R.drawable.ic_safe_24
        CryptoPortfolioIcon.Icon.Beach -> R.drawable.ic_beach_24
        CryptoPortfolioIcon.Icon.AirplaneMode -> R.drawable.ic_airplane_mode_24
        CryptoPortfolioIcon.Icon.Shirt -> R.drawable.ic_shirt_24
        CryptoPortfolioIcon.Icon.ShoppingBasket -> R.drawable.ic_shopping_basket_24
        CryptoPortfolioIcon.Icon.Favourite -> R.drawable.ic_favourite_24
        CryptoPortfolioIcon.Icon.Bookmark -> R.drawable.ic_bookmark_24
        CryptoPortfolioIcon.Icon.StartUp -> R.drawable.ic_start_up_24
        CryptoPortfolioIcon.Icon.Clock -> R.drawable.ic_circle_clock_24
        CryptoPortfolioIcon.Icon.Package -> R.drawable.ic_package_24
        CryptoPortfolioIcon.Icon.Gift -> R.drawable.ic_gift_24
    }
}

fun CryptoPortfolioIcon.toUM() = CryptoPortfolioIconUM(domainModel = this)
fun CryptoPortfolioIconUM.toDomain() = CryptoPortfolioIcon.ofCustomAccount(value = this.value, color = this.color)