-- Restore the Pembina LCS array after the 5.69 migration removed its lane DMSs.
--
-- Run with the IRIS server STOPPED (it caches these objects in memory), and
-- deploy a build of nd-5.87.0 that includes "ntcip: Fix LCS lane lookup and
-- send lane index" before starting it again.  Lane-to-sign mapping and
-- locations come from the 5.68 dump (tms-20260825.sql.gz).
--
--   psql -v ON_ERROR_STOP=1 -f pembina_lcs_restore.sql tms
--
-- EFFECT ON THE SIGNS: action plan Pembina_LCS (active, phase lane_open)
-- targets #PembinaLCS, so on the first poll after IRIS starts it SENDS
-- Pembina_LCS_Open ([g3,1,1]) to all three lane signs, and again daily at
-- 7 a.m. (time action Pembina_LCS_1).  Confirm on camera that all three
-- lanes show Open before running this.

BEGIN;

-- Preconditions: the lane DMSs are gone, the LCS is where the migration
-- left it, and ctl_85 pin 2 / pins 3-5 on each lane controller are free.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM iris.controller_io WHERE name IN
               ('DM-D6-I-29-600', 'DM-D6-I-29-610', 'DM-D6-I-29-620'))
    THEN
        RAISE EXCEPTION 'A Pembina lane DMS already exists';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM iris.lcs WHERE name = 'Pembina'
                   AND controller = 'ctl_85' AND pin = 1)
    THEN
        RAISE EXCEPTION 'LCS Pembina is not on ctl_85 pin 1';
    END IF;
    IF (SELECT count(*) FROM iris.lcs_state WHERE lcs = 'Pembina') <> 9 THEN
        RAISE EXCEPTION 'Expected 9 lcs_state rows for Pembina';
    END IF;
    IF EXISTS (SELECT 1 FROM iris.controller_io
               WHERE (controller = 'ctl_85' AND pin = 2)
                  OR (controller IN ('ctl_85', 'ctl_159', 'ctl_93')
                      AND pin IN (3, 4, 5)))
    THEN
        RAISE EXCEPTION 'A needed controller pin is already in use';
    END IF;
    -- The plan will drive the signs as soon as they exist; stop if it
    -- would send anything other than Open.
    IF EXISTS (SELECT 1 FROM iris.action_plan WHERE name = 'Pembina_LCS'
               AND active AND phase <> 'lane_open')
    THEN
        RAISE EXCEPTION 'Pembina_LCS plan is active but not in lane_open';
    END IF;
    -- The signs must be created with their sign config (see step 3)
    IF NOT EXISTS (SELECT 1 FROM iris.sign_config WHERE name = 'sc_80x64_1'
                   AND pixel_width = 80 AND pixel_height = 64)
    THEN
        RAISE EXCEPTION 'Sign config sc_80x64_1 (80x64) not found';
    END IF;
END $$;

-- 1) Locations for the lane signs (deleted along with the DMSs)
INSERT INTO iris.geo_loc (name, resource_n, roadway, road_dir, cross_street,
                          cross_dir, cross_mod, landmark, lat, lon)
VALUES
    ('DM-D6-I-29-600', 'dms', 'I-29', 1, NULL, 0, 0,
     '217.3:Pembina DMS 6612-3', 48.99816477229746, -97.23805708114203),
    ('DM-D6-I-29-610', 'dms', 'I-29', 1, NULL, 0, 0,
     '217.3:Pembina DMS 6612-2', 48.99822076237501, -97.23823674419886),
    ('DM-D6-I-29-620', 'dms', 'I-29', 1, NULL, 0, 0,
     '217.3:Pembina DMS 6612-1', 48.998276752389614, -97.23841640725568);

-- 2) Move the LCS array to pin 2 so lane 1's DMS can take ctl_85 pin 1
UPDATE iris.lcs SET pin = 2, lcs_type = 1, status = NULL
 WHERE name = 'Pembina';

-- 3) Recreate the lane DMSs WITH their sign config.  If it were left NULL,
--    the pixel check IRIS runs at startup (before its first config query)
--    would store an empty pixel_failures value, and IRIS would then reject
--    every message ("Malformed pixel_failures") and blank the signs until
--    the 4 a.m. settings job.  Sign details are what these signs reported
--    (dtl_14 / dtl_7); IRIS corrects them on its config query if needed.
INSERT INTO iris.dms (name, geo_loc, controller, pin, notes, preset,
                      sign_config, sign_detail)
VALUES
    ('DM-D6-I-29-600', 'DM-D6-I-29-600', 'ctl_85', 1,
     E'Border crossing LCS - Right lane (lane 1)\nDMS 6612-3\n'
     '#64High #Hidden #PembinaLCS #Small #Pembina',
     -- PRE_477 only if the LCS array does not already hold it (presets are
     -- unique per device, and 5.69 moved lane 1's preset to the array)
     (SELECT cp.name FROM iris.camera_preset cp WHERE cp.name = 'PRE_477'
         AND NOT EXISTS (SELECT 1 FROM iris.device_preset dp
                         WHERE dp.preset = cp.name)),
     'sc_80x64_1',
     (SELECT name FROM iris.sign_detail WHERE name = 'dtl_14')),
    ('DM-D6-I-29-610', 'DM-D6-I-29-610', 'ctl_159', 1,
     E'Border crossing LCS - Middle lane (lane 2)\nDMS 6612-2\n'
     '#64High #Hidden #PembinaLCS #Small #Pembina',
     NULL, 'sc_80x64_1',
     (SELECT name FROM iris.sign_detail WHERE name = 'dtl_14')),
    ('DM-D6-I-29-620', 'DM-D6-I-29-620', 'ctl_93', 1,
     E'Border crossing LCS - Left lane (lane 3)\nDMS 6612-1\n'
     '#64High #Hidden #PembinaLCS #Small #Pembina',
     NULL, 'sc_80x64_1',
     (SELECT name FROM iris.sign_detail WHERE name = 'dtl_7'));

-- 4) Point each lane's states at that lane's controller, with distinct pins
--    (Dark = 3, Open = 4, Closed = 5) and the message pattern to display.
--    Dark has no pattern: IRIS blanks the sign by releasing its lock.
UPDATE iris.lcs_state
   SET controller = CASE lane WHEN 1 THEN 'ctl_85'
                              WHEN 2 THEN 'ctl_159'
                              WHEN 3 THEN 'ctl_93' END,
       pin = CASE indication WHEN 1 THEN 3 WHEN 2 THEN 4 WHEN 5 THEN 5 END,
       msg_pattern = CASE indication WHEN 2 THEN 'Pembina_LCS_Open'
                                     WHEN 5 THEN 'Pembina_LCS_Closed' END,
       msg_num = NULL
 WHERE lcs = 'Pembina';

-- Check: every state should now map to a DMS on the same controller
SELECT ls.lane, ls.indication, ls.controller, ls.pin, d.name AS dms,
       ls.msg_pattern, mp.multi
  FROM iris.lcs_state ls
  LEFT JOIN iris.dms d ON d.controller = ls.controller
  LEFT JOIN iris.msg_pattern mp ON mp.name = ls.msg_pattern
 WHERE ls.lcs = 'Pembina'
 ORDER BY ls.lane, ls.indication;

COMMIT;
