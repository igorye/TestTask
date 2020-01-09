package com.bookmap.ordermanagement;

import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * {@link OrdersBook} holds collection of {@link BasicOrder}s and provides
 * simple interface for basic operations with them.
 */
public class OrdersBook {

    private static final Logger LOGGER = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());

    /**
     * Price Level contains all orders of the same price.
     */
    private static class PriceLevel implements Comparable<PriceLevel> {
        /**
         * actual Size of the price level(sum of sizes of all orders at current price level)
         */
        int size;

        final int price;

        /**
         * Orders contained at particular price level
         */
        final Queue<BasicOrder> orders = new LinkedList<>();

        private PriceLevel( int price ) {
            if (price == 0)
                throw new IllegalArgumentException("Price should be positive.");
            this.price = price;
        }

        private PriceLevel() {
            price = 0;
            size = 0;
        }

        int getSize() {
            return size;
        }

        boolean isEmpty() {
            return size == 0;
        }

        void addOrder( BasicOrder order ) {
            orders.offer(order);
            size += order.getSize();
        }

        void removeOrder( BasicOrder order ) {
            orders.remove(order);
            size -= order.getSize();
        }

        @Override
        public int compareTo( PriceLevel other ) {
            return price - other.price;
        }

        static Comparator<PriceLevel> reverseComparator() {
            return ( pl1, pl2 ) -> -pl1.compareTo(pl2);
        }

        public int getPrice() {
            return price;
        }

        BasicOrder getFirstOrder() {
            return orders.peek();
        }

        private void setSize( int size ) {
            this.size = size;
        }

        static private final PriceLevel EMPTY_PRICE_LEVEL = new PriceLevel() {
            @Override
            public int getSize() {
                return 0;
            }

            @Override
            void addOrder( BasicOrder order ) {
                throw new UnsupportedOperationException();
            }

            @Override
            void removeOrder( BasicOrder order ) {
                throw new UnsupportedOperationException();
            }

            @Override
            public int compareTo( PriceLevel other ) {
                return super.compareTo(other);
            }

            @Override
            public int getPrice() {
                return 0;
            }
        };

        int getOrdersCount() {
            return orders.size();
        }

        public String getOrdersStat() {
            return orders.stream()
                         .map(BasicOrder::getSize)
                         .map(String::valueOf)
                         .collect(Collectors.joining(", ", "[", "]"));
        }
    }

    /**
     * Tracks actual best Bid
     */
    private final PriorityQueue<PriceLevel> bids;

    /**
     * Tracks actual best Ask
     */
    private final PriorityQueue<PriceLevel> asks;

    /**
     * Holds all orders divided into price levels
     */
    private final HashMap<Integer, PriceLevel> orders;

    public OrdersBook( boolean skipInvalidOperations ) {
        this(1, skipInvalidOperations);
    }

    public OrdersBook() {
        this(1, true);
    }

    /**
     * Constructs Order Book instance and preallocate memory for @size price levels
     *
     * @param size                  estimated size of orders to store in the Order Book
     * @param skipInvalidOperations setting this flag to false will lead to exit program
     *                              if invalid command line occur
     */
    public OrdersBook( int size, boolean skipInvalidOperations ) {
        int halfSize = size / 2;
        if (halfSize == 0) halfSize = 1;
        bids = new PriorityQueue<>(halfSize, PriceLevel.reverseComparator());
        asks = new PriorityQueue<>(halfSize);
        orders = new HashMap<>(size);
    }

    /**
     * Append a new Bid or Ask into Order Book
     *
     * @param newOrder Bid or Ask order to be added into Order Book
     */
    public void addOrder( BasicOrder newOrder ) {
        if (newOrder.getSide() == BasicOrder.OrderSide.ASK) {
            setAsk(newOrder);
        } else {
            setBid(newOrder);
        }
    }

    private void setBid( BasicOrder order ) {
        PriceLevel bestAsk = asks.peek();
        if (bestAsk != null && order.getPrice() >= bestAsk.getPrice()) {
            throw new IllegalArgumentException(String.format(
                    "Spread should remain positive! Bid = (%d, %d), BestAsk = %d",
                    order.getPrice(),
                    order.getSize(),
                    bestAsk.getPrice())
            );
        }
        PriceLevel priceLevel = arrangeToPriceLevel(order, bids);
        LOGGER.log(Level.FINE,
                   "price level {0,number}: bids {1}",
                   new Object[] { priceLevel.getPrice(), priceLevel.getOrdersStat() });
    }

    private void setAsk( BasicOrder order ) {
        PriceLevel bestBid = bids.peek();
        if (bestBid != null && order.getPrice() <= bestBid.getPrice()) {
            throw new IllegalArgumentException(String.format(
                    "Spread should remain positive! Ask = (%d, %d), BestBid = %d",
                    order.getPrice(),
                    order.getSize(),
                    bestBid.getPrice())
            );
        }
        PriceLevel priceLevel = arrangeToPriceLevel(order, asks);
        LOGGER.log(Level.FINE,
                   "price level {0,number}: asks {1}",
                   new Object[] { priceLevel.getPrice(), priceLevel.getOrdersStat() });
    }

    /**
     * Arrange new order at a proper PriceLevel and save it to the orders
     *
     * @param order to be saved
     * @return a price level where new order has been arranged
     */
    private PriceLevel arrangeToPriceLevel(
            BasicOrder order,
            PriorityQueue<PriceLevel> orders )
    {
        int price = order.getPrice();
        boolean levelIsPresent = this.orders.containsKey(price);
        PriceLevel priceLevel = this.orders.computeIfAbsent(price, i -> new PriceLevel(price));
        priceLevel.addOrder(order);
        if (!levelIsPresent)
            orders.add(priceLevel);
        return priceLevel;
    }

    /**
     * Execute sell deal of @size units of most expensive bids
     *
     * @param size quantity of units to sell
     */
    public void sell( int size ) {
        if (size < 0)
            throw new IllegalArgumentException("\"Size\" should be positive");
        PriceLevel priceLevel = bids.peek();
        int bestBid = priceLevel != null ? priceLevel.getPrice() : 0;
        final int availableAtBestBid = orders.getOrDefault(bestBid, PriceLevel.EMPTY_PRICE_LEVEL).getSize();
        if (size <= availableAtBestBid) {
            deal(size, bids);
        } else {
            deal(availableAtBestBid, bids);
            sell(size - availableAtBestBid);
        }
    }

    /**
     * Execute buy deal of @size units of cheapest asks
     *
     * @param size quantity of units to buy
     */
    public void buy( int size ) {
        if (size < 0) {
            throw new IllegalArgumentException("\"Size\" should be positive");
        }
        PriceLevel priceLevel = asks.peek();
        int bestAsk = priceLevel != null ? priceLevel.getPrice() : 0;
        final int availableAtBestAsk = orders.getOrDefault(bestAsk, PriceLevel.EMPTY_PRICE_LEVEL).getSize();
        if (size <= availableAtBestAsk) {
            deal(size, asks);
        } else {
            deal(availableAtBestAsk, asks);
            buy(size - availableAtBestAsk);
        }
    }

    /**
     * Provides facilities for buy/sell operations
     *
     * @param size            quantity of units to be dealt
     * @param bestPriceLevels orders collection involved in deal
     */
    private void deal( int size, PriorityQueue<PriceLevel> bestPriceLevels ) {
        if (size == 0) return;
        PriceLevel bestPriceLevel = bestPriceLevels.peek();
        int bestPrice = (bestPriceLevel == null) ? 0 : bestPriceLevel.getPrice();
        // is there a best price level for the deal
        if (bestPrice == 0) return;
        PriceLevel dealPriceLevel = orders.get(bestPrice);
        if (bestPriceLevel != dealPriceLevel)
            LOGGER.severe("best price level != deal level");
        if (dealPriceLevel == null) return;
        BasicOrder firstOrder = dealPriceLevel.getFirstOrder();
        int available = firstOrder.getSize();
        int reminder = available - size;
        if (reminder > 0) {
            firstOrder.setSize(reminder);
            dealPriceLevel.setSize(dealPriceLevel.getSize() - size);
        } else {
            dealPriceLevel.removeOrder(firstOrder);
            if (reminder == 0) {
                if (dealPriceLevel.isEmpty()) {
                    orders.remove(dealPriceLevel.getPrice());
                    bestPriceLevels.remove();
                }
            } else {
                deal(-reminder, bestPriceLevels);
            }
        }
    }

    /**
     * Returns size of all orders at specified price level
     *
     * @param price - value of price level
     * @return sum of the sizes at specified price level
     */
    public int querySize( int price ) {
        printStat(Level.INFO);
        if (price < 0) {
            throw new IllegalArgumentException("\"Price\" should be positive");
        }
        PriceLevel priceLevel = orders.getOrDefault(price, PriceLevel.EMPTY_PRICE_LEVEL);
        return priceLevel.getSize();
    }

    /**
     * Returns highest bid price among all bids
     *
     * @return highest bid price among all bids
     */
    public int queryBestBid() {
        printStat(Level.INFO);
        return bids.isEmpty() ? 0 : bids.peek().getPrice();
    }

    /**
     * Returns highest bid price among all asks
     *
     * @return highest bid price among all asks
     */
    public int queryBestAsk() {
        printStat(Level.INFO);
        return asks.isEmpty() ? 0 : asks.peek().getPrice();
    }

    private void printStat( Level logLevel ) {
        if (!LOGGER.isLoggable(logLevel)) return;
        LOGGER.log(logLevel, "Asks: {0}",
                   orders.values().stream()
                         .filter(pl -> pl.getFirstOrder().getSide() == BasicOrder.OrderSide.ASK)
                         .map(PriceLevel::getPrice)
                         .sorted(Comparator.reverseOrder())
                         .map(String::valueOf)
                         .collect(Collectors.joining(", ", "[", "]")));
        LOGGER.log(logLevel, "Total asks size: {0}",
                   orders.values().stream()
                         .filter(pl -> pl.getFirstOrder().getSide() == BasicOrder.OrderSide.ASK)
                         .mapToInt(PriceLevel::getSize)
                         .sum());
        LOGGER.log(logLevel, "Bids: {0}",
                   orders.values().stream()
                         .filter(pl -> pl.getFirstOrder().getSide() == BasicOrder.OrderSide.BID)
                         .map(PriceLevel::getPrice)
                         .sorted(Comparator.reverseOrder())
                         .map(String::valueOf)
                         .collect(Collectors.joining(", ", "[", "]")));
        LOGGER.log(logLevel, "Total bids size: {0}",
                   orders.values().stream()
                         .filter(pl -> pl.getFirstOrder().getSide() == BasicOrder.OrderSide.BID)
                         .mapToInt(PriceLevel::getSize)
                         .sum());
        LOGGER.log(logLevel, "Total orders: {0}",
                   orders.values().stream()
                         .filter(pl -> pl.getSize() != 0)
                         .mapToInt(PriceLevel::getOrdersCount)
                         .sum());
    }
}
