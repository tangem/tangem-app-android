package com.tangem.common.ui.news

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.label.Label
import com.tangem.core.ui.components.label.entity.LabelUM
import com.tangem.core.ui.res.TangemTheme
import kotlinx.collections.immutable.ImmutableList

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ArticleHeader(
    title: String,
    createdAt: String,
    score: Float,
    tags: ImmutableList<LabelUM>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        ArticleInfo(
            score = score,
            createdAt = createdAt,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = title,
            style = TangemTheme.typography.h2,
            color = TangemTheme.colors.text.primary1,
        )

        if (tags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                tags.forEach { tag ->
                    Label(
                        state = tag,
                    )
                }
            }
        }
    }
}