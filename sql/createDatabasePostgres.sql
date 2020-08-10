-- NOTE: THIS SCRIPT IS OUTDATED.
-- Script for creating a fresh database.
-- To run from a command prompt: sqlplus kooutofstock/kooutofstock @createDatabase.sql
-- SET sqlblanklines ON

-- Drop existing functions
DROP FUNCTION bitxor(integer, integer);
DROP FUNCTION bitor(integer, integer);
DROP FUNCTION bitand(integer, integer);

-- Drop existing tables & sequences
DROP TABLE useractions CASCADE;
DROP TABLE outofstockevents CASCADE;
DROP TABLE prdctstoprdctpkgs CASCADE;
DROP TABLE prdctctgrstoprdcts CASCADE;
DROP TABLE prdctgrpstoprdctctgrs CASCADE;
DROP TABLE prdctpkgs CASCADE;
DROP TABLE prdcts CASCADE;
DROP TABLE prdctctgrs CASCADE;
DROP TABLE prdctgrps CASCADE;
DROP TABLE dstbdstrctstostores CASCADE;
DROP TABLE dstbdvsnstodstbdstrcts CASCADE;
DROP TABLE bttlrslsrtstostores CASCADE;
DROP TABLE bttlrbrchstostores CASCADE;
DROP TABLE bttlrmktuntstobttlrbrchs CASCADE;
DROP TABLE bttlrbsnsuntstobttlrmktunts CASCADE;
DROP TABLE bttlrstobttlrbsnsunts CASCADE;
DROP TABLE stores CASCADE;
DROP TABLE dstbdstrcts CASCADE;
DROP TABLE dstbdvsns CASCADE;
DROP TABLE bttlrslsrts CASCADE;
DROP TABLE bttlrbrchs CASCADE;
DROP TABLE bttlrmktunts CASCADE;
DROP TABLE bttlrbsnsunts CASCADE;
DROP TABLE bttlrs CASCADE;
DROP TABLE settings CASCADE;

DROP SEQUENCE pkgenerator CASCADE;

BEGIN TRANSACTION;
SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;

-- create primary key generator (sequence)
CREATE SEQUENCE pkgenerator START 1000 INCREMENT 1000;

-- create tables & indexes
CREATE TABLE settings
(
	-- Instrumented Columns
	primarykey INTEGER PRIMARY KEY,
	metaflags INTEGER DEFAULT 0 NOT NULL,
	timecreated TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

	-- Specific Columns
	category VARCHAR(100),
	name VARCHAR(100),
	value VARCHAR(200)
);

