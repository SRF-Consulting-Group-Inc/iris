# NDDOT Variable Speed Limits

NDDOT variable speed limit (VSL) signs post a speed limit calculated from
[weather sensor] data and traffic speeds.  For each VSL sign, IRIS calculates
a _recommended speed_, applies approval rules, and posts the speed to the sign
using the `[vsl]` [action tag].

Nothing here is used unless the `nddot_vsl_enable` [system attribute] is
`true`.

## Setup

1. Run `sql/add-nddot-vsl.sql` once on the database.  It does not change the
   database version.
2. Set `nddot_vsl_enable` to `true`, then restart the server.  Clients must
   log in again.
3. Add `#VSL` to the notes of each VSL sign.  This is the reserved VSL
   [hashtag] for [DMS], used by the action plan to find VSL signs.
4. On the sign's **VSL** tab, press **Create**, then set:
   - **Hashtag**: the sign's own hashtag, such as `#Vsl255`
   - **Minimum Speed** and **Maximum Speed** (mph)
   - **Approval Mode** (see [approval](#approval))
5. Add the sign's hashtag to the notes of its weather sensors, and of any
   [detector]s used for traffic speed.  A sign can have one or more weather
   sensors.  Detector notes are limited to 32 characters, so keep the hashtag
   short.
6. Create an [action plan] with:
   - a [message pattern] containing the `[vsl]` tag (such as one named `VSL`
     with MULTI `[vsl]`)
   - a [device action] with the `#VSL` hashtag, the deployed phase, and the
     message pattern **name**

The **VSL** tab shows warnings for missing setup, such as no `#VSL` hashtag in
the sign's notes or no weather sensors with the sign's hashtag.

## Calculation

The calculation runs every `nddot_vsl_cycle_rate` minutes, after the latest
traffic data is collected.  For each VSL sign:

1. Find the weather sensors with the sign's hashtag, and skip any which
   [can't be used](#faults)
2. Choose an equation for each sensor with the [decision
   tree](#decision-tree)
3. Calculate a _candidate speed_, combined with the traffic speed (if any),
   and no higher than the sign's maximum speed
4. Take the lowest candidate speed of all sensors
5. Round down to a multiple of `nddot_vsl_speed_rounding`
6. Keep the result within the sign's minimum and maximum speeds

The _traffic speed_ is the lowest 30-second speed of the sign's detectors.
Detectors which are not sampling, or do not measure speed, are not used.

### Surface Categories

Each weather sensor's pavement surface status is grouped into a category.  Any
other status makes the sensor unusable.

Category       | Surface status
---------------|---------------------------------------------------------
Dry            | `dry`
Wet            | `traceMoisture`, `wet`, `chemicallyWet`, `dew`, `absorption`, `absorptionAtDewpoint`
Slick in spots | `iceWatch`, `snowWatch`, `frost`
Slick          | `iceWarning`, `snowWarning`

### Decision Tree

The branches are checked in order, and the first match is used.  Slick
pavement (including slick in spots) has a maximum speed of 65 mph.

Surface | Condition                         | Equation
--------|-----------------------------------|----------------
Dry     | humidity ≥ 95%                    | use wet branches
Dry     | visibility > 345 ft               | 0
Dry     | visibility ≤ 345 ft               | 1
Wet     | surface temperature ≤ 32 °F       | use slick branches
Wet     | visibility ≤ 420 ft               | 2
Wet     | surface temperature > 36 °F       | 0
Wet     | humidity < 55%                    | 4
Wet     | otherwise                         | 5
Slick   | visibility ≤ 520 ft               | 3
Slick   | humidity < 55%                    | 4
Slick   | humidity < 90%                    | 5
Slick   | otherwise                         | 6

### Equations

Equation | Speed (mph)                          | Traffic speed
---------|--------------------------------------|--------------
0        | sign's maximum speed                 | not used
1        | (√(13.47 + 0.12 V) − 3.67) / 0.06 + 5 | ceiling
2        | (√(13.47 + 0.2 V) − 3.67) / 0.1 + 5   | ceiling
3        | (√(13.47 + 0.48 V) − 3.67) / 0.24 + 5 | ceiling
4        | 0.5 T + 0.3 W + 15                   | blend
5        | 0.35 T + 0.35 W + 0.3 H              | blend
6        | 0.35 T + 0.15 W + 0.5 H              | blend

- V: visibility (ft)
- T: 40.5 e^(0.01 S), where S is surface temperature (°F)
- W: 75 − 0.031 A^1.63, where A is average wind speed (mph)
- H: 457 − 15.152 R + 0.199 R² − 0.0009 R³, where R is relative humidity
  (percent)

A traffic speed **ceiling** limits the result to the traffic speed.  A
**blend** is 70 percent traffic speed and 30 percent equation result.

## Approval

Each sign has an **approval mode**:

Mode           | Description
---------------|-----------------------------------------------------------
Manual         | Every change needs operator approval
Semi-Automated | A change is posted after `nddot_vsl_review_timeout` minutes, unless an operator rejects it
Automated      | Every change is posted without approval

The approval rules are checked in order, comparing the recommended speed to
the posted speed:

1. **Minimum change**: a difference smaller than `nddot_vsl_min_speed_change`
   is ignored
2. **Threshold**: a change at or above `nddot_vsl_increase_threshold` (or
   `nddot_vsl_decrease_threshold`) cancels all waiting periods, is not
   limited, and is processed right away
3. **Quiet period**: after an operator rejects a change, the posted speed is
   held for `nddot_vsl_quiet_period` minutes
4. **Rate limit**: changes are held until `nddot_vsl_increase_rate` (or
   `nddot_vsl_decrease_rate`) minutes after the last posted change
5. **Step limit**: a change is limited to `nddot_vsl_increase_limit` (or
   `nddot_vsl_decrease_limit`)

A sign with no posted speed skips these rules, but still follows its approval
mode.

With the default values, neither step limit ever reduces a change.  Speeds are
multiples of the rounding increment, so every change below a threshold
(15 mph) is at most 10 mph, which is within both limits.  Changes at or above a
threshold are not limited.

Pending requests are listed in **View ➔ Message Signs ➔ VSL Approvals**, and
on the sign's **VSL** tab.  **Approve** posts the recommended speed right
away.  **Reject** keeps the posted speed and starts the quiet period.  Both
are logged with the operator's name.

## Override

Any operator message on a VSL sign is an _override_.  Speeds are still
calculated and logged, but not approved or posted.  Blanking the sign cancels
the override, and VSL resumes on the next calculation.

**VSL Override**, on a VSL sign's right-click menu, sends an operator message
with a chosen speed.  It uses the _situation_ lock reason, and is only
available for active signs.  Users with **Operate** access can only send text
matching the sign's [message lines], so add a message line for each override
speed.  Users with **Manage** access are not limited to message lines, but
banned words are still checked.

## Faults

A weather sensor can't be used if any of these is true:

- It has no sample, or the sample is older than `rwis_obs_age_limit_secs`
- It reports a pavement sensor error
- Its surface status has no [category](#surface-categories)
- Surface temperature, visibility, wind speed or humidity is missing

When none of a sign's weather sensors can be used, the sign has no recommended
speed, and keeps its posted speed.  Operators can still override it.

## Notifications

VSL notifications are written to RSS 2.0 feeds, which can be read by
GovDelivery.  Feeds are configured in **View ➔ Message Signs ➔ VSL Feeds**:

- **Name**: letters, digits and underscores.  The feed is written to
  `vsl_`*name*`.xml` in the XML output directory (`/var/www/html/iris_xml/`)
  every minute.
- **Title**: feed title
- **Hashtag**: the feed covers VSL signs with this hashtag in their notes.  A
  sign can be covered by more than one feed.
- **Requests**, **Overrides**, **Posted**, **Faults**: types to include

Type                   | Sent when                     | Details
-----------------------|-------------------------------|-------------------
VSL approval requested | A change needs approval       | Posted speed, recommended speed, weather sensor, road surface, surface temperature, visibility, wind speed, and (semi-automated) when it will be posted
VSL override           | An operator message appears   | Operator, override speed
VSL override cleared   | The operator message is gone  | Operator of the override, posted speed
VSL speed changed      | A speed is posted             | Previous speed, posted speed, approving operator
VSL fault              | A sensor or communication fault starts | Fault, last deployed speed, speed being deployed
VSL fault resolved     | The fault ends                | Fault, posted speed

Every notification includes the time, sign name and sign location.  A
communication fault is a sign which is offline or not active.  A new posted
speed during a communication fault sends another fault notification.

Approval requests and faults are _conditions_.  A repeat of the same condition
within `nddot_vsl_notify_repeat` minutes is not sent.  Speed changes and
overrides are always sent.

Notifications are kept in memory, up to 20 per feed.  After a server restart,
each feed starts empty, and faults found on the first calculation are sent
again.

## Events

VSL events are stored in the `event.vsl_event` table (`vsl_event_view`), with
the sign, speed and operator.  They are not purged unless purging is enabled
for `vsl_event` in [event configuration].

Event           | Description
----------------|------------------------------------------
VSL Pending     | A change needs approval
VSL Approved    | An operator approved a change
VSL Rejected    | An operator rejected a change
VSL Posted      | A speed was posted
VSL Unavailable | No weather sensor can be used
VSL Available   | A weather sensor can be used again

Calculation details for each sign are written to the `vsl` debug log, if the
`/var/log/iris/vsl` file exists.

## Permissions

The `vsl_sign` and `vsl_feed` resource types are based on `dms`, so
[permissions] for DMS also apply.  Hashtag permissions on a sign also apply to
its VSL settings.

Action                          | Access level
--------------------------------|-------------
View VSL settings and approvals | View
Approve or reject a change      | Operate
VSL Override                    | Operate
Change VSL sign or feed settings | Configure

## System Attributes

System Attribute               | Default | Description
-------------------------------|---------|------------------------------------
`nddot_vsl_cycle_rate`         | 1       | Minutes between calculations
`nddot_vsl_decrease_limit`     | 15      | Largest decrease in one change, except threshold changes (mph)
`nddot_vsl_decrease_rate`      | 1       | Minutes between changes when decreasing speed
`nddot_vsl_decrease_threshold` | 15      | Smallest decrease which cancels waiting periods (mph)
`nddot_vsl_enable`             | false   | Enable NDDOT VSL signs (requires server restart)
`nddot_vsl_increase_limit`     | 10      | Largest increase in one change, except threshold changes (mph)
`nddot_vsl_increase_rate`      | 2       | Minutes between changes when increasing speed
`nddot_vsl_increase_threshold` | 15      | Smallest increase which cancels waiting periods (mph)
`nddot_vsl_min_speed_change`   | 5       | Smallest difference which triggers a change (mph)
`nddot_vsl_notify_repeat`      | 30      | Minutes to suppress repeat notifications for an unchanged condition (0 to never suppress)
`nddot_vsl_quiet_period`       | 10      | Minutes to hold the posted speed after an operator rejects a change
`nddot_vsl_review_timeout`     | 2       | Minutes to wait for approval in semi-automated mode
`nddot_vsl_speed_rounding`     | 5       | Recommended speeds are rounded down to a multiple of this value (mph)

## Turning VSL Off

Deactivate the VSL action plan **before** setting `nddot_vsl_enable` to
`false`, then restart the server.  With VSL off, the `[vsl]` tag has no posted
speed, so messages using it are not displayed.  VSL settings and events stay
in the database.


[action plan]: action_plans.html
[action tag]: action_plans.html#action-tags
[detector]: vehicle_detection.html
[device action]: action_plans.html#device-actions
[DMS]: dms.html
[event configuration]: events.html#configuration
[hashtag]: hashtags.html
[message lines]: message_patterns.html#message-lines
[message pattern]: message_patterns.html
[permissions]: permissions.html
[system attribute]: system_attributes.html
[weather sensor]: weather_sensors.html
