package com.tangem.feature.swap.domain.models

import com.tangem.feature.swap.domain.models.data.DataError

/**
 * Model that aggregate data model from repository return with error [DataError] if it exists
 *
 * @param T model type
 * @property dataModel
 * @property error possible from repository [DataError]
 */
data class AggregatedSwapDataModel<T>(
    val dataModel: T?,
    val error: DataError = DataError.NO_ERROR,
)
