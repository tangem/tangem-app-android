package com.tangem.features.feed.ui.feed.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.buttons.SecondarySmallButton
import com.tangem.core.ui.components.buttons.SmallButtonConfig
import com.tangem.core.ui.extensions.TextReference

@Composable
internal fun Header(onSeeAllClick: () -> Unit, isLoading: Boolean = false, title: @Composable () -> Unit) {
    AnimatedContent(isLoading) { animatedState ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (animatedState) {
                RectangleShimmer(modifier = Modifier.size(width = 104.dp, height = 18.dp))
            } else {
                title()
                SecondarySmallButton(
                    config = SmallButtonConfig(
                        text = TextReference.Res(R.string.common_see_all),
                        onClick = onSeeAllClick,
                    ),
                )
            }
        }
    }
}