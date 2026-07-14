# Timezone 운영 정책

## 기준

- MVP의 애플리케이션 및 DB 날짜·시간 기준은 `Asia/Seoul`이다.
- 애플리케이션 코드와 Spring 설정에서 timezone을 강제로 변경하지 않는다.
- 배포 환경과 PostgreSQL 접속 Role 설정으로 timezone을 보장한다.
- `LocalDateTime`과 PostgreSQL `TIMESTAMP WITHOUT TIME ZONE`에는 timezone 정보가 포함되지 않으므로 배포 전후 검증이 필수다.

## 애플리케이션 실행 환경

애플리케이션을 실행하는 서버 또는 컨테이너에 다음 환경 변수를 설정한다.

```text
TZ=Asia/Seoul
JAVA_TOOL_OPTIONS=-Duser.timezone=Asia/Seoul
```

- `TZ`는 OS 및 컨테이너의 기본 timezone을 지정한다.
- `JAVA_TOOL_OPTIONS`는 JVM의 `user.timezone`을 지정한다.
- 실행 환경에 timezone 데이터가 설치되어 있어야 한다.
- 로컬, 개발, 운영, 배치 실행 환경에 같은 값을 적용한다.

## PostgreSQL

애플리케이션 전용 PostgreSQL Role에 timezone을 설정한다.

```sql
ALTER ROLE brifo_app SET timezone TO 'Asia/Seoul';
```

`brifo_app`은 실제 애플리케이션 접속 Role 이름으로 변경한다.

Role 단위 설정을 우선 사용한다. DB 전체 설정이 필요한 경우에만 다음과 같이 설정한다.

```sql
ALTER DATABASE brifo SET timezone TO 'Asia/Seoul';
```

설정 변경 후 기존 connection pool의 연결에는 바로 반영되지 않을 수 있으므로 애플리케이션 연결을 새로 생성해야 한다.

## 배포 검증

### JVM

배포 환경에서 JVM 속성을 확인한다.

```sh
java -XshowSettings:properties -version
```

출력의 `user.timezone`이 `Asia/Seoul`이어야 한다.

OS 또는 컨테이너 timezone도 확인한다.

```sh
date +%Z
```

예상 결과는 `KST`이다.

### PostgreSQL

애플리케이션이 사용하는 것과 동일한 Role로 접속하여 확인한다.

```sql
SHOW timezone;
```

예상 결과는 `Asia/Seoul`이다.

connection pool을 사용하는 실제 애플리케이션 연결에서도 같은 결과인지 확인한다.

## 배포 차단 조건

다음 중 하나라도 충족하면 배포를 완료하지 않는다.

- JVM `user.timezone`이 `Asia/Seoul`이 아니다.
- OS 또는 컨테이너 timezone이 KST가 아니다.
- 애플리케이션 DB Role의 `SHOW timezone` 결과가 `Asia/Seoul`이 아니다.
- 개발 서버, API 서버, 배치 서버가 서로 다른 timezone을 사용한다.

## 영향 범위

timezone이 다르면 다음 기능의 날짜 경계가 달라질 수 있다.

- 출석 보상의 일일 중복 판정과 연속 출석
- 오늘 생성한 decision 및 뉴스 카드 조회
- 주간·월간 AP 집계
- Diary 캘린더와 통계
- Agent 연속 근무일
- 약관 동의·철회 시각
- 알림 및 상대 시간 표시

특히 `attendance_rewards`의 `(user_id, created_at::date)` unique index는 DB 세션 timezone 정책과 함께 관리해야 한다.

## 변경 관리

- 배포 환경 변수나 PostgreSQL Role 설정을 변경할 때는 애플리케이션과 배치를 함께 검증한다.
- 서버 지역이나 컨테이너 이미지가 변경되면 timezone 설정을 다시 확인한다.
- 향후 시스템 전체를 UTC로 전환하려면 API 날짜 경계, DB 컬럼 타입, 일 단위 unique 제약을 함께 재설계한다.
