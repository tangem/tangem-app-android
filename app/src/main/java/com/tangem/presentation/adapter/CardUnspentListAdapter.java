package com.tangem.presentation.adapter;

import android.content.Context;
import android.os.Build;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.tangem.wallet.R;
import com.tangem.wallet.Tangem_Card;

import static android.text.Html.FROM_HTML_MODE_COMPACT;

/**
 * Created by dvol on 17.07.2017.
 */

public class CardUnspentListAdapter extends BaseAdapter {
    private LayoutInflater mLayoutInflater;
    private Context mContext;
    private Tangem_Card mCard;

    public CardUnspentListAdapter(LayoutInflater layoutInflater, Tangem_Card card) {
        mLayoutInflater = layoutInflater;
        mContext = layoutInflater.getContext();
        mCard = card;
    }

    @Override
    public int getCount() {
        if (mCard != null && mCard.getUnspentTransactions() != null)
            return mCard.getUnspentTransactions().size();
        return 0;
    }

    @Override
    public Object getItem(int i) {
        if (mCard != null && mCard.getUnspentTransactions() != null)
            return mCard.getUnspentTransactions().get(i);
        return null;
    }

    @Override
    public long getItemId(int i) {
        if (mCard != null && mCard.getUnspentTransactions() != null)
            return mCard.getUnspentTransactions().get(i).txID.hashCode();
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {
        if (convertView == null) {
            convertView = mLayoutInflater.inflate(R.layout.card_unspent_list_item, viewGroup,
                    false);
        }
        TextView tvItem = (TextView) convertView.findViewById(R.id.tvItem);

        Tangem_Card.UnspentTransaction unspentTransaction = (Tangem_Card.UnspentTransaction) getItem(position);

        String html=String.format("<b>%d mBTC</b><br>%s", unspentTransaction.Amount, unspentTransaction.txID);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            tvItem.setText(Html.fromHtml(html, FROM_HTML_MODE_COMPACT));
        } else {
            tvItem.setText(Html.fromHtml(html.toString()));
        }
        return convertView;
    }

    public void Clear() {
        mCard.getUnspentTransactions().clear();
        notifyDataSetChanged();
    }

    public void UpdateUnspent(String tx_hash, int value, int height) {
        Tangem_Card.UnspentTransaction newUT=new Tangem_Card.UnspentTransaction();
        newUT.txID=tx_hash;
        newUT.Amount=value;
        newUT.Height=height;
        mCard.getUnspentTransactions().add(newUT);
        notifyDataSetChanged();
    }
}
