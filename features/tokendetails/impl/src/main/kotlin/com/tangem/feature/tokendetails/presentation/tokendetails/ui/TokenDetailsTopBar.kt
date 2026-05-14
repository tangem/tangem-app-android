package com.tangem.feature.tokendetails.presentation.tokendetails.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import com.tangem.common.ui.account.AccountIcon
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.core.ui.components.account.AccountIconSize
import com.tangem.core.ui.components.dropdownmenu.TangemDropdownItem
import com.tangem.core.ui.components.dropdownmenu.TangemDropdownMenu
import com.tangem.core.ui.components.dropdownmenu.TangemDropdownMenuItem
import com.tangem.core.ui.ds.image.DeviceIconUM
import com.tangem.core.ui.ds.image.TangemDeviceIcon
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.ds.topbar.TangemTopBarActionContent
import com.tangem.core.ui.ds.topbar.TangemTopBarActionUM
import com.tangem.core.ui.extensions.resolveAnnotatedReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.extensions.themedColor
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsTopAppBarUM
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsTopAppBarUM.TitleState
import kotlinx.collections.immutable.persistentListOf
import com.tangem.core.ui.R as CoreUiR

@Composable
internal fun TokenDetailsTopBar(topAppBarUM: TokenDetailsTopAppBarUM, modifier: Modifier = Modifier) {
    TangemTopBar(
        modifier = modifier.statusBarsPadding(),
        startContent = {
            TangemTopBarActionContent(
                actionUM = TangemTopBarActionUM(
                    iconRes = CoreUiR.drawable.ic_back_24,
                    onClick = topAppBarUM.onBackClick,
                    ghostModeProgress = 1f,
                ),
            )
        },
        endContent = if (topAppBarUM.menuItems.isNotEmpty()) {
            {
                var isDropdownMenuShown by rememberSaveable { mutableStateOf(false) }
                Box {
                    TangemTopBarActionContent(
                        actionUM = TangemTopBarActionUM(
                            iconRes = CoreUiR.drawable.ic_more_default_24,
                            onClick = { isDropdownMenuShown = true },
                            ghostModeProgress = 1f,
                        ),
                    )
                    TangemDropdownMenu(
                        expanded = isDropdownMenuShown,
                        modifier = Modifier.background(TangemTheme.colors.background.primary),
                        onDismissRequest = { isDropdownMenuShown = false },
                        content = {
                            topAppBarUM.menuItems.fastForEach { menuItem ->
                                TangemDropdownItem(
                                    item = menuItem,
                                    dismissParent = { isDropdownMenuShown = false },
                                )
                            }
                        },
                    )
                }
            }
        } else {
            null
        },
        content = {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = TangemTheme.dimens2.x1),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x0_5),
            ) {
                TokenDetailsTitle(titleState = topAppBarUM.titleState)
                Text(
                    text = topAppBarUM.subtitle.resolveAnnotatedReference(),
                    color = TangemTheme.colors2.text.neutral.secondary,
                    style = TangemTheme.typography2.captionMedium12,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
    )
}

@Composable
private fun TokenDetailsTitle(titleState: TitleState) {
    val appearance = TitleAppearance(
        style = TangemTheme.typography2.bodySemibold16,
        iconSize = TangemTheme.dimens2.x5,
        spacing = TangemTheme.dimens2.x1,
    )

    when (titleState) {
        is TitleState.Simple -> {
            Text(
                text = titleState.tokenName,
                color = TangemTheme.colors2.text.neutral.primary,
                style = appearance.style,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = MIN_TITLE_FONT_SIZE,
                    maxFontSize = MAX_TITLE_FONT_SIZE,
                ),
            )
        }
        is TitleState.WithWallet -> {
            AdaptiveTokenWithSecondaryRow(
                tokenName = titleState.tokenName,
                secondaryName = titleState.walletName,
                template = stringResourceSafe(
                    id = CoreUiR.string.token_details_toolbar_title_token_in_wallet,
                    titleState.tokenName,
                    titleState.walletName,
                ),
                appearance = appearance,
                icon = {
                    TangemDeviceIcon(
                        state = titleState.deviceIconUM,
                        modifier = Modifier.size(appearance.iconSize),
                    )
                },
            )
        }
        is TitleState.WithAccount -> {
            val accountNameStr = titleState.accountName.resolveAnnotatedReference().toString()
            AdaptiveTokenWithSecondaryRow(
                tokenName = titleState.tokenName,
                secondaryName = accountNameStr,
                template = stringResourceSafe(
                    id = CoreUiR.string.token_details_toolbar_title_token_in_account,
                    titleState.tokenName,
                    accountNameStr,
                ),
                appearance = appearance,
                icon = {
                    AccountIcon(
                        name = titleState.accountName,
                        icon = titleState.accountIconUM,
                        size = AccountIconSize.ExtraSmall,
                    )
                },
            )
        }
    }
}

