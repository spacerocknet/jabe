DROP KEYSPACE IF EXISTS spacerock;

CREATE KEYSPACE IF NOT EXISTS spacerock
	WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

use spacerock;

///////////////////////////////////////////////////////////////////////////////////////////////
// game_info
CREATE TABLE IF NOT EXISTS spacerock.game_info (
	gid int PRIMARY KEY,
	game_name text,
	game_description text,
	categories set<text>,
	battles_per_game int
) WITH 
read_repair_chance = 0.1
AND dclocal_read_repair_chance = 0.0
AND gc_grace_seconds = 864000
AND compaction = {'class' : 'SizeTieredCompactionStrategy', 'min_threshold' : 4, 'max_threshold' : 32 } 
AND replicate_on_write = true 
AND caching = 'KEYS_ONLY' 
AND compression = {'sstable_compression' : 'org.apache.cassandra.io.compress.SnappyCompressor'};

CREATE INDEX game_info_name on game_info(game_name);

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

///////////////////////////////////////////////////////////////////////////////////////////////
// categories 
CREATE TABLE IF NOT EXISTS spacerock.categories (
	category text PRIMARY KEY,
	game_list set<int>,
	description text
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
	qid bigint PRIMARY KEY,
	category text,
	question text,
	right_answer text,
	df int,	
	ans1 text,
	ans2 text,
	ans3 text
	
) WITH 
read_repair_chance = 0.1
AND dclocal_read_repair_chance = 0.0
AND gc_grace_seconds = 864000
AND compaction = {'class' : 'SizeTieredCompactionStrategy', 'min_threshold' : 4, 'max_threshold' : 32 } 
AND replicate_on_write = true 
AND caching = 'KEYS_ONLY' 
AND compression = {'sstable_compression' : 'org.apache.cassandra.io.compress.SnappyCompressor'};


CREATE INDEX quizzes_category on quizzes(category);

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

CREATE INDEX user_username on users(user_name);


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

CREATE INDEX start_sku on sku(start_time);
CREATE INDEX expired_sku on sku(expired_time);

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

CREATE INDEX phone_device on device(phone);
CREATE INDEX uid_device on device(uid);

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
