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
	
CREATE TABLE iris.vid_source_template
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
	CONSTRAINT vid_source_template_pkey PRIMARY KEY (name)
);

ALTER TABLE iris.vid_source_template
    OWNER to tms;
	
ALTER TABLE iris._camera ADD COLUMN cam_template character varying(20);
 
ALTER TABLE iris._camera ADD CONSTRAINT _camera_cam_template_fkey FOREIGN KEY (cam_template)
        REFERENCES iris.camera_template (name) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION;

CREATE TABLE iris.camera_vid_source_order
(
	name character varying(20),
	camera_template character varying(20),
	source_order integer,
	source_template character varying(20),
	CONSTRAINT camera_vid_source_order_pkey PRIMARY KEY (name)
);
ALTER TABLE iris.camera_vid_source_order
    OWNER to tms;
	
ALTER TABLE iris.camera_vid_source_order ADD CONSTRAINT camera_vid_source_order_camera_template_fkey FOREIGN KEY (camera_template)
        REFERENCES iris.camera_template (name) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION;
		
ALTER TABLE iris.camera_vid_source_order ADD CONSTRAINT camera_vid_source_order_source_template_fkey FOREIGN KEY (source_template)
        REFERENCES iris.vid_source_template (name) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION;