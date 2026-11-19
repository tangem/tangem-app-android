package com.tangem.features.polymarket.impl.fasttrading.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_info_16

@Composable
internal fun PolymarketFastTradingInfoTiles(onDurationInfoClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        InfoTile(
            caption = stringResourceSafe(R.string.prediction_session_key_duration_caption),
            value = stringResourceSafe(R.string.prediction_session_key_duration_value),
            onInfoClick = onDurationInfoClick,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        )
        InfoTile(
            caption = stringResourceSafe(R.string.prediction_session_key_name_caption),
            value = stringResourceSafe(R.string.prediction_session_key_name_value),
            onInfoClick = null,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        )
    }
}

@Composable
private fun InfoTile(caption: String, value: String, onInfoClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    val infoLabel = stringResourceSafe(R.string.prediction_session_key_what_is)
    val clickModifier = if (onInfoClick != null) {
        Modifier.clickable(onClickLabel = infoLabel, onClick = onInfoClick)
    } else {
        Modifier
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(TangemTheme.colors3.bg.secondary)
            .then(clickModifier)
            .padding(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = caption,
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.secondary,
            )
            if (onInfoClick != null) {
                Icon(
                    imageVector = Icons.ic_info_16,
                    contentDescription = infoLabel,
                    tint = TangemTheme.colors3.icon.secondary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = TangemTheme.typography3.heading.small,
            color = TangemTheme.colors3.text.primary,
        )
    }
}

@Composable
internal fun PolymarketFastTradingTextSection(
    title: String,
    text: String,
    isSeparated: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (isSeparated) {
            HorizontalDivider(thickness = 1.dp, color = TangemTheme.colors3.border.secondary)
        }
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = TangemTheme.typography3.heading.small,
                color = TangemTheme.colors3.text.primary,
            )
            Text(
                text = text,
                style = TangemTheme.typography3.body.medium,
                color = TangemTheme.colors3.text.secondary,
            )
        }
    }
}

internal enum class PolymarketFastTradingSection(val titleRes: Int, val textRes: Int) {
    What(R.string.prediction_session_key_faq_what_title, R.string.prediction_session_key_faq_what_text),
    Duration(R.string.prediction_session_key_faq_duration_title, R.string.prediction_session_key_faq_duration_text),
    Can(R.string.prediction_session_key_faq_can_title, R.string.prediction_session_key_faq_can_text),
    Cant(R.string.prediction_session_key_faq_cant_title, R.string.prediction_session_key_faq_cant_text),
    Control(R.string.prediction_session_key_faq_control_title, R.string.prediction_session_key_faq_control_text),
}

@Composable
internal fun PolymarketFastTradingSections(
    sections: List<PolymarketFastTradingSection>,
    firstSeparatedIndex: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(horizontal = 24.dp)) {
        sections.forEachIndexed { index, section ->
            PolymarketFastTradingTextSection(
                title = stringResourceSafe(section.titleRes),
                text = stringResourceSafe(section.textRes),
                isSeparated = index >= firstSeparatedIndex,
            )
        }
    }
}

@Preview(name = "Light", widthDp = 390)
@Preview(name = "Dark", widthDp = 390, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PolymarketFastTradingInfoTilesPreview() {
    TangemThemePreviewRedesign {
        Column(modifier = Modifier.background(TangemTheme.colors3.bg.primary)) {
            PolymarketFastTradingInfoTiles(onDurationInfoClick = {})
        }
    }
}