\set ON_ERROR_STOP

SET SESSION AUTHORIZATION 'tms';
BEGIN;

SELECT iris.update_version('5.61.0', '5.62.0');

-- Add notify channels
CREATE TRIGGER encoder_type_notify_trig
    AFTER INSERT OR UPDATE OR DELETE ON iris.encoder_type
    FOR EACH STATEMENT EXECUTE FUNCTION iris.table_notify();

CREATE TRIGGER encoder_stream_notify_trig
    AFTER INSERT OR UPDATE OR DELETE ON iris.encoder_stream
    FOR EACH STATEMENT EXECUTE FUNCTION iris.table_notify();

CREATE TRIGGER incident_detail_notify_trig
    AFTER INSERT OR UPDATE OR DELETE ON event.incident_detail
    FOR EACH STATEMENT EXECUTE FUNCTION iris.table_notify();

CREATE TRIGGER inc_advice_notify_trig
    AFTER INSERT OR UPDATE OR DELETE ON iris.inc_advice
    FOR EACH STATEMENT EXECUTE FUNCTION iris.table_notify();

CREATE TRIGGER inc_descriptor_notify_trig
    AFTER INSERT OR UPDATE OR DELETE ON iris.inc_descriptor
    FOR EACH STATEMENT EXECUTE FUNCTION iris.table_notify();

CREATE TRIGGER inc_locator_notify_trig
    AFTER INSERT OR UPDATE OR DELETE ON iris.inc_locator
    FOR EACH STATEMENT EXECUTE FUNCTION iris.table_notify();

CREATE TRIGGER camera_preset_notify_trig
    AFTER INSERT OR UPDATE OR DELETE ON iris.camera_preset
    FOR EACH STATEMENT EXECUTE FUNCTION iris.table_notify();

-- Add impact constraint to incident
ALTER TABLE event.incident ADD CONSTRAINT impact_ck
    CHECK (impact ~ '^[!?\.]*$');

-- Rename iris_user to user_id in client_event
DROP VIEW client_event_view;

ALTER TABLE event.client_event RENAME TO old_client_event;

CREATE TABLE event.client_event (
    id SERIAL PRIMARY KEY,
    event_date TIMESTAMP WITH time zone DEFAULT NOW() NOT NULL,
    event_desc INTEGER NOT NULL REFERENCES event.event_description,
    host_port VARCHAR(64) NOT NULL,
    user_id VARCHAR(15)
);

INSERT INTO event.client_event (event_date, event_desc, host_port, user_id)
SELECT event_date, event_desc_id, host_port, iris_user
FROM event.old_client_event;

DROP TABLE event.old_client_event;

CREATE VIEW client_event_view AS
    SELECT ev.id, event_date, ed.description, host_port, user_id
    FROM event.client_event ev
    JOIN event.event_description ed ON ev.event_desc = ed.event_desc_id;
GRANT SELECT ON client_event_view TO PUBLIC;

-- Rename iris_user to user_id in gate_arm_event
DROP VIEW gate_arm_event_view;

ALTER TABLE event.gate_arm_event RENAME TO old_gate_arm_event;

CREATE TABLE event.gate_arm_event (
    id SERIAL PRIMARY KEY,
    event_date TIMESTAMP WITH time zone DEFAULT NOW() NOT NULL,
    event_desc INTEGER NOT NULL REFERENCES event.event_description,
    device_id VARCHAR(20),
    user_id VARCHAR(15),
    fault VARCHAR(32)
);

INSERT INTO event.gate_arm_event (
    event_date, event_desc, device_id, user_id, fault
)
SELECT event_date, event_desc_id, device_id, iris_user, fault
FROM event.old_gate_arm_event;

DROP TABLE event.old_gate_arm_event;

CREATE VIEW gate_arm_event_view AS
    SELECT ev.id, event_date, ed.description, device_id, user_id, fault
    FROM event.gate_arm_event ev
    JOIN event.event_description ed ON ev.event_desc = ed.event_desc_id;
GRANT SELECT ON gate_arm_event_view TO PUBLIC;

-- Add meter_lock_event
CREATE TABLE event.meter_lock_event (
    id SERIAL PRIMARY KEY,
    event_date TIMESTAMP WITH time zone DEFAULT NOW() NOT NULL,
    event_desc INTEGER NOT NULL REFERENCES event.event_description,
    ramp_meter VARCHAR(20) NOT NULL REFERENCES iris._ramp_meter
        ON DELETE CASCADE,
    m_lock INTEGER REFERENCES iris.meter_lock,
    user_id VARCHAR(15)
);

CREATE VIEW meter_lock_event_view AS
    SELECT ev.id, event_date, ed.description, ramp_meter,
           lk.description AS m_lock, user_id
    FROM event.meter_lock_event ev
    JOIN event.event_description ed ON ev.event_desc = ed.event_desc_id
    JOIN iris.meter_lock lk ON ev.m_lock = lk.id;
GRANT SELECT ON meter_lock_event_view TO PUBLIC;

INSERT INTO event.event_description (event_desc_id, description)
    VALUES (402, 'Meter LOCK');

INSERT INTO iris.event_config (name, enable_store, enable_purge, purge_days)
    VALUES ('meter_lock_event', true, false, 0);

-- Add phase and user_id to action_plan_event
DROP VIEW action_plan_event_view;

ALTER TABLE event.action_plan_event RENAME TO old_action_plan_event;

CREATE TABLE event.action_plan_event (
    id SERIAL PRIMARY KEY,
    event_date TIMESTAMP WITH time zone DEFAULT NOW() NOT NULL,
    event_desc INTEGER NOT NULL REFERENCES event.event_description,
    action_plan VARCHAR(16) NOT NULL,
    phase VARCHAR(12),
    user_id VARCHAR(15)
);

INSERT INTO event.action_plan_event (
    event_date, event_desc, action_plan, user_id
)
SELECT event_date, event_desc_id, action_plan, detail
FROM event.old_action_plan_event
WHERE event_desc_id = 900 OR event_desc_id = 901;

INSERT INTO event.action_plan_event (
    event_date, event_desc, action_plan, phase
)
SELECT event_date, event_desc_id, action_plan, detail::VARCHAR(12)
FROM event.old_action_plan_event
WHERE event_desc_id = 902;

DROP TABLE event.old_action_plan_event;

CREATE VIEW action_plan_event_view AS
    SELECT ev.id, event_date, ed.description, action_plan, phase, user_id
    FROM event.action_plan_event ev
    JOIN event.event_description ed ON ev.event_desc = ed.event_desc_id;
GRANT SELECT ON action_plan_event_view TO PUBLIC;

-- Add user_id to beacon_event
DROP VIEW beacon_event_view;

ALTER TABLE event.beacon_event RENAME TO old_beacon_event;

CREATE TABLE event.beacon_event (
    id SERIAL PRIMARY KEY,
    event_date TIMESTAMP WITH time zone DEFAULT NOW() NOT NULL,
    beacon VARCHAR(20) NOT NULL REFERENCES iris._beacon ON DELETE CASCADE,
    state INTEGER NOT NULL REFERENCES iris.beacon_state,
    user_id VARCHAR(15)
);

INSERT INTO event.beacon_event (event_date, beacon, state)
SELECT event_date, beacon, state
FROM event.old_beacon_event;

DROP TABLE event.old_beacon_event;

CREATE VIEW beacon_event_view AS
    SELECT be.id, event_date, beacon, bs.description AS state, user_id
    FROM event.beacon_event be
    JOIN iris.beacon_state bs ON be.state = bs.id;
GRANT SELECT ON beacon_event_view TO PUBLIC;

COMMIT;