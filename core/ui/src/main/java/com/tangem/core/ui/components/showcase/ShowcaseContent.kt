package com.tangem.core.ui.components.showcase

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.showcase.model.ShowcaseItemModel
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import kotlinx.collections.immutable.ImmutableList

@Composable
fun ShowcaseContent(
    @DrawableRes headerIconRes: Int,
    headerText: TextReference,
    showcaseItems: ImmutableList<ShowcaseItemModel>,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        modifier = modifier,
    ) {
        Icon(
            painter = painterResource(id = headerIconRes),
            contentDescription = null,
            tint = TangemTheme.colors.icon.primary1,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(56.dp),
        )
        Text(
            text = headerText.resolveReference(),
            style = TangemTheme.typography.h2,
            color = TangemTheme.colors.text.primary1,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(
                    start = 34.dp,
                    end = 34.dp,
                    top = 28.dp,
                ),
        )
        Column(
            modifier = Modifier
                .padding(
                    start = 34.dp,
                    end = 34.dp,
                    top = 28.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            repeat(showcaseItems.size) { index ->
                ShowcaseItem(
                    iconRes = showcaseItems[index].iconRes,
                    title = showcaseItems[index].title,
                    subtitle = showcaseItems[index].subTitle,
                )
            }
        }
    }
}