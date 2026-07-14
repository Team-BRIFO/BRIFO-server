## JPA Entity Rules

이 문서는 Flyway PostgreSQL 스키마와 JPA 엔티티를 함께 관리하기 위한 규칙을 정의합니다.

핵심 원칙:

> DB는 깨진 데이터가 들어오지 못하게 막고, JPA는 정상 도메인 객체를 만든다.


## Flyway Rules

Flyway SQL에는 DB가 반드시 보장해야 하는 규칙을 남깁니다.

- `PRIMARY KEY`
- `FOREIGN KEY`
- `NOT NULL`
- `UNIQUE`
- partial unique index
- 일반 index
- `CHECK`
- sequence
- DB 생성 식별자 default

## JPA Rules

JPA는 DDL 생성자가 아니라 Flyway 스키마에 대한 매핑 계층입니다.

`spring.jpa.hibernate.ddl-auto`는 `validate`를 사용합니다.

엔티티에는 다음 정보를 명시합니다.

- `@Table(name = "...")`
- `@Column(name = "...")`
- `nullable`
- `length`
- `precision`, `scale`
- `insertable`, `updatable`
- enum string 매핑
- JSONB 매핑
- 단방향 연관관계

엔티티에는 unique/index/check 제약을 중복 선언하지 않습니다. 해당 제약은 Flyway SQL에서 관리합니다.

## BaseEntity

`BaseEntity`는 공통 생성 시각만 가집니다.

```kotlin
@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseEntity {
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null
        protected set
}
```

`BaseEntity`에 `id`, `updatedAt`, `deletedAt`, `publicId`는 넣지 않습니다.

이유:

- id는 테이블별 sequence를 사용합니다.
- `updated_at`은 모든 테이블에 존재하지 않습니다.
- `deleted_at`은 soft delete 대상 테이블에만 존재합니다.
- `public_id`는 모든 테이블에 존재하지 않습니다.

## Id Rules

내부 id는 API에 노출하지 않습니다.

각 엔티티는 테이블별 sequence를 사용합니다.

```kotlin
@Id
@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "usersIdGenerator")
@SequenceGenerator(
    name = "usersIdGenerator",
    sequenceName = "users_id_seq",
    allocationSize = 50,
)
@Column(name = "id", nullable = false, updatable = false)
var id: Long? = null
    protected set
```

규칙:

- sequence 이름은 `{table_name}_id_seq`로 작성합니다.
- generator 이름은 `{EntityName}IdGenerator`로 작성합니다.
- `allocationSize = 50`을 사용합니다.
- id는 생성자와 팩토리 메소드 인자에서 제외합니다.
- id는 API 응답, 요청, cursor에 사용하지 않습니다.

## Public Id Rules

외부 노출 식별자는 `public_id`를 사용합니다.

PostgreSQL 최신 버전을 기준으로 UUID v7을 DB에서 생성합니다.

```sql
public_id UUID NOT NULL UNIQUE DEFAULT uuidv7()
```

엔티티에서는 DB default에 맡깁니다.

```kotlin
@Column(name = "public_id", nullable = false, insertable = false, updatable = false)
@Generated(event = [EventType.INSERT])
var publicId: UUID? = null
    protected set
```

규칙:

- `publicId`는 생성자와 팩토리 메소드 인자에서 제외합니다.
- `publicId`는 애플리케이션에서 생성하지 않습니다.
- insert 전 엔티티의 `publicId`는 null일 수 있습니다.
- insert 후 DB 생성값을 엔티티에 반영하기 위해 `@Generated(event = [EventType.INSERT])`를 사용합니다.
- API cursor에는 내부 id 대신 UUID v7 기반 `publicId`를 사용합니다.

## Default Value Rules

도메인 기본값은 엔티티에 둡니다.

예시:

```kotlin
@Column(name = "balance_ap", nullable = false)
var balanceAp: Int = DEFAULT_BALANCE_AP
    protected set
```

DB default는 운영 fallback으로 남길 수 있습니다. 단, 애플리케이션이 DB default에만 의존하는 도메인 값은 만들지 않습니다.

DB default에 맡기는 값:

- 내부 id
- `public_id`
- 직접 SQL fallback용 `created_at`
- 직접 SQL fallback용 `updated_at`

엔티티에서 초기화하는 값:

- 숫자 기본값
- boolean 기본값
- enum 상태 기본값
- 도메인 문구 기본값

## Time Rules

| DB 타입 | Kotlin 타입 |
| --- | --- |
| `TIMESTAMP` | `LocalDateTime` |
| `DATE` | `LocalDate` |

