package com.tangem.tap.common.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.appcompattheme.AppCompatTheme
import com.tangem.wallet.R

@Composable
fun Hand(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .padding(vertical = 8.dp)
                .width(32.dp)
                .height(4.dp)
                .background(
                    color = colorResource(id = R.color.icon_inactive),
                    shape = RoundedCornerShape(size = 2.dp),
                ),
        )
    }
}

// region Preview
@Composable
private fun HandSample(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(color = colorResource(id = R.color.background_secondary))
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Hand()
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun HandPreview() {
    AppCompatTheme {
        HandSample()
    }
}
// endregion Preview
