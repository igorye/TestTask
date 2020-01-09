package com.bookmap.ordermanagement;

import com.bookmap.util.StopWatch;

import java.io.BufferedReader;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.Boolean.parseBoolean;
import static java.lang.System.getProperty;

/**
 * Created by Igor Yevstropov on 21.03.2018.
 */
public class BasicOrderManagement {

    public static final Logger LOGGER = Logger.getLogger(MethodHandles.lookup().lookupClass().getName());

    private static final String  PRICE_FORMAT_IS_INVALID_FMT      = "Price format is invalid - \"%s\"";
    private static final String  SIZE_FORMAT_IS_INVALID_FMT       = "Size format is invalid - \"%s\"";
    private static final boolean SKIP_INVALID_OPERATIONS          = parseBoolean(getProperty("skipInvalidOperations",
                                                                                             "true"));
    private static final Level   INVALID_OPERATIONS_LOGGING_LEVEL = SKIP_INVALID_OPERATIONS
                                                                            ? Level.WARNING
                                                                            : Level.SEVERE;

    private static OrdersBook ordersBook;

    public static void main( String[] args ) {
        if (args.length == 0) {
            LOGGER.severe("Specify an input filename");
            System.exit(1);
        }
        int runs = 1;
        double[] durations = new double[runs];
        final StopWatch timer = new StopWatch();
        for (int i = 0; i < runs; i++) {
            try (BufferedReader br = Files.newBufferedReader(Paths.get(args[0]))) {
                ordersBook = new OrdersBook(SKIP_INVALID_OPERATIONS);
                br.lines().forEach(BasicOrderManagement::dispatchCommand);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "File processing terminated due to invalid command - {0}", e.getMessage());
            }
            durations[i] = timer.elapsed();
            timer.reset();
        }
        printStat(durations);
    }

    /**
     * Prints simple statistics about execution time
     *
     * @param durations
     */
    private static void printStat( double[] durations ) {
        Arrays.sort(durations);
        LOGGER.log(Level.INFO, "min - {0,number,#.###}", durations[0]);
        LOGGER.log(Level.INFO, "max - {0,number,#.###}", durations[durations.length - 1]);
        LOGGER.log(Level.INFO, "avg - {0,number,#.###}", Arrays.stream(durations).average().orElse(0d));
    }

    /**
     * Commands dispatcher method
     *
     * @param line command line to process
     */
    private static void dispatchCommand( String line ) {
        if (line.isEmpty()) return;
        LOGGER.log(Level.INFO, "dispatching {0}", line);
        String[] args = line.split(",");
        if (!validCmd(args[0])) return;
        char cmd = args[0].charAt(0);
        try {
            if (cmd == 'u' && validSideArg(args[3])) {
                updateOrderBook(args);
            } else if (cmd == 'o' && isValidOperationArg(args[1])) {
                executeOperation(args);
            } else if (cmd == 'q' && validQueryCmdArg(args[1])) {
                executeQuery(args);
            }
        } catch (Exception e) {
            LOGGER.log(INVALID_OPERATIONS_LOGGING_LEVEL,
                       "Failed to perform '{0}' - {1}",
                       new Object[] { line, e.getMessage() });
            if (SKIP_INVALID_OPERATIONS) {
                LOGGER.log(Level.WARNING, "Skipping command {0, string}", line);
            } else {
                throw e;
            }
        }
    }

    /**
     * Checks correctness of the query command arg
     *
     * @param arg command line argument to validate
     */
    private static boolean validQueryCmdArg( String arg ) {
        boolean valid = arg.matches("(best_((ask)|(bid))|(size))");
        if (!valid) {
            LOGGER.log(Level.WARNING,
                       "Invalid query command. " +
                               "Looking for \"best_ask\", \"best_bid\" or \"size\", but \"{0}\" found",
                       arg);
        }
        return valid;
    }

    /**
     * Executes an update operation on the order book. In particular, adds a new order
     *
     * @param args arguments of executed operation
     */
    private static void updateOrderBook( String[] args ) {
        String sideArg = args[3];
        int price;
        int size;
        try {
            price = Integer.parseInt(args[1]);
            if (price == 0) return;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(String.format(PRICE_FORMAT_IS_INVALID_FMT, args[1]));
        }
        try {
            size = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(String.format(SIZE_FORMAT_IS_INVALID_FMT, args[2]));
        }
        BasicOrder.OrderSide side = sideArg.equals("bid") ? BasicOrder.OrderSide.BID : BasicOrder.OrderSide.ASK;
        BasicOrder newOrder = new BasicOrder(price, size, side);
        ordersBook.addOrder(newOrder);
    }

    /**
     * Implements order processing - buying and selling
     *
     * @param args
     */
    private static void executeOperation( String[] args ) {
        final String operationArg = args[1];
        int size;
        try {
            size = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(String.format(SIZE_FORMAT_IS_INVALID_FMT, args[2]));
        }
        if (operationArg.equals("buy")) {
            ordersBook.buy(size);
        } else {
            ordersBook.sell(size);
        }
    }

    private static void executeQuery( String[] args ) {
        if (args.length > 2) {
            int priceLevel;
            try {
                priceLevel = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(String.format(PRICE_FORMAT_IS_INVALID_FMT, args[2]));
            }
            System.out.println(ordersBook.querySize(priceLevel));
        } else {
            int price;
            price = args[1].equals("best_bid") ? ordersBook.queryBestBid() : ordersBook.queryBestAsk();
            final int size = ordersBook.querySize(price);
            System.out.printf("%d,%d%n", price, size);
            LOGGER.log(Level.INFO, "{0,number},{1,number}", new Object[] { price, size });
        }
    }

    private static boolean isValidOperationArg( String arg ) {
        boolean valid = arg.matches("(buy)|(sell)");
        if (!valid) {
            LOGGER.log(Level.WARNING, "Invalid order operation. " +
                                              "Looking for \"buy\" or \"sell\", but \"{0}\" found", arg);
        }
        return valid;
    }

    private static boolean validSideArg( String arg ) {
        boolean valid = arg.matches("(bid)|(ask)");
        if (!valid) {
            LOGGER.log(Level.WARNING, "Invalid side argument. " +
                                              "Looking for \"bid\" or \"ask\", but \"{0}\" found", arg);
        }
        return valid;
    }

    private static boolean validCmd( String arg ) {
        boolean valid = arg.matches("[uoq]");
        if (!valid) {
//      String errMsg = String.format("Invalid command symbol found. " +
//                                        "Looking for \"u\", \"o\", \"q\" but \"%s\" found", arg);
            LOGGER.log(Level.WARNING, "Invalid command symbol found. " +
                                              "Looking for \"u\", \"o\", \"q\" but \"{0}\" found", arg);
        }
        return valid;
    }

}
