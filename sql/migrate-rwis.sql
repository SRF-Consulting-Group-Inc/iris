\set ON_ERROR_STOP

SET SESSION AUTHORIZATION 'tms';
BEGIN;

SELECT iris.update_version('5.42.0', '5.42.2');

-- Add rwis_sign table
CREATE TABLE iris.rwis_sign (
    name VARCHAR(32) PRIMARY KEY,
    rwis_conditions VARCHAR(256) NOT NULL DEFAULT '',
    msg_pattern VARCHAR(32)
);

-- Add weather_sensor_override column to dms table/views
DROP VIEW iris.dms CASCADE;
ALTER TABLE iris._dms ADD COLUMN weather_sensor_override VARCHAR(256) NOT NULL DEFAULT '';

CREATE OR REPLACE FUNCTION iris.dms_notify() RETURNS TRIGGER AS
    $dms_notify$
BEGIN
    IF (NEW.msg_user IS DISTINCT FROM OLD.msg_user) THEN
        NOTIFY dms, 'msg_user';
    ELSIF (NEW.msg_sched IS DISTINCT FROM OLD.msg_sched) THEN
        NOTIFY dms, 'msg_sched';
    ELSIF (NEW.msg_current IS DISTINCT FROM OLD.msg_current) THEN
        NOTIFY dms, 'msg_current';
    ELSIF (NEW.expire_time IS DISTINCT FROM OLD.expire_time) THEN
        NOTIFY dms, 'expire_time';
    ELSIF (NEW.status IS DISTINCT FROM OLD.status) THEN
        NOTIFY dms, 'status';
    ELSIF (NEW.stuck_pixels IS DISTINCT FROM OLD.stuck_pixels) THEN
        NOTIFY dms, 'stuck_pixels';
    ELSIF (NEW.weather_sensor_override IS DISTINCT FROM OLD.weather_sensor_override) THEN
        NOTIFY dms, 'weather_sensor_override';
    ELSE
        NOTIFY dms;
    END IF;
    RETURN NULL; -- AFTER trigger return is ignored
END;
$dms_notify$ LANGUAGE plpgsql;

CREATE VIEW iris.dms AS
    SELECT d.name, geo_loc, controller, pin, notes, gps, static_graphic,
           purpose, hidden, beacon, preset, sign_config, sign_detail,
           msg_user, msg_sched, msg_current, expire_time, status, stuck_pixels,
           weather_sensor_override
    FROM iris._dms d
    JOIN iris.controller_io cio ON d.name = cio.name
    JOIN iris._device_preset p ON d.name = p.name;

CREATE OR REPLACE FUNCTION iris.dms_insert() RETURNS TRIGGER AS
    $dms_insert$
BEGIN
    INSERT INTO iris.controller_io (name, resource_n, controller, pin)
         VALUES (NEW.name, 'dms', NEW.controller, NEW.pin);
    INSERT INTO iris._device_preset (name, preset)
         VALUES (NEW.name, NEW.preset);
    INSERT INTO iris._dms (
        name, geo_loc, notes, gps, static_graphic, purpose, hidden, beacon,
        sign_config, sign_detail, msg_user, msg_sched, msg_current,
        expire_time, status, stuck_pixels, weather_sensor_override
    ) VALUES (
        NEW.name, NEW.geo_loc, NEW.notes, NEW.gps, NEW.static_graphic,
        NEW.purpose, NEW.hidden, NEW.beacon, NEW.sign_config, NEW.sign_detail,
        NEW.msg_user, NEW.msg_sched, NEW.msg_current, NEW.expire_time,
        NEW.status, NEW.stuck_pixels, NEW.weather_sensor_override
    );
    RETURN NEW;
END;
$dms_insert$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION iris.dms_update() RETURNS TRIGGER AS
    $dms_update$
