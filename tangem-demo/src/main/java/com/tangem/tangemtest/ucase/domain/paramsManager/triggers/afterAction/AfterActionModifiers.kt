package com.tangem.tangemtest.ucase.domain.paramsManager.triggers.afterAction

import com.tangem.tangemtest._arch.structure.abstraction.Item
import com.tangem.tasks.TaskEvent

/**
[REDACTED_AUTHOR]
 *
 * The After Action Modification class family is intended for modifying parameters (if necessary)
 * after calling CardManager.anyAction.
 * Returns a list of parameters that have been modified
 */
interface AfterActionModification {
    fun modify(taskEvent: TaskEvent<*>, paramsList: List<Item>): List<Item>
}