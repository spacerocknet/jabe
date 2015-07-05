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
CREATE TABLE IF NOT EXISTS spacerock.devices (
	duuid text,
	uid text,
	os text,
	platform text,
	model text,
	phone text,
	registered_time timestamp,
	PRIMARY KEY (duuid, uid)
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


///////////////////////////////////////////////////////////////////////////////////////////////
// user-game-profile
CREATE TABLE IF NOT EXISTS spacerock.user_game_profile (
	uid text,
	game_id int,
	level int,
	occupation int,
	ability_ids set<int>,
	skills set<int>,
	diamonds_balance int,
	coin_balance int,
	PRIMARY KEY (uid, game_id, level)
) WITH
read_repair_chance = 0.1
AND dclocal_read_repair_chance = 0.0
AND gc_grace_seconds = 864000
AND compaction = {'class' : 'SizeTieredCompactionStrategy', 'min_threshold' : 4, 'max_threshold' : 32 }
AND replicate_on_write = true
AND caching = 'KEYS_ONLY'
AND compression = {'sstable_compression' : 'org.apache.cassandra.io.compress.SnappyCompressor'};

///////////////////////////////////////////////////////////////////////////////////////////////
// profession_prob
CREATE TABLE IF NOT EXISTS spacerock.profession_prob (
	profession_level text,
	probs map<text, int>,
	PRIMARY KEY (profession_level)
) WITH
read_repair_chance = 0.1
AND dclocal_read_repair_chance = 0.0
AND gc_grace_seconds = 864000
AND compaction = {'class' : 'SizeTieredCompactionStrategy', 'min_threshold' : 4, 'max_threshold' : 32 }
AND replicate_on_write = true
AND caching = 'KEYS_ONLY'
AND compression = {'sstable_compression' : 'org.apache.cassandra.io.compress.SnappyCompressor'};

///////////////////////////////////////////////////////////////////////////////////////////////
// piece
CREATE TABLE IF NOT EXISTS spacerock.piece (
	piece_id int,
	category_name text,
	description text,
	image_id text,
	tittle text,
	PRIMARY KEY (piece_id)
) WITH
read_repair_chance = 0.1
AND dclocal_read_repair_chance = 0.0
AND gc_grace_seconds = 864000
AND compaction = {'class' : 'SizeTieredCompactionStrategy', 'min_threshold' : 4, 'max_threshold' : 32 }
AND replicate_on_write = true
AND caching = 'KEYS_ONLY'
AND compression = {'sstable_compression' : 'org.apache.cassandra.io.compress.SnappyCompressor'};

///////////////////////////////////////////////////////////////////////////////////////////////
// boost
CREATE TABLE IF NOT EXISTS spacerock.boost (
	boost_id int,
	can_do int,
	description text,
	PRIMARY KEY (boost_id)
) WITH
read_repair_chance = 0.1
AND dclocal_read_repair_chance = 0.0
AND gc_grace_seconds = 864000
AND compaction = {'class' : 'SizeTieredCompactionStrategy', 'min_threshold' : 4, 'max_threshold' : 32 }
AND replicate_on_write = true
AND caching = 'KEYS_ONLY'
AND compression = {'sstable_compression' : 'org.apache.cassandra.io.compress.SnappyCompressor'};

///////////////////////////////////////////////////////////////////////////////////////////////
// achievement
CREATE TABLE IF NOT EXISTS spacerock.achievement (
	achievement_id int,
	description text,
	PRIMARY KEY (achievement_id)
) WITH
read_repair_chance = 0.1
AND dclocal_read_repair_chance = 0.0
AND gc_grace_seconds = 864000
AND compaction = {'class' : 'SizeTieredCompactionStrategy', 'min_threshold' : 4, 'max_threshold' : 32 }
AND replicate_on_write = true
AND caching = 'KEYS_ONLY'
AND compression = {'sstable_compression' : 'org.apache.cassandra.io.compress.SnappyCompressor'};

///////////////////////////////////////////////////////////////////////////////////////////////
// video
CREATE TABLE IF NOT EXISTS spacerock.video (
	video_id bigint,
	video_name text,
	short_description text,
	full_description text,
	view_count bigint,
	link text,
	published_time bigint,
	PRIMARY KEY (video_id)
) WITH
read_repair_chance = 0.1
AND dclocal_read_repair_chance = 0.0
AND gc_grace_seconds = 864000
AND compaction = {'class' : 'SizeTieredCompactionStrategy', 'min_threshold' : 4, 'max_threshold' : 32 }
AND replicate_on_write = true
AND caching = 'KEYS_ONLY'
AND compression = {'sstable_compression' : 'org.apache.cassandra.io.compress.SnappyCompressor'};

///////////////////////////////////////////////////////////////////////////////////////////////
// video_name_id
CREATE TABLE IF NOT EXISTS spacerock.video_name_id (
	video_id bigint,
	video_name text,
	PRIMARY KEY (video_name)
) WITH
read_repair_chance = 0.1
AND dclocal_read_repair_chance = 0.0
AND gc_grace_seconds = 864000
AND compaction = {'class' : 'SizeTieredCompactionStrategy', 'min_threshold' : 4, 'max_threshold' : 32 }
AND replicate_on_write = true
AND caching = 'KEYS_ONLY'
AND compression = {'sstable_compression' : 'org.apache.cassandra.io.compress.SnappyCompressor'};

///////////////////////////////////////////////////////////////////////////////////////////////
// uid_video
CREATE TABLE IF NOT EXISTS spacerock.uid_video (
	video_id bigint,
	day_epoch int,
	uid text,
	watched_time bigint,
	PRIMARY KEY (uid, day_epoch, video_id, watched_time)
) WITH
read_repair_chance = 0.1
AND dclocal_read_repair_chance = 0.0
AND gc_grace_seconds = 864000
AND compaction = {'class' : 'SizeTieredCompactionStrategy', 'min_threshold' : 4, 'max_threshold' : 32 }
AND replicate_on_write = true
AND caching = 'KEYS_ONLY'
AND compression = {'sstable_compression' : 'org.apache.cassandra.io.compress.SnappyCompressor'};

///////////////////////////////////////////////////////////////////////////////////////////////
// video_uid
CREATE TABLE IF NOT EXISTS spacerock.video_uid (
	video_id bigint,
	uid text,
	watched_time bigint,
	PRIMARY KEY (video_id, uid, watched_time)
) WITH
read_repair_chance = 0.1
AND dclocal_read_repair_chance = 0.0
AND gc_grace_seconds = 864000
AND compaction = {'class' : 'SizeTieredCompactionStrategy', 'min_threshold' : 4, 'max_threshold' : 32 }
AND replicate_on_write = true
AND caching = 'KEYS_ONLY'
AND compression = {'sstable_compression' : 'org.apache.cassandra.io.compress.SnappyCompressor'};

///////////////////////////////////////////////////////////////////////////////////////////////
// ability
CREATE TABLE IF NOT EXISTS spacerock.ability (
	ability_id int,
	ability_name text,
	description text,
	value int,
	PRIMARY KEY (ability_id)
) WITH
read_repair_chance = 0.1
AND dclocal_read_repair_chance = 0.0
AND gc_grace_seconds = 864000
AND compaction = {'class' : 'SizeTieredCompactionStrategy', 'min_threshold' : 4, 'max_threshold' : 32 }
AND replicate_on_write = true
AND caching = 'KEYS_ONLY'
AND compression = {'sstable_compression' : 'org.apache.cassandra.io.compress.SnappyCompressor'};

///////////////////////////////////////////////////////////////////////////////////////////////
// avatar
CREATE TABLE IF NOT EXISTS spacerock.avatar (
	avatar_id bigint,
	hair_id bigint,
	hat_id bigint,
	shirt_id bigint,
	shoes_id bigint,
	PRIMARY KEY (avatar_id)
) WITH
read_repair_chance = 0.1
AND dclocal_read_repair_chance = 0.0
AND gc_grace_seconds = 864000
AND compaction = {'class' : 'SizeTieredCompactionStrategy', 'min_threshold' : 4, 'max_threshold' : 32 }
AND replicate_on_write = true
AND caching = 'KEYS_ONLY'
AND compression = {'sstable_compression' : 'org.apache.cassandra.io.compress.SnappyCompressor'};

///////////////////////////////////////////////////////////////////////////////////////////////
// avatar_history
CREATE TABLE IF NOT EXISTS spacerock.avatar_history (
	uid text,
	avatar_id bigint,
	PRIMARY KEY (uid, avatar_id)
) WITH
read_repair_chance = 0.1
AND dclocal_read_repair_chance = 0.0
AND gc_grace_seconds = 864000
AND compaction = {'class' : 'SizeTieredCompactionStrategy', 'min_threshold' : 4, 'max_threshold' : 32 }
AND replicate_on_write = true
AND caching = 'KEYS_ONLY'
AND compression = {'sstable_compression' : 'org.apache.cassandra.io.compress.SnappyCompressor'};


///////////////////////////////////////////////////////////////////////////////////////////////
// game_sessions
CREATE TABLE IF NOT EXISTS spacerock.game_sessions (
	game_session_id text,
	state int,
	uid_1 text,
	puzzle_pieces_1 int,
	uid_1_last_move bigint,
	uid_2 text,
	puzzle_pieces_2 int,
	uid_2_last_move bigint,
	current_turn int,
	current_round int,
	attributes map<text, text>,
	PRIMARY KEY (game_session_id)
) WITH
read_repair_chance = 0.1
AND dclocal_read_repair_chance = 0.0
AND gc_grace_seconds = 864000
AND compaction = {'class' : 'SizeTieredCompactionStrategy', 'min_threshold' : 4, 'max_threshold' : 32 }
AND replicate_on_write = true
AND caching = 'KEYS_ONLY'
AND compression = {'sstable_compression' : 'org.apache.cassandra.io.compress.SnappyCompressor'};

///////////////////////////////////////////////////////////////////////////////////////////////
// user_game_sessions
CREATE TABLE IF NOT EXISTS spacerock.user_game_sessions (
	uid text,
	game_session_ids set<text>,
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
// open_game_session
CREATE TABLE IF NOT EXISTS spacerock.open_game_sessions (
	game_session_id text,
	PRIMARY KEY (game_session_id)
) WITH
read_repair_chance = 0.1
AND dclocal_read_repair_chance = 0.0
AND gc_grace_seconds = 864000
AND compaction = {'class' : 'SizeTieredCompactionStrategy', 'min_threshold' : 4, 'max_threshold' : 32 }
AND replicate_on_write = true
AND caching = 'KEYS_ONLY'
AND compression = {'sstable_compression' : 'org.apache.cassandra.io.compress.SnappyCompressor'};
