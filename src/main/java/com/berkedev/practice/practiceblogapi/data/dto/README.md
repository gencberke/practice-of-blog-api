# DTO MODÜLÜ - ÖĞRENME ÇIKTILARI

## 1. DTO NEDİR VE SPRING ARKADA NE YAPAR?

### Basit Tanım
DTO (Data Transfer Object) = API ile veritabanı arasında veri taşıyan basit Java sınıfları

### Spring Arka Planda Ne Yapar?

**HTTP Request → DTO Dönüşümü (Deserialization):**
```
1. Client JSON gönderir:
   {"username": "john", "email": "john@mail.com", "password": "123456"}
   
2. @RequestBody annotation controller'da görülür

3. Jackson (Spring'in JSON kütüphanesi) devreye girer

4. JSON'ı DTO'ya dönüştürür (ObjectMapper)
   - Field isimleri match edilir (username → username)
   - Type conversion yapılır (String → String, number → Long)
   - Setter'lar veya NoArgsConstructor kullanılır
   
5. @Valid annotation varsa Bean Validation çalışır
   - Her @NotBlank, @Size vs. kontrol edilir
   - Hata varsa MethodArgumentNotValidException

6. Validation başarılı → DTO controller metoduna gelir

Örnek:
POST /api/users
Body: {"username": "john", ...}
    ↓
Jackson deserialize eder
    ↓
UserCreateRequest object oluşur
    ↓
Validation çalışır (@NotBlank, @Size vs.)
    ↓
Controller metoduna gelir
```

**DTO → HTTP Response Dönüşümü (Serialization):**
```
1. Controller UserResponse döner

2. Spring @ResponseBody görür (veya @RestController)

3. Jackson devreye girer

4. DTO'yu JSON'a dönüştürür
   - Getter'lar çağrılır
   - Field'lar JSON key'leri olur
   - LocalDateTime → ISO-8601 string
   
5. HTTP Response olarak client'a gider

Örnek:
UserResponse user = new UserResponse();
user.setUsername("john");
    ↓
Jackson serialize eder
    ↓
{"username": "john", "email": "john@mail.com", ...}
    ↓
HTTP Response
```

---

## 2. TODO APP'TE DTO KULLANILMADI

### TodoController (DTO olmadan)
```java
@PostMapping
public Todo createTodo(@RequestBody Todo todo) {  // ❌ Entity direkt kullanıldı
    return todoService.save(todo);
}

@GetMapping("/{id}")
public Todo getTodo(@PathVariable Long id) {  // ❌ Entity direkt döndü
    return todoService.findById(id);
}
```

**Problemler:**
- ⚠️ Entity direkt dış dünyaya açık (güvenlik riski)
- ⚠️ Client istemediği field'ları da görür
- ⚠️ Validation eksik
- ⚠️ Password gibi hassas bilgiler döner
- ⚠️ Partial update yapılamaz

---

## 3. BLOG API'DE DTO KULLANIMI - NEDEN DTO?

### 1. Güvenlik (Data Hiding)
```java
// Entity'de password var
@Entity
public class User {
    private String password;  // Hassas bilgi!
}

// Response DTO'da password YOK
public class UserResponse {
    // private String password yok! ✅
    private Long id;
    private String username;
    private String email;
}
```

### 2. Circular Reference Önleme
```java
// Entity'de sonsuz döngü riski
@Entity
public class User {
    @OneToMany(mappedBy = "author")
    private List<Post> posts;  // Post → User → Post → User → ...
}

// DTO'da sadece gerekli data
public class UserResponse {
    private Long id;
    private String username;
    // posts yok! veya PostSummary var
}
```

### 3. API Contract Control
```java
// Client sadece gerekli field'ları gönderir
public class UserCreateRequest {
    private String username;  // ID yok, createdAt yok
    private String email;
    private String password;
}

// Client sadece güncellenmesi gereken field'ları gönderir
public class UserUpdateRequest {
    private String email;     // username yok (güncellenemez!)
    private String fullName;
    // Hepsi nullable → partial update
}
```

### 4. Validation
```java
public class UserCreateRequest {
    @NotBlank(message = "Username required")
    @Size(min = 5, max = 20)
    private String username;  // Validation entity'de değil, DTO'da!
}
```

---

