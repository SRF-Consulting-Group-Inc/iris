\set ON_ERROR_STOP

-- NDDOT variable speed limit (VSL) signs.
--
-- Run once on an existing database.  This script does not change the
-- database version, since VSL is not part of an IRIS release.  Nothing here
-- is used unless the nddot_vsl_enable system attribute is true.

SET SESSION AUTHORIZATION 'tms';
BEGIN;

-- Add VSL system attributes
INSERT INTO iris.system_attribute (name, value) VALUES
    ('nddot_vsl_cycle_rate', '1'),
    ('nddot_vsl_enable', 'false'),
    ('nddot_vsl_speed_rounding', '5');

-- Add VSL approval mode look-up table
CREATE TABLE iris.vsl_approval_mode (
    id INTEGER PRIMARY KEY,
    description VARCHAR(32) NOT NULL
);

COPY iris.vsl_approval_mode (id, description) FROM stdin;
0	Manual
1	Semi-Automated
2	Automated
\.

-- Add VSL recommendation state look-up table
CREATE TABLE iris.vsl_state (
    id INTEGER PRIMARY KEY,
    description VARCHAR(32) NOT NULL
);

COPY iris.vsl_state (id, description) FROM stdin;
0	None
1	Pending
2	Approved
3	Rejected
4	Held
\.

-- Add vsl_sign table
--
-- A VSL sign is a DMS, so it shares the DMS name.  Its weather sensors and
-- detectors are the ones whose notes contain the sign's hashtag.
CREATE TABLE iris.vsl_sign (
    name VARCHAR(20) PRIMARY KEY REFERENCES iris._dms ON DELETE CASCADE,
    hashtag VARCHAR(16),
    min_speed INTEGER NOT NULL,
    max_speed INTEGER NOT NULL,
    approval_mode INTEGER NOT NULL REFERENCES iris.vsl_approval_mode,
    rec_speed INTEGER,
    rec_state INTEGER NOT NULL REFERENCES iris.vsl_state,
    rec_time TIMESTAMP WITH time zone,
    posted_speed INTEGER,
    posted_time TIMESTAMP WITH time zone,
    quiet_expires TIMESTAMP WITH time zone,

    CONSTRAINT vsl_hashtag_ck CHECK (hashtag ~ '^#[A-Za-z0-9]+$'),
    CONSTRAINT vsl_speed_ck CHECK (min_speed > 0 AND max_speed >= min_speed),
    CONSTRAINT vsl_rec_speed_ck CHECK (rec_speed > 0),
    CONSTRAINT vsl_posted_speed_ck CHECK (posted_speed > 0)
);

CREATE FUNCTION iris.vsl_sign_notify() RETURNS TRIGGER AS
    $vsl_sign_notify$
BEGIN
    IF (NEW.hashtag IS DISTINCT FROM OLD.hashtag) OR
       (NEW.min_speed IS DISTINCT FROM OLD.min_speed) OR
       (NEW.max_speed IS DISTINCT FROM OLD.max_speed) OR
       (NEW.approval_mode IS DISTINCT FROM OLD.approval_mode)
    THEN
        NOTIFY vsl_sign;
    ELSE
        PERFORM pg_notify('vsl_sign', NEW.name);
    END IF;
    RETURN NULL; -- AFTER trigger return is ignored
END;
$vsl_sign_notify$ LANGUAGE plpgsql;

CREATE TRIGGER vsl_sign_notify_trig
    AFTER UPDATE ON iris.vsl_sign
    FOR EACH ROW EXECUTE FUNCTION iris.vsl_sign_notify();

CREATE TRIGGER vsl_sign_table_notify_trig
    AFTER INSERT OR DELETE ON iris.vsl_sign
    FOR EACH STATEMENT EXECUTE FUNCTION iris.table_notify();

CREATE VIEW vsl_sign_view AS
    SELECT v.name, v.hashtag, v.min_speed, v.max_speed,
           am.description AS approval_mode, v.rec_speed,
           st.description AS rec_state, v.rec_time, v.posted_speed,
           v.posted_time, v.quiet_expires, l.roadway, l.road_dir,
           l.cross_mod, l.cross_street, l.cross_dir, l.landmark, l.lat, l.lon,
           l.corridor, l.location
    FROM iris.vsl_sign v
    JOIN iris.vsl_approval_mode am ON v.approval_mode = am.id
    JOIN iris.vsl_state st ON v.rec_state = st.id
    JOIN iris._dms d ON v.name = d.name
    LEFT JOIN geo_loc_view l ON d.geo_loc = l.name;
GRANT SELECT ON vsl_sign_view TO PUBLIC;

-- Add vsl_sign resource type, based on dms for permissions
INSERT INTO iris.resource_type (name, base) VALUES ('vsl_sign', 'dms');

-- Add VSL event types
INSERT INTO event.event_description (event_desc_id, description) VALUES
    (10001, 'VSL Pending'),
    (10002, 'VSL Approved'),
    (10003, 'VSL Rejected'),
    (10004, 'VSL Posted'),
    (10005, 'VSL Unavailable'),
    (10006, 'VSL Available');

-- Add VSL event table
--
-- No foreign key on vsl_sign, so that the log outlives the sign.
CREATE TABLE event.vsl_event (
    id SERIAL PRIMARY KEY,
    event_date TIMESTAMP WITH time zone DEFAULT NOW() NOT NULL,
    event_desc INTEGER NOT NULL REFERENCES event.event_description,
    vsl_sign VARCHAR(20) NOT NULL,
    speed INTEGER,
    user_id VARCHAR(15)
);

CREATE VIEW vsl_event_view AS
    SELECT ve.id, event_date, ed.description, vsl_sign, speed, user_id
    FROM event.vsl_event ve
    JOIN event.event_description ed ON ve.event_desc = ed.event_desc_id;
GRANT SELECT ON vsl_event_view TO PUBLIC;

-- Add config for VSL events, kept as an audit log
INSERT INTO iris.event_config (name, enable_store, enable_purge, purge_days)
VALUES ('vsl_event', true, false, 0);

COMMIT;
