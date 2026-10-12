package com.tangem.features.collectibles.impl.machinedetails.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.fade.TangemFade
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_error_20_filled
import com.tangem.features.collectibles.impl.machinedetails.ui.state.CollectiblesMachineDetailsUM

internal const val PRIZE_CARD_ASPECT_RATIO = 179f / 228f

@Composable
internal fun CollectiblesPrizeCard(state: CollectiblesMachineDetailsUM.PrizeUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PrizeImage(imageUrl = state.imageUrl, grade = state.grade)

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = state.title.resolveReference(),
                style = TangemTheme.typography3.subheading.medium,
                color = TangemTheme.colors3.text.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = state.value.resolveReference(),
                style = TangemTheme.typography3.subheading.medium,
                color = TangemTheme.colors3.text.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PrizeImage(imageUrl: String?, grade: TextReference?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(PRIZE_CARD_ASPECT_RATIO)
            .clip(RoundedCornerShape(20.dp))
            .background(TangemTheme.colors3.bg.secondary)
            .border(width = 1.dp, color = TangemTheme.colors3.border.primary, shape = RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center,
    ) {
        SubcomposeAsyncImage(
            modifier = Modifier.fillMaxSize(),
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            error = { PrizeImagePlaceholder() },
            contentScale = ContentScale.Crop,
            contentDescription = null,
        )

        if (grade != null) {
            TangemFade(
                position = TangemFade.Position.Top,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(56.dp),
            )
            TangemBadge(
                text = grade,
                size = TangemBadge.Size.X4,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp),
            )
        }
    }
}

@Composable
private fun PrizeImagePlaceholder(modifier: Modifier = Modifier) {
    TangemIcon(
        tangemIconUM = TangemIconUM.Icon(
            imageVector = Icons.ic_error_20_filled,
            tintReference = { TangemTheme.colors3.icon.tertiary },
        ),
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(TangemTheme.colors3.bg.opaque.primary)
            .padding(10.dp)
            .size(20.dp),
    )
}