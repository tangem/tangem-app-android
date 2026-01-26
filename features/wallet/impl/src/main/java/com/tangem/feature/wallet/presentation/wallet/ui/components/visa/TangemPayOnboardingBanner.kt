package com.tangem.feature.wallet.presentation.wallet.ui.components.visa

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.tangem.core.ui.components.SpacerH
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
        ConstraintLayout(
            modifier = Modifier.fillMaxWidth(),
        ) {
            val (image, text, close) = createRefs()

            Image(
                painter = painterResource(R.drawable.ic_close_24),
                contentDescription = null,
                modifier = Modifier
                    .size(16.dp)
                    .clickable(onClick = state.closeOnClick)
                    .constrainAs(close) {
                        top.linkTo(parent.top, margin = 16.dp)
                        end.linkTo(parent.end, margin = 16.dp)
                    },
                colorFilter = ColorFilter.tint(TangemTheme.colors.icon.inactive),
            )

            Column(
                modifier = Modifier
                    .constrainAs(text) {
                        top.linkTo(parent.top)
                        start.linkTo(image.end, margin = 12.dp)
                        end.linkTo(close.start, margin = 12.dp)
                        width = Dimension.fillToConstraints
                    }
                    .padding(top = 16.dp, bottom = 16.dp),
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

            Image(
                painter = painterResource(R.drawable.img_tangem_pay_onboarding_banner),
                contentDescription = null,
                modifier = Modifier
                    .padding(top = 8.dp, start = 24.dp)
                    .constrainAs(image) {
                        start.linkTo(parent.start)
                        top.linkTo(text.top)
                        bottom.linkTo(text.bottom)
                        height = Dimension.fillToConstraints
                    },
            )
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