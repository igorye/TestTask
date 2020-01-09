package com.bookmap.ordermanagement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.bookmap.ordermanagement.BasicOrder.OrderSide.ASK;
import static com.bookmap.ordermanagement.BasicOrder.OrderSide.BID;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by igorye on 09.01.2020.
 */
class OrdersBookOperationTest {

    public static final int        FIRST_BEST_ASK_PRICE  = 5;
    public static final int        FIRST_ASK_SIZE        = 4;
    public static final int        SECOND_BEST_ASK_PRICE = 10;
    public static final int        FIRST_BEST_BID_PRICE  = 3;
    public static final int        FIRST_BID_SIZE        = 5;
    public static final int        SECOND_BEST_BID_PRICE = 2;
    public static final int        THIRD_BID_SIZE        = 15;
    public static final int        THIRD_ASK_SIZE        = 12;
    private             OrdersBook book;

    @BeforeEach
    void setUp() {
        book = new OrdersBook();
        book.addOrder(new BasicOrder(FIRST_BEST_ASK_PRICE, FIRST_ASK_SIZE, ASK));
        book.addOrder(new BasicOrder(FIRST_BEST_ASK_PRICE, 7, ASK));
        book.addOrder(new BasicOrder(SECOND_BEST_ASK_PRICE, THIRD_ASK_SIZE, ASK));

        book.addOrder(new BasicOrder(FIRST_BEST_BID_PRICE, FIRST_BID_SIZE, BID));
        book.addOrder(new BasicOrder(FIRST_BEST_BID_PRICE, 9, BID));
        book.addOrder(new BasicOrder(SECOND_BEST_BID_PRICE, THIRD_BID_SIZE, BID));
    }

    @Test
    void buyOne() {
        int bestAskPrice = book.queryBestAsk();
        int bestAskSize = book.querySize(bestAskPrice);
        book.buy(1);

        assertNotEquals(bestAskSize, book.querySize(bestAskPrice), "Best Ask size should have decreased");
        assertEquals(bestAskSize - 1, book.querySize(bestAskPrice), "Best Ask size should have decreased by 1");
        assertEquals(FIRST_BEST_ASK_PRICE, book.queryBestAsk(), "Best Ask price shouldn't have changed");
    }

    @Test
    void buyOrderSizedAmount() {
        int bestAskPrice = book.queryBestAsk();
        int bestAskSize = book.querySize(bestAskPrice);
        book.buy(FIRST_ASK_SIZE);

        assertNotEquals(bestAskSize, book.querySize(bestAskPrice), "Best Ask size should have decreased");
        assertEquals(bestAskSize - FIRST_ASK_SIZE, book.querySize(bestAskPrice));
        assertEquals(FIRST_BEST_ASK_PRICE, book.queryBestAsk(), "Best Ask price shouldn't have changed");
    }

    @Test
    void buyOrderSizedAmountPlusOne() {
        int bestAskPrice = book.queryBestAsk();
        int bestAskSize = book.querySize(bestAskPrice);

        book.buy(FIRST_ASK_SIZE + 1);

        assertEquals(bestAskSize - (FIRST_ASK_SIZE + 1), book.querySize(bestAskPrice));
        assertEquals(FIRST_BEST_ASK_PRICE, book.queryBestAsk(), "Best Ask price shouldn't have changed");
    }

    @Test
    void buyPriceLevelSizeAmount() {
        int bestAskPrice = book.queryBestAsk();
        int bestAskSize = book.querySize(bestAskPrice);
        book.buy(bestAskSize);
        int newBestAskPrice = book.queryBestAsk();

        assertNotEquals(bestAskPrice, newBestAskPrice, "Best Ask price should have changed");
        assertEquals(SECOND_BEST_ASK_PRICE,
                     newBestAskPrice,
                     "Best Ask price should have changed to SECOND_BEST_ASK_PRICE");
        assertEquals(0,
                     book.querySize(bestAskPrice),
                     "Orders at FIRST_BEST_ASK_PRICE should have been removed completely");
    }

