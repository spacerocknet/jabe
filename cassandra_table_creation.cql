DROP KEYSPACE IF EXISTS spacerock;

CREATE KEYSPACE IF NOT EXISTS spacerock
	WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

use spacerock;

///////////////////////////////////////////////////////////////////////////////////////////////
// game_info
CREATE TABLE IF NOT EXISTS spacerock.game_info (
	gid int,
	game_name text,
	game_description text,
	categories set<text>,
	battles_per_game int,
	PRIMARY KEY (gid)
) WITH 
read_repair_chance = 0.1
AND dclocal_read_repair_chance = 0.0
AND gc_grace_seconds = 864000
AND compaction = {'class' : 'SizeTieredCompactionStrategy', 'min_threshold' : 4, 'max_threshold' : 32 } 
AND replicate_on_write = true 
AND caching = 'KEYS_ONLY' 
AND compression = {'sstable_compression' : 'org.apache.cassandra.io.compress.SnappyCompressor'};

///////////////////////////////////////////////////////////////////////////////////////////////
// game_info_name
CREATE TABLE IF NOT EXISTS spacerock.game_info_name (
	game_name text,
	gid int,
	PRIMARY KEY (game_name)
) WITH
read_repair_chance = 0.1
AND dclocal_read_repair_chance = 0.0
AND gc_grace_seconds = 864000
AND compaction = {'class' : 'SizeTieredCompactionStrategy', 'min_threshold' : 4, 'max_threshold' : 32 }
AND replicate_on_write = true
AND caching = 'KEYS_ONLY'
AND compression = {'sstable_compression' : 'org.apache.cassandra.io.compress.SnappyCompressor'};


///////////////////////////////////////////////////////////////////////////////////////////////
// game_result
CREATE TABLE IF NOT EXISTS spacerock.game_result (
	game_id int,
	level int,
	score bigint,
	uid text,
	PRIMARY KEY(game_id, level, uid)
) WITH 
read_repair_chance = 0.1
AND dclocal_read_repair_chance = 0.0
AND gc_grace_seconds = 864000
AND compaction = {'class' : 'SizeTieredCompactionStrategy', 'min_threshold' : 4, 'max_threshold' : 32 } 
AND replicate_on_write = true 
AND caching = 'KEYS_ONLY' 
AND compression = {'sstable_compression' : 'org.apache.cassandra.io.compress.SnappyCompressor'};

CREATE INDEX gresult_uid on game_result(uid);
// Use secondary index because the query that use uid is navigated first by game_id;
// a little hard??

///////////////////////////////////////////////////////////////////////////////////////////////
// categories 
CREATE TABLE IF NOT EXISTS spacerock.categories (
	category text,
	game_list set<int>,
	description text,
	PRIMARY KEY (category)
) WITH 
read_repair_chance = 0.1
AND dclocal_read_repair_chance = 0.0
AND gc_grace_seconds = 864000
AND compaction = {'class' : 'SizeTieredCompactionStrategy', 'min_threshold' : 4, 'max_threshold' : 32 } 
AND replicate_on_write = true 
AND caching = 'KEYS_ONLY' 
AND compression = {'sstable_compression' : 'org.apache.cassandra.io.compress.SnappyCompressor'};

///////////////////////////////////////////////////////////////////////////////////////////////
// quizzes
CREATE TABLE IF NOT EXISTS spacerock.quizzes (
	qid bigint,
	category text,
	question text,
	right_answer text,
	df int,	
	ans1 text,
	ans2 text,
	ans3 text,
	PRIMARY KEY (qid)
) WITH 
read_repair_chance = 0.1
AND dclocal_read_repair_chance = 0.0
AND gc_grace_seconds = 864000
AND compaction = {'class' : 'SizeTieredCompactionStrategy', 'min_threshold' : 4, 'max_threshold' : 32 } 
AND replicate_on_write = true 
AND caching = 'KEYS_ONLY' 
AND compression = {'sstable_compression' : 'org.apache.cassandra.io.compress.SnappyCompressor'};

