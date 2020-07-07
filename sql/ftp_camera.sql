\set ON_ERROR_STOP

SET SESSION AUTHORIZATION 'tms';

BEGIN;

DROP VIEW iris.camera CASCADE;
DROP FUNCTION iris.camera_insert();
DROP FUNCTION iris.camera_update();
DROP FUNCTION iris.camera_delete();

CREATE TABLE iris.camera_ftp_svr (
    name character varying(20) NOT NULL,
    ftp_username character varying(20),
    ftp_password character varying(64),
    ref_interval integer,
    ftp_path character varying(50),
    same_filename boolean,
    ftp_filename character varying(30)
);

ALTER TABLE ONLY iris.camera_ftp_svr
    ADD CONSTRAINT camera_ftp_svr_name_fkey FOREIGN KEY (name) REFERENCES iris._camera(name);

CREATE VIEW iris.camera AS
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
    e.ftp_username,
    e.ftp_password,
    e.ref_interval,
    e.ftp_path,
    e.same_filename,
    e.ftp_filename
   FROM iris._camera c
     JOIN iris._device_io d ON c.name = d.name
     JOIN iris.camera_ftp_svr e ON c.name = e.name;
     
CREATE VIEW camera_view AS
	SELECT c.name, c.notes, cam_num, encoder_type, c.encoder, c.enc_mcast,
	       c.encoder_channel, c.publish, c.video_loss, c.geo_loc,
	       l.roadway, l.road_dir, l.cross_mod, l.cross_street, l.cross_dir,
	       l.lat, l.lon,
	       c.controller, ctr.comm_link, ctr.drop_id, ctr.condition
	FROM iris.camera c
	LEFT JOIN geo_loc_view l ON c.geo_loc = l.name
	LEFT JOIN controller_view ctr ON c.controller = ctr.name;
GRANT SELECT ON camera_view TO PUBLIC;
     
INSERT INTO iris.encoding(id, description) values (6,'FTP');
 
CREATE FUNCTION iris.camera_insert() RETURNS TRIGGER AS
	$camera_insert$
BEGIN
	INSERT INTO iris._device_io (name, controller, pin)
	     VALUES (NEW.name, NEW.controller, NEW.pin);
	INSERT INTO iris._camera (name, geo_loc, notes, cam_num, encoder_type,
	            encoder, enc_mcast, encoder_channel, publish, video_loss)
	     VALUES (NEW.name, NEW.geo_loc, NEW.notes, NEW.cam_num,
	             NEW.encoder_type, NEW.encoder, NEW.enc_mcast,
	             NEW.encoder_channel, NEW.publish, NEW.video_loss);
	INSERT INTO iris.camera_ftp_svr (name, ftp_username, ftp_password,
		     ref_interval, ftp_path, same_filename, ftp_filename)
	     VALUES (NEW.name, NEW.ftp_username, NEW.ftp_password,
		     NEW.ref_interval, NEW.ftp_path, NEW.same_filename, NEW.ftp_filename);
	RETURN NEW;
END;
$camera_insert$ LANGUAGE plpgsql;

CREATE TRIGGER camera_insert_trig
    INSTEAD OF INSERT ON iris.camera
    FOR EACH ROW EXECUTE PROCEDURE iris.camera_insert();

CREATE FUNCTION iris.camera_update() RETURNS TRIGGER AS
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
	       video_loss = NEW.video_loss
	 WHERE name = OLD.name;
  	 UPDATE iris.camera_ftp_svr
  	    SET ftp_username = NEW.ftp_username,
  	        ftp_password = NEW.ftp_password,
  	        ref_interval = NEW.ref_interval,
  	        ftp_path = NEW.ftp_path,
  	        same_filename = NEW.same_filename,
  	        ftp_filename = New.ftp_filename
  	  WHERE name = OLD.name;
	RETURN NEW;
END;
$camera_update$ LANGUAGE plpgsql;

CREATE TRIGGER camera_update_trig
    INSTEAD OF UPDATE ON iris.camera
    FOR EACH ROW EXECUTE PROCEDURE iris.camera_update();
    
CREATE FUNCTION iris.camera_delete() RETURNS TRIGGER AS
	$camera_delete$
BEGIN
	DELETE FROM iris.camera_ftp_svr WHERE name = OLD.name;
	DELETE FROM iris._device_io WHERE name = OLD.name;
	IF FOUND THEN
		RETURN OLD;
	ELSE
		RETURN NULL;
	END IF;
END;
$camera_delete$ LANGUAGE plpgsql;

CREATE TRIGGER camera_delete_trig
    INSTEAD OF DELETE ON iris.camera
    FOR EACH ROW EXECUTE PROCEDURE iris.camera_delete();
    
COMMIT;