`created_at`은 `BaseEntity`의 `createdAt`으로 매핑합니다.

`updated_at`이 있는 엔티티는 개별 필드로 선언합니다.

```kotlin
@LastModifiedDate
@Column(name = "updated_at", nullable = false)
var updatedAt: LocalDateTime? = null
    protected set
```

도메인 사건 시각은 컬럼명을 유지합니다.

- `fetched_at`
- `crawled_at`
- `called_at`
- `agreed_at`
- `awarded_at`
- `learned_at`

이 값들은 `createdAt`으로 통일하지 않습니다.

## Soft Delete Rules

`deleted_at`이 있는 엔티티는 Hibernate soft delete를 사용합니다.

```kotlin
@SQLDelete(sql = "UPDATE users SET deleted_at = now() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
```

규칙:

- 삭제는 물리 삭제하지 않고 `deleted_at`을 갱신합니다.
- 일반 조회에서는 삭제된 row를 제외합니다.
- 삭제된 row까지 조회해야 하는 기능은 별도 native query 또는 별도 조회 전략을 검토합니다.
- soft delete 대상 테이블의 unique 제약은 필요한 경우 partial unique index로 작성합니다.

예시:

```sql
CREATE UNIQUE INDEX idx_users_provider_social_id_active_unique
    ON users (provider, social_id)
    WHERE deleted_at IS NULL;
```

## Enum Rules

enum은 항상 string으로 저장합니다.

```kotlin
@Enumerated(EnumType.STRING)
@Column(name = "provider", nullable = false, length = 20)
var provider: OAuthProvider
    protected set
```

규칙:

- enum class 이름은 `UpperCamelCase`를 사용합니다.
- enum 상수는 DB CHECK 값과 동일한 `UPPER_SNAKE_CASE`를 사용합니다.
- DB CHECK 제약은 Flyway SQL에 남깁니다.

## Relationship Rules

연관관계는 처음에는 단방향만 사용합니다.

```kotlin
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "user_id", nullable = false)
var user: User
    protected set
```

규칙:

- 기본 fetch는 `LAZY`입니다.
- `@OneToMany`는 필요해질 때 추가합니다.
- FK 제약은 Flyway SQL에서 관리합니다.
- cascade는 명확한 생명주기 종속이 있을 때만 사용합니다.

## JSONB Rules

JSONB 컬럼은 Hibernate JSON 타입으로 매핑합니다.

`news_cards.points`, `news_cards.keywords`는 `List<String>`으로 매핑합니다.

```kotlin
@JdbcTypeCode(SqlTypes.JSON)
@Column(name = "points", nullable = false, columnDefinition = "jsonb")
var points: List<String>
    protected set
```

구조가 고정되지 않은 JSONB는 `JsonNode` 사용을 검토합니다.

## Decimal Rules

DB의 `DECIMAL` 타입은 `BigDecimal`로 매핑합니다.

```kotlin
@Column(name = "price", nullable = false, precision = 12, scale = 2)
var price: BigDecimal
    protected set
```

`Double`은 저장 모델에 사용하지 않습니다.

이유:

- `Double`은 이진 부동소수점이라 십진수를 정확히 표현하지 못합니다.
- `BigDecimal`은 DB `DECIMAL`, `NUMERIC`과 의미가 맞습니다.

## Constructor Rules

엔티티는 `data class`로 만들지 않습니다.

생성 방식:

- 일반 `class`를 사용합니다.
- 주 생성자는 `private constructor`로 둡니다.
- 생성은 `companion object`의 팩토리 메소드로 처리합니다.
- 변경은 의미가 명확한 도메인 메소드로 처리합니다.
- 외부 setter는 열지 않습니다.

생성자와 팩토리 메소드에서 제외하는 값:

- `id`
- `publicId`
- `createdAt`
- `updatedAt`
- `deletedAt`
- 도메인 기본값으로 충분한 컬럼
- DB 또는 JPA가 자동 관리하는 컬럼

## Repository Rules

모든 테이블에 repository를 생성합니다.

```kotlin
interface UserRepository : JpaRepository<User, Long>
```

규칙:

- repository id 타입은 내부 id 타입인 `Long`입니다.
- API 계층에서는 내부 id를 노출하지 않습니다.
- 외부 조회는 `publicId` 기반 메소드를 사용합니다.
- soft delete 대상은 Hibernate restriction으로 일반 조회에서 삭제 row를 제외합니다.
