package com.tangem.core.ui.components.snackbar

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerW
import com.tangem.core.ui.components.haze.hazeEffectTangem
import com.tangem.core.ui.ds.button.SecondaryTangemButton
import com.tangem.core.ui.ds.button.TangemButtonShape
import com.tangem.core.ui.ds.button.TangemButtonSize
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

/**
 * Top snackbar with an optional leading icon and an optional action button.
 * Intended to be used with [TangemTopSnackbarHost].
 *
 * @param snackbarMessage message data
 * @param modifier        modifier
 */
@Suppress("LongMethod")
@Composable
fun TangemTopSnackbar(snackbarMessage: SnackbarMessage, modifier: Modifier = Modifier) {
    val actionLabel = snackbarMessage.actionLabel
    val action = snackbarMessage.action
    val hasAction = actionLabel != null && action != null

    var isTextOverflowing by remember { mutableStateOf(false) }
    val shape = if (isTextOverflowing) {
        RoundedCornerShape(TangemTheme.dimens2.x5)
    } else {
        TangemTheme.shapes.roundedCornersXLarge
    }

    Column(
        modifier = modifier
            .shadow(elevation = TangemTheme.dimens.elevation4, shape = shape, clip = false)
            .background(color = TangemTheme.colors2.controls.backgroundDefault, shape = shape)
            .clip(shape)
            .hazeEffectTangem()
            .sizeIn(minHeight = TangemTheme.dimens2.x11)
            .padding(start = TangemTheme.dimens2.x5, end = TangemTheme.dimens2.x1)
            .padding(vertical = TangemTheme.dimens2.x1),
        verticalArrangement = Arrangement.Center,
    ) {
        Row(
            modifier = Modifier.padding(top = if (isTextOverflowing) TangemTheme.dimens2.x3 else 0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (snackbarMessage.startIconId != null) {
                Icon(
                    painter = painterResource(id = snackbarMessage.startIconId),
                    contentDescription = null,
                    modifier = Modifier.size(TangemTheme.dimens2.x5),
                    tint = TangemTheme.colors2.graphic.neutral.secondary,
                )

                SpacerW(TangemTheme.dimens2.x2)
            }

            Text(
                text = snackbarMessage.message.resolveReference(),
                modifier = Modifier.weight(1f, fill = false),
                color = TangemTheme.colors.text.secondary,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                style = TangemTheme.typography.body2,
                onTextLayout = { if (hasAction) isTextOverflowing = it.hasVisualOverflow },
            )

            SpacerW(TangemTheme.dimens2.x4)

            if (hasAction && !isTextOverflowing) {
                SecondaryTangemButton(
                    text = actionLabel,
                    onClick = action,
                    size = TangemButtonSize.X9,
                    shape = TangemButtonShape.Rounded,
                )
            }
        }

        if (hasAction && isTextOverflowing) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = TangemTheme.dimens2.x4,
                        bottom = TangemTheme.dimens2.x1,
                    ),
                contentAlignment = Alignment.CenterEnd,
            ) {
                SecondaryTangemButton(
                    text = actionLabel,
                    onClick = action,
                    size = TangemButtonSize.X9,
                    shape = TangemButtonShape.Rounded,
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_TangemTopSnackbar_WithAction() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier.padding(TangemTheme.dimens.spacing16),
            verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing16),
        ) {
            TangemTopSnackbar(
                snackbarMessage = SnackbarMessage(
                    startIconId = R.drawable.ic_eye_off_outline_24,
                    message = stringReference("Balances hidden"),
                    actionLabel = stringReference("Undo"),
                    action = {},
                ),
            )
            TangemTopSnackbar(
                snackbarMessage = SnackbarMessage(
                    startIconId = R.drawable.ic_eye_off_outline_24,
                    message = stringReference(
                        "Balances hidden long long text that should be truncated with ellipsis at the end",
                    ),
                    actionLabel = stringReference("Undo"),
                    action = {},
                ),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_TangemTopSnackbar_NoAction() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier.padding(TangemTheme.dimens.spacing16),
            verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing16),
        ) {
            TangemTopSnackbar(
                snackbarMessage = SnackbarMessage(
                    startIconId = R.drawable.ic_check_24,
                    message = stringReference("Text copied to clipboard"),
                ),
            )
            TangemTopSnackbar(
                snackbarMessage = SnackbarMessage(
                    message = stringReference("Operation completed"),
                ),
            )
            TangemTopSnackbar(
                snackbarMessage = SnackbarMessage(
                    message = stringReference(
                        "Operation completed long long text that should be truncated with ellipsis at the end",
                    ),
                ),
            )
        }
    }
}