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
import com.tangem.core.ui.components.SpacerH8
import com.tangem.tap.domain.moduleMessage.ModuleMessageConverter
import com.tangem.wallet.R

/**
 * Created by Anton Zhilenkov on 19/04/2022.
 */
@Composable
fun AddCustomTokenWarning(
    warning: ModuleMessage,
    converter: ModuleMessageConverter,
    modifier: Modifier = Modifier,
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
                text = converter.convert(warning).message,
                color = colorResource(id = R.color.white),
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}
