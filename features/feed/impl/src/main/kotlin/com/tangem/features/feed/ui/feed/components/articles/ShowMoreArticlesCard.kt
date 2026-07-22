package com.tangem.features.feed.ui.feed.components.articles

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme

@Composable
fun ShowMoreArticlesCard(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(color = TangemTheme.colors3.bg.secondary)
            .clickable(onClick = onClick)
            .padding(vertical = 41.dp, horizontal = 16.dp),
    ) {
        Image(
            modifier = Modifier.size(40.dp),
            imageVector = ImageVector.vectorResource(R.drawable.ic_show_more_news_48),
            contentDescription = stringResourceSafe(R.string.common_show_more),
        )

        SpacerH(10.dp)

        Text(
            text = stringResourceSafe(R.string.news_all_news),
            style = TangemTheme.typography3.body.medium,
            color = TangemTheme.colors3.text.primary,
        )

        SpacerH(4.dp)

        Text(
            text = stringResourceSafe(R.string.news_stay_in_the_loop),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.secondary,
        )
    }
}