-- create tables & indexes
CREATE TABLE bttlrs
(
	-- Instrumented Columns
	primarykey INTEGER PRIMARY KEY,
	metaflags INTEGER DEFAULT 0 NOT NULL,
	timecreated TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

	-- Specific Columns
	name VARCHAR(100) NOT NULL UNIQUE,
	timelastuploaded TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE bttlrbsnsunts
(
	-- Instrumented Columns
	primarykey INTEGER PRIMARY KEY,
	metaflags INTEGER DEFAULT 0 NOT NULL,
	timecreated TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

	-- Specific Columns
	name VARCHAR(100) NOT NULL UNIQUE,
	timelastuploaded TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE bttlrmktunts
(
	-- Instrumented Columns
	primarykey INTEGER PRIMARY KEY,
	metaflags INTEGER DEFAULT 0 NOT NULL,
	timecreated TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

	-- Specific Columns
	name VARCHAR(100) NOT NULL UNIQUE,
	timelastuploaded TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE bttlrbrchs
(
	-- Instrumented Columns
	primarykey INTEGER PRIMARY KEY,
	metaflags INTEGER DEFAULT 0 NOT NULL,
	timecreated TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

	-- Specific Columns
	name VARCHAR(100) NOT NULL UNIQUE,
	timelastuploaded TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE bttlrslsrts
(
	-- Instrumented Columns
	primarykey INTEGER PRIMARY KEY,
	metaflags INTEGER DEFAULT 0 NOT NULL,
	timecreated TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

	-- Specific Columns
	name VARCHAR(100) NOT NULL UNIQUE,
	timelastuploaded TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE dstbdvsns
(
	-- Instrumented Columns
	primarykey INTEGER PRIMARY KEY,
	metaflags INTEGER DEFAULT 0 NOT NULL,
	timecreated TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

	-- Specific Columns
	id INTEGER NOT NULL UNIQUE,
	name VARCHAR(100) NOT NULL UNIQUE,
	timelastuploaded TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE dstbdstrcts
(
	-- Instrumented Columns
	primarykey INTEGER PRIMARY KEY,
	metaflags INTEGER DEFAULT 0 NOT NULL,
	timecreated TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

	-- Specific Columns
	id INTEGER NOT NULL UNIQUE,
	name VARCHAR(100) NOT NULL UNIQUE,
	cd VARCHAR(10) NOT NULL UNIQUE,
	timelastuploaded TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE stores
(
	-- Instrumented Columns
	primarykey INTEGER PRIMARY KEY,
	metaflags INTEGER DEFAULT 0 NOT NULL,
	timecreated TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

	-- Specific Columns
	id INTEGER NOT NULL UNIQUE,
	name VARCHAR(100) NOT NULL,
	address VARCHAR(100),
	city VARCHAR(50),
	zip VARCHAR(10),
	state VARCHAR(2),
	phonenumber VARCHAR(20),
	dateopened DATE,
	datelastremodeled DATE,
	dateclosed DATE,
	lifestyle VARCHAR(10),
	distributorcom VARCHAR(10),
	totalsellingarea INTEGER,
	totalflmcount INTEGER,
	timelastuploaded TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE bttlrstobttlrbsnsunts
(
	-- Instrumented Columns
	primarykey INTEGER PRIMARY KEY,
	metaflags INTEGER DEFAULT 0 NOT NULL,
	timecreated TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

	-- Specific Columns
	bottler INTEGER NOT NULL,
	bottlerbusinessunit INTEGER NOT NULL,
	timelastuploaded TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

	-- Constraints
	FOREIGN KEY (bottler) REFERENCES bttlrs(primarykey),
	FOREIGN KEY (bottlerbusinessunit) REFERENCES bttlrbsnsunts(primarykey),
	UNIQUE (bottler, bottlerbusinessunit)
);

CREATE INDEX idx1000 ON bttlrstobttlrbsnsunts(bottler);
CREATE UNIQUE INDEX idx1010 ON bttlrstobttlrbsnsunts(bottlerbusinessunit);

CREATE TABLE bttlrbsnsuntstobttlrmktunts
(
	-- Instrumented Columns
	primarykey INTEGER PRIMARY KEY,
	metaflags INTEGER DEFAULT 0 NOT NULL,
	timecreated TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

	-- Specific Columns
	bottlerbusinessunit INTEGER NOT NULL,
	bottlermarketunit INTEGER NOT NULL,
	timelastuploaded TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

	-- Constraints
	FOREIGN KEY (bottlerbusinessunit) REFERENCES bttlrbsnsunts(primarykey),
	FOREIGN KEY (bottlermarketunit) REFERENCES bttlrmktunts(primarykey),
	UNIQUE (bottlerbusinessunit, bottlermarketunit)
);

CREATE INDEX idx1100 ON bttlrbsnsuntstobttlrmktunts(bottlerbusinessunit);
CREATE UNIQUE INDEX idx1110 ON bttlrbsnsuntstobttlrmktunts(bottlermarketunit);

CREATE TABLE bttlrmktuntstobttlrbrchs
(
	-- Instrumented Columns
	primarykey INTEGER PRIMARY KEY,
	metaflags INTEGER DEFAULT 0 NOT NULL,
	timecreated TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

	-- Specific Columns
	bottlermarketunit INTEGER NOT NULL,
	bottlerbranch INTEGER NOT NULL,
	timelastuploaded TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

	-- Constraints
	FOREIGN KEY (bottlermarketunit) REFERENCES bttlrmktunts(primarykey),
	FOREIGN KEY (bottlerbranch) REFERENCES bttlrbrchs(primarykey),
	UNIQUE (bottlermarketunit, bottlerbranch)
);

CREATE INDEX idx1200 ON bttlrmktuntstobttlrbrchs(bottlermarketunit);
CREATE UNIQUE INDEX idx1210 ON bttlrmktuntstobttlrbrchs(bottlerbranch);

CREATE TABLE bttlrbrchstostores
(
	-- Instrumented Columns
	primarykey INTEGER PRIMARY KEY,
	metaflags INTEGER DEFAULT 0 NOT NULL,
	timecreated TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

	-- Specific Columns
	bottlerbranch INTEGER NOT NULL,
	store INTEGER NOT NULL,
	timelastuploaded TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

	-- Constraints
	FOREIGN KEY (bottlerbranch) REFERENCES bttlrbrchs(primarykey),
	FOREIGN KEY (store) REFERENCES stores(primarykey),
	UNIQUE (bottlerbranch, store)
);

CREATE INDEX idx1300 ON bttlrbrchstostores(bottlerbranch);
CREATE UNIQUE INDEX idx1310 ON bttlrbrchstostores(store);

CREATE TABLE bttlrslsrtstostores
(
	-- Instrumented Columns
	primarykey INTEGER PRIMARY KEY,
	metaflags INTEGER DEFAULT 0 NOT NULL,
	timecreated TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

	-- Specific Columns
	bottlersalesroute INTEGER NOT NULL,
	store INTEGER NOT NULL,
	timelastuploaded TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

	-- Constraints
	FOREIGN KEY (bottlersalesroute) REFERENCES bttlrslsrts(primarykey),
	FOREIGN KEY (store) REFERENCES stores(primarykey),
	UNIQUE (bottlersalesroute, store)
);

CREATE INDEX idx1400 ON bttlrslsrtstostores(bottlersalesroute);
CREATE UNIQUE INDEX idx1410 ON bttlrslsrtstostores(store);

CREATE TABLE dstbdvsnstodstbdstrcts
(
	-- Instrumented Columns
	primarykey INTEGER PRIMARY KEY,
	metaflags INTEGER DEFAULT 0 NOT NULL,
	timecreated TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

	-- Specific Columns
	distributordivision INTEGER NOT NULL,
	distributordistrict INTEGER NOT NULL,
	timelastuploaded TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

	-- Constraints
	FOREIGN KEY (distributordivision) REFERENCES dstbdvsns(primarykey),
	FOREIGN KEY (distributordistrict) REFERENCES dstbdstrcts(primarykey),
	UNIQUE (distributordivision, distributordistrict)
);

CREATE INDEX idx1500 ON dstbdvsnstodstbdstrcts(distributordivision);
CREATE UNIQUE INDEX idx1510 ON dstbdvsnstodstbdstrcts(distributordistrict);

CREATE TABLE dstbdstrctstostores
(
	-- Instrumented Columns
	primarykey INTEGER PRIMARY KEY,
	metaflags INTEGER DEFAULT 0 NOT NULL,
	timecreated TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

	-- Specific Columns
	distributordistrict INTEGER NOT NULL,
	store INTEGER NOT NULL,
	timelastuploaded TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

	-- Constraints
	FOREIGN KEY (distributordistrict) REFERENCES dstbdstrcts(primarykey),
	FOREIGN KEY (store) REFERENCES stores(primarykey),
	UNIQUE (distributordistrict, store)
);

CREATE INDEX idx1600 ON dstbdstrctstostores(distributordistrict);
CREATE UNIQUE INDEX idx1610 ON dstbdstrctstostores(store);

CREATE TABLE prdctgrps
(
	-- Instrumented Columns
	primarykey INTEGER PRIMARY KEY,
	metaflags INTEGER DEFAULT 0 NOT NULL,
	timecreated TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

	-- Specific Columns
	id INTEGER NOT NULL UNIQUE,
	name VARCHAR(100) NOT NULL UNIQUE,
	timelastuploaded TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE prdctctgrs
(
	-- Instrumented Columns
	primarykey INTEGER PRIMARY KEY,
	metaflags INTEGER DEFAULT 0 NOT NULL,
	timecreated TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

	-- Specific Columns
	id INTEGER NOT NULL UNIQUE,
	name VARCHAR(100) NOT NULL UNIQUE,
	timelastuploaded TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE prdcts
(
	-- Instrumented Columns
	primarykey INTEGER PRIMARY KEY,
	metaflags INTEGER DEFAULT 0 NOT NULL,
	timecreated TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

	-- Specific Columns
	upcid BIGINT NOT NULL UNIQUE,
	description VARCHAR(100) NOT NULL,
	timelastuploaded TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE prdctpkgs
(
	-- Instrumented Columns
	primarykey INTEGER PRIMARY KEY,
	metaflags INTEGER DEFAULT 0 NOT NULL,
	timecreated TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

	-- Specific Columns
	name VARCHAR(50) NOT NULL UNIQUE,
	timelastuploaded TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE prdctgrpstoprdctctgrs
(
	-- Instrumented Columns
	primarykey INTEGER PRIMARY KEY,
	metaflags INTEGER DEFAULT 0 NOT NULL,
	timecreated TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

	-- Specific Columns
	productgroup INTEGER NOT NULL,
	productcategory INTEGER NOT NULL,
	timelastuploaded TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

	-- Constraints
	FOREIGN KEY (productgroup) REFERENCES prdctgrps(primarykey),
	FOREIGN KEY (productcategory) REFERENCES prdctctgrs(primarykey),
	UNIQUE (productgroup, productcategory)
);

CREATE INDEX idx1700 ON prdctgrpstoprdctctgrs(productgroup);
CREATE UNIQUE INDEX idx1710 ON prdctgrpstoprdctctgrs(productcategory);

CREATE TABLE prdctctgrstoprdcts
(
	-- Instrumented Columns
	primarykey INTEGER PRIMARY KEY,
	metaflags INTEGER DEFAULT 0 NOT NULL,
	timecreated TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

	-- Specific Columns
	productcategory INTEGER NOT NULL,
	product INTEGER NOT NULL,
	timelastuploaded TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

	-- Constraints
	FOREIGN KEY (productcategory) REFERENCES prdctctgrs(primarykey),
	FOREIGN KEY (product) REFERENCES prdcts(primarykey),
	UNIQUE (productcategory, product)
);

CREATE INDEX idx1800 ON prdctctgrstoprdcts(productcategory);
CREATE UNIQUE INDEX idx1810 ON prdctctgrstoprdcts(product);

CREATE TABLE prdctstoprdctpkgs
(
	-- Instrumented Columns
	primarykey INTEGER PRIMARY KEY,
	metaflags INTEGER DEFAULT 0 NOT NULL,
	timecreated TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

	-- Specific Columns
	product INTEGER NOT NULL,
	productpackage INTEGER NOT NULL,
	timelastuploaded TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

	-- Constraints
	FOREIGN KEY (product) REFERENCES prdcts(primarykey),
	FOREIGN KEY (productpackage) REFERENCES prdctpkgs(primarykey),
	UNIQUE(product, productpackage)
);

CREATE INDEX idx1850 ON prdctstoprdctpkgs(product);
-- The following index is NOT UNIQUE: a package may be repeated,
-- either for the same product or different ones
CREATE INDEX idx1860 ON prdctstoprdctpkgs(productpackage);

CREATE TABLE outofstockevents
(
	-- Instrumented Columns
	primarykey INTEGER PRIMARY KEY,
	metaflags INTEGER DEFAULT 0 NOT NULL,
	timecreated TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

	-- Specific Columns
	store INTEGER NOT NULL,
	dateoccurred DATE NOT NULL,
	product INTEGER NOT NULL,
	reason VARCHAR(10) NOT NULL,
	count INTEGER NOT NULL,
	lostsalesquantity FLOAT NOT NULL,
	lostsalesamount FLOAT NOT NULL,
	vendornumber INTEGER NOT NULL,
	vendorsubnumber INTEGER NOT NULL,
	dsdwhsflag VARCHAR(5) NOT NULL,
	vendorname VARCHAR(100),

	-- Constraints
	FOREIGN KEY (store) REFERENCES stores(primarykey),
	FOREIGN KEY (product) REFERENCES prdcts(primarykey)
);

CREATE INDEX idx1900 ON outofstockevents(store);
CREATE INDEX idx1910 ON outofstockevents(product);

CREATE TABLE useractions
(
	-- Instrumented Columns
	primarykey INTEGER PRIMARY KEY,
	metaflags INTEGER DEFAULT 0 NOT NULL,
	timecreated TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

	-- Specific Columns
	name VARCHAR(100) NOT NULL, -- NOT UNIQUE
	timelastuploaded TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
	category VARCHAR(20) NOT NULL,
	description VARCHAR(400) NOT NULL

	-- Constraints
);

CREATE OR REPLACE FUNCTION bitand(integer, integer) RETURNS integer
    AS 'select $1 & $2;'
    LANGUAGE SQL
    IMMUTABLE
    RETURNS NULL ON NULL INPUT;

CREATE OR REPLACE FUNCTION bitor(integer, integer) RETURNS integer
    AS 'select $1 | $2;'
    LANGUAGE SQL
    IMMUTABLE
    RETURNS NULL ON NULL INPUT;

CREATE OR REPLACE FUNCTION bitxor(integer, integer) RETURNS integer
    AS 'select $1 # $2;'
    LANGUAGE SQL
    IMMUTABLE
    RETURNS NULL ON NULL INPUT;

INSERT INTO settings (primarykey, category, name, value) VALUES (nextval('pkgenerator'), 'IgnoredEvent', 'ProductCategory', 'BEER');
INSERT INTO settings (primarykey, category, name, value) VALUES (nextval('pkgenerator'), 'IgnoredEvent', 'ProductCategory', 'BEVERAGE MIXERS');
INSERT INTO settings (primarykey, category, name, value) VALUES (nextval('pkgenerator'), 'IgnoredEvent', 'ProductCategory', 'DESSERT WINE');
INSERT INTO settings (primarykey, category, name, value) VALUES (nextval('pkgenerator'), 'IgnoredEvent', 'ProductCategory', 'SPARKLING WINE');
INSERT INTO settings (primarykey, category, name, value) VALUES (nextval('pkgenerator'), 'IgnoredEvent', 'ProductCategory', 'TABLE WINE');
INSERT INTO settings (primarykey, category, name, value) VALUES (nextval('pkgenerator'), 'IgnoredEvent', 'ProductCategory', 'VERMOUTH');

COMMIT TRANSACTION;
