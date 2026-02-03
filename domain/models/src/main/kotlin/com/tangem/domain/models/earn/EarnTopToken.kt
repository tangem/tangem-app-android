package com.tangem.domain.models.earn

import arrow.core.Either

typealias EarnTopToken = Either<EarnError, List<EarnTokenWithCurrency>>