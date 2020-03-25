package com.tangem.tangemtest.card_use_cases.resources.initializers

import com.tangem.tangemtest.R
import com.tangem.tangemtest._arch.structure.base.Id
import com.tangem.tangemtest.card_use_cases.resources.ActionRes
import com.tangem.tangemtest.card_use_cases.resources.ActionType
import com.tangem.tangemtest.card_use_cases.resources.ResourceHolder

/**
[REDACTED_AUTHOR]
 */
class ActionResources {

    fun init(holder: ResourceHolder<Id>) {
        holder.register(ActionType.Scan, ActionRes(R.string.action_card_scan, R.string.info_action_card_scan, R.id.action_nav_entry_point_to_nav_scan))
        holder.register(ActionType.Sign, ActionRes(R.string.action_card_sign, R.string.info_action_card_sign, R.id.action_nav_entry_point_to_nav_sign))
        holder.register(ActionType.Personalize, ActionRes(R.string.action_personalize, R.string.info_action_personalize, R.id.action_nav_entry_point_to_nav_personalize))
        holder.register(ActionType.Depersonalize, ActionRes(R.string.action_depersonalize, R.string.info_action_depersonalize, R.id.action_nav_entry_point_to_nav_depersonalize))
        holder.register(ActionType.CreateWallet, ActionRes(R.string.action_wallet_create, R.string.info_action_wallet_create, R.id.action_nav_entry_point_to_nav_wallet_create))
        holder.register(ActionType.PurgeWallet, ActionRes(R.string.action_wallet_purge, R.string.info_action_wallet_purge, R.id.action_nav_entry_point_to_nav_wallet_purge))
        holder.register(ActionType.ReadIssuerData, ActionRes(R.string.action_issuer_read_data, R.string.info_action_issuer_read_data, R.id.action_nav_entry_point_to_nav_issuer_read_data))
        holder.register(ActionType.WriteIssuerData, ActionRes(R.string.action_issuer_write_data, R.string.info_action_issuer_write_data, R.id.action_nav_entry_point_to_nav_issuer_write_data))
        holder.register(ActionType.ReadIssuerExData, ActionRes(R.string.action_issuer_read_ex_data, R.string.info_action_issuer_read_ex_data, R.id.action_nav_entry_point_to_nav_issuer_read_ex_data))
        holder.register(ActionType.WriteIssuerExData, ActionRes(R.string.action_issuer_write_ex_data, R.string.info_action_issuer_write_ex_data, R.id.action_nav_entry_point_to_nav_issuer_write_ex_data))
        holder.register(ActionType.ReadUserData, ActionRes(R.string.action_user_read_data, R.string.info_action_user_read_data, R.id.action_nav_entry_point_to_nav_user_read_data))
        holder.register(ActionType.WriteUserData, ActionRes(R.string.action_user_write_data, R.string.info_action_user_write_data, R.id.action_nav_entry_point_to_nav_user_write_data))
//        holder.register(ActionType.Unknown, getIfNotContains())
    }
}