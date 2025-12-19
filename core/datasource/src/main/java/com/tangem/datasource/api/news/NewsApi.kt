package com.tangem.datasource.api.news

import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.news.models.response.NewsCategoriesResponse
import com.tangem.datasource.api.news.models.response.NewsDetailsResponse
import com.tangem.datasource.api.news.models.response.NewsListResponse
import com.tangem.datasource.api.news.models.response.NewsTrendingResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface NewsApi {

    @GET(NEWS_PATH)
    suspend fun getNews(
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("lang") language: String? = null,
        @Query("asOf") snapshot: String? = null,
        @Query("tokenIds") tokenIds: List<String>? = null,
        @Query("categoryIds") categoryIds: List<Int>? = null,
    ): ApiResponse<NewsListResponse>

    @GET("$NEWS_PATH/{newsId}")
    suspend fun getNewsDetails(
        @Path("newsId") newsId: Int,
        @Query("lang") language: String? = null,
    ): ApiResponse<NewsDetailsResponse>

    @GET("$NEWS_PATH/trending")
    suspend fun getTrendingNews(
        @Query("limit") limit: Int? = null,
        @Query("lang") language: String? = null,
    ): ApiResponse<NewsTrendingResponse>

    @GET("$NEWS_PATH/categories")
    suspend fun getCategories(): ApiResponse<NewsCategoriesResponse>

    private companion object {

        private const val NEWS_PATH = "v1/news"
    }
}