# Assignment and delegation changes

## Assignment behavior

New incidents are automatically assigned during creation. The policy is:
1. Find active technicians whose skills contain the incident category (case-insensitive).
2. If multiple technicians match, choose the one with the lowest active incident count.
3. If nobody matches the category, choose the active technician with the lowest active incident count.
4. If no active technician exists, the incident is created and remains unassigned.

## Endpoints

### Administrator override
`POST /api/assignments/override` (role: `ADMINISTRATOR`)

```json
{"incidentId": 12, "technicianId": 4}
```

### Technician delegation (needs admin approval)
`POST /api/assignments/{incidentId}/delegate` (role: `TECHNICIAN`) with `{"technicianId": 4}` creates a PENDING request. The incident stays with the requester; admins are notified.

- `GET /api/assignments/delegations?status=PENDING|APPROVED|REJECTED|CANCELLED|ALL` (ADMINISTRATOR)
- `GET /api/assignments/delegations/mine` (TECHNICIAN)
- `POST /api/assignments/delegations/{id}/approve` (ADMINISTRATOR): moves the incident to the target, updates workloads, notifies both technicians.
- `POST /api/assignments/delegations/{id}/reject` (ADMINISTRATOR): incident stays; requester is notified.

An admin override on an incident cancels its pending request. Only one pending request per incident.

### Automatic assignment compatibility endpoint
`POST /api/assignments?incidentId=12&strategy=SKILL_THEN_WORKLOAD` (role: `ADMINISTRATOR`)

This legacy endpoint now runs the automatic skill-first/workload-second policy. The old manual strategy selection should no longer be exposed as an assignment button in the client.

## Work-start authorization

Administrators receive HTTP 403 if they attempt to change an incident to `IN_PROGRESS`. Technicians can change status only for incidents currently assigned to their technician login. Other existing status rules remain in place.

## Notification visibility

Delegation notifications are addressed to the administrator username and returned by `GET /api/notifications` for that administrator. Existing notifications without a recipient remain visible as before.

## Employee notifications on assignment

The reporting employee gets exactly one in-app message for the first assignment ("Your incident '...' has been assigned to X."), and one in-app message plus one email whenever the technician later changes (admin override or approved delegation). Technicians without a linked login get no recipient-less (everyone-visible) alert.


## 2026-11-22 specification-compliance additions

Added endpoints for the remaining incident-management requirements:
- `PUT /api/incidents/{id}` for controlled incident detail updates.
- `GET/POST /api/incidents/{id}/notes` for additional information, investigation notes, support requests and responses.
- `GET/POST /api/incidents/{id}/feedback` for post-resolution employee feedback.
- `GET/POST/PUT /api/services` for affected IT service management.
- `GET/POST/PUT /api/categories` for administrator-managed incident categories.
- `GET/PUT /api/sla-policies` for configurable response/resolution/warning targets.
- `GET/PUT /api/users` for administrator user management.

The incident model now tracks response SLA, first response time, SLA warning/risk state, affected service and potential duplicates. Lifecycle transitions are enforced server-side. Audit status changes record previous and new state. Duplicate detection and SLA risk detection provide two explicit intelligent/automated features.


## 2026-11-22 extra features (assignment setting, acknowledge, suggested fixes)

### Admin chooses the assignment strategy
`GET /api/settings/assignment-strategy` and `PUT /api/settings/assignment-strategy` with `{"strategy": "LEAST_LOADED"}` (role: `ADMINISTRATOR`).
The choice is stored in the new `system_settings` table and used for every new incident:
- `SKILL_THEN_WORKLOAD` (default): the policy described above.
- `SKILL_ONLY`: only technicians with the matching skill; if nobody has it, the incident stays unassigned for an admin to assign.
- `LEAST_LOADED`: the technician with the fewest active incidents, whatever their skills.

Each value maps to one `AssignmentStrategy` class (Strategy pattern); `SkillThenWorkloadAssignment` combines the two existing strategies. The audit entry records which one was used (`AUTOMATIC_LEAST_LOADED`, ...). An unknown name returns 400.

### Technician acknowledges an incident
`POST /api/incidents/{id}/acknowledge` (role: `TECHNICIAN`, only the assigned technician, only while `OPEN`, only once).
It records `acknowledgedAt` (and `firstResponseAt` if not set yet, so it counts for the response SLA), writes an `ACKNOWLEDGED` audit entry and tells the reporting employee in the app. The incident stays `OPEN`. Starting work also fills `acknowledgedAt` if the technician skipped this step.

### Suggested fixes from similar incidents
`GET /api/incidents/{id}/suggestions` (roles: `TECHNICIAN`, `ADMINISTRATOR`) returns up to 3 resolved or closed incidents that look alike, best first:
`[{"incidentId": 2, "title": "...", "category": "SOFTWARE", "resolutionNotes": "...", "matchPercent": 82}]`.
The score is word overlap (Jaccard) of the titles (60%) and descriptions (40%), plus a bonus for the same category; matches under 20% are left out. `TextSimilarity` is shared with duplicate detection.

### Database
Hibernate adds the `incident.acknowledged_at` column and the `system_settings` table on start. Existing data is kept (tested on a copy of an older database).


## 2026-11-22 final additions (State pattern, affected users, SLA per service, workload limit)

### Incident lifecycle uses the State pattern
Package `i_itsm.state`: `IncidentState` (interface) with `OpenState`, `InProgressState`, `ResolvedState`, `ClosedState`, and `IncidentStates` to find the state for a status text.
Each state knows its allowed next statuses, whether it can be acknowledged or edited, whether it needs resolution notes, and what changes when an incident enters it (dates, clearing the resolution on reopen). `IncidentService.updateStatus` asks the current state instead of using a switch. Same rules as before: OPEN -> IN_PROGRESS -> RESOLVED -> CLOSED, and RESOLVED/CLOSED -> OPEN. Closing now also records `closedAt` (it was never set before).

### Number of affected users
New incident field `affectedUsers` (1 to 100000, default 1), sent on create and on `PUT /api/incidents/{id}`.
Priority: impact + urgency give the base, HIGH criticality raises it one level, then 10-99 affected people raise it one more level and 100 or more raise it two levels (never above P1).

### SLA per IT service
New table `service_sla_policies` (service + priority + response/resolution/warning minutes). It replaces the standard policy for incidents of that service and priority. Admin only:
- `GET /api/sla-policies/services`
- `PUT /api/sla-policies/services/{serviceId}/{priority}` with `{"responseTimeMinutes":10,"resolutionTimeMinutes":60,"warningTimeMinutes":15}`
- `DELETE /api/sla-policies/services/{serviceId}/{priority}` (back to the standard SLA)
Each incident stores `appliedSla`, e.g. "Payment Gateway P1 (service SLA)" or "Standard P3". Changing an incident's service recalculates its SLA.

### Technician workload limit
`GET /api/settings/workload-limit` (technician, admin) and `PUT /api/settings/workload-limit` with `{"maxActiveIncidents": 8}` (admin, 1-100, default 10).
Automatic assignment skips technicians at the limit. Admin override, delegation requests and delegation approvals to a full technician fail with the custom `TechnicianWorkloadExceededException` (HTTP 409, clear message). If nobody can take a new incident, it stays unassigned, an `ASSIGNMENT_FAILED` audit entry is written and administrators get an in-app notification.

### Bug fix
Saving SLA times on the Settings page returned "400 Bad Request": the JSON reader used the `SLAPolicy(priority, resolutionTimeHours)` constructor. That constructor is now marked `@JsonCreator(mode = DISABLED)`, and `SlaPolicyJsonTest` checks it.