/**
 * Adaptive title for [TitleState.WithAccount] / [TitleState.WithWallet].
 *
 * Phrase template carries the [IMAGE_PLACEHOLDER] marker — translator decides where
 * the icon sits (e.g. "Tether in [⭐] Portfolio" or "Tether in My Wallet [⭐]").
 * RTL is handled by BiDi inside the single [Text].
 *
 * Width-driven cascade:
 *  1–2. Full phrase as single [Text] with inline icon; [TextOverflow.Ellipsis]
 *       trims the secondary name tail when needed.
 *  3.   No meaningful tail left → fall back to `[tokenName] [icon]` in a [Row];
 *       icon is a sibling so ellipsis trims only tokenName, never the icon.
 *  4–5. tokenName itself doesn't fit → [TextAutoSize] shrinks to
 *       [MIN_TITLE_FONT_SIZE], then [TextOverflow.Ellipsis] tails.
 *
 * #1/#2 vs #3 is decided here via [rememberTextMeasurer]; #4/#5 are delegated
 * to [TextAutoSize] + [TextOverflow.Ellipsis] in the fallback branch.
 */
@Composable
private fun AdaptiveTokenWithSecondaryRow(
    tokenName: String,
    secondaryName: String,
    template: String,
    appearance: TitleAppearance,
    icon: @Composable () -> Unit,
) {
    val (beforeIcon, afterIcon) = remember(template) { splitTemplate(template) }
    val inlineContent = rememberIconInlineContent(appearance.iconSize, icon)

    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        val isFullTextShown = rememberShouldShowFullText(
            beforeIcon = beforeIcon,
            afterIcon = afterIcon,
            secondaryName = secondaryName,
            appearance = appearance,
            maxWidthPx = constraints.maxWidth,
        )
        if (isFullTextShown) {
            FullPhraseTitle(
                beforeIcon = beforeIcon,
                afterIcon = afterIcon,
                style = appearance.style,
                inlineContent = inlineContent,
            )
        } else {
            FallbackTokenWithIconTitle(
                tokenName = tokenName,
                appearance = appearance,
                icon = icon,
            )
        }
    }
}

private fun splitTemplate(template: String): Pair<String, String> {
    val parts = template.split(IMAGE_PLACEHOLDER, limit = 2)
    return if (parts.size == 2) parts[0] to parts[1] else template to ""
}

@Composable
private fun rememberIconInlineContent(iconSize: Dp, icon: @Composable () -> Unit): Map<String, InlineTextContent> {
    val iconSizeSp = with(LocalDensity.current) { iconSize.toSp() }
    val currentIcon by rememberUpdatedState(icon)
    return remember(iconSizeSp) {
        mapOf(
            ICON_INLINE_ID to InlineTextContent(
                placeholder = Placeholder(
                    width = iconSizeSp,
                    height = iconSizeSp,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                ),
                children = {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        currentIcon()
                    }
                },
            ),
        )
    }
}

@Composable
private fun rememberShouldShowFullText(
    beforeIcon: String,
    afterIcon: String,
    secondaryName: String,
    appearance: TitleAppearance,
    maxWidthPx: Int,
): Boolean {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(beforeIcon, afterIcon, secondaryName, maxWidthPx, appearance, density) {
        if (maxWidthPx <= 0) return@remember true
        val fullTextWidthPx = measurer
            .measure(text = beforeIcon + afterIcon, style = appearance.style, softWrap = false)
            .size.width
        val secondaryWidthPx = measurer
            .measure(text = secondaryName, style = appearance.style, softWrap = false)
            .size.width
        val staticWidthPx = (fullTextWidthPx - secondaryWidthPx).coerceAtLeast(0)
        val iconReservePx = with(density) {
            (appearance.iconSize + appearance.spacing * 2).toPx()
        }.toInt()
        val minSecondaryPx = with(density) { MIN_SECONDARY_NAME_WIDTH.toPx() }.toInt()
        staticWidthPx + iconReservePx + minSecondaryPx <= maxWidthPx
    }
}

