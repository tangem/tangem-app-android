package com.tangem.datasource.utils

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.firstOrNull

suspend fun <T> DataStore<T>.getSyncOrNull(): T? = data.firstOrNull()