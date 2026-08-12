# 결정일기 공유 이미지 설정

## 처리 흐름

`POST /api/diaries/{diaryId}/share-images`는 다음 순서로 동작한다.

1. 인증 사용자가 소유한 결정일기를 조회한다.
2. 저장된 S3 객체 key가 없으면 PNG 이미지를 생성해 private S3 버킷에 업로드한다.
3. 객체 key는 결정일기 UUID를 사용한 `{diaryId}.png` 형식으로 DB에 저장한다.
4. 객체를 내려받을 수 있는 15분 유효 presigned URL을 반환한다.

기존 이미지가 있으면 다시 생성하지 않고 동일한 객체 key로 새로운 presigned URL만 발급한다. 실제 OS 공유 시트나 외부 서비스로 이미지를 전달하는 동작은 클라이언트가 담당한다.

## 애플리케이션 설정

| 변수 | 기본값 | 설명 |
| --- | --- | --- |
| `SHARE_IMAGE_BUCKET` | `disabled` | 공유 이미지 전용 private S3 버킷 |
| `AWS_REGION` | `ap-northeast-2` | S3 버킷 리전 |
| `SHARE_IMAGE_FONT_FAMILY` | `Noto Sans CJK KR` | 이미지 렌더링에 사용할 한글 폰트 패밀리 |
| `SHARE_IMAGE_WIDTH` | `1200` | 생성 이미지 너비 |
| `SHARE_IMAGE_HEIGHT` | `630` | 생성 이미지 높이 |

개발 배포 스크립트는 기존 SSM Parameter Store의 `/brifo/dev/SHARE_IMAGE_BUCKET` 값을 컨테이너 환경 변수로 전달한다.

## AWS 설정

버킷과 EC2 Instance Role은 애플리케이션과 별도로 구성한다.

- S3 버킷의 모든 public access를 차단한다.
- 애플리케이션 EC2 Instance Role에 버킷 객체에 대한 `s3:PutObject`, `s3:GetObject` 권한을 부여한다.
- 권한 리소스는 공유 이미지 전용 버킷의 `arn:aws:s3:::BUCKET_NAME/*`로 제한한다.

애플리케이션은 AWS SDK 기본 자격 증명 공급자 체인을 사용하므로 액세스 키를 컨테이너에 직접 전달하지 않는다.

배포 Docker 이미지에는 `Noto Sans CJK KR` 폰트가 포함된다.
