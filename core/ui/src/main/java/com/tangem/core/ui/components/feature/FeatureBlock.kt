package com.tangem.core.ui.components.feature

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

@Composable
fun FeatureBlock(title: String, description: String, iconRes: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
    ) {
        Icon(
            modifier = Modifier
                .padding(horizontal = 12.dp),
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = TangemTheme.colors.icon.primary1,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
        ) {
            Text(
                text = title,
                style = TangemTheme.typography.subtitle1,
                color = TangemTheme.colors.text.primary1,
            )
            Text(
                modifier = Modifier
                    .padding(top = 4.dp),
                text = description,
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors.text.secondary,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewFeatureBlock() {
    TangemThemePreview {
        Column(
            modifier = Modifier
                .background(TangemTheme.colors.background.primary)
                .padding(16.dp),
        ) {
            FeatureBlock(
                title = stringResourceSafe(R.string.backup_info_save_title),
                description = stringResourceSafe(R.string.backup_info_save_description, "12"),
                iconRes = R.drawable.ic_lock_24,
            )
            Spacer(modifier = Modifier.height(24.dp))
            FeatureBlock(
                title = stringResourceSafe(R.string.backup_info_keep_title),
                description = stringResourceSafe(R.string.backup_info_keep_description),
                iconRes = R.drawable.ic_settings_24,
            )
        }
    }
}