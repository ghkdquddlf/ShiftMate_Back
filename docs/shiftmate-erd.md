# ShiftMate ERD

This ERD is derived from the current JPA entity model under `src/main/java/com/example/shiftmate/domain/**/entity`.

- Naming assumption: column names follow Spring Boot's default snake_case physical naming strategy.
- Scope: Redis-backed refresh tokens, uploaded files, and external storage objects are intentionally excluded.
- Domain-split ERD with column descriptions: [shiftmate-domain-erd.md](./shiftmate-domain-erd.md)

```mermaid
erDiagram
    USERS {
        bigint id PK
        string email
        string name
        string password
        string phone_number
        string provider
        string provider_id
        boolean profile_completed
        datetime created_at
    }

    USER_DOCUMENTS {
        bigint id PK
        bigint user_id FK
        string type
        string original_file_name
        string content_type
        bigint size
        string file_path
        string file_url
        datetime created_at
        datetime updated_at
        datetime deleted_at
    }

    STORES {
        bigint id PK
        string name
        string location
        time open_time
        time close_time
        int n_shifts
        string brn
        bigint user_id FK
        string alias
        string template_type
        bigint monthly_sales
        string image_path
        string image_original_file_name
        string image_content_type
        bigint image_size
        datetime created_at
        datetime updated_at
        datetime deleted_at
    }

    STORE_MEMBERS {
        bigint id PK
        bigint store_id FK
        bigint user_id FK
        string role
        string member_rank
        string department
        int hourly_wage
        int min_hours_per_week
        string status
        datetime created_at
        datetime updated_at
        datetime deleted_at
    }

    SHIFT_TEMPLATES {
        bigint id PK
        bigint store_id FK
        string name
        time start_time
        time end_time
        int required_staff
        string template_type
        string shift_type
        string day_type
    }

    SHIFT_ASSIGNMENTS {
        bigint id PK
        bigint member_id FK
        bigint shift_template_id FK
        date work_date
        datetime updated_start_time
        datetime updated_end_time
        datetime created_at
        datetime updated_at
        datetime deleted_at
    }

    ATTENDANCE {
        bigint attendance_id PK
        bigint assignment_id FK
        datetime clock_in_at
        datetime clock_out_at
        string status
        string work_status
        datetime created_at
        datetime updated_at
        datetime deleted_at
    }

    EMPLOYEE_PREFERENCES {
        bigint id PK
        bigint member_id FK
        bigint shift_template_id FK
        string day_of_week
        string type
        datetime created_at
        datetime updated_at
        datetime deleted_at
    }

    OPEN_SHIFT_REQUEST {
        bigint id PK
        bigint store_id FK
        bigint shift_template_id FK
        date work_date
        string note
        string request_status
        datetime created_at
        datetime updated_at
        datetime deleted_at
    }

    OPEN_SHIFT_APPLY {
        bigint id PK
        bigint request_id FK
        bigint applicant_id FK
        string apply_status
        datetime created_at
        datetime updated_at
        datetime deleted_at
    }

    SUBSTITUTE_REQUESTS {
        bigint substitute_id PK
        bigint shiftassignment_id FK
        bigint requester_id FK
        string status
        string reason
        datetime created_at
        datetime updated_at
        datetime deleted_at
    }

    SUBSTITUTE_APPLICATIONS {
        bigint application_id PK
        bigint request_id FK
        bigint applicant_id FK
        string status
        datetime created_at
        datetime updated_at
        datetime deleted_at
    }

    PASSWORD_RESET_TOKENS {
        bigint id PK
        string token
        bigint user_id FK
        datetime expires_at
    }

    SIGNUP_EMAIL_VERIFICATIONS {
        bigint id PK
        string email
        string code
        datetime expires_at
        boolean verified
        datetime verified_at
    }

    USERS ||--o{ USER_DOCUMENTS : uploads
    USERS ||--o{ STORES : owns
    USERS ||--o{ STORE_MEMBERS : joins
    USERS ||--o{ PASSWORD_RESET_TOKENS : receives

    STORES ||--o{ STORE_MEMBERS : has
    STORES ||--o{ SHIFT_TEMPLATES : defines
    STORES ||--o{ OPEN_SHIFT_REQUEST : opens

    STORE_MEMBERS ||--o{ SHIFT_ASSIGNMENTS : works
    STORE_MEMBERS ||--o{ EMPLOYEE_PREFERENCES : sets
    STORE_MEMBERS ||--o{ OPEN_SHIFT_APPLY : applies
    STORE_MEMBERS ||--o{ SUBSTITUTE_REQUESTS : requests
    STORE_MEMBERS ||--o{ SUBSTITUTE_APPLICATIONS : applies

    SHIFT_TEMPLATES ||--o{ SHIFT_ASSIGNMENTS : instantiates
    SHIFT_TEMPLATES ||--o{ EMPLOYEE_PREFERENCES : referenced_by
    SHIFT_TEMPLATES ||--o{ OPEN_SHIFT_REQUEST : used_by

    SHIFT_ASSIGNMENTS ||--o| ATTENDANCE : tracks
    SHIFT_ASSIGNMENTS ||--o{ SUBSTITUTE_REQUESTS : requested_for

    OPEN_SHIFT_REQUEST ||--o{ OPEN_SHIFT_APPLY : receives
    SUBSTITUTE_REQUESTS ||--o{ SUBSTITUTE_APPLICATIONS : receives
```

## Notes

- `users.email` is unique.
- `users(provider, provider_id)` has a composite unique constraint for social login identities.
- `user_documents(user_id, type)` has a composite unique constraint, so each user keeps one document per type.
- `attendance.assignment_id` is unique, which makes `shift_assignments` to `attendance` effectively one-to-zero-or-one.
- Only one active substitute request per assignment is allowed by service logic, but it is not enforced by a database unique constraint.
- Duplicate open-shift and substitute applications are prevented in service logic, not by a database unique constraint.
- Approving an open-shift application creates a new `shift_assignments` row, but there is no direct foreign key from `open_shift_request` or `open_shift_apply` to that new assignment.
