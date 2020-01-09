package com.bookmap.ordermanagement;

/**
 * Basic Order holds properties of particular Order that is stored in Order Book
 */
public class BasicOrder {

    public enum OrderSide {BID, ASK};

    private int size;
    private final int price;
    private final OrderSide side;

    public BasicOrder(int price, int size, OrderSide side) {
        if (price <= 0)
            throw new IllegalArgumentException("Price should be positive.");
        this.price = price;
        if (size <= 0)
            throw new IllegalArgumentException("Size should be positive.");
        this.size = size;
        this.side = side;
    }

    public int getPrice() {
        return price;
    }

    public int getSize() {
        return size;
    }

    public BasicOrder setSize(int size) {
        if (size <= 0)
            throw new IllegalArgumentException("Size should be positive.");
        this.size = size;
        return this;
    }

    public OrderSide getSide() {
        return side;
    }

}
