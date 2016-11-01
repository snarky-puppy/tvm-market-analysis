DROP DATABASE IF EXISTS slope;
CREATE DATABASE slope;
USE slope;

DROP USER IF EXISTS slope;
CREATE USER slope IDENTIFIED BY 'slope';
GRANT ALL PRIVILEGES ON slope.* TO slope@localhost IDENTIFIED BY 'slope';

CREATE TABLE yahoo_data (
	id INTEGER PRIMARY KEY AUTO_INCREMENT,

    symbol_id INTEGER NOT NULL,
    dt DATE NOT NULL,
	close DOUBLE(18,6) NOT NULL,
	open DOUBLE(18,6) NOT NULL,
    volume INTEGER NOT NULL,

	UNIQUE KEY yahoo_uniq (symbol_id, dt)
);
ALTER TABLE yahoo_data modify volume bigint;

CREATE TABLE active_symbols (
	id INTEGER PRIMARY KEY AUTO_INCREMENT,
	exchange CHAR(16) NOT NULL,
	symbol CHAR(32) NOT NULL,
	sector CHAR(32) NOT NULL,
    last_check DATE,
	UNIQUE KEY mapping_uniq (exchange, symbol)
);

ALTER TABLE active_symbols modify symbol char(64);

LOAD DATA LOCAL INFILE 'active_symbols.csv' INTO TABLE active_symbols
	FIELDS TERMINATED BY ','
	LINES TERMINATED BY '\n'
	IGNORE 1 LINES (Symbol,Exchange,Sector);

