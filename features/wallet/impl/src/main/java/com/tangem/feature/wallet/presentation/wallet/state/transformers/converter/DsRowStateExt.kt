package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import androidx.compose.ui.graphics.vector.ImageVector
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.ds2.avatar.TangemAvatar
import com.tangem.core.ui.ds2.tokenicon.TangemTokenIcon
import com.tangem.core.ui.ds2.util.TangemPriceChange
import com.tangem.core.ui.extensions.stringReference
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
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.domain.models.account.CryptoPortfolioIcon
import java.math.BigDecimal

internal fun BigDecimal.toPriceChangeState(): TangemPriceChange.State = TangemPriceChange.State(
    value = stringReference(format { percent() }),
    direction = when (PriceChangeType.fromBigDecimal(this)) {
        PriceChangeType.UP -> TangemPriceChange.Direction.Up
        PriceChangeType.DOWN -> TangemPriceChange.Direction.Down
        PriceChangeType.NEUTRAL -> TangemPriceChange.Direction.Neutral
    },
)

// TODO([REDACTED_TASK_KEY]): replace with the account-avatar colour DS tokens once they are added
internal fun CryptoPortfolioIcon.Color.toFillColor(): TangemAvatar.FillColor = when (this) {
    CryptoPortfolioIcon.Color.Azure -> TangemAvatar.FillColor.DarkBlue
    CryptoPortfolioIcon.Color.CaribbeanBlue -> TangemAvatar.FillColor.LightBlue
    CryptoPortfolioIcon.Color.DullLavender -> TangemAvatar.FillColor.Lavender
    CryptoPortfolioIcon.Color.CandyGrapeFizz -> TangemAvatar.FillColor.Violet
    CryptoPortfolioIcon.Color.SweetDesire -> TangemAvatar.FillColor.LightPurple
    CryptoPortfolioIcon.Color.PalatinateBlue -> TangemAvatar.FillColor.DarkBlue
    CryptoPortfolioIcon.Color.FuchsiaNebula -> TangemAvatar.FillColor.DarkPurple
    CryptoPortfolioIcon.Color.MexicanPink -> TangemAvatar.FillColor.Pink
    CryptoPortfolioIcon.Color.Pelati -> TangemAvatar.FillColor.Red
    CryptoPortfolioIcon.Color.Pattypan -> TangemAvatar.FillColor.Yellow
    CryptoPortfolioIcon.Color.UFOGreen -> TangemAvatar.FillColor.LightGreen
    CryptoPortfolioIcon.Color.VitalGreen -> TangemAvatar.FillColor.DarkGreen
}

@Suppress("CyclomaticComplexMethod")
internal fun CryptoPortfolioIcon.Icon.toAvatarIcon(): ImageVector = when (this) {
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

internal fun CurrencyIconState.toTokenIconState(): TangemTokenIcon.UiState = when (this) {
    is CurrencyIconState.CoinIcon -> tokenIcon(url = url)
    is CurrencyIconState.TokenIcon -> tokenIcon(url = url)
    is CurrencyIconState.CustomTokenIcon -> tokenIcon(url = null)
    CurrencyIconState.Loading -> TangemTokenIcon.UiState.Shimmer
    else -> TangemTokenIcon.UiState.Error
}

private fun CurrencyIconState.tokenIcon(url: String?): TangemTokenIcon.UiState = TangemTokenIcon.UiState.Token(
    TangemTokenIcon.State(
        url = url,
        topIcon = topBadgeIconResId,
        isGrayscale = isGrayscale,
        indicator = TangemTokenIcon.State.Indicator().takeIf { shouldShowCustomBadge },
    ),
)