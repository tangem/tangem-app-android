package com.tangem.domain.marketing

import kotlinx.coroutines.flow.Flow

interface PromoPageRepository {

    fun observePage(): Flow<String?>

    /** `null` drops the kept page: the last link wins even without a page */
    suspend fun setPage(page: String?)

    suspend fun consumePage(): String?
}