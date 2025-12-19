package com.tangem.feature.wallet.presentation.wallet.ui.components.visa

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.SpacerH4
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.wallet.state.model.TangemPayState

private const val GRADIENT_START_COLOR = 0xFF252934
private const val GRADIENT_END_COLOR = 0xFF12141E
private const val GRADIENT_OFFSET_X = 0f
private const val GRADIENT_OFFSET_Y = 80F
private const val GRADIENT_RADIUS = 200F

@Composable
internal fun TangemPayOnboardingBanner(state: TangemPayState.OnboardingBanner, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(Color(GRADIENT_START_COLOR), Color(GRADIENT_END_COLOR)),
                    center = Offset(GRADIENT_OFFSET_X, GRADIENT_OFFSET_Y),
                    radius = GRADIENT_RADIUS,
                ),
            )
            .clickable(onClick = state.onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.img_tangem_pay_onboarding_banner),
                contentDescription = null,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .width(55.dp)
                    .height(95.dp)
                    .offset(y = 10.dp),
            )

            SpacerH4()

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResourceSafe(R.string.tangempay_onboarding_banner_title),
                    style = TangemTheme.typography.subtitle2,
                    color = TangemTheme.colors.text.constantWhite,
                )

                SpacerH(6.dp)

                Text(
                    text = stringResourceSafe(R.string.tangempay_onboarding_banner_description),
                    style = TangemTheme.typography.caption2,
                    color = TangemTheme.colors.text.tertiary,
                )
            }
            Box(
                modifier = Modifier.fillMaxHeight(),
                contentAlignment = Alignment.TopEnd,
            ) {
                Image(
                    modifier = Modifier
                        .clip(TangemTheme.shapes.roundedCornersXMedium)
                        .clickable(onClick = state.closeOnClick)
                        .padding(4.dp)
                        .padding(top = 12.dp)
                        .size(12.dp),
                    painter = painterResource(id = R.drawable.ic_close_24),
                    colorFilter = ColorFilter.tint(TangemTheme.colors.icon.inactive),
                    contentDescription = null,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewTangemOnboardingBanner() {
    TangemThemePreview {
        TangemPayOnboardingBanner(
            TangemPayState.OnboardingBanner(
                onClick = {},
                closeOnClick = {},
            ),
        )
    }
}