package com.tangem.features.onramp.main.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R as CoreUiR
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_cross_24
import com.tangem.core.ui.test.TopAppBarTestTags
import com.tangem.utils.annotations.RemoveWithToggle

@Composable
@RemoveWithToggle(toggleName = "AND_19143_ONRAMP_DESIGN_SYSTEM_ENABLED", description = "Rename over the old file")
internal fun CloseButtonDs(state: TopAppBarButtonUM, modifier: Modifier = Modifier) {
    val closeDescription = stringResourceSafe(CoreUiR.string.common_close)
    TangemSurface(
        modifier = modifier
            .size(44.dp)
            .semantics {
                role = Role.Button
                contentDescription = closeDescription
            }
            .testTag(TopAppBarTestTags.CLOSE_BUTTON),
        color = TangemTheme.colors3.bg.transparent.primary,
        shape = CircleShape,
        border = BorderStroke(width = 1.dp, color = TangemTheme.colors3.bg.transparent.secondary),
        enabled = state.isEnabled,
        onClick = state.onClicked,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(24.dp),
                imageVector = Icons.ic_cross_24,
                tint = TangemTheme.colors3.icon.primary,
                contentDescription = null,
            )
        }
    }
}