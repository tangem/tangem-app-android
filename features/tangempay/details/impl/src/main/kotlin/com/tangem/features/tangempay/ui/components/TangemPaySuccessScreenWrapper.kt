package com.tangem.features.tangempay.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.SpacerHMax
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_success_24
import com.tangem.features.tangempay.details.impl.R

private const val DEFAULT_FADE_COLOR = 0xFF9FC824
private val BlurRadius = 192.dp

@Suppress("MagicNumber")
@Composable
internal fun TangemPaySuccessScreenWrapper(
    title: TextReference,
    subtitle: TextReference,
    buttonText: TextReference,
    onButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
    fadeColor: Color = Color(DEFAULT_FADE_COLOR),
    titleTestTag: String? = null,
    subtitleTestTag: String? = null,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .blur(BlurRadius)
                .drawBehind {
                    val w = size.width
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                fadeColor,
                                Color.Transparent,
                            ),
                            center = Offset(w / 2f, -w * .1f),
                            radius = w + w * .2f,
                            tileMode = TileMode.Clamp,
                        ),
                    )
                },
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(top = 72.dp, start = 24.dp, end = 24.dp),
        ) {
            Icon(
                modifier = Modifier.size(28.dp),
                imageVector = Icons.ic_success_24,
                tint = TangemTheme.colors3.icon.primary,
                contentDescription = null,
            )
            SpacerH(TangemTheme.dimens2.x4)
            Text(
                modifier = titleTestTag?.let { Modifier.testTag(it) } ?: Modifier,
                text = title.resolveReference(),
                style = TangemTheme.typography3.heading.medium,
                color = TangemTheme.colors3.text.primary,
            )
            Text(
                modifier = subtitleTestTag?.let { Modifier.testTag(it) } ?: Modifier,
                text = subtitle.resolveReference(),
                style = TangemTheme.typography3.heading.medium,
                color = TangemTheme.colors3.text.secondary,
            )
            SpacerHMax()
            TangemButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = TangemTheme.dimens2.x3),
                onClick = onButtonClick,
                size = TangemButton.Size.X12,
                text = buttonText,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    TangemThemePreviewRedesign {
        TangemPaySuccessScreenWrapper(
            title = resourceReference(R.string.tangempay_card_details_change_pin_success_title),
            subtitle = resourceReference(R.string.tangempay_card_details_change_pin_success_description),
            buttonText = resourceReference(R.string.common_close),
            onButtonClick = {},
        )
    }
}