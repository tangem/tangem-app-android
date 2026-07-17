package com.tangem.features.tangempay.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.*
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
import com.tangem.core.ui.test.TangemPayTestTags
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.TangemPayDailyLimitBlockState

@Composable
internal fun TangemPayDailyLimitBlock(state: TangemPayDailyLimitBlockState, modifier: Modifier = Modifier) {
    CurrentLimitBlockV2(state, modifier.padding(top = TangemTheme.dimens2.x2))
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
                        .layoutId(TangemRowLayoutId.TAIL)
                        .testTag(TangemPayTestTags.DAILY_LIMIT_CHANGE_BUTTON),
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
                modifier = modifier.testTag(TangemPayTestTags.DAILY_LIMIT_CURRENT_VALUE),
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

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewV2() {
    TangemThemePreviewRedesign {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TangemPayDailyLimitBlock(state = TangemPayDailyLimitBlockState.Content.stub())
            TangemPayDailyLimitBlock(state = TangemPayDailyLimitBlockState.Error(onReloadClick = {}))
            TangemPayDailyLimitBlock(state = TangemPayDailyLimitBlockState.Loading)
        }
    }
}