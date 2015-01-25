DROP KEYSPACE IF EXISTS spacerock;

CREATE KEYSPACE IF NOT EXISTS spacerock
	WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

use spacerock;

///////////////////////////////////////////////////////////////////////////////////////////////
// app_config
CREATE TABLE IF NOT EXISTS spacerock.app_config (
	key timeuuid PRIMARY KEY,
	battles_per_game int,
	categories list<text>
) WITH 
read_repair_chance = 0.1
AND dclocal_read_repair_chance = 0.0
AND gc_grace_seconds = 864000
AND compaction = {'class' : 'SizeTieredCompactionStrategy', 'min_threshold' : 4, 'max_threshold' : 32 } 
AND replicate_on_write = true 
AND caching = 'KEYS_ONLY' 
AND compression = {'sstable_compression' : 'org.apache.cassandra.io.compress.SnappyCompressor'};

///////////////////////////////////////////////////////////////////////////////////////////////
// game_info
CREATE TABLE IF NOT EXISTS spacerock.game_info (
	gid int PRIMARY KEY,
	game_name text,
	game_description text,
	categories list<text>,
	battles_per_game int
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
	uid text PRIMARY KEY,
	results list<text>
) WITH 
read_repair_chance = 0.1
AND dclocal_read_repair_chance = 0.0
AND gc_grace_seconds = 864000
AND compaction = {'class' : 'SizeTieredCompactionStrategy', 'min_threshold' : 4, 'max_threshold' : 32 } 
AND replicate_on_write = true 
AND caching = 'KEYS_ONLY' 
AND compression = {'sstable_compression' : 'org.apache.cassandra.io.compress.SnappyCompressor'};

///////////////////////////////////////////////////////////////////////////////////////////////
// categories 
CREATE TABLE IF NOT EXISTS spacerock.categories (
	category text PRIMARY KEY,
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
	loc_state text,
	loc_region text,
	loc_country text,
	app_name text,
	registered_time bigint,
	last_seen bigint,
	platform text,
	os text,
	model text,
	phone text,
	device_uuid text,
	PRIMARY KEY (uid, device_uuid, user_name)
) WITH 
read_repair_chance = 0.1
AND dclocal_read_repair_chance = 0.0
AND gc_grace_seconds = 864000
AND compaction = {'class' : 'SizeTieredCompactionStrategy', 'min_threshold' : 4, 'max_threshold' : 32 } 
AND replicate_on_write = true 
AND caching = 'KEYS_ONLY' 
AND compression = {'sstable_compression' : 'org.apache.cassandra.io.compress.SnappyCompressor'};


///////////////////////////////////////////////////////////////////////////////////////////////
// results
CREATE TABLE IF NOT EXISTS spacerock.results (
	userid uuid PRIMARY KEY,
	results list<bigint>
) WITH 
read_repair_chance = 0.1
AND dclocal_read_repair_chance = 0.0
AND gc_grace_seconds = 864000
AND compaction = {'class' : 'SizeTieredCompactionStrategy', 'min_threshold' : 4, 'max_threshold' : 32 } 
AND replicate_on_write = true 
AND caching = 'KEYS_ONLY' 
AND compression = {'sstable_compression' : 'org.apache.cassandra.io.compress.SnappyCompressor'};
