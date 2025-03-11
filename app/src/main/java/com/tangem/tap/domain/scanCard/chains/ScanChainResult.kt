package com.tangem.tap.domain.scanCard.chains

import arrow.core.Either
import com.tangem.domain.card.ScanCardException
import com.tangem.domain.models.scan.ScanResponse

internal typealias ScanChainResult = Either<ScanCardException, ScanResponse>