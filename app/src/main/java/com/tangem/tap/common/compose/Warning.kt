package com.tangem.tap.common.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tangem.common.module.ModuleMessage
import com.tangem.tap.common.moduleMessage.ModuleMessageConverter
import com.tangem.wallet.R

/**
[REDACTED_AUTHOR]
 */
@Composable
fun AddCustomTokenWarning(
    modifier: Modifier = Modifier,
    warning: ModuleMessage,
    converter: ModuleMessageConverter
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = colorResource(id = R.color.warning_warning),
        elevation = 4.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = stringResource(id = R.string.common_warning),
                color = colorResource(id = R.color.white),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            SpacerH8()
            Text(
                text = converter.convert(warning),
                color = colorResource(id = R.color.white),
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }

    }
}