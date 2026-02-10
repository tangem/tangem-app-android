package com.tangem.core.ui.ds.row.header

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerWMax
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.extensions.resolveAnnotatedReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.test.TokenElementsTestTags

/**
 * UI model for header row component
 *
 * @param headerRowUM UI model for the header row
 * @param modifier    Modifier for the composable
 */
@Composable
fun TangemHeaderRow(headerRowUM: TangemHeaderRowUM, modifier: Modifier = Modifier) {
    TangemHeaderRow(
        headTangemIconUM = headerRowUM.startIconUM,
        footerTangemIconRes = headerRowUM.endIconRes,
        title = headerRowUM.title,
        subtitle = headerRowUM.subtitle,
        modifier = modifier,
    )
}

/**
 * Composable function that represents a header row with customizable title and head content.
 *
 * @param modifier         Modifier for the composable
 * @param subtitle         Optional subtitle as a TextReference
 * @param onItemClick      Optional click callback for the row
 * @param footerTangemIconRes Optional drawable resource ID for the footer icon
 * @param titleContent     Composable lambda for the title content
 * @param headContent      Composable lambda for the head content
 */
@Composable
fun TangemHeaderRow(
    modifier: Modifier = Modifier,
    subtitle: TextReference? = null,
    onItemClick: (() -> Unit)? = null,
    @DrawableRes footerTangemIconRes: Int? = null,
    titleContent: @Composable (Modifier) -> Unit,
    headContent: @Composable (Modifier) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickableSingle(enabled = onItemClick != null, onClick = { onItemClick?.invoke() })
            .padding(
                top = TangemTheme.dimens2.x4,
                bottom = TangemTheme.dimens2.x3,
                start = TangemTheme.dimens2.x4,
                end = TangemTheme.dimens2.x4,
            ),
    ) {
        headContent(
            Modifier
                .padding(end = TangemTheme.dimens2.x2)
                .size(TangemTheme.dimens2.x4),
        )
        titleContent(Modifier)
        AnimatedVisibility(
            visible = subtitle != null,
        ) {
            val wrappedSubtitle = remember(this) { requireNotNull(subtitle) }
            Text(
                text = wrappedSubtitle.resolveAnnotatedReference(),
                style = TangemTheme.typography2.captionSemibold12,
                color = TangemTheme.colors2.text.neutral.secondary,
                maxLines = 1,
                modifier = Modifier
                    .testTag(tag = TokenElementsTestTags.TOKEN_CRYPTO_AMOUNT)
                    .padding(start = TangemTheme.dimens2.x1),
            )
        }
        SpacerWMax()
        AnimatedVisibility(
            visible = footerTangemIconRes != null,
        ) {
            val wrappedIconUM = remember(this) { requireNotNull(footerTangemIconRes) }
            Icon(
                imageVector = ImageVector.vectorResource(id = wrappedIconUM),
                contentDescription = null,
                tint = TangemTheme.colors2.graphic.neutral.secondary,
                modifier = Modifier.size(TangemTheme.dimens2.x4),
            )
        }
    }
}

/**
 * Composable function that represents a header row with title, optional subtitle, and optional icons.
 *
 * @param title             Title as a TextReference
 * @param modifier          Modifier for the composable
 * @param subtitle          Optional subtitle as a TextReference
 * @param headTangemIconUM  Optional TangemIconUM for the head icon
 * @param footerTangemIconRes Optional drawable resource ID for the footer icon
 * @param isEnabled         Boolean indicating if the row is clickable
 * @param onItemClick       Optional click callback for the row
 */
@Composable
fun TangemHeaderRow(
    title: TextReference,
    modifier: Modifier = Modifier,
    subtitle: TextReference? = null,
    headTangemIconUM: TangemIconUM? = null,
    @DrawableRes footerTangemIconRes: Int? = null,
    isEnabled: Boolean = false,
    onItemClick: (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickableSingle(enabled = isEnabled && onItemClick != null, onClick = { onItemClick?.invoke() })
            .padding(
                top = TangemTheme.dimens2.x4,
                bottom = TangemTheme.dimens2.x3,
                start = TangemTheme.dimens2.x4,
                end = TangemTheme.dimens2.x4,
            ),
    ) {
        AnimatedVisibility(
            visible = headTangemIconUM != null,
        ) {
            val wrappedIconUM = remember(this) { requireNotNull(headTangemIconUM) }
            TangemIcon(
                tangemIconUM = wrappedIconUM,
                modifier = Modifier
                    .padding(end = TangemTheme.dimens2.x2)
                    .size(TangemTheme.dimens2.x4),
            )
        }
        Text(
            text = title.resolveAnnotatedReference(),
            style = TangemTheme.typography2.captionSemibold12,
            color = TangemTheme.colors2.text.neutral.primary,
            maxLines = 1,
            modifier = Modifier.testTag(tag = TokenElementsTestTags.TOKEN_TITLE),
        )
        AnimatedVisibility(
            visible = subtitle != null,
        ) {
            val wrappedSubtitle = remember(this) { requireNotNull(subtitle) }
            Text(
                text = wrappedSubtitle.resolveAnnotatedReference(),
                style = TangemTheme.typography2.captionSemibold12,
                color = TangemTheme.colors2.text.neutral.secondary,
                maxLines = 1,
                modifier = Modifier
                    .testTag(tag = TokenElementsTestTags.TOKEN_CRYPTO_AMOUNT)
                    .padding(start = TangemTheme.dimens2.x1),
            )
        }
        SpacerWMax()
        AnimatedVisibility(
            visible = footerTangemIconRes != null,
        ) {
            val wrappedIconUM = remember(this) { requireNotNull(footerTangemIconRes) }
            Icon(
                imageVector = ImageVector.vectorResource(id = wrappedIconUM),
                contentDescription = null,
                tint = TangemTheme.colors2.graphic.neutral.secondary,
                modifier = Modifier.size(TangemTheme.dimens2.x4),
            )
        }
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun TangemHeaderRow_Preview(@PreviewParameter(PreviewProvider::class) params: TangemHeaderRowUM) {
    TangemThemePreviewRedesign {
        TangemHeaderRow(
            headerRowUM = params,
            modifier = Modifier.background(TangemTheme.colors2.surface.level3),
        )
    }
}

private class PreviewProvider : PreviewParameterProvider<TangemHeaderRowUM> {
    override val values: Sequence<TangemHeaderRowUM>
        get() = sequenceOf(
            TangemHeaderRowUM(
                id = "1",
                startIconUM = TangemIconUM.Currency(
                    currencyIconState = CurrencyIconState.Locked,
                ),
                endIconRes = R.drawable.ic_minimize_24,
                title = stringReference("Account"),
                subtitle = stringReference("\$ 42,900.17"),
            ),
            TangemHeaderRowUM(
                id = "2",
                title = stringReference("Account"),
            ),
        )
}
// endregion