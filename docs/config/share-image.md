# 결정일기 공유 이미지 설정

## 처리 흐름

`POST /api/diaries/{diaryId}/share-images`는 다음 순서로 동작한다.

1. 인증 사용자가 소유한 결정일기를 조회한다.
2. 저장된 공유 이미지 URL이 있으면 이미지를 다시 생성하지 않고 반환한다.
3. 결정일기 데이터를 PNG 이미지로 렌더링한다.
4. 결정일기 UUID를 사용하는 동일한 객체 키로 S3 호환 저장소에 업로드한다.
5. 짧은 잠금 트랜잭션에서 이미지 URL과 생성 시각을 기록한다.

실제 OS 공유 시트나 외부 서비스로 이미지를 전송하는 동작은 클라이언트가 담당한다.

## 필수 환경 변수

| 변수 | 설명 | 예시 |
| --- | --- | --- |
| `SHARE_IMAGE_BUCKET` | 공유 이미지 객체를 저장하는 버킷 | `brifo-share-images-dev` |
| `SHARE_IMAGE_PUBLIC_BASE_URL` | 업로드한 객체를 읽는 CDN 또는 공개 엔드포인트 | `https://cdn-dev.brifo.app` |

애플리케이션은 AWS SDK 기본 자격 증명 공급자 체인을 사용한다. EC2나 ECS에서는 액세스 키를 컨테이너에 직접 저장하지 않고, 버킷에 `PutObject` 권한이 있는 IAM Role을 사용한다.

개발 배포 스크립트는 다음 SSM Parameter Store 값을 컨테이너 환경 변수로 전달한다.

- `/brifo/dev/SHARE_IMAGE_BUCKET`
- `/brifo/dev/SHARE_IMAGE_PUBLIC_BASE_URL`

## 선택 환경 변수

| 변수 | 기본값 | 설명 |
| --- | --- | --- |
| `AWS_REGION` | `ap-northeast-2` | 저장소 리전 |
| `SHARE_IMAGE_KEY_PREFIX` | `diary-share-images` | 객체 키 접두사 |
| `SHARE_IMAGE_STORAGE_ENDPOINT` | 빈 값 | S3 호환 저장소 endpoint override |
| `SHARE_IMAGE_PATH_STYLE_ACCESS` | `false` | 로컬 S3 호환 저장소의 path-style 접근 여부 |
| `SHARE_IMAGE_FONT_FAMILY` | `Noto Sans CJK KR` | 이미지 렌더링에 사용할 한글 폰트 패밀리 |
| `SHARE_IMAGE_WIDTH` | `1200` | 생성 이미지 너비 |
| `SHARE_IMAGE_HEIGHT` | `630` | 생성 이미지 높이 |

버킷은 공개 쓰기와 공개 읽기를 모두 허용하지 않는다. 읽기는 CloudFront Origin Access Control(OAC)을 통해서만 제공한다.

배포 Docker 이미지에는 SIL Open Font License 1.1인 `Noto Sans CJK KR`이 `fonts-noto-cjk` 패키지로 번들링된다. 이미지 빌드 과정에서 `fc-match`로 설치 여부를 검증하며, 렌더러는 이 폰트 패밀리를 명시적으로 사용한다.

## AWS 개발 인프라 배포

`infra/share-image-dev.yaml`은 다음 리소스를 한 스택으로 관리한다.

- 공개 접근이 차단되고 서버 측 암호화와 버전 관리가 적용된 S3 버킷
- S3 Origin Access Control과 이미지 다운로드용 CORS 응답 정책을 사용하는 CloudFront 배포
- 기존 배포 EC2 Instance Role에 공유 이미지 경로 `PutObject`만 허용하는 IAM 정책
- `/brifo/dev/SHARE_IMAGE_BUCKET`, `/brifo/dev/SHARE_IMAGE_PUBLIC_BASE_URL` SSM 파라미터

AWS CLI 인증 후 기존 EC2 Instance Role 이름을 전달하여 배포한다.

```bash
./scripts/deploy-share-image-infra.sh <EC2_INSTANCE_ROLE_NAME>
```

스택명, 리전, 버킷명을 지정해야 하면 환경 변수를 사용한다.

```bash
STACK_NAME=brifo-dev-share-image \
AWS_REGION=ap-northeast-2 \
SHARE_IMAGE_BUCKET_NAME=brifo-share-images-dev \
./scripts/deploy-share-image-infra.sh <EC2_INSTANCE_ROLE_NAME>
```

CloudFormation 스택을 제거하더라도 S3 버킷은 데이터 보호를 위해 유지된다.
