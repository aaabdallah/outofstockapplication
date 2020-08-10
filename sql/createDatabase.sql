-- Script for creating a fresh database.
-- To run from a command prompt: sqlplus kooutofstock/kooutofstock @createDatabase.sql
SET sqlblanklines ON
SET autocommit OFF

-- Drop view of store locations
DROP MATERIALIZED VIEW storelocations;

-- Drop existing tables & sequences
DROP TABLE useractions CASCADE CONSTRAINTS;
DROP TABLE outofstockevents CASCADE CONSTRAINTS;
DROP TABLE prdctstoprdctpkgs CASCADE CONSTRAINTS;
DROP TABLE prdctctgrstoprdcts CASCADE CONSTRAINTS;
DROP TABLE prdctgrpstoprdctctgrs CASCADE CONSTRAINTS;
DROP TABLE prdctpkgs CASCADE CONSTRAINTS;
DROP TABLE prdcts CASCADE CONSTRAINTS;
DROP TABLE prdctctgrs CASCADE CONSTRAINTS;
DROP TABLE prdctgrps CASCADE CONSTRAINTS;
DROP TABLE dstbdstrctstostores CASCADE CONSTRAINTS;
DROP TABLE dstbdvsnstodstbdstrcts CASCADE CONSTRAINTS;
DROP TABLE bttlrslsrtstostores CASCADE CONSTRAINTS;
DROP TABLE bttlrbrchstostores CASCADE CONSTRAINTS;
DROP TABLE bttlrmktuntstobttlrbrchs CASCADE CONSTRAINTS;
DROP TABLE bttlrbsnsuntstobttlrmktunts CASCADE CONSTRAINTS;
DROP TABLE bttlrstobttlrbsnsunts CASCADE CONSTRAINTS;
DROP TABLE stores CASCADE CONSTRAINTS;
DROP TABLE dstbdstrcts CASCADE CONSTRAINTS;
DROP TABLE dstbdvsns CASCADE CONSTRAINTS;
DROP TABLE bttlrslsrts CASCADE CONSTRAINTS;
DROP TABLE bttlrbrchs CASCADE CONSTRAINTS;
DROP TABLE bttlrmktunts CASCADE CONSTRAINTS;
DROP TABLE bttlrbsnsunts CASCADE CONSTRAINTS;
DROP TABLE bttlrs CASCADE CONSTRAINTS;
DROP TABLE settings CASCADE CONSTRAINTS;

DROP SEQUENCE pkgenerator;

-- create primary key generator (sequence)
CREATE SEQUENCE pkgenerator START WITH 1000 INCREMENT BY 1000;
-- prime the generator (necessary)
SELECT pkgenerator.nextval FROM dual;

CREATE TABLE settings
(
	-- Instrumented Columns
	primarykey INTEGER PRIMARY KEY,
	metaflags INTEGER DEFAULT 0 NOT NULL,
	timecreated TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

	-- Specific Columns
	category VARCHAR2(100),
	name VARCHAR2(100),
	value VARCHAR2(200)
);

