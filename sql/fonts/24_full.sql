\set ON_ERROR_STOP
SET SESSION AUTHORIZATION 'tms';
BEGIN;

INSERT INTO iris.font (name, f_number, height, width, line_spacing,
    char_spacing, version_id) VALUES ('24_full', 16, 24, 0, 5, 4, 0);

COPY iris.glyph (name, font, code_point, width, pixels) FROM stdin;
24_full_48	24_full	48	12	H4P8cO4HwDwDwDwDwDwDwDwDwDwDwDwDwDwDwDwD4HcOP8H4
24_full_49	24_full	49	6	EMc88MMMMMMMMMMMMMMMMM//
24_full_50	24_full	50	12	P4f84OwHADADADADAHAOAcA4BwDgHAOAcA4AwAwAwAwA////
24_full_51	24_full	51	12	P4f84OwHADADADADADAHAOA8A8AOAHADADADADADwH4Of8P4
24_full_52	24_full	52	12	AMAcA8B8DsHMOMcM4MwMwMwM////AMAMAMAMAMAMAMAMAMAM
24_full_53	24_full	53	12	////wAwAwAwAwAwAwA4A/8f+AHADADADADADADADAH4O/8P4
24_full_54	24_full	54	12	H+P/cD4AwAwAwAwAwAwA/4/84OwHwDwDwDwDwDwD4HcOP8H4
24_full_55	24_full	55	11	///8AYAwBgDAGAcAwDgGAcAwDgGAcAwDgGAcAwDgGAMA
24_full_56	24_full	56	12	H4P8cO4HwDwDwDwD4HcOP8P8cO4HwDwDwDwDwDwD4HcOP8H4
24_full_57	24_full	57	12	H4P8cO4HwDwDwDwDwDwD4Hf/P/ADADADADADADADAHwO/8f4
\.

COMMIT;
