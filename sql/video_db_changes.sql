INSERT INTO iris.system_attribute (name, value)
VALUES ('vid_connect_autostart', true),
		('vid_connect_fail_next_source', true),
		('vid_connect_fail_sec', 20),
		('vid_lost_timeout_sec', 10),
		('vid_reconnect_auto', true),
		('vid_reconnect_timeout_sec', 10),
		('gstreamer_current_version','1.16.2');

CREATE TABLE iris._camera_template
(
	name character varying(20),
	notes text,
	label text,
	CONSTRAINT camera_template_pkey PRIMARY KEY (name)
);

ALTER TABLE iris._camera_template
    OWNER to tms;
	
CREATE OR REPLACE VIEW iris.camera_template
 AS
 SELECT name,
		notes,
		label
   FROM iris._camera_template;

ALTER TABLE iris.camera_template
    OWNER TO tms;

CREATE TABLE iris._vid_src_template
(
	name character varying(20),
	label text,
	config text,
	default_port integer,
	subnets text,
	latency integer,
	encoder character varying(64),
	scheme text,
	codec text,
	rez_width integer,
	rez_height integer,
	multicast boolean,
	notes text
	CONSTRAINT vid_src_template_pkey PRIMARY KEY (name)
);

ALTER TABLE iris._vid_src_template
    OWNER to tms;
	
CREATE OR REPLACE VIEW iris.vid_src_template
 AS
 SELECT name,
		label,
		config,
		default_port,
		subnets,
		latency,
		encoder,
		scheme,
		codec,
		rez_width,
		rez_height,
		multicast,
		notes
   FROM iris._vid_src_template;

ALTER TABLE iris.vid_src_template
    OWNER TO tms;
	
ALTER TABLE iris._camera ADD COLUMN cam_template character varying(20);
 
ALTER TABLE iris._camera ADD CONSTRAINT _camera_cam_template_fkey FOREIGN KEY (cam_template)
        REFERENCES iris._camera_template (name) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION;

CREATE OR REPLACE FUNCTION iris.camera_update() RETURNS TRIGGER AS
	$camera_update$
BEGIN
	UPDATE iris._device_io
	   SET controller = NEW.controller,
	       pin = NEW.pin
	 WHERE name = OLD.name;
	UPDATE iris._camera
	   SET geo_loc = NEW.geo_loc,
	       notes = NEW.notes,
	       cam_num = NEW.cam_num,
	       encoder_type = NEW.encoder_type,
	       encoder = NEW.encoder,
	       enc_mcast = NEW.enc_mcast,
	       encoder_channel = NEW.encoder_channel,
	       publish = NEW.publish,
	       video_loss = NEW.video_loss,
	       cam_template = NEW.cam_template
	 WHERE name = OLD.name;
	RETURN NEW;
END;
$camera_update$ LANGUAGE plpgsql;

CREATE TABLE iris._cam_vid_src_ord
(
	name character varying(24),
	camera_template character varying(20),
	src_order integer,
	src_template character varying(20),
	CONSTRAINT camera_vid_src_order_pkey PRIMARY KEY (name)
);
ALTER TABLE iris._cam_vid_src_ord
    OWNER to tms;
	
	
CREATE OR REPLACE VIEW iris.cam_vid_src_ord
 AS
 SELECT name,
		camera_template,
		src_order,
		src_template
   FROM iris._cam_vid_src_ord;

ALTER TABLE iris.cam_vid_src_ord
    OWNER TO tms;
	
ALTER TABLE iris._cam_vid_src_ord ADD CONSTRAINT cam_vid_src_ord_camera_template_fkey FOREIGN KEY (camera_template)
        REFERENCES iris._camera_template (name) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION;
		
--ALTER TABLE iris.cam_vid_src_ord ADD CONSTRAINT cam_vid_src_ord_src_template_fkey FOREIGN KEY (src_template)
--        REFERENCES iris.vid_src_template (name) MATCH SIMPLE
--        ON UPDATE NO ACTION
--        ON DELETE NO ACTION;
		
CREATE OR REPLACE VIEW iris.camera
 AS
 SELECT c.name,
    c.geo_loc,
    d.controller,
    d.pin,
    c.notes,
    c.cam_num,
    c.encoder_type,
    c.encoder,
    c.enc_mcast,
    c.encoder_channel,
    c.publish,
    c.video_loss,
	c.cam_template
   FROM iris._camera c
     JOIN iris._device_io d ON c.name::text = d.name::text;

INSERT INTO iris.sonar_type(name) VALUES ('cam_vid_src_ord');
INSERT INTO iris.sonar_type(name) VALUES ('camera_template');
INSERT INTO iris.sonar_type(name) VALUES ('vid_src_template');

INSERT INTO iris.privilege (name,capability,type_n,obj_n,attr_n,group_n,write) VALUES
						   ('PRV_003F','camera_tab','camera_template','','','',false),
						   ('PRV_003G','camera_tab','cam_vid_src_ord','','','',false),
						   ('PRV_003H','camera_tab','vid_src_template','','','',false),
						   ('PRV_004A','camera_admin','camera_template','','','',true),
						   ('PRV_004B','camera_admin','cam_vid_src_ord','','','',true),
						   ('PRV_004C','camera_admin','vid_src_template','','','',true);