## 4. REQUEST DTO'LARI

### Create Request DTO

**UserCreateRequest:**
```java
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class UserCreateRequest {

    @NotBlank(message = "Username required")
    @Size(min = 5, max = 20, message = "Username must be between 5 to 20 characters")
    String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    String password;
    
    String fullName;  // Opsiyonel, @NotBlank yok
}
```

**Özellikler:**
- ID YOK (sunucu oluşturur)
- createdAt YOK (sunucu oluşturur)
- Zorunlu field'lar için @NotBlank, @NotNull
- Default değerler olabilir (örn: `published = false`)
- İlişkili entity'ler için sadece ID (örn: `categoryId`)

**PostCreateRequest:**
```java
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class PostCreateRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 5, max = 200)
    private String title;

    @NotBlank(message = "Slug is required")
    @Size(min = 5, max = 200)
    private String slug;

    @NotBlank(message = "Content is required")
    @Size(min = 50)
    private String content;

    private Boolean published = false;  // Default değer!

    @NotNull(message = "Category is required")
    private Long categoryId;  // Category entity değil, ID!
    
    private List<Long> tagIds = new ArrayList<>();  // Tag entity'ler değil, ID'ler!
}
```

**Neden ID kullanıyoruz:**
```java
// ❌ YANLIŞ: Entity gönder
{
  "title": "My Post",
  "category": {
    "id": 1,
    "name": "Tech",
    "description": "..."
  }
}

// ✅ DOĞRU: Sadece ID gönder
{
  "title": "My Post",
  "categoryId": 1  // Basit ve net
        
}
```

---

### Update Request DTO

**UserUpdateRequest:**
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {

    @Email(message = "Email must be valid")
    String email;  // Nullable!
    
    String fullName;  // Nullable!

    @Size(min = 6, message = "Password must be at least 6 characters")
    String password;  // Nullable!
    
    // username YOK! → Güncellenemez (business rule)
}
```

**Özellikler:**
- **TÜM FIELD'LAR NULLABLE** (partial update için)
- Güncellenemeyen field'lar YOK (örn: username, createdAt)
- Validation sadece dolu field'lara çalışır
- ID YOK (URL'den gelir: PUT /users/1)

**Partial Update Nasıl Çalışır:**
```java
// Client sadece email güncellemek istiyor
PUT /api/users/1
{
  "email": "newemail@mail.com"
  // password ve fullName gönderilmedi (null)
}

// Service'de:
public User updateUser(Long id, UserUpdateRequest request) {
    User user = userRepository.findById(id).orElseThrow();
    
    // Sadece dolu field'ları güncelle
    if (request.getEmail() != null) {
        user.setEmail(request.getEmail());
    }
    
    if (request.getPassword() != null) {
        user.setPassword(passwordEncoder.encode(request.getPassword()));
    }
    
    if (request.getFullName() != null) {
        user.setFullName(request.getFullName());
    }
    
    // username güncellenmedi (business rule) ✅
    
    return userRepository.save(user);
}
```

**PostUpdateRequest:**
```java
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class PostUpdateRequest {
    
    @Size(min = 5, max = 200)
    private String title;  // Nullable
    
    @Size(min = 5, max = 200)
    private String slug;  // Nullable
    
    @Size(min = 50)
    private String content;  // Nullable
    
    private Boolean published;  // Nullable, default değer YOK!
    
    private Long categoryId;  // Nullable
    
    private List<Long> tagIds;  // Nullable
}
```

**Create vs Update Farkı:**
```java
// CREATE: published default false
private Boolean published = false;  // Default var

// UPDATE: published nullable
private Boolean published;  // Default yok, null olabilir

// Neden:
// Create: Her yeni post draft olsun (business rule)
// Update: Sadece değiştirmek istersek gönder
```

---

## 5. RESPONSE DTO'LARI

### UserResponse
```java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private Long id;
    private String fullName;
    private String username;
    private LocalDateTime createdAt;
    private String email;
    
    // password YOK! ✅ Güvenlik
}
```

**Özellikler:**
- ID VAR (client'a dönülür)
- createdAt, updatedAt VAR (timestamp'ler)
- password, hassas bilgiler YOK
- İlişkili entity'ler için Response DTO kullan

### PostResponse
```java
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PostResponse {
    private Long id;
    private String title;
    private String slug;
    private String content;
    private Boolean published;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;

    // İlişkili entity'ler Response DTO olarak!
    private UserResponse author;  // User entity değil!
    private CategoryResponse category;  // Category entity değil!
    private List<TagResponse> tags = new ArrayList<>();  // Tag entity'ler değil!
}
```

**Neden Response DTO kullanıyoruz:**
```java
// ❌ YANLIŞ: Entity kullan
public class PostResponse {
    private User author;  // Entity!
    
