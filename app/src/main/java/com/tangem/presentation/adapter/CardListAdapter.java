package com.tangem.presentation.adapter;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.tangem.domain.wallet.Blockchain;
import com.tangem.domain.wallet.CoinEngine;
import com.tangem.domain.wallet.CoinEngineFactory;
import com.tangem.domain.wallet.TangemCard;
import com.tangem.wallet.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.tangem.domain.wallet.TangemCard.CountOurTx;

public class CardListAdapter extends RecyclerView.Adapter<CardListAdapter.CardViewHolder> {

    public interface UiCallbacks {
        void onViewCard(Bundle cardInfo);
    }

    static class CardViewHolder extends RecyclerView.ViewHolder {
        private CardView cv;
        private TextView tvBalance, tvOffline, tvBalanceEquivalent, tvWallet, tvInputs, tvStatusInBlockchain, tvCardID, tvLastInput, lbLastInput, lbLastOutput, tvLastOutput, tvBlockchain;
        private TextView tvType, tvTypeBg, tvVoid;
        private ImageView imgBlockchain, imgSecurityNotification;
        private View llCardLoaded, llCardEmpty, llCardError, llCardPurged;

        CardViewHolder(View itemView) {
            super(itemView);
            cv = itemView.findViewById(R.id.cvCard);
            tvBalance = itemView.findViewById(R.id.tvBalance);
            tvOffline = itemView.findViewById(R.id.tvOffline);
            tvBalanceEquivalent = itemView.findViewById(R.id.tvBalanceEquivalent);
            tvWallet = itemView.findViewById(R.id.tvWallet);
            tvCardID = itemView.findViewById(R.id.tvCardID);
            if (tvCardID != null) tvCardID.requestFocus();
            tvType = itemView.findViewById(R.id.tvType);
            tvTypeBg = itemView.findViewById(R.id.tvTypeBg);
            tvVoid = itemView.findViewById(R.id.tvVoid);
            tvInputs = itemView.findViewById(R.id.tvInputs);
            tvLastInput = itemView.findViewById(R.id.tvLastInput);
            lbLastInput = itemView.findViewById(R.id.lbLastInput);
            lbLastOutput = itemView.findViewById(R.id.lbLastOutput);
            tvLastOutput = itemView.findViewById(R.id.tvLastOutput);
            tvBlockchain = itemView.findViewById(R.id.tvBlockchain);
            imgBlockchain = itemView.findViewById(R.id.imgBlockchain);
            llCardLoaded = itemView.findViewById(R.id.cardLoaded);
            llCardEmpty = itemView.findViewById(R.id.cardEmpty);
            llCardError = itemView.findViewById(R.id.cardError);
            llCardPurged = itemView.findViewById(R.id.cardPurged);
            imgSecurityNotification = itemView.findViewById(R.id.imgSecurityNotification);
        }
    }

    private LayoutInflater mLayoutInflater;
    private Context mContext;
    private UiCallbacks mUiCallbacks;
    private List<TangemCard> mCards = new ArrayList<>(100);

