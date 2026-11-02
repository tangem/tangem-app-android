package com.tangem.common.ui.userwallet

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.res.TangemThemePreview

@Composable
fun SocialLoginWalletIcon(modifier: Modifier = Modifier, size: Dp = 16.dp) {
    Image(
        modifier = modifier.size(size),
        painter = painterResource(R.drawable.img_google_32),
        contentDescription = null,
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SocialLoginWalletIconPreview() {
    TangemThemePreview {
        SocialLoginWalletIcon()
    }
}