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
	reject_reason ENUM('NOTEVENT', 'NOTPROCESSED', 'ZSCORE', 'CATEGORY', 'VOLUME', 'INVESTAMT', 'MININVEST', 'OK') NOT NULL DEFAULT 'NOTEVENT',
	reject_data DOUBLE(18,6),

	UNIQUE KEY trigger_uniq (exchange, symbol, trigger_date)
);

CREATE TABLE compounder_state (
	id INTEGER PRIMARY KEY AUTO_INCREMENT,

	dt DATE NOT NULL,

	start_bank DOUBLE(18,6) NOT NULL,
	min_invest DOUBLE(18,6) NOT NULL,
	compound_tally DOUBLE(18,6) NOT NULL,
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

LOAD DATA INFILE 'active_symbols.csv' INTO TABLE active_symbols
	IGNORE 1 LINES
	FIELDS TERMINATED BY ','
	LINES TERMINATED BY '\n';

CREATE TABLE positions (
	id INTEGER PRIMARY KEY AUTO_INCREMENT,
	trigger_id INTEGER NOT NULL,

	unit_price DOUBLE(6,2) NOT NULL
	filled BOOLEAN NOT NULL DEFAULT FALSE,


	FOREIGN KEY(trigger_id)
		REFERENCES(triggers.id);

);