    public CardListAdapter(LayoutInflater layoutInflater, Bundle instate, UiCallbacks uiCallbacks) {
        mLayoutInflater = layoutInflater;
        mContext = layoutInflater.getContext();
        mUiCallbacks = uiCallbacks;
        if (instate != null) {
            // restore state

            ArrayList<String> UIDs = instate.getStringArrayList("card_UID");

            if (UIDs != null) {
                for (String UID : UIDs) {
                    TangemCard card = new TangemCard(UID);
                    card.LoadFromBundle(instate.getBundle(String.format("card_%s", UID)));
                    mCards.add(card);
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        return mCards == null ? 0 : mCards.size();
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_list_card, parent, false);
        CardViewHolder cvh = new CardViewHolder(v);
        return cvh;
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        try {
            TangemCard card = mCards.get(position);
            CoinEngine engine = CoinEngineFactory.Create(card.getBlockchain());
            int color = android.R.color.black;
            switch (card.getStatus()) {
                case NotPersonalized:
                    color = R.color.card_state_error;
                    holder.llCardError.setVisibility(View.VISIBLE);
                    holder.llCardLoaded.setVisibility(View.GONE);
                    holder.llCardEmpty.setVisibility(View.GONE);
                    holder.llCardPurged.setVisibility(View.GONE);
                    holder.imgBlockchain.setVisibility(View.INVISIBLE);
                    holder.imgSecurityNotification.setVisibility(View.INVISIBLE);
                    holder.tvCardID.setVisibility(View.INVISIBLE);
                    holder.tvCardID.setError(null);
                    holder.tvType.setVisibility(View.INVISIBLE);
                    holder.tvTypeBg.setVisibility(View.INVISIBLE);
                    holder.tvVoid.setVisibility(View.INVISIBLE);
                    break;

                case Empty:
                    color = R.color.card_state_empty;
                    holder.llCardEmpty.setVisibility(View.VISIBLE);
                    holder.llCardLoaded.setVisibility(View.GONE);
                    holder.llCardError.setVisibility(View.GONE);
                    holder.llCardPurged.setVisibility(View.GONE);
                    holder.imgBlockchain.setVisibility(View.VISIBLE);
                    if (card.getBlockchain() != null) {
                        holder.imgBlockchain.setImageResource(card.getBlockchain().getImageResource(this.mContext, card.getTokenSymbol()));
                        holder.imgBlockchain.setVisibility(View.VISIBLE);
                    } else {
                        holder.imgBlockchain.setVisibility(View.INVISIBLE);
                    }
                    if (card.useDefaultPIN1() || card.useDefaultPIN2() || card.getPauseBeforePIN2() > 0 || card.useDevelopersFirmware()) {
                        //holder.imgSecurityNotification.setVisibility(View.VISIBLE);
                    } else {
                        holder.imgSecurityNotification.setVisibility(View.INVISIBLE);
                    }
                    holder.tvCardID.setText(card.getCIDDescription());
                    holder.tvCardID.setError(null);
                    holder.tvCardID.setVisibility(View.VISIBLE);

                    holder.tvType.setVisibility(View.VISIBLE);
                    holder.tvTypeBg.setVisibility(View.VISIBLE);
                    if (card.isReusable()) {
                        holder.tvType.setText(R.string.reusable_reusable);
                        holder.tvTypeBg.setBackgroundColor(ContextCompat.getColor(mContext, R.color.type_wallet));
                    } else {
                        holder.tvType.setText(R.string.banknote_banknote);
                        holder.tvTypeBg.setBackgroundColor(ContextCompat.getColor(mContext, R.color.type_banknote));
                    }
                    holder.tvVoid.setVisibility(View.INVISIBLE);
                    break;

                case Purged:
                    color = R.color.card_state_purged;
                    holder.llCardEmpty.setVisibility(View.GONE);
                    holder.llCardLoaded.setVisibility(View.GONE);
                    holder.llCardError.setVisibility(View.GONE);
                    holder.llCardPurged.setVisibility(View.VISIBLE);
                    holder.imgBlockchain.setVisibility(View.VISIBLE);
                    if (card.getBlockchain() != null) {
                        holder.imgBlockchain.setImageResource(card.getBlockchain().getImageResource(this.mContext, card.getTokenSymbol()));
                        holder.imgBlockchain.setVisibility(View.VISIBLE);
                    } else
                        holder.imgBlockchain.setVisibility(View.INVISIBLE);
                    holder.imgSecurityNotification.setVisibility(View.INVISIBLE);
                    holder.tvCardID.setText(card.getCIDDescription());
                    holder.tvCardID.setVisibility(View.VISIBLE);
                    holder.tvCardID.setError(null);
                    holder.tvType.setVisibility(View.VISIBLE);
                    holder.tvTypeBg.setVisibility(View.VISIBLE);
                    holder.tvType.setText(R.string.banknote_banknote);
                    holder.tvTypeBg.setBackgroundColor(ContextCompat.getColor(mContext, R.color.type_banknote));
                    holder.tvVoid.setVisibility(View.INVISIBLE);
                    break;

                case Loaded:
                    holder.llCardLoaded.setVisibility(View.VISIBLE);
                    holder.llCardError.setVisibility(View.GONE);
                    holder.llCardEmpty.setVisibility(View.GONE);
                    holder.llCardPurged.setVisibility(View.GONE);
                    holder.tvCardID.setText(card.getCIDDescription());
                    holder.tvCardID.setVisibility(View.VISIBLE);
                    holder.tvWallet.setText(card.getShortWalletString());
                    holder.tvBlockchain.setText(card.getBlockchainName());

                    if (card.getBlockchain() != null) {
                        holder.imgBlockchain.setImageResource(card.getBlockchain().getImageResource(this.mContext, card.getTokenSymbol()));
                        holder.imgBlockchain.setVisibility(View.VISIBLE);
                    } else {
                        holder.imgBlockchain.setVisibility(View.INVISIBLE);
                    }

                    if (!engine.HasBalanceInfo(card)) {
                        color = R.color.card_state_loaded;
                    } else if (engine.IsBalanceNotZero(card)) {
                        color = R.color.card_state_loaded_with_coins;
                    } else {
                        color = R.color.card_state_loaded_with_zero;
                    }

                    if (holder.tvStatusInBlockchain != null) {
                        if (card.hasBalanceInfo() && card.hasUnspentInfo() && card.getUnspentTransactions().size() > 0) {
                            holder.tvStatusInBlockchain.setText(R.string.ok);
                            holder.tvStatusInBlockchain.setTextColor(ContextCompat.getColor(mContext, R.color.confirmed));
                        } else if (!card.hasBalanceInfo() || !card.hasUnspentInfo()) {
                            holder.tvStatusInBlockchain.setText(R.string.no_data_string);
                            holder.tvStatusInBlockchain.setTextColor(ContextCompat.getColor(mContext, R.color.primary_dark));
                        } else {
                            holder.tvStatusInBlockchain.setText(R.string.not_found);
                            holder.tvStatusInBlockchain.setTextColor(ContextCompat.getColor(mContext, R.color.not_confirmed));
                        }
                    }

                    if (engine.HasBalanceInfo(card) || card.getOfflineBalance() == null) {
                        String balance = engine.GetBalance(card);
                        holder.tvBalance.setText(balance);
                        holder.tvBalanceEquivalent.setText(engine.GetBalanceEquivalent(card));
                        holder.tvBalance.setTextColor(Color.BLACK);
                        holder.tvOffline.setVisibility(View.INVISIBLE);

                    } else {

                        String offlineAmount = engine.ConvertByteArrayToAmount(card, card.getOfflineBalance());
                        holder.tvBalance.setText(engine.GetAmountDescription(card, offlineAmount));
                        holder.tvBalanceEquivalent.setText(engine.GetAmountEqualentDescriptor(card, offlineAmount));
                        holder.tvOffline.setVisibility(View.VISIBLE);
                    }
                    if (!card.getAmountEquivalentDescriptionAvailable()) {
                        //holder.tvBalanceEquivalent.setError("Service unavailable");
                    } else {
                        holder.tvBalanceEquivalent.setError(null);
                    }

                    String error = card.getError();
                    //holder.tvCardID.setError(error);

                    if (holder.tvInputs != null) {
                        holder.tvInputs.setText(card.getInputsDescription());
                    }

                    boolean visibleFlag = engine != null ? engine.InOutPutVisible() : true;
                    int visibleIOPuts = visibleFlag ? View.VISIBLE : View.GONE;
                    if (holder.tvLastInput != null) {
                        holder.tvLastInput.setText(card.getLastInputDescription());
                        if (card.getLastInputDescription().contains("awaiting"))
                            holder.tvLastInput.setTextColor(ContextCompat.getColor(mContext, R.color.not_confirmed));
                        else if (card.getLastInputDescription().contains("None") || card.getLastInputDescription().contains("--"))
                            holder.tvLastInput.setTextColor(ContextCompat.getColor(mContext, R.color.primary_dark));
                        else
                            holder.tvLastInput.setTextColor(ContextCompat.getColor(mContext, R.color.confirmed));

                        holder.tvLastInput.setVisibility(visibleIOPuts);
                    }

                    if (holder.lbLastInput != null) {
                        holder.lbLastInput.setVisibility(visibleIOPuts);
                    }

                    if (holder.tvLastOutput != null) {
                        holder.tvLastOutput.setText(card.getLastOutputDescription());
                        holder.tvLastOutput.setVisibility(visibleIOPuts);
                    }

                    if (holder.lbLastOutput != null) {
                        holder.lbLastOutput.setVisibility(visibleIOPuts);
                    }

                    if (card.useDefaultPIN1() || card.useDefaultPIN2() || card.getPauseBeforePIN2() > 0 || card.useDevelopersFirmware()) {
//                        holder.imgSecurityNotification.setVisibility(View.VISIBLE);
                    } else {
                        holder.imgSecurityNotification.setVisibility(View.INVISIBLE);
                    }

                    holder.tvType.setVisibility(View.VISIBLE);
                    holder.tvTypeBg.setVisibility(View.VISIBLE);
                    if (card.isReusable()) {
                        holder.tvType.setText(R.string.reusable_reusable);
                        holder.tvTypeBg.setBackgroundColor(ContextCompat.getColor(mContext, R.color.type_wallet));
                        holder.tvVoid.setVisibility(View.INVISIBLE);
                    } else {
                        holder.tvType.setText(R.string.banknote_banknote);
                        if (card.getRemainingSignatures() != card.getMaxSignatures()) {
                            holder.tvVoid.setVisibility(View.VISIBLE);
                            holder.tvTypeBg.setBackgroundColor(ContextCompat.getColor(mContext, R.color.msg_err));
                        } else {
                            holder.tvVoid.setVisibility(View.INVISIBLE);
                            holder.tvTypeBg.setBackgroundColor(ContextCompat.getColor(mContext, R.color.type_banknote));
                        }
                    }
                    break;
            }

            if (card.useDevelopersFirmware()) {
                holder.tvType.setText(R.string.developer_kit);
                holder.tvTypeBg.setBackgroundColor(ContextCompat.getColor(mContext, R.color.fab));
                holder.tvVoid.setVisibility(View.INVISIBLE);
            }

            holder.cv.setOnClickListener(new CardClickListener(position));
            holder.cv.setCardBackgroundColor(mContext.getResources().getColor(color));

        } catch (Exception e) {
            Log.e("onBindViewHolder", e.toString());
        }
    }

    public void onSaveInstanceState(Bundle outstate) {
        ArrayList<String> UIDs = new ArrayList<>(mCards.size());
        for (TangemCard card : mCards) {
            Bundle B = new Bundle();
            card.SaveToBundle(B);
            outstate.putBundle(String.format("card_%s", card.getUID()), B);
            UIDs.add(card.getUID());
        }
        outstate.putStringArrayList("card_UID", UIDs);
    }

    public void clearCards() {
        mCards.clear();
        notifyDataSetChanged();
    }

    public TangemCard getCard(int cardIndex) {
        return mCards.get(cardIndex);
    }

    public TangemCard getCardByWallet(String walletAddress) {
        for (TangemCard c : mCards) {
            if (c.getWallet().equals(walletAddress)) {
                return c;
            }
        }
        return null;
    }

    public void addCard(TangemCard card) {
        mCards.add(0, card);
        notifyDataSetChanged();
    }

    public void removeCard(int cardIndex) {
        mCards.remove(cardIndex);
        notifyItemRemoved(cardIndex);
    }

    public void removeCard(TangemCard card) {
        int i;
        for (i = 0; i < mCards.size(); i++) {
            if (Arrays.equals(mCards.get(i).getCID(), card.getCID())) {
                break;
            }
        }

        if (i == mCards.size()) return;

        removeCard(i);
    }

    public void updateCard(TangemCard card) {
        int i;
        for (i = 0; i < mCards.size(); i++) {
            if (Arrays.equals(mCards.get(i).getCID(), card.getCID())) {
                break;
            }
        }

        if (i == mCards.size()) {
            mCards.add(card);
        } else {
            mCards.remove(i);
            mCards.add(i, card);
        }
        notifyDataSetChanged();
    }

    public void UpdateWalletBalance(String walletAddress, Long balanceConfirmed, Long balanceUnconfirmed, String validationNodeDescription) {
        for (TangemCard c : mCards) {
            if (c.getWallet().equals(walletAddress)) {
                c.setBalanceConfirmed(balanceConfirmed);
                c.setBalanceUnconfirmed(balanceUnconfirmed);
                c.setDecimalBalance(String.valueOf(balanceConfirmed));
                c.setValidationNodeDescription(validationNodeDescription);
                notifyDataSetChanged();
                return;
            }
        }
    }

    public void UpdateWalletCoutConfirmTx(String walletAddress, BigInteger nonce) {
        for (TangemCard c : mCards) {
            if (c.getWallet().equals(walletAddress)) {
                c.SetConfirmTXCount(nonce);
                notifyDataSetChanged();
                return;
            }
        }
    }

    public void UpdateWalletBlockchain(String walletAddress, Blockchain blockchain) {
        for (TangemCard c : mCards) {
            if (c.getWallet().equals(walletAddress)) {
                c.setBlockchainID(blockchain.getID());
                notifyDataSetChanged();
                return;
            }
        }
    }

    public void AddWalletBlockchainNameToken(String walletAddress) {
        for (TangemCard c : mCards) {
            if (c.getWallet().equals(walletAddress)) {
                c.addTokenToBlockchainName();
                notifyDataSetChanged();
                return;
            }
        }
    }

    public void UpdateWalletBalance(String walletAddress, Long balanceConfirmed, String balanceString, String validationNodeDescription) {
        for (TangemCard c : mCards) {
            if (c.getWallet().equals(walletAddress)) {
                c.setBalanceConfirmed(balanceConfirmed);
                c.setBalanceUnconfirmed(0L);
                c.setDecimalBalance(String.valueOf(balanceString));
                c.setValidationNodeDescription(validationNodeDescription);
                notifyDataSetChanged();
                return;
            }
        }
    }

    public void UpdateWalletBalanceOnlyAlter(String walletAddress, String balanceAlter) {
        for (TangemCard c : mCards) {
            if (c.getWallet().equals(walletAddress)) {
                c.setDecimalBalanceAlter(balanceAlter);
                notifyDataSetChanged();
                return;
            }
        }
    }

    public void UpdateWalletUnspent(String walletAddress, JSONArray jsUnspentArray) {
        for (TangemCard c : mCards) {
            if (c.getWallet().equals(walletAddress)) {
                try {
                    c.getUnspentTransactions().clear();
                    for (int i = 0; i < jsUnspentArray.length(); i++) {
                        JSONObject jsUnspent = jsUnspentArray.getJSONObject(i);
                        TangemCard.UnspentTransaction trUnspent = new TangemCard.UnspentTransaction();
                        trUnspent.txID = jsUnspent.getString("tx_hash");
                        trUnspent.Amount = jsUnspent.getInt("value");
                        trUnspent.Height = jsUnspent.getInt("height");
                        c.getUnspentTransactions().add(trUnspent);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                notifyDataSetChanged();
                return;
            }
        }
    }

    public void UpdateWalletHistory(String walletAddress, JSONArray jsHistoryArray) {
        for (TangemCard c : mCards) {
            if (c.getWallet().equals(walletAddress)) {
                try {
                    c.getHistoryTransactions().clear();
                    for (int i = 0; i < jsHistoryArray.length(); i++) {
                        JSONObject jsUnspent = jsHistoryArray.getJSONObject(i);
                        TangemCard.HistoryTransaction trHistory = new TangemCard.HistoryTransaction();
                        trHistory.txID = jsUnspent.getString("tx_hash");
                        trHistory.Height = jsUnspent.getInt("height");
                        c.getHistoryTransactions().add(trHistory);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                notifyDataSetChanged();
                return;
            }
        }
    }

    public void UpdateWalletHeader(String walletAddress, JSONObject jsHeader) {
        for (TangemCard c : mCards) {
            if (c.getWallet().equals(walletAddress)) {
                try {
                    c.getHaedersInfo();
                    c.UpdateHeaderInfo(new TangemCard.HeaderInfo(jsHeader.getInt("block_height"), jsHeader.getInt("timestamp")));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                notifyDataSetChanged();
                return;
            }
        }
    }

    public void UpdateRate(String walletAddress, float rate) {
        for (TangemCard c : mCards) {
            if (c.getWallet().equals(walletAddress)) {
                c.setRate(rate);
                notifyDataSetChanged();
                break;
            }
        }
    }

    public void UpdateRateAlter(String walletAddress, float rate) {
        for (TangemCard c : mCards) {
            if (c.getWallet().equals(walletAddress)) {
                c.setRateAlter(rate);
                notifyDataSetChanged();
                break;
            }
        }
    }

    public void UpdateTransaction(String walletAddress, String txHash, String raw) {
        for (TangemCard c : mCards) {
            if (c.getWallet().equals(walletAddress)) {

                List<TangemCard.UnspentTransaction> listTx = c.getUnspentTransactions();
                for (TangemCard.UnspentTransaction tx : listTx) {
                    if (tx.txID.equals(txHash)) {
                        tx.Raw = raw;
                    }
                }

                List<TangemCard.HistoryTransaction> listHTx = c.getHistoryTransactions();
                for (TangemCard.HistoryTransaction tx : listHTx) {
                    if (tx.txID.equals(txHash)) {
                        tx.Raw = raw;
                        CountOurTx(listHTx);
                    }
                }
                notifyDataSetChanged();
                return;
            }
        }
    }

    public void UpdateWalletError(String walletAddress, String error) {
        for (TangemCard c : mCards) {
            if (c.getWallet().equals(walletAddress)) {
                c.setError(error);
                notifyDataSetChanged();
                return;
            }
        }
    }

    private class CardClickListener implements View.OnClickListener {
        private TangemCard card;
        private int pos;

        CardClickListener(int position) {
            card = mCards.get(position);
            pos = position;
        }

        @Override
        public void onClick(View v) {
            Bundle b = new Bundle();
            b.putString("UID", card.getUID());
            Bundle bCard = new Bundle();
            card.SaveToBundle(bCard);
            b.putBundle("Card", bCard);
            mUiCallbacks.onViewCard(b);
        }
    }

}