    // Problem 1: password gibi hassas bilgiler dönülür
    // Problem 2: Circular reference (User → Post → User → ...)
    // Problem 3: Lazy loading exception
}

// ✅ DOĞRU: Response DTO kullan
public class PostResponse {
    private UserResponse author;  // Response DTO!
    
    // ✅ password yok
    // ✅ Circular reference yok
    // ✅ Lazy loading problem yok
}
```

**Circular Reference Problem:**
```
Entity kullanımı:

Post entity:
{
  "id": 1,
  "title": "My Post",
  "author": {
    "id": 1,
    "username": "john",
    "posts": [
      {
        "id": 1,
        "title": "My Post",
        "author": {
          "id": 1,
          "username": "john",
          "posts": [
            ...  // SONSUZ DÖNGÜ! ⚠️
          ]
        }
      }
    ]
  }
}

Response DTO kullanımı:

PostResponse:
{
  "id": 1,
  "title": "My Post",
  "author": {
    "id": 1,
    "username": "john"
    // posts YOK! ✅
  }
}
```

---

## 6. BEAN VALIDATION ANNOTATIONS

### @NotBlank
**Ne işe yarar:** String null olamaz, boş olamaz, sadece whitespace olamaz

**Spring ne yapar:**
```
1. Field null mu? → Hata
2. Field empty string mi? ("") → Hata
3. Field sadece whitespace mi? ("   ") → Hata
4. Validation geçti → OK
```

```java
@NotBlank(message = "Username required")
private String username;

// ❌ null → Hata
// ❌ "" → Hata
// ❌ "   " → Hata
// ✅ "john" → Geçer
```

**Parametreler:**
- `message`: Hata mesajı

---

### @NotNull
**Ne işe yarar:** Null olamaz (ama boş string olabilir!)

**@NotBlank vs @NotNull farkı:**
```java
// @NotBlank (String için)
@NotBlank
private String username;
// null ❌, "" ❌, "   " ❌, "john" ✅

// @NotNull (her tip için)
@NotNull
private String username;
// null ❌, "" ✅, "   " ✅, "john" ✅

@NotNull
private Long categoryId;
// null ❌, 0 ✅, 1 ✅
```

**Ne zaman kullanılır:**
- ID'ler için (Long, Integer)
- Boolean için
- İlişkili entity ID'leri için

```java
@NotNull(message = "Category is required")
private Long categoryId;  // ✅

@NotNull(message = "Published status required")
private Boolean published;  // ✅
```

---

### @Size
**Ne işe yarar:** String, Collection, Array için uzunluk kontrolü

**Parametreler:**
```java
@Size(
    min = 5,        // Minimum uzunluk (default 0)
    max = 20,       // Maximum uzunluk (default Integer.MAX_VALUE)
    message = "..." // Hata mesajı
)
```

**String için:**
```java
@Size(min = 5, max = 20, message = "Username must be between 5 to 20 characters")
private String username;

// "john" → 4 karakter → Hata ❌
// "johnny" → 6 karakter → Geçer ✅
// "verylongusername123456" → 22 karakter → Hata ❌
```

**List için:**
```java
@Size(min = 1, max = 10, message = "At least 1 tag, max 10 tags")
private List<Long> tagIds;

// [] → 0 eleman → Hata ❌
// [1, 2, 3] → 3 eleman → Geçer ✅
// [1, 2, ..., 11] → 11 eleman → Hata ❌
```

**Spring ne yapar:**
```
1. Field null mu? → @Size çalışmaz (pass)
2. Field.length() veya .size() hesaplanır
3. min <= length <= max kontrolü
4. Hata varsa MethodArgumentNotValidException
```

**Sadece @Size vs @Size + @NotBlank:**
```java
// Sadece @Size (null olabilir)
@Size(min = 5, max = 20)
private String username;
// null ✅, "" ❌, "john" ❌, "johnny" ✅

