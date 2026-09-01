package com.tangem.domain.collectibles.models

data class CollectibleGachaMachineCategories(
    val categories: List<MachineCategory>,
    val totalMachinesQuantity: Int,
) {
    data class MachineCategory(
        val id: String,
        val name: String,
        val machinesQuantity: Int,
    )
}