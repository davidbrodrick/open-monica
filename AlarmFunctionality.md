# Introduction #

This document describes the functionality within MoniCA for dealing with alarms and notifications of alarm conditions.

# Defining Alarm Criteria #

Any point in MoniCA may [define](MonitorPointsFileFormat.md) zero or more AlarmCheck classes which examine the value of the point and may flag abnormal conditions. A simple example would be checking if a numeric value (eg. a pressure) is within an expected range or not.

The following table contains information about the different kinds of AlarmCheck classes available in MoniCA, their function, and what arguments they take.

| **Class** | **Description** | **Arguments** |
|:----------|:----------------|:--------------|
| Boolean | Flags an alarm if the point's value matches the specified boolean state. | **State:** "true" to flag a true boolean input as an alarm condition, "false" if a false condition represents an alarm.<br /> **Num Cycles:** An optional argument which specifies how many consecutive updates must be in the state before flagging the alarm (default is one). |
| ExternalMask | Aborts evaluation of any subsequent AlarmChecks defined for this point, depending on the current alarm state of another point. | **Mask Point:** The name of the point whose alarm status should be checked. **Mask Logic:** (Optional) If "true" then we mask the alarm if the mask point is currently alarming. If "false" then we mask the alarm if the mask point is not alarming (default is true). |
| Range | Checks if a numeric value is within a given range. | **Lower:** The lower bound for the numeric range. <br />**Upper:** The upper bound. <br />**In/Out:** Optional argument can be "true" to specify that a value outside of the range is the alarm condition (the default), or "false" if the input being inside the range is the alarm condition. <br />**Num Cycles:** An optional argument which specifies how many consecutive updates must be in the state before flagging the alarm (default is one). |
| StringMatch | Checks if the input matches any of a given set of strings. | **Num Cycles:** An optional argument which specifies how many consecutive updates must be in the state before flagging the alarm (default is one). This argument may be completely omitted. <br />**Mode:** If set to "true" then a matched string indicates the value is okay while a negative match indicates an alarm, if "false" then a matched string indicates an alarm. <br />**Strings:** Any other arguments specified are interpreted as the strings to check the value against. |
| ValueMatch | Checks if the numeric input is an exact match for the given value. | **Value:** A value (interpreted as a double) to be checked against. <br />**Mode:** If "true" then any value except the one specified is taken as an alarm, if "false" then a match is deemed to be an alarm. <br />**Num Cycles:** An optional argument which specifies how many consecutive updates must be in the state before flagging the alarm (default is one). |

# Notifications #

Normally defining the alarm criteria in itself will only cause the point to be highlighted if a user is inspecting it in the MoniCA GUI client. The Notification framework is intended to augment this by allowing automated actions, such as sending an email, to be taken based on a points alarm state.

If an automatic notification is desired then one or more Notifications can be defined in the [point's definition](MonitorPointsFileFormat.md).

At the moment there are only a couple of trivial Notification classes. We envisage richer functionality such as manipulating other points (ie. taking automated corrective action), etc. will be implemented over time.

| **Class** | **Description** | **Arguments** |
|:----------|:----------------|:--------------|
| EmailOnAlarm | Sends an email on the rising edge of an alarm state transition. | **Recipient:** The email address to send the email to, eg. "fred@email.com". <br />**Sender:** The email address of the sender - this field may be completely omitted and a default address will be used. <br />**Subject:** The subject line of the email, eg. "Warning from MoniCA". <br />**Body:** The body text of the email, eg. "Warning\nThe new value is $V". [Substitutions](Substitutions.md) may be used in the subject and email body. |
| EmailOnAlarmChange | Sends an email on the rising or falling edge of an alarm state transition. | The arguments are as per EmailOnAlarm. |

Please note that the Notifications will not be executed if the point is an an Acknowledged or Shelved state, described below.

# Priority Alarms #

In addition to just highlighting abnormal values and optionally taking notification actions, MoniCA also includes new functionality for managing high priority alarms which may require the attention of operator.

## Different Priorities ##

By default points have priority "NO PRIORITY" which means an alarm condition will not enter into the higher level alarm management machinery. This can be changed by defining an alarm priority in the [point definition](MonitorPointsFileFormat.md).

| **Integer** | **Priority** | **Description** |
|:------------|:-------------|:----------------|
| -1 | NO PRIORITY | Alarm conditions do not enter in to the alarm management systems at all. This is the default. |
| 0 | INFORMATION | An "alarm" for this point may not represent a condition which requires an action from the operator, but the operator may benefit from being made aware of the condition. |
| 1 | MINOR | An alarm of this category may require an action when it is convenient. |
| 2 | MAJOR | An alarm of this category may require an action to be taken as a matter of some urgency. |
| 3 | SEVERE | This category should be used for alarms which require an immediate action with utmost priority. |

## Guidance Message ##

Alarms with a priority include a "guidance" message which will be presented to the operator when the alarm is triggered. The guidance message can use [Substitutions](Substitutions.md) to incorporate the current values of other points within the system along with other information. The guidance message template is contained in the [point definition](MonitorPointsFileFormat.md).

## Acknowledgement ##

An active alarm may be acknowledged, which means that it has been noted by a duly authorised operator and corrective action will be undertaken (or perhaps that the alarm may be safely ignored). The action of acknowledging an alarm implies that the operator is taking responsibility for the alarm, as the alarm will no longer be in an active alarming state to alert other users of the system.

An alarm will remain acknowledged until the underlying alarm condition is resolved. If the alarm is triggered again in the future it will again be presented to the operator as an active alarm.

Note that acknowledging an alarm will prevent any Notifications that belong to the point from being executed while ever the alarm is in an acknowledged state.

## Shelving ##

An alarm may be shelved when in an active alarm state or preemptively, for instance if maintenance work is going to be carried out on a system which is going to trigger an alarm condition.

The action of shelving an active alarm implies that the operator is taking responsibility for the alarm.

A shelved alarm will remain shelved until such time that it is explicitly unshelved, even if it goes in and out of an alarm state during this time.

Note that shelving an alarm will prevent any Notifications that belong to the point from being executed while ever the alarm remains in the shelved state.

# Alarms in the MoniCA GUI Client #

The MoniCA client includes an AlarmManager which can be used to manipulate the state of alarms.

## Ignoring ##

In addition to being able to view, acknowledge and shelve alarms, the MoniCA client can also locally ignore an alarm. An alarm may be ignored, for instance, when an operator does not have the authority or area of responsibility to centrally acknowledge an alarm.

# Other Interfaces #

Information about alarms can be obtained and manipulated through the [MoniCA ASCII Interface](ClientASCII.md).

Complete control of the alarm machinery is also available through the [Web Client Javascript Library](WebClient.md).