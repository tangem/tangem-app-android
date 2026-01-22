package com.tangem.core.ui.ds.row.token.internal

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.util.fastForEach
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.components.text.applyBladeBrush
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
import com.tangem.core.ui.extensions.orMaskWithStars
import com.tangem.core.ui.extensions.resolveAnnotatedReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

@Composable
internal fun TokenRowEndTopContent(
    endContentUM: TangemTokenRowUM.EndContentUM,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    when (endContentUM) {
        is TangemTokenRowUM.EndContentUM.Content -> Content(
            modifier = modifier,
            endContentUM = endContentUM,
            isBalanceHidden = isBalanceHidden,
        )
        TangemTokenRowUM.EndContentUM.Empty -> Unit
        TangemTokenRowUM.EndContentUM.Loading -> TextShimmer(
            style = TangemTheme.typography2.bodySemibold16,
            modifier = modifier.width(TangemTheme.dimens2.x18),
            radius = TangemTheme.dimens2.x25,
        )
    }
}

@Composable
private fun Content(
    endContentUM: TangemTokenRowUM.EndContentUM.Content,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedVisibility(
            visible = endContentUM.icons.isNotEmpty(),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = TangemTheme.dimens2.x1),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x1),
            ) {
                endContentUM.icons.fastForEach { icon ->
                    Icon(
                        modifier = Modifier.size(TangemTheme.dimens2.x3),
                        painter = rememberVectorPainter(image = ImageVector.vectorResource(icon.iconRes)),
                        tint = icon.tintReference(),
                        contentDescription = null,
                    )
                }
            }
        }

        Text(
            modifier = Modifier,
            text = endContentUM.text.orMaskWithStars(isBalanceHidden).resolveAnnotatedReference(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TangemTheme.typography2.bodySemibold16.applyBladeBrush(
                isEnabled = endContentUM.isFlickering,
                textColor = if (endContentUM.isAvailable) {
                    TangemTheme.colors2.text.neutral.primary
                } else {
                    TangemTheme.colors2.text.status.disabled
                },
            ),
        )
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun TokenRowEndTopContent_Preview(
    @PreviewParameter(TokenRowEndContentPreviewProvider::class) params: TangemTokenRowUM.EndContentUM,
) {
    TangemThemePreviewRedesign {
        TokenRowEndTopContent(
            endContentUM = params,
            isBalanceHidden = false,
        )
    }
}

private class TokenRowEndContentPreviewProvider : PreviewParameterProvider<TangemTokenRowUM.EndContentUM> {
    override val values: Sequence<TangemTokenRowUM.EndContentUM>
        get() = sequenceOf(
            TangemTokenRowPreviewData.topEndContentUM,
        )
}
// endregion