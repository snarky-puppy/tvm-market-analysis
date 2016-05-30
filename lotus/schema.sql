DROP DATABASE IF EXISTS lotus2;
CREATE DATABASE lotus2;
USE lotus2;

DROP USER IF EXISTS lotus2;
CREATE USER lotus2 IDENTIFIED BY 'lotus2';
GRANT ALL PRIVILEGES ON lotus2.* TO lotus2@localhost IDENTIFIED BY 'lotus2';

CREATE TABLE triggers (
	id INTEGER PRIMARY KEY AUTO_INCREMENT,

	exchange CHAR(16) NOT NULL,
	symbol CHAR(16) NOT NULL,
	trigger_date DATE NOT NULL,

	price DOUBLE(18,6) NOT NULL,
	zscore DOUBLE(18,6) NOT NULL,

	avg_volume DOUBLE(18,6) NOT NULL,
	avg_price DOUBLE(18,6) NOT NULL,

	event BOOLEAN NOT NULL DEFAULT false,
	reject_reason ENUM('NOTEVENT', 'NOTPROCESSED', 'ZSCORE', 'CATEGORY', 'VOLUME', 'INVESTAMT', 'NOFUNDS', 'OK') NOT NULL DEFAULT 'NOTEVENT',
	reject_data DOUBLE(18,6),

	UNIQUE KEY trigger_uniq (exchange, symbol, trigger_date)
);

CREATE TABLE compounder_state (
	id INTEGER PRIMARY KEY AUTO_INCREMENT,

	dt DATE NOT NULL,

	start_bank DOUBLE(18,2) NOT NULL,
	min_invest DOUBLE(18,2) NOT NULL,
	cash DOUBLE(18,2) NOT NULL,
	compound_tally DOUBLE(18,2) NOT NULL,
	tally_slice DOUBLE(18,2) NOT NULL,
	tally_slice_cnt INTEGER NOT NULL,
	spread INTEGER NOT NULL,
	invest_pc INTEGER NOT NULL,

	UNIQUE KEY dt_uniq (dt)
);

CREATE TABLE active_symbols (
	id INTEGER PRIMARY KEY AUTO_INCREMENT,
	exchange CHAR(16) NOT NULL,
	symbol CHAR(16) NOT NULL,
	sector CHAR(32) NOT NULL,
	UNIQUE KEY mapping_uniq (exchange, symbol)
);

LOAD DATA LOCAL INFILE 'active_symbols.csv' INTO TABLE active_symbols
	FIELDS TERMINATED BY ','
	LINES TERMINATED BY '\n'
	IGNORE 1 LINES (Exchange,Symbol,Sector);

CREATE TABLE investments (
	id INTEGER PRIMARY KEY AUTO_INCREMENT,
	trigger_id INTEGER NOT NULL,

	-- compounder accounting; cmp_total = cmp_min + cmp_val
	cmp_min DOUBLE(12,2) NOT NULL,
	cmp_val DOUBLE(12,2) NOT NULL,
	cmp_total DOUBLE(12,2) NOT NULL,

	/* IB state */
	buy_order_id BIGINT NOT NULL,
	sell_order_id BIGINT NOT NULL,
	buy_perm_id BIGINT NOT NULL,
	sell_perm_id BIGINT,

	state ENUM('UNCONFIRMED', 'BUYPRESUBMITTED', 'BUY', 'BUYFILLED', 'SELLPRESUBMITTED', 'SELL', 'SELLFILLED', 'ERROR') NOT NULL DEFAULT 'UNCONFIRMED',

	/* buying */

	-- 0.1% higher than trigger close price 
	buy_limit DOUBLE(12,2) NOT NULL,

	buy_dt DATE NOT NULL,

	-- Quantity of stocks needed to fill cmp_total
	qty INTEGER NOT NULL,

	-- Total price of qty stocks (will be less than cmp_total)
	qty_val DOUBLE(12,2) NOT NULL,

	-- number of stocks actually filled
	qty_filled INTEGER,

	-- Price of stocks actually filled
	qty_filled_val DOUBLE(12,2),

	/* selling */
	-- sell when price reaches this limit (+10%)
	sell_limit DOUBLE(12,2) NOT NULL,

	-- sell when date reaches this limit (84 days)
	sell_dt_limit DATE NOT NULL,

	-- actual sell price
	sell_price DOUBLE(12,2),

	-- date of selling
	sell_dt_start DATE,
	sell_dt_end DATE,

	-- realised profit / loss
	real_pnl DOUBLE(12,2),

	-- something went wrong...
	error_code INTEGER,
	error_msg VARCHAR(265),

	UNIQUE KEY order_idx (con_id, perm_id),

	FOREIGN KEY (trigger_id)
		REFERENCES triggers(id)
);

CREATE TABLE investment_history (
	id INTEGER PRIMARY KEY AUTO_INCREMENT,
	investment_id INTEGER NOT NULL,

	dt DATE NOT NULL,
	close DOUBLE(12,2) NOT NULL,

	CONSTRAINT historically_correct UNIQUE(investment_id, dt),

	FOREIGN KEY (investment_id)
		REFERENCES investments(id)
);

CREATE TABLE daily_log (
	id INTEGER PRIMARY KEY AUTO_INCREMENT,
	
	dt DATE NOT NULL,
	liquidity DOUBLE(12,2) NOT NULL
);
