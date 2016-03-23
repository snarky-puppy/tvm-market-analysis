DROP DATABASE IF EXISTS lotus;
CREATE DATABASE lotus;
USE lotus;

DROP USER IF EXISTS lotus;
CREATE USER lotus IDENTIFIED BY 'lotus';
GRANT ALL PRIVILEGES ON lotus.* TO lotus@localhost IDENTIFIED BY 'lotus';

CREATE TABLE triggers (
	id INTEGER PRIMARY KEY AUTO_INCREMENT,

	exchange CHAR(8) NOT NULL,
	symbol CHAR(8) NOT NULL,
	trigger_date DATE NOT NULL,

	price DOUBLE(18,6) NOT NULL,
	zscore DOUBLE(18,6) NOT NULL,

	avg_volume DOUBLE(18,6) NOT NULL,
	avg_price DOUBLE(18,6) NOT NULL,

	event BOOLEAN NOT NULL DEFAULT false,
	expired BOOLEAN NOT NULL DEFAULT false,

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

/*
CREATE TABLE positions (
	id INTEGER PRIMARY KEY AUTO_INCREMENT,
	trigger_id INTEGER NOT NULL,


	FOREIGN KEY(trigger_id)
		REFERENCES(triggers.id);

);
*/
