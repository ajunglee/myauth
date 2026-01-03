# 여러 구현체가 있을 때 Spring의 동작 방식

## 문제 상황

```java
// 인터페이스
public interface ImageStorageService { ... }

// 구현체 1
@Service
public class LocalImageStorageService implements ImageStorageService { ... }

// 구현체 2
@Service
public class S3ImageStorageService implements ImageStorageService { ... }

// Controller
@RestController
public class ImageUploadController {
    private final ImageStorageService imageStorageService;  // ❓ 어느 것을 주입?
}
```

**에러 발생!**
```
NoUniqueBeanDefinitionException:
No qualifying bean of type 'ImageStorageService' available:
expected single matching bean but found 2:
localImageStorageService, s3ImageStorageService
```

---

## 해결 방법 3가지

### ✅ 방법 1: @Primary 사용 (가장 일반적)

**개념**: 여러 구현체 중 기본으로 사용할 것을 지정

```java
@Service
@Primary  // ⭐ 이것을 기본으로 주입
public class LocalImageStorageService implements ImageStorageService {
    // ...
}

@Service
public class S3ImageStorageService implements ImageStorageService {
    // ...
}
```

**장점**:
- 간단하고 명확
- Controller 코드 수정 불필요
- 대부분의 경우 기본 구현체가 명확할 때 사용

**단점**:
- 하나만 @Primary 지정 가능

---

### ✅ 방법 2: @Qualifier 사용

**개념**: 주입받을 때 어느 Bean을 사용할지 명시적으로 지정

```java
@RestController
public class ImageUploadController {

    private final ImageStorageService imageStorageService;

    public ImageUploadController(
        @Qualifier("localImageStorageService") ImageStorageService imageStorageService
    ) {
        this.imageStorageService = imageStorageService;
    }
}
```

**또는 필드 주입:**
```java
@RestController
public class ImageUploadController {

    @Autowired
    @Qualifier("localImageStorageService")  // Bean 이름 지정
    private ImageStorageService imageStorageService;
}
```

**장점**:
- 명시적이고 정확
- 여러 곳에서 다른 구현체를 사용할 수 있음

**단점**:
- 사용하는 곳마다 @Qualifier 추가 필요
- Bean 이름 하드코딩 (오타 위험)

---

### ✅ 방법 3: 변수명으로 자동 매칭

**개념**: 변수명을 Bean 이름과 동일하게 하면 자동으로 매칭

```java
@RestController
public class ImageUploadController {

    // 변수명이 Bean 이름과 일치 → 자동 매칭
    private final ImageStorageService localImageStorageService;

    // 또는
    private final ImageStorageService s3ImageStorageService;
}
```

**Bean 이름 규칙**:
- `LocalImageStorageService` → `localImageStorageService` (첫 글자 소문자)
- `S3ImageStorageService` → `s3ImageStorageService`

**장점**:
- 어노테이션 불필요
- 코드만 봐도 어느 구현체인지 명확

**단점**:
- 변수명이 Bean 이름에 종속됨
- 리팩토링 시 조심해야 함

---

## 실전 예시

### 개발/프로덕션 환경 분리

```java
@Service
@Primary
@Profile("dev")  // 개발 환경
public class LocalImageStorageService implements ImageStorageService {
    // 로컬 파일 시스템 사용
}

@Service
@Primary
@Profile("prod")  // 프로덕션 환경
public class S3ImageStorageService implements ImageStorageService {
    // AWS S3 사용
}
```

이렇게 하면 환경에 따라 자동으로 다른 구현체가 주입됩니다!

---

## 요약

| 방법 | 사용 시기 | 어노테이션 |
|------|----------|-----------|
| **@Primary** | 기본 구현체가 명확할 때 | 구현체에 `@Primary` |
| **@Qualifier** | 명시적으로 지정하고 싶을 때 | 주입부에 `@Qualifier("beanName")` |
| **변수명 매칭** | 간단하게 하고 싶을 때 | 없음 (변수명만 맞추면 됨) |

**권장**: 대부분의 경우 **@Primary**를 사용하는 것이 가장 간단하고 명확합니다.
