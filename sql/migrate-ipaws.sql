-- Reserve IPAWS Alert comm protocol value
INSERT INTO iris.comm_protocol (id, description) VALUES (42, 'IPAWS Alert');

-- Drop comm_link_view so we can alter the URI field length below
DROP VIEW public.comm_link_view;

-- Extend allowed length of URI field in comm_link table
ALTER TABLE iris.comm_link ALTER COLUMN uri TYPE character varying(128);

-- Recreate comm_link_view with the altered table
CREATE VIEW comm_link_view AS
        SELECT cl.name, cl.description, modem, uri, cp.description AS protocol,
               poll_enabled, poll_period, timeout
        FROM iris.comm_link cl
        JOIN iris.comm_protocol cp ON cl.protocol = cp.id;
GRANT SELECT ON comm_link_view TO PUBLIC;

-- IPAWS Alert Event table
CREATE TABLE event.ipaws
(
    name text COLLATE pg_catalog."default",
    identifier text COLLATE pg_catalog."default",
    sender text COLLATE pg_catalog."default",
    sent_date timestamp with time zone,
    status text COLLATE pg_catalog."default",
    message_type text COLLATE pg_catalog."default",
    scope text COLLATE pg_catalog."default",
    codes text[] COLLATE pg_catalog."default",
    note text COLLATE pg_catalog."default",
    alert_references text[] COLLATE pg_catalog."default",
    incidents text[] COLLATE pg_catalog."default",
    categories text[] COLLATE pg_catalog."default",
    event text COLLATE pg_catalog."default",
    response_types text[] COLLATE pg_catalog."default",
    urgency text COLLATE pg_catalog."default",
    severity text COLLATE pg_catalog."default",
    certainty text COLLATE pg_catalog."default",
    audience text COLLATE pg_catalog."default",
    effective_date timestamp with time zone,
    onset_date timestamp with time zone,
    expiration_date timestamp with time zone,
    sender_name text COLLATE pg_catalog."default",
    headline text COLLATE pg_catalog."default",
    alert_description text COLLATE pg_catalog."default",
    instruction text COLLATE pg_catalog."default",
    parameters jsonb,
    area jsonb,
    geo_poly geography(polygon),
    purgeable boolean DEFAULT false
);

TABLESPACE pg_default;

ALTER TABLE event.ipaws
    OWNER TO tms;
	
INSERT INTO iris.sonar_type (name) VALUES ('ipaws');

-- IPAWS Alert Notifier table
CREATE TABLE event.ipaws_alert_notifier (
	name VARCHAR(24) PRIMARY KEY,
	alert_id text,
	dms text[],
	multi text,
	approved_by text
);

ALTER TABLE event.ipaws_alert_notifier OWNER TO tms;

-- Extend sonar type fields to allow a longer name

ALTER TABLE iris.sonar_type ALTER COLUMN name TYPE varchar(32);

INSERT INTO iris.sonar_type (name) VALUES ('ipaws_alert_notifier');

-- Need to drop the privilege view first
DROP VIEW public.role_privilege_view;

ALTER TABLE iris.privilege ALTER COLUMN type_n TYPE varchar(32);

-- Recreate view
CREATE VIEW role_privilege_view AS
    SELECT role, role_capability.capability, type_n, obj_n, group_n, attr_n,
	       write
	FROM iris.role
	JOIN iris.role_capability ON role.name = role_capability.role
	JOIN iris.capability ON role_capability.capability = capability.name
	JOIN iris.privilege ON privilege.capability = capability.name
	WHERE role.enabled = 't' AND capability.enabled = 't';
GRANT SELECT ON role_privilege_view TO PUBLIC;
