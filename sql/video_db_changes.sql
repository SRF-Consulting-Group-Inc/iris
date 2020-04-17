INSERT INTO iris.system_attribute (name, value)
VALUES ('vid_connect_autostart', true),
		('vid_connect_fail_next_source', true),
		('vid_connect_fail_sec', 20),
		('vid_lost_timeout_sec', 10),
		('vid_reconnect_auto', true),
		('vid_reconnect_timeout_sec', 10);

CREATE TABLE iris.camera_template
(
	name character varying(20),
	notes text,
	label text,
	CONSTRAINT camera_template_pkey PRIMARY KEY (name)
);

ALTER TABLE iris.camera_template
    OWNER to tms;
	
CREATE TABLE iris.vid_src_template
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
	notes text,
	gst_stream boolean,
	CONSTRAINT vid_src_template_pkey PRIMARY KEY (name)
);

ALTER TABLE iris.vid_src_template
    OWNER to tms;
	
ALTER TABLE iris._camera ADD COLUMN cam_template character varying(20);
 
ALTER TABLE iris._camera ADD CONSTRAINT _camera_cam_template_fkey FOREIGN KEY (cam_template)
        REFERENCES iris.camera_template (name) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION;

CREATE TABLE iris.cam_vid_src_ord
(
	name character varying(20),
	camera_template character varying(20),
	src_order integer,
	src_template character varying(20),
	CONSTRAINT camera_vid_src_order_pkey PRIMARY KEY (name)
);
ALTER TABLE iris.cam_vid_src_ord
    OWNER to tms;
	
ALTER TABLE iris.camera_vid_src_order ADD CONSTRAINT camera_vid_src_order_camera_template_fkey FOREIGN KEY (camera_template)
        REFERENCES iris.camera_template (name) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION;
		
ALTER TABLE iris.camera_vid_srf_order ADD CONSTRAINT camera_vid_src_order_src_template_fkey FOREIGN KEY (source_template)
        REFERENCES iris.vid_src_template (name) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION;
		
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

INSERT INTO iris.privileges VALUES('PRV_003F','camera_tab','camera_template',,,,false),
								('PRV_003G','camera_tab','cam_vid_src_ord',,,,false),
								('PRV_003H','camera_tab','vid_src_template',,,,false),
								('PRV_004A','camera_admin','camera_template',,,,true),
								('PRV_004B','camera_admin','cam_vid_src_ord',,,,true),
								('PRV_004C','camera_admin','vid_src_template',,,,true);
