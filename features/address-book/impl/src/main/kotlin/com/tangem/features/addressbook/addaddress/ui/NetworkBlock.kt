package com.tangem.features.addressbook.addaddress.ui

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerH12
import com.tangem.core.ui.ds2.loader.TangemLoader
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.addressbook.addaddress.contract.AddAddressUM
import com.tangem.features.addressbook.addaddress.contract.AddAddressUM.ChosenNetworkState.Result.NetworkUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

private const val MAX_VISIBLE_NETWORKS = 3
private val NetworkIconSize = 24.dp

// Horizontal advance per icon. Smaller than the icon size so icons overlap; the bg-colored ring on
// the icon drawn on top carves the crescent cut-out from the icon below.
private val NetworkIconStep = 18.dp

@Composable
internal fun NetworkBlock(chosenNetworkState: AddAddressUM.ChosenNetworkState) {
    TangemRow(
        verticalAlignment = TangemRowVerticalAlignment.Center,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .fillMaxWidth()
            .background(color = TangemTheme.colors3.bg.secondary)
            .padding(horizontal = 4.dp),
        titleSlot = {
            Text(
                text = stringResourceSafe(R.string.common_network),
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors3.text.primary,
            )
        },
        endSlot = {
            SelectNetworkButton(chosenNetworkState)
        },
    )
}

@Composable
private fun SelectNetworkButton(chosenNetworkState: AddAddressUM.ChosenNetworkState) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (chosenNetworkState) {
            is AddAddressUM.ChosenNetworkState.Result -> NetworkIconsResolver(chosenNetworkState.networkUMList)
            AddAddressUM.ChosenNetworkState.Loading -> TangemLoader()
            AddAddressUM.ChosenNetworkState.Empty -> {
                Text(
                    modifier = Modifier.padding(start = 8.dp),
                    text = stringResourceSafe(R.string.address_book_select_network),
                    style = TangemTheme.typography.body2,
                    color = TangemTheme.colors3.text.secondary,
                )
                ChevronIcon()
            }
        }
    }
}

@Composable
private fun NetworkIconsResolver(networks: ImmutableList<NetworkUM>) {
    when (networks.size) {
        0 -> Unit
        1 -> {
            val network = networks.first()
            Image(
                painter = painterResource(id = network.iconResId),
                contentDescription = null,
            )
            Text(
                modifier = Modifier.padding(start = 8.dp),
                text = network.networkName,
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors3.text.secondary,
            )
            ChevronIcon()
        }
        // 3 and any larger count share the same rendering: up to MAX_VISIBLE_NETWORKS overlapping
        // icons, plus a "+N" badge that appears only when there are more than that.
        else -> {
            OverlappingNetworkIcons(networks)
            ChevronIcon()
        }
    }
}

@Composable
private fun OverlappingNetworkIcons(networks: ImmutableList<NetworkUM>) {
    val visible = networks.take(MAX_VISIBLE_NETWORKS)
    val remaining = networks.size - visible.size

    Box(modifier = Modifier.wrapContentWidth()) {
        visible.forEachIndexed { index, network ->
            Image(
                painter = painterResource(id = network.iconResId),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .padding(start = NetworkIconStep * index)
                    .networkIconRing()
                    .size(NetworkIconSize),
            )
        }
        if (remaining > 0) {
            Box(
                modifier = Modifier
                    .padding(start = NetworkIconStep * visible.size)
                    .networkIconRing()
                    .background(color = TangemTheme.colors3.bg.tertiary)
                    .size(NetworkIconSize),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "+$remaining",
                    style = TangemTheme.typography.caption1,
                    color = TangemTheme.colors3.text.secondary,
                )
            }
        }
    }
}

// bg-colored ring + clip applied to every overlapping element so the one drawn on top carves a
// crescent out of the one below it. The ring color must match the surface the icons sit on.
@Composable
private fun Modifier.networkIconRing(): Modifier = this
    .border(width = 2.dp, color = TangemTheme.colors3.bg.secondary, shape = CircleShape)
    .padding(2.dp)
    .clip(CircleShape)

@Composable
private fun ChevronIcon() {
    Image(
        modifier = Modifier.padding(start = 8.dp),
        painter = painterResource(id = R.drawable.ic_select_18_24),
        contentDescription = null,
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_NetworkBlock() {
    TangemThemePreviewRedesign {
        Column {
            NetworkBlock(
                chosenNetworkState = AddAddressUM.ChosenNetworkState.Result(
                    networkUMList = persistentListOf(
                        NetworkUM(networkName = "Ethereum", iconResId = R.drawable.img_eth_22),
                    ),
                ),
            )
            SpacerH12()
            NetworkBlock(
                chosenNetworkState = AddAddressUM.ChosenNetworkState.Result(
                    networkUMList = persistentListOf(
                        NetworkUM(networkName = "Ethereum", iconResId = R.drawable.img_eth_22),
                        NetworkUM(networkName = "BSC", iconResId = R.drawable.img_bsc_22),
                        NetworkUM(networkName = "Polygon", iconResId = R.drawable.img_polygon_22),
                    ),
                ),
            )
            SpacerH12()
            NetworkBlock(
                chosenNetworkState = AddAddressUM.ChosenNetworkState.Result(
                    networkUMList = List(15) {
                        NetworkUM(networkName = "Network", iconResId = R.drawable.img_eth_22)
                    }.toImmutableList(),
                ),
            )
            SpacerH12()
            NetworkBlock(chosenNetworkState = AddAddressUM.ChosenNetworkState.Loading)
            SpacerH12()
            NetworkBlock(chosenNetworkState = AddAddressUM.ChosenNetworkState.Empty)
        }
    }
}