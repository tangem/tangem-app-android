package com.tangem.common.ui.account

import androidx.compose.ui.graphics.vector.ImageVector
import com.tangem.core.ui.ds2.avatar.TangemAvatar
import com.tangem.core.ui.res.generated.assets.il_account_airplane
import com.tangem.core.ui.res.generated.assets.il_account_beach
import com.tangem.core.ui.res.generated.assets.il_account_bookmark
import com.tangem.core.ui.res.generated.assets.il_account_clock
import com.tangem.core.ui.res.generated.assets.il_account_family
import com.tangem.core.ui.res.generated.assets.il_account_gift
import com.tangem.core.ui.res.generated.assets.il_account_heart
import com.tangem.core.ui.res.generated.assets.il_account_home
import com.tangem.core.ui.res.generated.assets.il_account_letter
import com.tangem.core.ui.res.generated.assets.il_account_money
import com.tangem.core.ui.res.generated.assets.il_account_package
import com.tangem.core.ui.res.generated.assets.il_account_rocket
import com.tangem.core.ui.res.generated.assets.il_account_safe
import com.tangem.core.ui.res.generated.assets.il_account_shirt
import com.tangem.core.ui.res.generated.assets.il_account_shopping_bag
import com.tangem.core.ui.res.generated.assets.il_account_star
import com.tangem.core.ui.res.generated.assets.il_account_user
import com.tangem.core.ui.res.generated.assets.il_account_wallet
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.CryptoPortfolioIcon

// TODO([REDACTED_TASK_KEY]): render the initial for Letter accounts and the pending-operation indicator
fun Account.CryptoPortfolio.toAvatarState(): TangemAvatar.UiState = TangemAvatar.UiState.Account(
    imageVector = icon.value.toAvatarIcon(),
    color = icon.color.toFillColor(),
)

fun CryptoPortfolioIcon.Color.toFillColor(): TangemAvatar.FillColor = when (this) {
    CryptoPortfolioIcon.Color.Azure -> TangemAvatar.FillColor.AzureBlue
    CryptoPortfolioIcon.Color.CaribbeanBlue -> TangemAvatar.FillColor.CaribbeanBlue
    CryptoPortfolioIcon.Color.DullLavender -> TangemAvatar.FillColor.DullLavender
    CryptoPortfolioIcon.Color.CandyGrapeFizz -> TangemAvatar.FillColor.CandyGrapeFizz
    CryptoPortfolioIcon.Color.SweetDesire -> TangemAvatar.FillColor.SweetDesire
    CryptoPortfolioIcon.Color.PalatinateBlue -> TangemAvatar.FillColor.PalatinateBlue
    CryptoPortfolioIcon.Color.FuchsiaNebula -> TangemAvatar.FillColor.FuchsiaNebula
    CryptoPortfolioIcon.Color.MexicanPink -> TangemAvatar.FillColor.MexicanPink
    CryptoPortfolioIcon.Color.Pelati -> TangemAvatar.FillColor.Pelati
    CryptoPortfolioIcon.Color.Pattypan -> TangemAvatar.FillColor.Pattypan
    CryptoPortfolioIcon.Color.UFOGreen -> TangemAvatar.FillColor.UfoGreen
    CryptoPortfolioIcon.Color.VitalGreen -> TangemAvatar.FillColor.VitalGreen
}

@Suppress("CyclomaticComplexMethod")
fun CryptoPortfolioIcon.Icon.toAvatarIcon(): ImageVector = when (this) {
    CryptoPortfolioIcon.Icon.Letter -> Icons.il_account_letter
    CryptoPortfolioIcon.Icon.Star -> Icons.il_account_star
    CryptoPortfolioIcon.Icon.User -> Icons.il_account_user
    CryptoPortfolioIcon.Icon.Family -> Icons.il_account_family
    CryptoPortfolioIcon.Icon.Wallet -> Icons.il_account_wallet
    CryptoPortfolioIcon.Icon.Money -> Icons.il_account_money
    CryptoPortfolioIcon.Icon.Home -> Icons.il_account_home
    CryptoPortfolioIcon.Icon.Safe -> Icons.il_account_safe
    CryptoPortfolioIcon.Icon.Beach -> Icons.il_account_beach
    CryptoPortfolioIcon.Icon.AirplaneMode -> Icons.il_account_airplane
    CryptoPortfolioIcon.Icon.Shirt -> Icons.il_account_shirt
    CryptoPortfolioIcon.Icon.ShoppingBasket -> Icons.il_account_shopping_bag
    CryptoPortfolioIcon.Icon.Favourite -> Icons.il_account_heart
    CryptoPortfolioIcon.Icon.Bookmark -> Icons.il_account_bookmark
    CryptoPortfolioIcon.Icon.StartUp -> Icons.il_account_rocket
    CryptoPortfolioIcon.Icon.Clock -> Icons.il_account_clock
    CryptoPortfolioIcon.Icon.Package -> Icons.il_account_package
    CryptoPortfolioIcon.Icon.Gift -> Icons.il_account_gift
}