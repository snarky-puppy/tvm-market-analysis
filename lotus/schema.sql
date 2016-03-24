DROP DATABASE IF EXISTS lotus;
CREATE DATABASE lotus;
USE lotus;

DROP USER IF EXISTS lotus;
CREATE USER lotus IDENTIFIED BY 'lotus';
GRANT ALL PRIVILEGES ON lotus.* TO lotus@localhost IDENTIFIED BY 'lotus';

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
	compound_tally DOUBLE(18,2) NOT NULL,
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
	IGNORE 1 LINES;

CREATE TABLE positions (
	id INTEGER PRIMARY KEY AUTO_INCREMENT,
	trigger_id INTEGER NOT NULL,

	/* 0.1% higher than trigger close price */
	buy_limit DOUBLE(6,2) NOT NULL,

	qty INTEGER NOT NULL,

	min_invest DOUBLE(7,2) NOT NULL,
	compound_amount DOUBLE(7,2) NOT NULL,
	total_invest DOUBLE(7,2) NOT NULL,

	qty_filled INTEGER NOT NULL,
	submitted BOOLEAN NOT NULL DEFAULT FALSE,
	eod BOOLEAN NOT NULL DEFAULT FALSE,
	closed BOOLEAN NOT NULL DEFAULT FALSE,

	FOREIGN KEY (trigger_id)
		REFERENCES triggers(id)

);

