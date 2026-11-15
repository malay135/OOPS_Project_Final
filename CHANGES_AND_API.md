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