@Composable
private fun FullPhraseTitle(
    beforeIcon: String,
    afterIcon: String,
    style: TextStyle,
    inlineContent: Map<String, InlineTextContent>,
) {
    val fullText = remember(beforeIcon, afterIcon) {
        buildAnnotatedString {
            append(beforeIcon)
            appendInlineContent(ICON_INLINE_ID, IMAGE_PLACEHOLDER)
            append(afterIcon)
        }
    }
    Text(
        text = fullText,
        inlineContent = inlineContent,
        color = TangemTheme.colors2.text.neutral.primary,
        style = style,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun FallbackTokenWithIconTitle(tokenName: String, appearance: TitleAppearance, icon: @Composable () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(appearance.spacing, Alignment.CenterHorizontally),
    ) {
        Text(
            text = tokenName,
            color = TangemTheme.colors2.text.neutral.primary,
            style = appearance.style,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            autoSize = TextAutoSize.StepBased(
                minFontSize = MIN_TITLE_FONT_SIZE,
                maxFontSize = MAX_TITLE_FONT_SIZE,
            ),
            modifier = Modifier.weight(weight = 1f, fill = false),
        )
        icon()
    }
}

@Immutable
private data class TitleAppearance(
    val style: TextStyle,
    val iconSize: Dp,
    val spacing: Dp,
)

private const val ICON_INLINE_ID = "account_icon"
private const val IMAGE_PLACEHOLDER = "%image%"

private val MIN_TITLE_FONT_SIZE = 12.sp
private val MAX_TITLE_FONT_SIZE = 16.sp
private val MIN_SECONDARY_NAME_WIDTH = 48.dp

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun TokenDetailsTopBar_Preview(
    @PreviewParameter(TokenDetailsTopBarPreviewProvider::class) titleState: TitleState,
) {
    TangemThemePreviewRedesign {
        TokenDetailsTopBar(
            topAppBarUM = TokenDetailsTopAppBarUM(
                titleState = titleState,
                subtitle = stringReference("ERC-20 in Ethereum network"),
                onBackClick = {},
                menuItems = persistentListOf(
                    TangemDropdownMenuItem(
                        title = stringReference("Hide Token"),
                        textColor = themedColor { TangemTheme.colors.text.warning },
                        onClick = {},
                    ),
                ),
            ),
        )
    }
}

private class TokenDetailsTopBarPreviewProvider : PreviewParameterProvider<TitleState> {
    override val values: Sequence<TitleState>
        get() = sequenceOf(
            // === Base title states ===
            // Simple — 1 wallet, 1 account
            TitleState.Simple(tokenName = "Tether"),
            // WithWallet — N wallets, 1 account
            TitleState.WithWallet(
                tokenName = "Tether",
                walletName = "Tangem wallet",
                deviceIconUM = DeviceIconUM.Card(
                    mainColor = Color.DarkGray,
                    secondColor = null,
                ),
            ),
            // WithAccount — 1 wallet, N accounts
            TitleState.WithAccount(
                tokenName = "Tether",
                accountName = stringReference("Portfolio"),
                accountIconUM = AccountIconUM.CryptoPortfolio(
                    value = CryptoPortfolioIcon.Icon.Star,
                    color = CryptoPortfolioIcon.Color.Azure,
                ),
            ),
            // === AdaptiveTokenTitleRow cascade cases ===
            // Cascade #1 — full text fits as is (short token + short wallet)
            TitleState.WithWallet(
                tokenName = "BTC",
                walletName = "Main",
                deviceIconUM = DeviceIconUM.Card(
                    mainColor = Color.DarkGray,
                    secondColor = null,
                ),
            ),
            // Cascade #2 — full text doesn't fit, ellipsized tail still meaningful
            TitleState.WithWallet(
                tokenName = "Tether",
                walletName = "My Long Tangem Hardware Wallet",
                deviceIconUM = DeviceIconUM.Card(
                    mainColor = Color.DarkGray,
                    secondColor = null,
                ),
            ),
            // Cascade #3 — secondary part too small to be meaningful, drop to token-only + icon
            TitleState.WithWallet(
                tokenName = "USDCoinWrapped",
                walletName = "Super Extra Long Wallet Name That Definitely Wont Fit",
                deviceIconUM = DeviceIconUM.Card(
                    mainColor = Color.DarkGray,
                    secondColor = null,
                ),
            ),
            // Cascade #4 — even tokenName alone doesn't fit at full font size: TextAutoSize shrinks it
            TitleState.WithAccount(
                tokenName = "VeryLongTokenNameThatOverflowsVeryLongTokenNameThatOverflows",
                accountName = stringReference("Portfolio"),
                accountIconUM = AccountIconUM.CryptoPortfolio(
                    value = CryptoPortfolioIcon.Icon.Star,
                    color = CryptoPortfolioIcon.Color.Azure,
                ),
            ),
            // Cascade #5 — even at MIN_TITLE_FONT_SIZE doesn't fit: TextOverflow.Ellipsis tails it
            TitleState.Simple(
                tokenName = "ExtremelyLongTokenNameThatCannotPossiblyFitEvenAtMinFontSize",
            ),
            // Account variant — long account name triggers ellipsized tail (#2)
            TitleState.WithAccount(
                tokenName = "Tether",
                accountName = stringReference("My Personal Long Account Name"),
                accountIconUM = AccountIconUM.CryptoPortfolio(
                    value = CryptoPortfolioIcon.Icon.Star,
                    color = CryptoPortfolioIcon.Color.Azure,
                ),
            ),
            // Account variant — drop secondary, icon-only fallback (#3)
            TitleState.WithAccount(
                tokenName = "USDCoinWrapped",
                accountName = stringReference("Super Extra Long Account Name That Wont Fit Anywhere"),
                accountIconUM = AccountIconUM.CryptoPortfolio(
                    value = CryptoPortfolioIcon.Icon.Star,
                    color = CryptoPortfolioIcon.Color.Azure,
                ),
            ),
        )
}
// endregion Preview