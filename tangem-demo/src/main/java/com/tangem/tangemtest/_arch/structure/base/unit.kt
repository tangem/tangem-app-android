package com.tangem.tangemtest._arch.structure.base

/**
* [REDACTED_AUTHOR]
 */
interface Unit : Payload {
    val id: Id
    var parent: Unit?
}

abstract class BaseUnit : Unit {
    override var parent: Unit? = null
    override val payload: MutableMap<String, Any?> = mutableMapOf()
}

abstract class DataUnit<D>(
        override var viewModel: UnitViewModel<D>?
) : BaseUnit(), DataHolder<UnitViewModel<D>>