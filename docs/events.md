# Events

## Database Event Tables

There are a number of event tables in the [database] for logging different
types of events.  Most of these tables have a **view** in the public DB schema.

Table Name                | View
--------------------------|-------------------------
`action_plan_event`       | `action_plan_event_view`
`alarm_event`             | `alarm_event_view`
`beacon_event`            | `beacon_event_view`
`brightness_sample`       | N/A
`camera_switch_event`     | `camera_switch_event_view`
`camera_video_event`      | `camera_video_event_view`
`cap_alert`               | N/A
`client_event`            | `client_event_view`
`comm_event`              | `comm_event_view`
`detector_event`          | `detector_event_view`
`gate_arm_event`          | `gate_arm_event_view`
`incident`                | `incident_view`
`incident_update`         | `incident_update_view`
`meter_event`             | `meter_event_view`
`price_message_event`     | `price_message_event_view`
`sign_event`              | `sign_event_view`
`tag_read_event`          | `tag_read_event_view`
`travel_time_event`       | `travel_time_event_view`
`weather_sensor_sample`   | `weather_sensor_sample_view`
`weather_sensor_settings` | `weather_sensor_settings_view`

## Event Configuration

Each event type can be configured independently.

**Name**: Table name of event type

**Enable Store**: Enable/disable storing event type

**Enable Purge**: Enable/disable purging event type

**Purge Days**: Number of days to keep events before purging


[database]: database.html