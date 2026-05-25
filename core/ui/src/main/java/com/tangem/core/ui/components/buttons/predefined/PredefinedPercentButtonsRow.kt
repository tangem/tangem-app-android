package com.tangem.core.ui.components.buttons.predefined

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class PredefinedPercentButtonUM(
    val id: String,
    val label: TextReference,
    val onClick: () -> Unit,
)

@Composable
fun PredefinedPercentButtonsRow(items: ImmutableList<PredefinedPercentButtonUM>, modifier: Modifier = Modifier) {
    if (items.isEmpty()) return
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(TangemTheme.colors.button.secondary)
            .padding(start = 8.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
    ) {
        items.fastForEach { item ->
            key(item.id) {
                PercentPill(
                    item = item,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun PercentPill(item: PredefinedPercentButtonUM, modifier: Modifier = Modifier) {
    Text(
        text = item.label.resolveReference(),
        style = TangemTheme.typography.caption1,
        color = TangemTheme.colors.text.primary1,
        textAlign = TextAlign.Center,
        modifier = modifier
            .testTag(item.id)
            .clip(RoundedCornerShape(16.dp))
            .height(24.dp)
            .background(TangemTheme.colors.field.primary)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = item.onClick,
            )
            .padding(horizontal = 12.dp, vertical = 4.dp),
    )
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun PredefinedPercentButtonsRow_Preview() {
    TangemThemePreview {
        PredefinedPercentButtonsRow(
            items = persistentListOf(
                PredefinedPercentButtonUM(id = "25", label = stringReference("25%"), onClick = {}),
                PredefinedPercentButtonUM(id = "50", label = stringReference("50%"), onClick = {}),
                PredefinedPercentButtonUM(id = "75", label = stringReference("75%"), onClick = {}),
                PredefinedPercentButtonUM(id = "max", label = stringReference("Max"), onClick = {}),
            ),
        )
    }
}
// endregion