///////////////////////////////////////////////////////////////////////////////////////////////
// quizzes_category
CREATE TABLE IF NOT EXISTS spacerock.quizzes_category (
	category text,
	qids set<bigint>,
	PRIMARY KEY (category)
) WITH
read_repair_chance = 0.1
AND dclocal_read_repair_chance = 0.0
AND gc_grace_seconds = 864000
AND compaction = {'class' : 'SizeTieredCompactionStrategy', 'min_threshold' : 4, 'max_threshold' : 32 }
AND replicate_on_write = true
AND caching = 'KEYS_ONLY'
AND compression = {'sstable_compression' : 'org.apache.cassandra.io.compress.SnappyCompressor'};

///////////////////////////////////////////////////////////////////////////////////////////////
// users
CREATE TABLE IF NOT EXISTS spacerock.users (
	uid text,
	user_name text,
	first_name text,
	last_name text,
	email text,
	fb_id text,
	state text,
	region text,
	country text,
	apps text,
	registered_time bigint,
	last_seen bigint,
	platform text, // current device
	os text, // current device
	model text, // current device
	phone text, // current device
	device_uuid text, // current device
	device_list set<text>, // set of device uuid
	PRIMARY KEY (uid)
) WITH 
read_repair_chance = 0.1
AND dclocal_read_repair_chance = 0.0
AND gc_grace_seconds = 864000
AND compaction = {'class' : 'SizeTieredCompactionStrategy', 'min_threshold' : 4, 'max_threshold' : 32 } 
AND replicate_on_write = true 
AND caching = 'KEYS_ONLY' 
AND compression = {'sstable_compression' : 'org.apache.cassandra.io.compress.SnappyCompressor'};

///////////////////////////////////////////////////////////////////////////////////////////////
// user_username
CREATE TABLE IF NOT EXISTS spacerock.user_username (
	user_name text,
	uids set<text>,
	PRIMARY KEY (user_name)
) WITH
read_repair_chance = 0.1
AND dclocal_read_repair_chance = 0.0
AND gc_grace_seconds = 864000
AND compaction = {'class' : 'SizeTieredCompactionStrategy', 'min_threshold' : 4, 'max_threshold' : 32 }
AND replicate_on_write = true
AND caching = 'KEYS_ONLY'
AND compression = {'sstable_compression' : 'org.apache.cassandra.io.compress.SnappyCompressor'};

///////////////////////////////////////////////////////////////////////////////////////////////
// lock table
CREATE TABLE IF NOT EXISTS spacerock.lock (
	lock_name text,
	do_lock boolean,
	PRIMARY KEY (lock_name)
) WITH
read_repair_chance = 0.1
AND dclocal_read_repair_chance = 0.0
AND gc_grace_seconds = 864000
AND compaction = {'class' : 'SizeTieredCompactionStrategy', 'min_threshold' : 4, 'max_threshold' : 32 }
AND replicate_on_write = true
AND caching = 'KEYS_ONLY'
AND compression = {'sstable_compression' : 'org.apache.cassandra.io.compress.SnappyCompressor'};

///////////////////////////////////////////////////////////////////////////////////////////////
// uid blocks
CREATE TABLE IF NOT EXISTS spacerock.uid_blocks (
	block_id int,
	granted_server int,
	status boolean,
	ids set<text>,
	PRIMARY KEY (block_id)
) WITH
read_repair_chance = 0.1
AND dclocal_read_repair_chance = 0.0
AND gc_grace_seconds = 864000
AND compaction = {'class' : 'SizeTieredCompactionStrategy', 'min_threshold' : 4, 'max_threshold' : 32 }
AND replicate_on_write = true
AND caching = 'KEYS_ONLY'
AND compression = {'sstable_compression' : 'org.apache.cassandra.io.compress.SnappyCompressor'};

CREATE INDEX status_uidblock on uid_blocks(status);

///////////////////////////////////////////////////////////////////////////////////////////////
// sku
CREATE TABLE IF NOT EXISTS spacerock.sku (
	sku_id int,
	description text,
	unit_price float,
	start_time timestamp,
	expired_time timestamp,
	extra_data text,
	discount float,
	PRIMARY KEY (sku_id, start_time, expired_time)
) WITH
read_repair_chance = 0.1
AND dclocal_read_repair_chance = 0.0
AND gc_grace_seconds = 864000
AND compaction = {'class' : 'SizeTieredCompactionStrategy', 'min_threshold' : 4, 'max_threshold' : 32 }
AND replicate_on_write = true
AND caching = 'KEYS_ONLY'
AND compression = {'sstable_compression' : 'org.apache.cassandra.io.compress.SnappyCompressor'};

