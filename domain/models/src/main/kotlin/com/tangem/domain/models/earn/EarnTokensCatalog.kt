package com.tangem.domain.models.earn

import arrow.core.Either

/** Every earn opportunity the backend offers, held in full so search can filter it locally. */
typealias EarnTokensCatalog = Either<EarnError, List<EarnTokenWithCurrency>>