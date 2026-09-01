package com.tangem.features.commonfeatures.impl.choosetoken.ui

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
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds.button.SecondaryTangemButton
import com.tangem.core.ui.ds.button.TangemButtonSize
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.R as CoreUiR
import com.tangem.features.commonfeatures.impl.R

/** Empty-state card shown when the balance filter hides every token in the FROM token selector. */
@Suppress("MagicNumber")
@Composable
internal fun SwapSourceFilteredOutEmpty(onSeeAllClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(36.dp))
            .background(TangemTheme.colors3.bg.secondary)
            .padding(horizontal = 48.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(TangemTheme.colors3.bg.opaque.secondary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(CoreUiR.drawable.ic_information_24),
                    contentDescription = null,
                    tint = TangemTheme.colors3.icon.secondary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = resourceReference(R.string.swap_token_selector_empty_filtered_message).resolveReference(),
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.primary,
                textAlign = TextAlign.Center,
            )
        }
        SecondaryTangemButton(
            onClick = onSeeAllClick,
            text = resourceReference(R.string.common_see_all),
            size = TangemButtonSize.X9,
        )
    }
}

@Suppress("MagicNumber")
@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun SwapSourceFilteredOutEmptyPreview() {
    TangemThemePreviewRedesign {
        SwapSourceFilteredOutEmpty(
            onSeeAllClick = {},
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(16.dp),
        )
    }
}