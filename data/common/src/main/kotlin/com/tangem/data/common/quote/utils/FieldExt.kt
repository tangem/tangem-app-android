package com.tangem.data.common.quote.utils

import com.tangem.data.common.quote.QuotesFetcher

fun Set<QuotesFetcher.Field>.combine(): String = joinToString(separator = ",", transform = QuotesFetcher.Field::value)