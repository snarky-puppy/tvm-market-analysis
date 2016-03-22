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

	seen BOOLEAN NOT NULL DEFAULT false,
	actioned BOOLEAN NOT NULL DEFAULT false,
	ignored BOOLEAN NOT NULL DEFAULT false,

	UNIQUE KEY trigger_uniq (exchange, symbol, trigger_date)
);

