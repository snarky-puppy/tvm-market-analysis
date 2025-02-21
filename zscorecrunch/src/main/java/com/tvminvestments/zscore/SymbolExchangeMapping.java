package com.tvminvestments.zscore;

import com.tvminvestments.zscore.app.Conf;
import com.tvminvestments.zscore.db.Database;
import com.tvminvestments.zscore.db.DatabaseFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by horse on 2/07/2016.
 */
public class SymbolExchangeMapping {

    private static final Logger logger = LogManager.getLogger(SymbolExchangeMapping.class);

    private final Map<String, Database> exchangeDB = new HashMap<>();
    private final Map<String, String> symbolToExchange = new HashMap<>();

    public SymbolExchangeMapping() {
        // populate symbol->exchange map
        try {
            for (String market : Conf.listAllMarkets()) {
                Database db = DatabaseFactory.createDatabase(market);
                exchangeDB.put(market, db);
                for (String symbol : db.listSymbols()) {
                    if (symbolToExchange.containsKey(symbol)) {
                        logger.error(String.format("Duplicate symbol: symbol=%s exchanges=%s,%s",
                                symbol, symbolToExchange.get(symbol), market));
                        //System.exit(1);
                    }
                    symbolToExchange.put(symbol, market);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public String symbolToMarketName(String symbol) {
        return symbolToExchange.get(symbol);
    }

    public Database symbolToDatabase(String symbol) {
        return exchangeDB.get(symbolToExchange.get(symbol));
    }

    public boolean hasSymbol(String symbol) {
        return symbolToExchange.containsKey(symbol);
    }
}
