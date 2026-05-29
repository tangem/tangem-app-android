package com.tangem.feature.tokendetails.presentation.tokendetails.ui

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import com.tangem.core.ui.test.TokenDetailsScreenTestTags
import com.tangem.core.ui.test.TokenDetailsTopBarTestTags
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import com.tangem.common.ui.account.AccountIcon
import com.tangem.common.ui.account.AccountIconUM
import com.tangem.core.ui.R
import com.tangem.core.ui.components.account.AccountIconSize
import com.tangem.core.ui.components.dropdownmenu.TangemDropdownItem
import com.tangem.core.ui.components.dropdownmenu.TangemDropdownMenu
import com.tangem.core.ui.components.dropdownmenu.TangemDropdownMenuItem
import com.tangem.core.ui.components.haze.ProvideHaze
import com.tangem.core.ui.components.haze.hazeEffectTangem
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
import kotlinx.collections.immutable.*
import com.tangem.core.ui.R as CoreUiR

@Composable
internal fun TokenDetailsTopBar(topAppBarUM: TokenDetailsTopAppBarUM, modifier: Modifier = Modifier) {
    val actionModifier = Modifier
        .clip(CircleShape)
        .hazeEffectTangem { blurRadius = ACTION_BLUR_RADIUS }
    TangemTopBar(
        modifier = modifier
            .statusBarsPadding()
            .testTag(TokenDetailsTopBarTestTags.BACK_BUTTON),
        startContent = {
            TangemTopBarActionContent(
                modifier = actionModifier,
                actionUM = TangemTopBarActionUM(
                    iconRes = R.drawable.ic_arrow_back_28,
                    onClick = topAppBarUM.onBackClick,
                ),
            )
        },
        endContent = if (topAppBarUM.menuItems.isNotEmpty()) {
            {
                var isDropdownMenuShown by rememberSaveable { mutableStateOf(false) }
                Box {
                    TangemTopBarActionContent(
                        modifier = actionModifier.testTag(TokenDetailsTopBarTestTags.MORE_BUTTON),
                        actionUM = TangemTopBarActionUM(
                            iconRes = CoreUiR.drawable.ic_more_default_24,
                            onClick = { isDropdownMenuShown = true },
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
                Box(modifier = Modifier.testTag(TokenDetailsScreenTestTags.TOKEN_TITLE)) {
                    TokenDetailsTitle(titleState = topAppBarUM.titleState)
                }
                Text(
                    text = topAppBarUM.subtitle.resolveAnnotatedReference(),
                    color = TangemTheme.colors2.text.neutral.tertiary,
                    style = TangemTheme.typography2.captionSemibold12,
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
                template = formattedTitleTemplate(CoreUiR.string.token_details_toolbar_title_token_in_wallet),
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
            AdaptiveTokenWithSecondaryRow(
                tokenName = titleState.tokenName,
                secondaryName = titleState.accountName.resolveAnnotatedReference().toString(),
                template = formattedTitleTemplate(CoreUiR.string.token_details_toolbar_title_token_in_account),
                appearance = appearance,
                icon = {
                    AccountIcon(
                        name = titleState.accountName,
                        icon = titleState.accountIconUM,
                        size = AccountIconSize.RedesignExtraSmall,
                    )
                },
            )
        }
    }
}

/**
 * Adaptive title for [TitleState.WithAccount] / [TitleState.WithWallet].
 *
 * Raw template carries [TOKEN_MARKER] / [SECONDARY_MARKER] for the names and
 * [IMAGE_PLACEHOLDER] for the icon — translator decides where each sits
 * (e.g. "Tether in [⭐] Portfolio" or "Tether in My Wallet [⭐]"). Anything outside
 * the markers (connecting words, punctuation) is painted as tertiary text — no
 * locale-specific substring matching required. RTL is handled by BiDi inside the
 * single [Text].
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
    val segments = remember(template) { parseTemplate(template) }
    val inlineContent = rememberIconInlineContent(appearance.iconSize, icon)

    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        val isFullTextShown = rememberShouldShowFullText(
            segments = segments,
            tokenName = tokenName,
            appearance = appearance,
            maxWidthPx = constraints.maxWidth,
        )
        if (isFullTextShown) {
            FullPhraseTitle(
                segments = segments,
                tokenName = tokenName,
                secondaryName = secondaryName,
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

/**
 * Returns the title template with its `%1$s` / `%2$s` placeholders replaced by [TOKEN_MARKER] /
 * [SECONDARY_MARKER] sentinels (and `%%image%%` collapsed to [IMAGE_PLACEHOLDER]).
 *
 * Feeding the markers in as format args lets [String.format] resolve all escaping for us, so
 * [parseTemplate] sees a clean string and the real names stay un-substituted until render time.
 */
@Composable
private fun formattedTitleTemplate(@StringRes id: Int): String = stringResourceSafe(id, TOKEN_MARKER, SECONDARY_MARKER)

@Composable
private fun rememberIconInlineContent(
    iconSize: Dp,
    icon: @Composable () -> Unit,
): ImmutableMap<String, InlineTextContent> {
    val iconSizeSp = with(LocalDensity.current) { iconSize.toSp() }
    val currentIcon by rememberUpdatedState(icon)
    return remember(iconSizeSp) {
        persistentMapOf(
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
    segments: ImmutableList<TitleSegment>,
    tokenName: String,
    appearance: TitleAppearance,
    maxWidthPx: Int,
): Boolean {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(segments, tokenName, maxWidthPx, appearance, density) {
        if (maxWidthPx <= 0) return@remember true
        val staticText = buildString {
            for (segment in segments) {
                when (segment) {
                    TitleSegment.Token -> append(tokenName)
                    is TitleSegment.Plain -> append(segment.text)
                    TitleSegment.Secondary, TitleSegment.Image -> Unit
                }
            }
        }
        val staticWidthPx = if (staticText.isEmpty()) {
            0
        } else {
            measurer.measure(text = staticText, style = appearance.style, softWrap = false).size.width
        }
        val iconReservePx = with(density) {
            (appearance.iconSize + appearance.spacing * 2).toPx()
        }.toInt()
        val minSecondaryPx = with(density) { MIN_SECONDARY_NAME_WIDTH.toPx() }.toInt()
        staticWidthPx + iconReservePx + minSecondaryPx <= maxWidthPx
    }
}

@Composable
private fun FullPhraseTitle(
    segments: ImmutableList<TitleSegment>,
    tokenName: String,
    secondaryName: String,
    style: TextStyle,
    inlineContent: ImmutableMap<String, InlineTextContent>,
) {
    val tertiaryColor = TangemTheme.colors2.text.neutral.tertiary
    val fullText = remember(segments, tokenName, secondaryName, tertiaryColor) {
        buildAnnotatedString {
            for (segment in segments) {
                when (segment) {
                    TitleSegment.Token -> append(tokenName)
                    TitleSegment.Secondary -> append(secondaryName)
                    TitleSegment.Image -> appendInlineContent(ICON_INLINE_ID, IMAGE_PLACEHOLDER)
                    is TitleSegment.Plain -> withStyle(SpanStyle(color = tertiaryColor)) {
                        append(segment.text)
                    }
                }
            }
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

private sealed interface TitleSegment {
    data object Token : TitleSegment
    data object Secondary : TitleSegment
    data object Image : TitleSegment
    data class Plain(val text: String) : TitleSegment
}

/**
 * Splits the [String.format]ed template (see [formattedTitleTemplate]) into ordered segments.
 *
 * Markers appear in their resolved form: [TOKEN_MARKER] / [SECONDARY_MARKER] for the names and
 * [IMAGE_PLACEHOLDER] for the icon (escaping was already collapsed by [String.format]). Anything
 * else is plain connecting text painted as tertiary.
 */
private fun parseTemplate(template: String): ImmutableList<TitleSegment> {
    val segments = mutableListOf<TitleSegment>()
    val plain = StringBuilder()

    fun flushPlain() {
        if (plain.isNotEmpty()) {
            segments += TitleSegment.Plain(plain.toString())
            plain.clear()
        }
    }

    var i = 0
    while (i < template.length) {
        val marker = MARKERS.firstOrNull { (text, _) -> template.startsWith(text, i) }
        if (marker != null) {
            flushPlain()
            segments += marker.second
            i += marker.first.length
        } else {
            plain.append(template[i])
            i++
        }
    }
    flushPlain()
    return segments.toImmutableList()
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
private const val TOKEN_MARKER = "%1\$s"
private const val SECONDARY_MARKER = "%2\$s"

// Order matters: longer/overlapping markers must be tried first by [parseTemplate].
private val MARKERS: List<Pair<String, TitleSegment>> = listOf(
    IMAGE_PLACEHOLDER to TitleSegment.Image,
    TOKEN_MARKER to TitleSegment.Token,
    SECONDARY_MARKER to TitleSegment.Secondary,
)

private val ACTION_BLUR_RADIUS = 8.dp
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
        ProvideHaze {
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