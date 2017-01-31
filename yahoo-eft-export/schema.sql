-- date, open, high, low, close, volume, openInterest, symbol

CREATE TABLE IF NOT EXISTS yahoo_data (
    symbol_id INTEGER NOT NULL,
    dt DATE NOT NULL,
    open DOUBLE(18,6) NOT NULL,
    high DOUBLE(18,6) NOT NULL,
    low DOUBLE(18,6) NOT NULL,
	close DOUBLE(18,6) NOT NULL,
    volume bigint NOT NULL,
    openInterest DOUBLE(18,6) NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS yahoo_uniq ON yahoo_data(symbol_id, dt);

CREATE TABLE IF NOT EXISTS active_symbols (
	id INTEGER PRIMARY KEY,
	exchange CHAR(16) NOT NULL,
	symbol CHAR(64) NOT NULL,
	sector CHAR(32) NOT NULL,
    last_check DATE
);
CREATE UNIQUE INDEX IF NOT EXISTS mapping_uniq ON active_symbols(exchange, symbol);
