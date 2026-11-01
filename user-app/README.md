## Java Spring Boot — UserApp (CWE-862 Demo)

A deliberately vulnerable demo web application that illustrates **CWE-862: Missing Authorization**. The app provides user management (create, edit, delete, password reset) with working authentication but **no authorization checks** on sensitive admin-style endpoints. Any authenticated user can perform actions that should be restricted to administrators or to the resource owner.

### Build (WSL/Linux or Windows)

```bash
mvn -q -f Examples/CWE-862/Java/user-app/pom.xml package
```

### Run

```bash
mvn -f Examples/CWE-862/Java/user-app/pom.xml spring-boot:run
```

Or:

```bash
java -jar Examples/CWE-862/Java/user-app/target/user-app-1.0.0.jar
```

Server runs at: **http://localhost:8080**

The database file is created at `./data/app.db` (SQLite) and persists across restarts.

### Seeded accounts

| Email             | Password  | Role  |
|-------------------|-----------|--------|
| admin@example.com | Admin123! | ADMIN |
| user@example.com  | User123!  | USER  |

These are created automatically on first startup if they do not exist.

### Walkthrough: Demonstrating CWE-862

1. **Sign in as a normal user**
   - Open http://localhost:8080 and sign in with `user@example.com` / `User123!`.
   - You are logged in as a **USER** (not an administrator).

2. **Use the dashboard to perform “admin” actions**
   - After login you are on the **Dashboard**. The banner states that the actions below should be restricted to administrators, but they are not.
   - **Create user:** In “Create user” enter a new email, password, choose role **ADMIN**, and click “Create user”. The new user is created with ADMIN role even though you are only a USER.
   - **Edit user:** In the “All users” table, change the admin’s role to USER (or change email/enabled) and click “Edit”. The change is applied; there is no check that you are an admin or the owner.
   - **Delete user:** Click “Delete” for another user (e.g. a user you created). The user is deleted without any ownership or role check.

3. **Request a password reset token for the admin**
   - In the “All users” table, find the row for `admin@example.com` and note the **ID**.
   - Click **“Request reset token”** for that user. The dashboard shows a message with a **reset token** and a link to the reset page.

4. **Change the admin’s password using the token**
   - Click **“Open reset page”** (or go to the reset confirmation page and paste the token).
   - Enter the token and a **new password**, then submit. The password for the admin account is updated.
   - Sign in with `admin@example.com` and the new password to confirm. A normal USER was able to reset an ADMIN’s password because the “request reset token” and “confirm reset” flows do not enforce authorization.

5. **Why this works (CWE-862)**
   - **Authentication** is in place: only logged-in users can reach the dashboard and the sensitive endpoints.
   - **Authorization** is missing: the app does **not** check that the current user is an ADMIN or the resource owner before:
     - Creating users (including ADMINs)
     - Editing any user’s email, role, or enabled state
     - Deleting any user
     - Requesting a password reset token for any user by ID
   - The “request reset token” and “confirm reset” endpoints are reachable by any authenticated user and operate on any user (by ID or by token), so a low-privilege user can take over the admin account. This is **CWE-862: Missing Authorization**.

### Endpoints (summary)

| Endpoint | Method | Auth | Intended restriction | Enforced? |
|----------|--------|------|------------------------|-----------|
| /login | GET | any | — | — |
| /auth/register | POST | any | — | — |
| /dashboard | GET | authenticated | — | — |
| /admin/users | POST | authenticated | admin only | **No** (vulnerable) |
| /admin/users/{id}/edit | POST | authenticated | admin or owner | **No** (vulnerable) |
| /admin/users/{id}/delete | POST | authenticated | admin only | **No** (vulnerable) |
| /admin/users/{id}/password-reset/request | POST | authenticated | admin or owner | **No** (vulnerable) |
| /password-reset/confirm | POST | any | — | — |
| /me/profile | POST | authenticated | current user only | **Yes** (only updates principal) |

### Tech stack

- **Backend:** Java 17, Spring Boot 3.2, Spring Security (session-based form login), Spring Data JPA.
- **Frontend:** Thymeleaf (server-rendered HTML).
- **Database:** SQLite (file `./data/app.db`), Flyway migrations, BCrypt for password hashes.

### Notes

- This app is for **security training and SAST/demo purposes only**. Do not use in production.
- Fixing CWE-862 would require adding proper authorization (e.g. role checks or resource-ownership checks) on every sensitive endpoint and ensuring password-reset flows cannot be abused by unauthorized users.
