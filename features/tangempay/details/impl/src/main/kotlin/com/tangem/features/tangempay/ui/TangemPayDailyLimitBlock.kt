package com.tangem.features.tangempay.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.*
import com.tangem.core.ui.components.buttons.common.TangemButtonSize
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.row.TangemRowContainer
import com.tangem.core.ui.ds.row.TangemRowLayoutId
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.*
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_refresh_32
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.TangemPayDailyLimitBlockState

@Composable
internal fun TangemPayDailyLimitBlock(state: TangemPayDailyLimitBlockState, modifier: Modifier = Modifier) {
    if (LocalVisaRedesignEnabled.current) {
        CurrentLimitBlockV2(state, modifier.padding(top = TangemTheme.dimens2.x2))
    } else {
        TangemPayDailyLimitBlockV1(state, modifier)
    }
}

@Composable
private fun TangemPayDailyLimitBlockV1(state: TangemPayDailyLimitBlockState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = TangemTheme.colors.background.primary,
                shape = TangemTheme.shapes.roundedCornersMedium,
            )
            .padding(TangemTheme.dimens.spacing12),
    ) {
        Text(
            text = stringResourceSafe(R.string.tangempay_card_page_daily_limit_title),
            style = TangemTheme.typography.subtitle2,
            color = TangemTheme.colors.text.tertiary,
        )
        SpacerH4()
        CurrentLimitBlockV1(state)
    }
}

@Composable
private fun CurrentLimitBlockV1(state: TangemPayDailyLimitBlockState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    color = TangemTheme.colors.icon.accent.copy(alpha = 0.1f),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_limit_20),
                contentDescription = null,
                tint = TangemTheme.colors.icon.accent,
            )
        }
        SpacerW12()
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = stringResourceSafe(R.string.tangempay_card_page_daily_limit_current_limit),
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.tertiary,
            )
            when (state) {
                is TangemPayDailyLimitBlockState.Error,
                is TangemPayDailyLimitBlockState.Content,
                -> Text(
                    text = if (state is TangemPayDailyLimitBlockState.Content) state.limit else "—",
                    style = TangemTheme.typography.subtitle1,
                    color = TangemTheme.colors.text.primary1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                TangemPayDailyLimitBlockState.Loading -> TextShimmer(
                    style = TangemTheme.typography.subtitle1,
                    text = "$50,000",
                )
            }
        }
        SpacerW8()
        if (state is TangemPayDailyLimitBlockState.Content) {
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                SecondaryButton(
                    modifier = Modifier,
                    text = stringResourceSafe(R.string.tangempay_card_page_daily_limit_change),
                    onClick = state.onChangeClick,
                    size = TangemButtonSize.Small,
                )
            }
        }
    }
}

@Composable
private fun CurrentLimitBlockV2(state: TangemPayDailyLimitBlockState, modifier: Modifier = Modifier) {
    TangemRowContainer(
        modifier = modifier
            .clip(RoundedCornerShape(TangemTheme.dimens2.x6))
            .background(color = TangemTheme.colors3.bg.secondary),
        contentPadding = PaddingValues(TangemTheme.dimens2.x4),
    ) {
        LimitHeadIcon(
            modifier = Modifier.layoutId(TangemRowLayoutId.HEAD),
            state = state,
        )

        TitleLimit(
            modifier = Modifier
                .padding(start = TangemTheme.dimens2.x3)
                .layoutId(TangemRowLayoutId.START_TOP),
            state = state,
        )

        SubtitleLimit(
            modifier = Modifier
                .padding(start = TangemTheme.dimens2.x3)
                .layoutId(TangemRowLayoutId.START_BOTTOM),
            state = state,
        )

        when (state) {
            is TangemPayDailyLimitBlockState.Content -> {
                TangemButton(
                    modifier = Modifier
                        .padding(start = TangemTheme.dimens2.x3)
                        .layoutId(TangemRowLayoutId.TAIL),
                    variant = TangemButton.Variant.Secondary,
                    text = resourceReference(R.string.tangempay_card_page_daily_limit_change),
                    onClick = state.onChangeClick,
                    size = TangemButton.Size.X10,
                )
            }
            is TangemPayDailyLimitBlockState.Error -> {
                TangemButton(
                    modifier = Modifier
                        .padding(start = TangemTheme.dimens2.x3)
                        .layoutId(TangemRowLayoutId.TAIL),
                    iconStart = TangemIconUM.Icon(imageVector = Icons.ic_arrow_refresh_32),
                    variant = TangemButton.Variant.Secondary,
                    onClick = state.onReloadClick,
                    size = TangemButton.Size.X10,
                )
            }
            TangemPayDailyLimitBlockState.Loading -> {
                TangemShimmer(
                    modifier = Modifier
                        .padding(start = TangemTheme.dimens2.x3)
                        .layoutId(TangemRowLayoutId.TAIL)
                        .size(width = 64.dp, height = 40.dp),
                    radius = 20.dp,
                )
            }
        }
    }
}

