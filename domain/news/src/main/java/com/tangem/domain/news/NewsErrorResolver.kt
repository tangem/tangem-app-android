package com.tangem.domain.news

import com.tangem.domain.models.news.NewsError

interface NewsErrorResolver {

    fun resolve(throwable: Throwable?): NewsError
}