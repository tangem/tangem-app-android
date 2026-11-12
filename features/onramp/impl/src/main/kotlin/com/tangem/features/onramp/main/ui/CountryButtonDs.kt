package com.tangem.features.onramp.main.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
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
    val residenceClickLabel = stringResourceSafe(R.string.onramp_settings_residence)
    RoundGlassSurface(
        modifier = modifier
            .size(44.dp)
            .semantics {
                role = Role.Button
                onClick(label = residenceClickLabel, action = null)
                state.countryName?.let { contentDescription = it }
            }
            .testTag(TopAppBarTestTags.MORE_BUTTON),
        enabled = state.isEnabled,
        onClick = state.onClick,
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