    @Test
    void buyPriceLevelSizeAmountPlusOne() {
        int bestAskPrice = book.queryBestAsk();
        int bestAskSize = book.querySize(bestAskPrice);
        final int buyAmount = bestAskSize + 1;

        book.buy(buyAmount);
        int newBestAskPrice = book.queryBestAsk();

        assertEquals(0,
                     book.querySize(bestAskPrice),
                     "Orders at FIRST_BEST_ASK_PRICE should have been removed completely");
        assertEquals(SECOND_BEST_ASK_PRICE, newBestAskPrice, "Best Ask should have changed to SECOND_BEST_ASK_PRICE");
        assertEquals(THIRD_ASK_SIZE - 1, book.querySize(newBestAskPrice), "Order#3 should have changed by 1");
    }

    @Test
    void sellOne() {
        int bestBidPrice = book.queryBestBid();
        int bestBidSize = book.querySize(bestBidPrice);
        book.sell(1);

        assertNotEquals(bestBidSize, book.querySize(bestBidPrice), "Best Bid size should have decreased");
        assertEquals(bestBidSize - 1, book.querySize(bestBidPrice), "Best Bid size should have decreased by 1");
        assertEquals(FIRST_BEST_BID_PRICE, book.queryBestBid(), "Best Bid price shouldn't have changed");
    }

    @Test
    void sellOrderSizedAmount() {
        int bestBidPrice = book.queryBestBid();
        int bestBidSize = book.querySize(bestBidPrice);
        book.sell(FIRST_BID_SIZE);

        assertNotEquals(bestBidSize, book.querySize(bestBidPrice), "Best Bid size should have decreased");
        assertEquals(bestBidSize - FIRST_BID_SIZE, book.querySize(bestBidPrice));
        assertEquals(FIRST_BEST_BID_PRICE, book.queryBestBid(), "Best Bid price shouldn't have changed");
    }

    @Test
    void sellOrderSizedAmountPlusOne() {
        int bestBidPrice = book.queryBestBid();
        int bestBidSize = book.querySize(bestBidPrice);

        book.sell(FIRST_BID_SIZE + 1);

        assertEquals(bestBidSize - (FIRST_BID_SIZE + 1), book.querySize(bestBidPrice));
        assertEquals(FIRST_BEST_BID_PRICE, book.queryBestBid(), "Best Bid price shouldn't have changed");
    }

    @Test
    void sellPriceLevelSizeAmount() {
        int bestBidPrice = book.queryBestBid();
        int bestBidSize = book.querySize(bestBidPrice);
        book.sell(bestBidSize);
        int newBestBidPrice = book.queryBestBid();

        assertNotEquals(bestBidPrice, newBestBidPrice, "Best Bid price should have changed");
        assertEquals(SECOND_BEST_BID_PRICE,
                     newBestBidPrice,
                     "Best Bid price should have changed to SECOND_BEST_BID_PRICE");
        assertEquals(0,
                     book.querySize(bestBidPrice),
                     "Orders at FIRST_BEST_BID_PRICE should have been removed completely");
    }

    @Test
    void sellPriceLevelSizeAmountPlusOne() {
        int bestBidPrice = book.queryBestBid();
        int bestBidSize = book.querySize(bestBidPrice);
        final int sellAmount = bestBidSize + 1;

        book.sell(sellAmount);
        int newBestBidPrice = book.queryBestBid();

        assertEquals(0,
                     book.querySize(bestBidPrice),
                     "Orders at FIRST_BEST_BID_PRICE should have been removed completely");
        assertEquals(SECOND_BEST_BID_PRICE, newBestBidPrice, "Best Bid should have changed to SECOND_BEST_BID_PRICE");
        assertEquals(THIRD_BID_SIZE - 1, book.querySize(newBestBidPrice), "Order#3 should have changed by 1");
    }
}