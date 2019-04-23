package com.tangem.wallet.binance.client.domain.broadcast;

import com.tangem.wallet.binance.client.BinanceDexConstants;
import com.tangem.wallet.binance.client.domain.OrderSide;
import com.tangem.wallet.binance.client.domain.OrderType;
import com.tangem.wallet.binance.client.domain.TimeInForce;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class NewOrder {
    private String symbol;
    private OrderType orderType;
    private OrderSide side;
    private String price;
    private String quantity;
    private TimeInForce timeInForce;

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public OrderType getOrderType() {
        return orderType;
    }

    public void setOrderType(OrderType orderType) {
        this.orderType = orderType;
    }

    public OrderSide getSide() {
        return side;
    }

    public void setSide(OrderSide side) {
        this.side = side;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public TimeInForce getTimeInForce() {
        return timeInForce;
    }

    public void setTimeInForce(TimeInForce timeInForce) {
        this.timeInForce = timeInForce;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, BinanceDexConstants.BINANCE_DEX_TO_STRING_STYLE)
                .append("symbol", symbol)
                .append("orderType", orderType)
                .append("side", side)
                .append("price", price)
                .append("quantity", quantity)
                .append("timeInForce", timeInForce)
                .toString();
    }
}