///////////////////////////////////////////////////////////////////////////////////////////////
// billing
CREATE TABLE IF NOT EXISTS spacerock.billing (
	uid text,
	ts timestamp,
	game_id int,
	sku_id int,
	n_items int,
	discount float,
	PRIMARY KEY (uid, ts)
) WITH
read_repair_chance = 0.1
AND dclocal_read_repair_chance = 0.0
AND gc_grace_seconds = 864000
AND compaction = {'class' : 'SizeTieredCompactionStrategy', 'min_threshold' : 4, 'max_threshold' : 32 }
AND replicate_on_write = true
AND caching = 'KEYS_ONLY'
AND compression = {'sstable_compression' : 'org.apache.cassandra.io.compress.SnappyCompressor'};

CREATE INDEX gameid_billing on billing(game_id);

///////////////////////////////////////////////////////////////////////////////////////////////
// device
CREATE TABLE IF NOT EXISTS spacerock.device (
	duuid text,
	uid text,
	os text,
	platform text,
	model text,
	phone text,
	registered_time timestamp,
	PRIMARY KEY (duuid)
) WITH
read_repair_chance = 0.1
AND dclocal_read_repair_chance = 0.0
AND gc_grace_seconds = 864000
AND compaction = {'class' : 'SizeTieredCompactionStrategy', 'min_threshold' : 4, 'max_threshold' : 32 }
AND replicate_on_write = true
AND caching = 'KEYS_ONLY'
AND compression = {'sstable_compression' : 'org.apache.cassandra.io.compress.SnappyCompressor'};

///////////////////////////////////////////////////////////////////////////////////////////////
// device_phone
CREATE TABLE IF NOT EXISTS spacerock.device_phone (
	duuids set<text>,
	phone text,
	PRIMARY KEY (phone)
) WITH
read_repair_chance = 0.1
AND dclocal_read_repair_chance = 0.0
AND gc_grace_seconds = 864000
AND compaction = {'class' : 'SizeTieredCompactionStrategy', 'min_threshold' : 4, 'max_threshold' : 32 }
AND replicate_on_write = true
AND caching = 'KEYS_ONLY'
AND compression = {'sstable_compression' : 'org.apache.cassandra.io.compress.SnappyCompressor'};

///////////////////////////////////////////////////////////////////////////////////////////////
// device_uid
CREATE TABLE IF NOT EXISTS spacerock.device_uid (
	duuids set<text>,
	uid text,
	PRIMARY KEY (uid)
) WITH
read_repair_chance = 0.1
AND dclocal_read_repair_chance = 0.0
AND gc_grace_seconds = 864000
AND compaction = {'class' : 'SizeTieredCompactionStrategy', 'min_threshold' : 4, 'max_threshold' : 32 }
AND replicate_on_write = true
AND caching = 'KEYS_ONLY'
AND compression = {'sstable_compression' : 'org.apache.cassandra.io.compress.SnappyCompressor'};

///////////////////////////////////////////////////////////////////////////////////////////////
// auth-code
CREATE TABLE IF NOT EXISTS spacerock.authcode (
	code text,
	created_time timestamp,
	expired_time timestamp,
	status boolean,
	PRIMARY KEY (code)
) WITH
read_repair_chance = 0.1
AND dclocal_read_repair_chance = 0.0
AND gc_grace_seconds = 864000
AND compaction = {'class' : 'SizeTieredCompactionStrategy', 'min_threshold' : 4, 'max_threshold' : 32 }
AND replicate_on_write = true
AND caching = 'KEYS_ONLY'
AND compression = {'sstable_compression' : 'org.apache.cassandra.io.compress.SnappyCompressor'};

///////////////////////////////////////////////////////////////////////////////////////////////
// server-info
CREATE TABLE IF NOT EXISTS spacerock.serverinfo (
	server_ip inet,
	seq bigint,
	PRIMARY KEY (server_ip)
) WITH
read_repair_chance = 0.1
AND dclocal_read_repair_chance = 0.0
AND gc_grace_seconds = 864000
AND compaction = {'class' : 'SizeTieredCompactionStrategy', 'min_threshold' : 4, 'max_threshold' : 32 }
AND replicate_on_write = true
AND caching = 'KEYS_ONLY'
AND compression = {'sstable_compression' : 'org.apache.cassandra.io.compress.SnappyCompressor'};
