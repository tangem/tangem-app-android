package com.tangem.tap.common.compose.extensions

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Created by Anton Zhilenkov on 10/04/2022.
 */
@Composable
fun stringResourceDefault(@StringRes id: Int?, default: String = ""): String {
    val resources = LocalContext.current.resources
    return try {
        resources.getString(requireNotNull(id))
    } catch (ex: Resources.NotFoundException) {
        default
    } catch (ex: IllegalArgumentException) {
        default
    }
}