@Composable
private fun LimitHeadIcon(state: TangemPayDailyLimitBlockState, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(TangemTheme.dimens2.x10)
            .background(
                color = when (state) {
                    is TangemPayDailyLimitBlockState.Content,
                    TangemPayDailyLimitBlockState.Loading,
                    -> TangemTheme.colors3.bg.status.infoSubtle
                    is TangemPayDailyLimitBlockState.Error -> TangemTheme.colors3.bg.status.warningSubtle
                },
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            is TangemPayDailyLimitBlockState.Content,
            TangemPayDailyLimitBlockState.Loading,
            -> {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_limit_new_20),
                    contentDescription = null,
                    tint = TangemTheme.colors3.icon.brand,
                )
            }
            is TangemPayDailyLimitBlockState.Error -> {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_warning_20),
                    contentDescription = null,
                    tint = TangemTheme.colors3.icon.status.warning,
                )
            }
        }
    }
}

@Composable
private fun TitleLimit(state: TangemPayDailyLimitBlockState, modifier: Modifier = Modifier) {
    when (state) {
        is TangemPayDailyLimitBlockState.Content,
        TangemPayDailyLimitBlockState.Loading,
        -> {
            Text(
                modifier = modifier,
                text = stringResourceSafe(R.string.tangempay_card_page_daily_limit_title),
                style = TangemTheme.typography3.body.medium,
                color = TangemTheme.colors3.text.secondary,
            )
        }
        is TangemPayDailyLimitBlockState.Error -> {
            Text(
                modifier = modifier,
                text = stringResourceSafe(R.string.tangempay_card_page_daily_limit_error_title),
                style = TangemTheme.typography3.body.medium,
                color = TangemTheme.colors3.text.primary,
            )
        }
    }
}

@Composable
private fun SubtitleLimit(state: TangemPayDailyLimitBlockState, modifier: Modifier = Modifier) {
    when (state) {
        is TangemPayDailyLimitBlockState.Error -> {
            Text(
                modifier = modifier,
                text = stringResourceSafe(R.string.tangempay_card_page_daily_limit_error_subtitle),
                style = TangemTheme.typography3.body.medium,
                color = TangemTheme.colors3.text.secondary,
            )
        }
        is TangemPayDailyLimitBlockState.Content -> {
            Text(
                modifier = modifier,
                text = state.limit,
                style = TangemTheme.typography3.body.medium,
                color = TangemTheme.colors3.text.primary,
            )
        }
        TangemPayDailyLimitBlockState.Loading -> {
            TangemShimmer(modifier = modifier, style = TangemTheme.typography3.body.medium)
        }
    }
}

@Composable
internal fun TangemPayDailyLimitErrorBlock(modifier: Modifier = Modifier) {
    if (LocalVisaRedesignEnabled.current) return
    Notification(
        config = NotificationConfig(
            title = resourceReference(R.string.tangempay_card_page_daily_limit_error_title),
            subtitle = resourceReference(R.string.tangempay_card_page_daily_limit_error_description),
            iconResId = R.drawable.img_attention_20,
        ),
        containerColor = TangemTheme.colors.background.action,
        modifier = modifier.fillMaxWidth(),
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview() {
    TangemThemePreview {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TangemPayDailyLimitBlock(state = TangemPayDailyLimitBlockState.Content.stub())
            TangemPayDailyLimitBlock(state = TangemPayDailyLimitBlockState.Error(onReloadClick = {}))
            TangemPayDailyLimitBlock(state = TangemPayDailyLimitBlockState.Loading)
            TangemPayDailyLimitErrorBlock()
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewV2() {
    TangemThemePreviewRedesign {
        CompositionLocalProvider(
            LocalRedesignEnabled provides true,
            LocalVisaRedesignEnabled provides true,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TangemPayDailyLimitBlock(state = TangemPayDailyLimitBlockState.Content.stub())
                TangemPayDailyLimitBlock(state = TangemPayDailyLimitBlockState.Error(onReloadClick = {}))
                TangemPayDailyLimitBlock(state = TangemPayDailyLimitBlockState.Loading)
                TangemPayDailyLimitErrorBlock()
            }
        }
    }
}