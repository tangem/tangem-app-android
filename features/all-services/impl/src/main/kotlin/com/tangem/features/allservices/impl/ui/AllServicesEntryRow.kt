package com.tangem.features.allservices.impl.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowContentLead
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_chevron_right_24
import com.tangem.core.ui.res.generated.icons.ic_sign_plus_24

@Composable
internal fun AllServicesEntryRow(
    title: TextReference,
    subtitle: TextReference?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
) {
    TangemRow(
        modifier = modifier,
        contentLead = TangemRowContentLead.Start,
        verticalAlignment = TangemRowVerticalAlignment.Center,
        startSlot = { icon() },
        titleSlot = { TangemRowText(text = title, role = TangemRowTextRole.Title) },
        subtitleSlot = subtitle?.let { { TangemRowText(text = it, role = TangemRowTextRole.Subtitle) } },
        endSlot = {
            Icon(
                imageVector = Icons.ic_chevron_right_24,
                contentDescription = null,
                tint = TangemTheme.colors3.icon.secondary,
            )
        },
        onClick = onClick,
    )
}

@Composable
internal fun AllServicesEntryIcon(imageVector: ImageVector, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(40.dp)
            .background(color = TangemTheme.colors3.bg.secondary, shape = RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = imageVector, contentDescription = null, tint = TangemTheme.colors3.icon.accent.blue)
    }
}

@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun AllServicesEntryRow_Preview() {
    TangemThemePreviewRedesign {
        AllServicesEntryRow(
            icon = { AllServicesEntryIcon(imageVector = Icons.ic_sign_plus_24) },
            title = stringReference("Add wallet"),
            subtitle = stringReference("Separate funds by purpose"),
            onClick = {},
        )
    }
}