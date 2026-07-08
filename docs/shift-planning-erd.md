# ShiftMate Shift Planning ERD

직원 선호도, 가게 시프트 템플릿, 실제 시간표 생성에 필요한 테이블만 다시 정리한 문서입니다.

- 기준 소스
  - `src/main/java/com/example/shiftmate/domain/employeePreference/entity/EmployeePreference.java`
  - `src/main/java/com/example/shiftmate/domain/shiftTemplate/entity/ShiftTemplate.java`
  - `src/main/java/com/example/shiftmate/domain/shiftAssignment/entity/ShiftAssignment.java`
  - `src/main/java/com/example/shiftmate/domain/store/entity/Store.java`
  - `src/main/java/com/example/shiftmate/domain/storeMember/entity/StoreMember.java`
- 컬럼명 전제: Spring Boot 기본 snake_case 네이밍 전략 기준

## 핵심 ERD

```mermaid
erDiagram
    "STORES (매장)" {
        bigint id PK "매장 PK"
        string name "매장명"
        time open_time "영업 시작 시각"
        time close_time "영업 종료 시각"
        int n_shifts "하루 시프트 개수"
        string template_type "템플릿 운영 방식(COSTSAVER/HIGHSERVICE)"
    }

    "STORE_MEMBERS (매장 직원)" {
        bigint id PK "매장 멤버 PK"
        bigint store_id FK "소속 매장 FK"
        bigint user_id FK "실제 사용자 FK"
        string department "부서(HALL/KITCHEN)"
        int hourly_wage "시급"
        int min_hours_per_week "최소 주간 근무 시간"
        string status "멤버 상태(INVITED/ACTIVE)"
        datetime deleted_at "소프트 삭제 시각"
    }

    "SHIFT_TEMPLATES (시프트 템플릿)" {
        bigint id PK "시프트 템플릿 PK"
        bigint store_id FK "소속 매장 FK"
        string name "템플릿 이름(예: 오픈/미들/마감)"
        time start_time "기준 시작 시각"
        time end_time "기준 종료 시각"
        int required_staff "필요 인원 수(null이면 기본 1명)"
        string template_type "운영 타입(COSTSAVER/HIGHSERVICE)"
        string shift_type "시프트 성격(NORMAL/PEAK)"
        string day_type "요일 유형(WEEKDAY/HOLIDAY)"
    }

    "EMPLOYEE_PREFERENCES (직원 선호도)" {
        bigint id PK "직원 선호도 PK"
        bigint member_id FK "선호도를 등록한 직원 FK"
        bigint shift_template_id FK "대상 템플릿 FK"
        string day_of_week "적용 요일(MONDAY~SUNDAY)"
        string type "선호도 유형(UNAVAILABLE/NATURAL/PREFERRED)"
        datetime created_at "생성 시각"
        datetime updated_at "수정 시각"
        datetime deleted_at "소프트 삭제 시각"
    }

    "SHIFT_ASSIGNMENTS (실제 시간표)" {
        bigint id PK "실제 생성된 시간표 PK"
        bigint member_id FK "배정된 직원 FK"
        bigint shift_template_id FK "기반 템플릿 FK"
        date work_date "실제 근무 날짜"
        datetime updated_start_time "확정 시작 시각"
        datetime updated_end_time "확정 종료 시각"
        datetime created_at "생성 시각"
        datetime updated_at "수정 시각"
        datetime deleted_at "소프트 삭제 시각"
    }

    "STORES (매장)" ||--o{ "STORE_MEMBERS (매장 직원)" : has
    "STORES (매장)" ||--o{ "SHIFT_TEMPLATES (시프트 템플릿)" : defines
    "STORE_MEMBERS (매장 직원)" ||--o{ "EMPLOYEE_PREFERENCES (직원 선호도)" : sets
    "SHIFT_TEMPLATES (시프트 템플릿)" ||--o{ "EMPLOYEE_PREFERENCES (직원 선호도)" : referenced_by
    "STORE_MEMBERS (매장 직원)" ||--o{ "SHIFT_ASSIGNMENTS (실제 시간표)" : assigned_to
    "SHIFT_TEMPLATES (시프트 템플릿)" ||--o{ "SHIFT_ASSIGNMENTS (실제 시간표)" : generated_from
```

## 관계 해석

- `stores` 1 : N `shift_templates`
  - 한 매장은 여러 시프트 템플릿을 가질 수 있습니다.
- `stores` 1 : N `store_members`
  - 한 매장에는 여러 직원이 소속됩니다.
- `store_members` 1 : N `employee_preferences`
  - 한 직원은 여러 템플릿/요일 조합에 대해 선호도를 등록할 수 있습니다.
- `shift_templates` 1 : N `employee_preferences`
  - 하나의 템플릿에 대해 여러 직원이 선호도를 가질 수 있습니다.
- `store_members` 1 : N `shift_assignments`
  - 한 직원은 여러 실제 근무 배정을 가질 수 있습니다.
- `shift_templates` 1 : N `shift_assignments`
  - 실제 배정은 특정 템플릿을 기반으로 생성됩니다.

## 시간표 생성 로직 기준 메모

`ShiftAssignmentService.createSchedule()` 기준으로 보면 실제 시간표 생성은 아래 규칙을 따릅니다.

1. 생성 권한은 해당 매장의 `MANAGER` 만 가집니다.
2. 시작일은 반드시 월요일이어야 합니다.
3. 매장에 템플릿이 하나 이상 있어야 합니다.
4. 선호도는 `UNAVAILABLE` 을 제외한 데이터만 후보로 사용합니다.
5. 배정 가능한 직원은 `deleted_at IS NULL` 이고 `status` 가 `ACTIVE` 인 멤버입니다.
6. `required_staff` 가 비어 있으면 템플릿당 기본 1명을 배정 대상으로 봅니다.
7. 먼저 `min_hours_per_week` 가 부족한 직원부터 우선 배정합니다.
8. 같은 조건이면 `PREFERRED` 가 `NATURAL` 보다 우선합니다.
9. 같은 날짜에 기존 배정 시간과 겹치면 중복 배정하지 않습니다.
10. 실제 생성 결과는 `shift_assignments` 에 저장됩니다.

## 참고

- 전체 도메인 분리 ERD: [shiftmate-domain-erd.md](./shiftmate-domain-erd.md)
- 전체 상세 ERD: [shiftmate-erd.md](./shiftmate-erd.md)
