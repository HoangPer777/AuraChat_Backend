# AuraChat Admin API

All endpoints use `/api/admin`, require a Bearer token belonging to an active user with role `ADMIN`, and return the standard `DataResponse` envelope.

## Bootstrap the first administrator

Set these environment variables before starting the backend:

```env
ADMIN_EMAIL=admin@example.com
ADMIN_PASSWORD=a-strong-password
```

If the email already exists, that account is promoted to `ADMIN` and activated. If it does not exist, both variables are required and the password must contain at least 8 characters. No default administrator is created.

After changing an existing account's role, log in again so the frontend user state contains the new role.

## Endpoints

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/admin/users?page=0&size=20&q=&status=&role=` | Search, filter and paginate users |
| GET | `/api/admin/users/{id}` | Get a user |
| PATCH | `/api/admin/users/{id}` | Update `displayName`, `bio`, or `role` |
| POST | `/api/admin/users/{id}/deactivate` | Change `ACTIVE` to `DEACTIVATED` |
| POST | `/api/admin/users/{id}/activate` | Change `DEACTIVATED` to `ACTIVE` |
| POST | `/api/admin/users/{id}/terminate` | Permanently soft-terminate an account |
| POST | `/api/admin/ban-ip` | Ban an IPv4 or IPv6 address |
| DELETE | `/api/admin/ban-ip/{ipAddress}` | Remove an IP ban |
| GET | `/api/admin/banned-ips?page=0&size=20` | List banned IPs |
| GET | `/api/admin/statistics?startDate=2026-06-01&endDate=2026-06-21` | Get dashboard statistics |
| GET | `/api/admin/media?page=0&size=20&q=&mediaType=&ownerId=&includeDeleted=` | Search and paginate media |
| GET | `/api/admin/media/stats` | Media storage statistics |
| GET | `/api/admin/media/{id}` | Get media detail with owner info |
| DELETE | `/api/admin/media/{id}` | Delete media from ImageKit and database |
| GET | `/api/admin/posts?page=0&size=20&q=&authorId=&includeDeleted=` | Search and paginate posts |
| GET | `/api/admin/posts/{id}` | Get post detail with engagement counts |
| GET | `/api/admin/posts/{id}/comments?page=0&size=50` | List post comments |
| DELETE | `/api/admin/posts/{id}` | Soft-delete a post |
| DELETE | `/api/admin/posts/comments/{commentId}` | Delete a comment (and replies if top-level) |
| GET | `/api/admin/moderation/flags?status=PENDING&contentType=POST` | Moderation queue |
| GET | `/api/admin/moderation/flags/stats` | Pending counts by type |
| POST | `/api/admin/moderation/flags/{id}/dismiss` | Dismiss flag, keep content |
| POST | `/api/admin/moderation/flags/{id}/remove-content` | Delete flagged content |
| POST | `/api/admin/moderation/flags/{id}/warn-user` | Warn user (FCM + warningCount) |
| POST | `/api/admin/moderation/media/{mediaId}/flag` | Manually flag media |
| GET/POST/DELETE | `/api/admin/moderation/keywords` | Manage sensitive keywords |

## Image moderation (Sightengine)

When `SIGHTENGINE_API_USER` and `SIGHTENGINE_API_SECRET` are set, every image upload is checked with Sightengine `nudity-2.1`. Sensitive images create a `MEDIA` flag with reason `SENSITIVE_IMAGE`.

## User state rules

```text
ACTIVE <-> DEACTIVATED
ACTIVE/DEACTIVATED -> TERMINATED
TERMINATED cannot be activated
```

Deactivation and termination revoke refresh tokens, remove Redis presence, reject REST JWT authentication, and reject new WebSocket connections. Administrators cannot deactivate, terminate, or remove the admin role from their own account.

## Statistics

- DAU is the number of distinct message senders in the selected date range.
- Message volume includes messages created in the selected date range.
- Online count comes from Redis heartbeat entries.
- The full response is cached in Redis for five minutes.
- Date boundaries use the `Asia/Ho_Chi_Minh` timezone and `endDate` is inclusive at API level.
