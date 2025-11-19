package com.tangem.blockchainsdk.utils

import com.tangem.blockchain.common.Blockchain
import com.tangem.core.configtoggle.blockchain.ExcludedBlockchainsManager
import org.jetbrains.annotations.TestOnly
import javax.inject.Inject

@Suppress("MemberNameEqualsClassName")
class ExcludedBlockchains @Inject internal constructor(
    private val excludedBlockchainsManager: ExcludedBlockchainsManager,
) : Set<Blockchain> {

    private val excludedBlockchainsSet: Set<Blockchain> by lazy(mode = LazyThreadSafetyMode.NONE) {
        excludedBlockchainsManager.excludedBlockchainsIds.fold(mutableSetOf()) { acc, blockchainId ->
            val blockchain = Blockchain.fromId(blockchainId)
            acc.add(blockchain)

            blockchain.getTestnetVersion()?.let { acc.add(it) }

            acc
        }
    }

    override val size: Int
        get() = excludedBlockchainsSet.size

    @TestOnly
    constructor() : this(
        excludedBlockchainsManager = object : ExcludedBlockchainsManager {

            override val excludedBlockchainsIds: Set<String> = emptySet()
        },
    )

    override fun contains(element: Blockchain): Boolean = excludedBlockchainsSet.contains(element)

    override fun containsAll(elements: Collection<Blockchain>): Boolean = excludedBlockchainsSet.containsAll(elements)

    override fun isEmpty(): Boolean = excludedBlockchainsSet.isEmpty()

    override fun iterator(): Iterator<Blockchain> = excludedBlockchainsSet.iterator()
}