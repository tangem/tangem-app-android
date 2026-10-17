package com.tangem.domain.collectibles.account

import com.tangem.domain.collectibles.models.CollectibleGachaMachine
import com.tangem.domain.collectibles.models.total
import com.tangem.domain.models.account.CollectiblesVaultBalance

fun CollectiblesVaultBalance.covers(machine: CollectibleGachaMachine): Boolean = usdcAmount >= machine.total