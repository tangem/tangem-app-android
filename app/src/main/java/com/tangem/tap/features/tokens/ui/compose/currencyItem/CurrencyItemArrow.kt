package com.tangem.tap.features.tokens.ui.compose.currencyItem

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.wallet.R

/**
 * Created by Anton Zhilenkov on 27/07/2022.
 */
@Composable
fun CurrencyItemArrow(
    rowHeight: Dp,
    isLastArrow: Boolean,
) {
    Box(Modifier.height(rowHeight)) {
        if (!isLastArrow) {
            MiddleArrowView(rowHeight)
        } else {
            LastArrowView(rowHeight)
        }
    }
}

@Suppress("MagicNumber")
@Composable
private fun MiddleArrowView(rowHeight: Dp) {
    Box {
        Box(
            modifier = Modifier
                .padding(start = 0.5.dp),
        ) {
            Box(
                modifier = Modifier
                    .background(color = Color(0xFFDEDEDE))
                    .width(0.8.dp)
                    .fillMaxHeight(),
            )
        }

        LastArrowView(rowHeight)
    }
}

@Composable
private fun LastArrowView(rowHeight: Dp) {
    Image(
        modifier = Modifier
            .height(rowHeight / 2 + 3.5.dp),
        painter = painterResource(id = R.drawable.ic_link),
        contentDescription = null,
    )
}

@Preview
@Composable
private fun ArrowViewPreview() {
    Box(Modifier.background(color = Color.White)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(start = 37.dp),
        ) {
            CurrencyItemArrow(
                rowHeight = 62.dp,
                isLastArrow = false,
            )
            CurrencyItemArrow(
                rowHeight = 62.dp,
                isLastArrow = true,
            )
        }
    }
}
