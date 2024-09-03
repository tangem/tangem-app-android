package com.tangem.datasource.utils

import android.net.Uri

fun Uri?.isNullOrEmpty(): Boolean {
    return this == null || this == Uri.EMPTY
}