-- create tables & indexes
CREATE TABLE bttlrs
(
	-- Instrumented Columns
	primarykey INTEGER PRIMARY KEY,
	metaflags INTEGER DEFAULT 0 NOT NULL,
	timecreated TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

	-- Specific Columns
	name VARCHAR2(100) NOT NULL UNIQUE,
	timelastuploaded TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE bttlrbsnsunts
(
	-- Instrumented Columns
	primarykey INTEGER PRIMARY KEY,
	metaflags INTEGER DEFAULT 0 NOT NULL,
	timecreated TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

	-- Specific Columns
	name VARCHAR2(100) NOT NULL UNIQUE,
	timelastuploaded TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE bttlrmktunts
(
	-- Instrumented Columns
	primarykey INTEGER PRIMARY KEY,
	metaflags INTEGER DEFAULT 0 NOT NULL,
	timecreated TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

	-- Specific Columns
	name VARCHAR2(100) NOT NULL UNIQUE,
	timelastuploaded TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE bttlrbrchs
(
	-- Instrumented Columns
	primarykey INTEGER PRIMARY KEY,
	metaflags INTEGER DEFAULT 0 NOT NULL,
	timecreated TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

	-- Specific Columns
	name VARCHAR2(100) NOT NULL UNIQUE,
	timelastuploaded TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE bttlrslsrts
(
	-- Instrumented Columns
	primarykey INTEGER PRIMARY KEY,
	metaflags INTEGER DEFAULT 0 NOT NULL,
	timecreated TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

	-- Specific Columns
	name VARCHAR2(100) NOT NULL UNIQUE,
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
	name VARCHAR2(100) NOT NULL UNIQUE,
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
	name VARCHAR2(100) NOT NULL UNIQUE,
	cd VARCHAR2(10) NOT NULL UNIQUE,
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
	name VARCHAR2(100) NOT NULL,
	address VARCHAR2(100),
	city VARCHAR2(50),
	zip VARCHAR2(10),
	state VARCHAR2(2),
	phonenumber VARCHAR2(20),
	dateopened DATE,
	datelastremodeled DATE,
	dateclosed DATE,
	lifestyle VARCHAR2(10),
	distributorcom VARCHAR2(10),
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
	name VARCHAR2(100) NOT NULL UNIQUE,
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
	name VARCHAR2(100) NOT NULL UNIQUE,
	timelastuploaded TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE prdcts
(
	-- Instrumented Columns
	primarykey INTEGER PRIMARY KEY,
	metaflags INTEGER DEFAULT 0 NOT NULL,
	timecreated TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

	-- Specific Columns
	upcid INTEGER NOT NULL UNIQUE,
	description VARCHAR2(100) NOT NULL,
	timelastuploaded TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE prdctpkgs
(
	-- Instrumented Columns
	primarykey INTEGER PRIMARY KEY,
	metaflags INTEGER DEFAULT 0 NOT NULL,
	timecreated TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,

	-- Specific Columns
	name VARCHAR2(50) NOT NULL UNIQUE,
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
	reason VARCHAR2(10) NOT NULL,
	count INTEGER NOT NULL,
	lostsalesquantity FLOAT NOT NULL,
	lostsalesamount FLOAT NOT NULL,
	vendornumber INTEGER NOT NULL,
	vendorsubnumber INTEGER NOT NULL,
	dsdwhsflag VARCHAR2(5) NOT NULL,
	vendorname VARCHAR2(100),

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
	name VARCHAR2(100) NOT NULL, -- NOT UNIQUE
	timelastuploaded TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
	category VARCHAR2(20) NOT NULL,
	description VARCHAR2(400) NOT NULL

	-- Constraints
);

-- Function for performing aggregate string concatenation for use
-- in certain reports that need this for the GROUP BY clauses.
DROP FUNCTION stragg;

DROP TYPE BODY string_agg_type;

DROP TYPE string_agg_type;

CREATE OR REPLACE TYPE string_agg_type AS object
(
	total varchar2(500),

	static function ODCIAggregateInitialize(sctx IN OUT string_agg_type )
	return number,

	member function ODCIAggregateIterate(self IN OUT string_agg_type, value IN varchar2 )
	return number,

	member function ODCIAggregateTerminate(self IN string_agg_type, returnValue OUT varchar2,
	flags IN number)
	return number,

	member function ODCIAggregateMerge(self IN OUT string_agg_type, ctx2 IN string_agg_type)
	return number
);
/

CREATE OR REPLACE TYPE BODY string_agg_type
is
	static function ODCIAggregateInitialize(sctx IN OUT string_agg_type)
	return number
	is
		begin
			sctx := string_agg_type( null );
			return ODCIConst.Success;
		end;

	member function ODCIAggregateIterate(self IN OUT string_agg_type,
	value IN varchar2 )
	return number
	is
		begin
			self.total := self.total || ', ' || value;
			return ODCIConst.Success;
		end;

	member function ODCIAggregateTerminate(self IN string_agg_type,
	returnValue OUT varchar2,
	flags IN number)
	return number
	is
		begin
			returnValue := ltrim(self.total,', ');
			return ODCIConst.Success;
		end;

	member function ODCIAggregateMerge(self IN OUT string_agg_type,
	ctx2 IN string_agg_type)
	return number
	is
		begin
			self.total := self.total || ctx2.total;
			return ODCIConst.Success;
		end;
end;
/

CREATE OR REPLACE FUNCTION stragg(input varchar2 )
	RETURN varchar2
	PARALLEL_ENABLE AGGREGATE USING string_agg_type;
/

DROP FUNCTION bitor;

CREATE OR REPLACE FUNCTION bitor( x IN NUMBER, y IN NUMBER ) RETURN NUMBER  AS
BEGIN
    RETURN x + y - bitand(x,y);
END;
/

DROP FUNCTION bitxor;

CREATE OR REPLACE FUNCTION bitxor( x IN NUMBER, y IN NUMBER ) RETURN NUMBER  AS
BEGIN
    RETURN bitor(x,y) - bitand(x,y);
END;
/

INSERT INTO settings (primarykey, category, name, value) VALUES (pkgenerator.nextval, 'IgnoredEvent', 'ProductCategory', 'BEER');
INSERT INTO settings (primarykey, category, name, value) VALUES (pkgenerator.nextval, 'IgnoredEvent', 'ProductCategory', 'BEVERAGE MIXERS');
INSERT INTO settings (primarykey, category, name, value) VALUES (pkgenerator.nextval, 'IgnoredEvent', 'ProductCategory', 'BEVERAGE WINE');
INSERT INTO settings (primarykey, category, name, value) VALUES (pkgenerator.nextval, 'IgnoredEvent', 'ProductCategory', 'DESSERT WINE');
INSERT INTO settings (primarykey, category, name, value) VALUES (pkgenerator.nextval, 'IgnoredEvent', 'ProductCategory', 'SPARKLING WINE');
INSERT INTO settings (primarykey, category, name, value) VALUES (pkgenerator.nextval, 'IgnoredEvent', 'ProductCategory', 'TABLE WINE');
INSERT INTO settings (primarykey, category, name, value) VALUES (pkgenerator.nextval, 'IgnoredEvent', 'ProductCategory', 'VERMOUTH');

-- Create a single view into the store locations
CREATE MATERIALIZED VIEW storelocations
REFRESH COMPLETE ON COMMIT
AS
SELECT stores.primarykey AS store, stores.id AS storeid, 
dstbdstrctstostores.distributordistrict AS distributordistrict,
dstbdstrcts.name AS distributordistrictname,
dstbdvsnstodstbdstrcts.distributordivision AS distributordivision,
dstbdvsns.name AS distributordivisionname,
bttlrbrchstostores.bottlerbranch AS bottlerbranch,
bttlrbrchs.name AS bottlerbranchname,
bttlrmktuntstobttlrbrchs.bottlermarketunit AS bottlermarketunit,
bttlrmktunts.name AS bottlermarketunitname,
bttlrbsnsuntstobttlrmktunts.bottlerbusinessunit AS bottlerbusinessunit,
bttlrbsnsunts.name AS bottlerbusinessunitname,
bttlrstobttlrbsnsunts.bottler AS bottler,
bttlrs.name AS bottlername,
bttlrslsrtstostores.bottlersalesroute AS bottlersalesroute,
bttlrslsrts.name AS bottlersalesroutename
FROM stores, dstbdstrctstostores, dstbdvsnstodstbdstrcts,
dstbdstrcts, dstbdvsns,
bttlrstobttlrbsnsunts, bttlrbsnsuntstobttlrmktunts,
bttlrmktuntstobttlrbrchs, bttlrbrchstostores,
bttlrbrchs, bttlrmktunts, bttlrbsnsunts, bttlrs,
bttlrslsrtstostores, bttlrslsrts
WHERE
dstbdstrctstostores.store = stores.primarykey
AND dstbdstrctstostores.distributordistrict = dstbdvsnstodstbdstrcts.distributordistrict
AND dstbdstrctstostores.distributordistrict = dstbdstrcts.primarykey
AND dstbdvsnstodstbdstrcts.distributordivision = dstbdvsns.primarykey
AND bttlrbrchstostores.store = stores.primarykey
AND bttlrbrchstostores.bottlerbranch = bttlrmktuntstobttlrbrchs.bottlerbranch
AND bttlrmktuntstobttlrbrchs.bottlermarketunit = bttlrbsnsuntstobttlrmktunts.bottlermarketunit
AND bttlrbsnsuntstobttlrmktunts.bottlerbusinessunit = bttlrstobttlrbsnsunts.bottlerbusinessunit
AND bttlrbrchstostores.bottlerbranch = bttlrbrchs.primarykey
AND bttlrmktuntstobttlrbrchs.bottlermarketunit = bttlrmktunts.primarykey
AND bttlrbsnsuntstobttlrmktunts.bottlerbusinessunit = bttlrbsnsunts.primarykey
AND bttlrstobttlrbsnsunts.bottler = bttlrs.primarykey
AND bttlrslsrtstostores.store = stores.primarykey
AND bttlrslsrtstostores.bottlersalesroute = bttlrslsrts.primarykey
AND bitand(stores.metaflags, 1)+0 = 0
AND bitand(dstbdstrcts.metaflags, 1)+0 = 0
AND bitand(dstbdvsns.metaflags, 1)+0 = 0
AND bitand(bttlrbrchs.metaflags, 1)+0 = 0
AND bitand(bttlrmktunts.metaflags, 1)+0 = 0
AND bitand(bttlrbsnsunts.metaflags, 1)+0 = 0
AND bitand(bttlrs.metaflags, 1)+0 = 0
AND bitand(bttlrslsrts.metaflags, 1)+0 = 0;

-- Create a single view into the product descriptions (without product group information)
CREATE MATERIALIZED VIEW productdescriptions
REFRESH COMPLETE ON COMMIT
AS
SELECT prdcts.primarykey AS product, 
prdcts.upcid AS productupcid, prdcts.description AS productdescription,
prdctctgrs.primarykey AS productcategory,
prdctctgrs.id AS productcategoryid, prdctctgrs.name AS productcategoryname,
prdctpkgs.primarykey AS productpackage,
prdctpkgs.name AS productpackagename
FROM prdctctgrs, prdcts, prdctpkgs, prdctctgrstoprdcts, prdctstoprdctpkgs
WHERE
prdctctgrstoprdcts.productcategory = prdctctgrs.primarykey
AND prdctctgrstoprdcts.product = prdcts.primarykey
AND prdctstoprdctpkgs.product = prdcts.primarykey
AND prdctstoprdctpkgs.productpackage = prdctpkgs.primarykey
AND bitand(prdctctgrs.metaflags, 1)+0 = 0
AND bitand(prdcts.metaflags, 1)+0 = 0
AND bitand(prdctpkgs.metaflags, 1)+0 = 0
AND bitand(prdctctgrstoprdcts.metaflags, 1)+0 = 0
AND bitand(prdctstoprdctpkgs.metaflags, 1)+0 = 0

COMMIT;
