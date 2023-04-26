package com.tangem.data.source.card.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.plus

// FIXME: Remove
internal val scope: CoroutineScope = GlobalScope + Dispatchers.IO
