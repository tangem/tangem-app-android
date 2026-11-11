package com.tangem.features.onramp.main.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.tangem.core.ui.components.CircleShimmer
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.test.TopAppBarTestTags
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.main.entity.OnrampCountryButtonUM
import com.tangem.utils.annotations.RemoveWithToggle

@Composable
@RemoveWithToggle(toggleName = "AND_19143_ONRAMP_DESIGN_SYSTEM_ENABLED", description = "Rename over the old file")
internal fun CountryButtonDs(state: OnrampCountryButtonUM, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(TangemTheme.colors3.bg.transparent.secondary)
            .clickable(
                enabled = state.isEnabled,
                role = Role.Button,
                onClickLabel = stringResourceSafe(R.string.onramp_settings_residence),
                onClick = state.onClick,
            )
            .semantics { state.countryName?.let { contentDescription = it } }
            .testTag(TopAppBarTestTags.MORE_BUTTON),
        contentAlignment = Alignment.Center,
    ) {
        if (state.flagUrl != null) {
            SubcomposeAsyncImage(
                modifier = Modifier.size(24.dp).clip(CircleShape),
                model = state.flagUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                loading = { CircleShimmer(modifier = Modifier.fillMaxSize()) },
                error = {
                    Box(Modifier.fillMaxSize().background(TangemTheme.colors3.border.primary, CircleShape))
                },
            )
        }
    }
}