BEGIN
    UPDATE iris.controller_io
       SET controller = NEW.controller,
           pin = NEW.pin
     WHERE name = OLD.name;
    UPDATE iris._device_preset
       SET preset = NEW.preset
     WHERE name = OLD.name;
    UPDATE iris._dms
       SET geo_loc = NEW.geo_loc,
           notes = NEW.notes,
           gps = NEW.gps,
           static_graphic = NEW.static_graphic,
           purpose = NEW.purpose,
           hidden = NEW.hidden,
           beacon = NEW.beacon,
           sign_config = NEW.sign_config,
           sign_detail = NEW.sign_detail,
           msg_user = NEW.msg_user,
           msg_sched = NEW.msg_sched,
           msg_current = NEW.msg_current,
           expire_time = NEW.expire_time,
           status = NEW.status,
           stuck_pixels = NEW.stuck_pixels,
           weather_sensor_override = NEW.weather_sensor_override
     WHERE name = OLD.name;
    RETURN NEW;
END;
$dms_update$ LANGUAGE plpgsql;

CREATE VIEW dms_view AS
    SELECT d.name, d.geo_loc, d.controller, d.pin, d.notes, d.gps,
           d.static_graphic, dp.description AS purpose, d.hidden, d.beacon,
           p.camera, p.preset_num, d.sign_config, d.sign_detail,
           default_font, msg_user, msg_sched, msg_current, expire_time,
           status, stuck_pixels, weather_sensor_override,
           l.roadway, l.road_dir, l.cross_mod, l.cross_street,
           l.cross_dir, l.landmark, l.lat, l.lon, l.corridor, l.location
    FROM iris.dms d
    LEFT JOIN iris.camera_preset p ON d.preset = p.name
    LEFT JOIN geo_loc_view l ON d.geo_loc = l.name
    LEFT JOIN iris.device_purpose dp ON d.purpose = dp.id
    LEFT JOIN iris.sign_config sc ON d.sign_config = sc.name;
GRANT SELECT ON dms_view TO PUBLIC;

CREATE VIEW dms_message_view AS
    SELECT d.name, msg_current, cc.description AS condition,
           fail_time IS NOT NULL AS failed, multi, msg_owner, flash_beacon,
           msg_priority, duration, expire_time
    FROM iris.dms d
    LEFT JOIN iris.controller c ON d.controller = c.name
    LEFT JOIN iris.condition cc ON c.condition = cc.id
    LEFT JOIN iris.sign_message sm ON d.msg_current = sm.name;
GRANT SELECT ON dms_message_view TO PUBLIC;

-- Add basic PDMS RWIS message patterns
INSERT INTO iris.msg_pattern (name, multi, flash_beacon, compose_hashtag) VALUES
    ('RWIS_1_Slippery', 'SLIPPERY[nl]ROAD[nl]DETECTED[np]USE[nl]CAUTION', false, NULL),
    ('RWIS_2_ReducedVisib', 'REDUCED[nl]VISIBILITY[nl]DETECTED[np]USE[nl]CAUTION', false, NULL),
    ('RWIS_3_Wind40mph', 'WIND GST[nl]>40 MPH[nl]DETECTED[np]USE[nl]CAUTION', false, NULL),
    ('RWIS_4_Wind60mph', 'WIND GST[nl]>60 MPH[nl]DETECTED[np]REDUCE[nl]SPEED', false, NULL),
    ('RWIS_5_VerySlippery', 'SLIPPERY[nl]ROAD[nl]DETECTED[np]REDUCE[nl]SPEED', false, NULL),
    ('RWIS_6_LowVisib', 'LOW[nl]VISBLITY[nl]DETECTED[np]REDUCE[nl]SPEED', false, NULL),
    ('RWIS_7_IceDetected', 'ICE[nl]DETECTED[np]REDUCE[nl]SPEED', false, NULL);

-- Add basic RWIS admin permissions
INSERT INTO iris.capability (name, enabled) VALUES ('rwis', true);
INSERT INTO iris.resource_type (name) VALUES ('rwis_sign');
INSERT INTO iris.role_capability (role, capability) VALUES ('administrator', 'rwis');
INSERT INTO iris.privilege (name, capability, type_n, obj_n, group_n, attr_n, write) VALUES
    ('PRV_RWIS', 'rwis', 'rwis_sign', '', '', '', false);


INSERT INTO iris.system_attribute (name, value) VALUES
    ('rwis_auto_max_m', '805'),
    ('rwis_cycle_sec', '-1'),
    ('rwis_msg_priority', '9');

COMMIT;
