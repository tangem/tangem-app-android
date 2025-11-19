package com.tangem.domain.news.model

import com.tangem.domain.news.models.ShortArticle
import com.tangem.pagination.BatchFlow
import com.tangem.pagination.BatchingContext

typealias NewsListBatchingContext = BatchingContext<Int, NewsListConfig, Nothing>

typealias NewsListBatchFlow = BatchFlow<Int, List<ShortArticle>, Nothing>