// @Size + @NotBlank (null olamaz)
@NotBlank
@Size(min = 5, max = 20)
private String username;
// null ❌, "" ❌, "john" ❌, "johnny" ✅
```

---

### @Email
**Ne işe yarar:** Email format kontrolü

**Spring ne yapar:**
```
1. Field null mu? → @Email çalışmaz (pass)
2. Regex ile email formatı kontrol edilir
3. user@domain.com pattern'i
```

```java
@Email(message = "Email must be valid")
private String email;

// null → Geçer (email opsiyonelse)
// "" → Hata ❌
// "john" → Hata ❌
// "john@" → Hata ❌
// "john@mail" → Hata ❌
// "john@mail.com" → Geçer ✅
```

**@Email + @NotBlank:**
```java
@NotBlank(message = "Email is required")
@Email(message = "Email must be valid")
private String email;

// null → Hata ❌ (@NotBlank)
// "" → Hata ❌ (@NotBlank)
// "invalid" → Hata ❌ (@Email)
// "john@mail.com" → Geçer ✅
```

---

### Validation Çalışma Sırası

```java
@NotBlank(message = "Username required")
@Size(min = 5, max = 20, message = "Username must be between 5 to 20")
private String username;

// Spring validation sırası:
// 1. @NotBlank kontrolü
//    - null → HATA, dur!
//    - "" → HATA, dur!
//    - "   " → HATA, dur!
// 2. @Size kontrolü (sadece @NotBlank geçerse)
//    - length < 5 → HATA
//    - length > 20 → HATA
// 3. Hepsi geçti → OK ✅
```

---

## 7. LOMBOK ANNOTATIONS

### @Data
**Ne işe yarar:** Getter, Setter, toString, equals, hashCode, RequiredArgsConstructor üretir

```java
@Data
public class UserResponse {
    private Long id;
    private String username;
}

// Lombok oluşturur:
// - getId(), setId()
// - getUsername(), setUsername()
// - toString()
// - equals()
// - hashCode()
```

**⚠️ Entity'lerde @Data kullanma!**
```java
// ❌ KÖTÜ: Entity'de @Data
@Entity
@Data
public class User {
    @OneToMany
    private List<Post> posts;
}

// Problem:
// toString() → user.toString() → posts.toString() → user.toString() → ...
// Infinite loop! StackOverflowError!

// ✅ İYİ: Entity'de @Getter + @Setter
@Entity
@Getter
@Setter
public class User {
    @OneToMany
    private List<Post> posts;
}
```

---

### @Builder
**Ne işe yarar:** Builder pattern (fluent API) oluşturur

```java
@Builder
@Data
public class UserCreateRequest {
    private String username;
    private String email;
    private String password;
}

// Kullanım:
UserCreateRequest request = UserCreateRequest.builder()
    .username("john")
    .email("john@mail.com")
    .password("123456")
    .build();
```

**⚠️ @Builder kullanırken @NoArgsConstructor + @AllArgsConstructor gerekli!**
```java
// ❌ YANLIŞ: Sadece @Builder
@Builder
@Data
public class UserCreateRequest {
    // ...
}
// Jackson deserialize edemez! (NoArgsConstructor yok)

// ✅ DOĞRU: Üçü birlikte
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class UserCreateRequest {
    // ...
}
```

---

### @NoArgsConstructor
**Ne işe yarar:** Parametresiz constructor oluşturur

```java
@NoArgsConstructor
public class UserCreateRequest {
    private String username;
}

// Lombok oluşturur:
// public UserCreateRequest() {}
```

**Neden gerekli:**
- Jackson (JSON deserialize) için zorunlu
- Spring MVC @RequestBody için zorunlu

**Jackson nasıl çalışır:**
```
1. JSON gelir: {"username": "john"}
2. Jackson NoArgsConstructor çağırır
3. Boş object oluşur: new UserCreateRequest()
4. Setter'lar çağrılır: setUsername("john")
5. Object dolu: UserCreateRequest(username="john")
```

---

### @AllArgsConstructor
**Ne işe yarar:** Tüm field'ları alan constructor oluşturur

```java
@AllArgsConstructor
public class UserCreateRequest {
    private String username;
    private String email;
}

