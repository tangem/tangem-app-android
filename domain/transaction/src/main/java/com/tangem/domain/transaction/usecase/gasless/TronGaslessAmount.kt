package com.tangem.domain.transaction.usecase.gasless

import com.tangem.blockchain.common.Amount
import java.math.BigInteger

internal fun Amount.toTronGaslessBaseUnits(): BigInteger? = value?.movePointRight(decimals)?.toBigInteger()