// Lombok oluşturur:
// public UserCreateRequest(String username, String email) {
//     this.username = username;
//     this.email = email;
// }
```

**Neden kullanılır:**
- @Builder ile birlikte kullanılır

---

### DTO'da Lombok Best Practice

```java
// ✅ DTO için ideal kombinasyon
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCreateRequest {
    
    @NotBlank(message = "Username required")
    @Size(min = 5, max = 20)
    private String username;
    
    @NotBlank(message = "Email is required")
    @Email
    private String email;
    
    @NotBlank(message = "Password is required")
    @Size(min = 6)
    private String password;
    
    private String fullName;
}
```

**Neden bu kombinasyon:**
- `@Data`: Getter, Setter, toString, equals, hashCode
- `@Builder`: Test'ler ve fluent API için
- `@NoArgsConstructor`: Jackson için (zorunlu)
- `@AllArgsConstructor`: @Builder için (zorunlu)

---

## 8. İLİŞKİLİ ENTITY'LER VE DTO

### Request DTO'da İlişkiler

**Entity kullanmıyoruz, ID kullanıyoruz:**
```java
// ❌ YANLIŞ
public class PostCreateRequest {
    private Category category;  // Entity!
    private List<Tag> tags;     // Entity list!
}

// ✅ DOĞRU
public class PostCreateRequest {
    @NotNull(message = "Category is required")
    private Long categoryId;  // Sadece ID!
    
    private List<Long> tagIds = new ArrayList<>();  // ID listesi!
}
```

---

### Response DTO'da İlişkiler

**Entity kullanmıyoruz, Response DTO kullanıyoruz:**
```java
// ❌ YANLIŞ
public class PostResponse {
    private User author;        // Entity!
    private Category category;  // Entity!
    private List<Tag> tags;     // Entity list!
}

// ✅ DOĞRU
public class PostResponse {
    private Long id;
    private String title;
    
    // İlişkiler Response DTO olarak
    private UserResponse author;         // Response DTO!
    private CategoryResponse category;   // Response DTO!
    private List<TagResponse> tags = new ArrayList<>();  // Response DTO list!
}
```

---

## 9. BEST PRACTICES

### ✅ İYİ

**1. Request/Response ayrımı**
```java
// ✅ Create
UserCreateRequest

// ✅ Update
UserUpdateRequest

// ✅ Response
UserResponse
```

**2. Nullable field'larda validation**
```java
// Update Request'te
@Email  // null gelirse çalışmaz, dolu gelirse kontrol eder
private String email;
```

**3. Default değerler field'da**
```java
private Boolean published = false;  // ✅
```

**4. İlişkiler için ID**
```java
// Request'te
private Long categoryId;  // ✅

// Response'da
private CategoryResponse category;  // ✅
```

---

### ❌ KÖTÜ

**1. Entity direkt kullanma**
```java
@PostMapping
public User create(@RequestBody User user) {  // ❌
    return userService.save(user);
}
```

**2. Update'de default değer**
```java
// Update Request'te
private Boolean published = false;  // ❌ Partial update bozulur
```

**3. Response'da password**
```java
public class UserResponse {
    private String password;  // ❌ Güvenlik riski
}
```

**4. Response'da Entity**
```java
public class PostResponse {
    private User author;  // ❌ Circular reference
}
```

---

## 10. TODO APP → BLOG API GEÇİŞ

### TODO App'te Yoktu
- ❌ DTO kullanımı
- ❌ Request/Response ayrımı
- ❌ Bean Validation
- ❌ Partial update pattern

### Blog API'de Eklendi
- 🆕 Request DTO (Create/Update ayrımı)
- 🆕 Response DTO (Data hiding)
- 🆕 Bean Validation annotations
- 🆕 Lombok kullanımı
- 🆕 İlişkili entity'ler için ID/Response DTO pattern
- 🆕 Partial update support
- 🆕 Circular reference çözümü

---

## 11. SONRAKI ADIMLAR

✅ **DTO Layer tamamlandı!**

Sırada:
1. **Mapper Layer** - Entity ↔ DTO dönüşümleri
2. **Service Layer** - Business logic
3. **Controller** - REST endpoints
4. **Exception Handling** - Global exception handler

Hangi modüle geçelim? 🚀
