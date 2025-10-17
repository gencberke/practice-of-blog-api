# MAPPER MODÜLÜ - ÖĞRENME ÇIKTILARI

## 1. MAPPER NEDİR VE SPRING ARKADA NE YAPAR?

### Basit Tanım
Mapper = Entity ↔ DTO dönüşümlerini yapan sınıflar

### Neden Mapper Gerekli?

**Problem:**
```java
// Service'de elle mapping yapmak:
public UserResponse createUser(UserCreateRequest request) {
    // Entity oluşturma - 10 satır kod
    User user = new User();
    user.setUsername(request.getUsername());
    user.setEmail(request.getEmail());
    user.setPassword(request.getPassword());
    user.setFullName(request.getFullName());
    
    User savedUser = userRepository.save(user);
    
    // Response oluşturma - 10 satır daha
    UserResponse response = new UserResponse();
    response.setId(savedUser.getId());
    response.setUsername(savedUser.getUsername());
    response.setEmail(savedUser.getEmail());
    response.setFullName(savedUser.getFullName());
    response.setCreatedAt(savedUser.getCreatedAt());
    
    return response;
}
// Toplam 20+ satır boilerplate! ⚠️
// Her entity için aynı kod tekrar! ⚠️
// Service'de iş mantığı kayboldu! ⚠️
```

**Çözüm: Mapper Pattern**
```java
// UserMapper'da:
public User toEntity(UserCreateRequest request) {
    return User.builder()
        .username(request.getUsername())
        .email(request.getEmail())
        .password(request.getPassword())
        .fullName(request.getFullName())
        .build();
}

public UserResponse toResponse(User user) {
    return UserResponse.builder()
        .id(user.getId())
        .username(user.getUsername())
        .email(user.getEmail())
        .fullName(user.getFullName())
        .createdAt(user.getCreatedAt())
        .build();
}

// Service'de:
public UserResponse createUser(UserCreateRequest request) {
    User user = userMapper.toEntity(request);     // 1 satır
    User savedUser = userRepository.save(user);
    return userMapper.toResponse(savedUser);      // 1 satır
}
// Temiz, okunabilir, iş mantığına odaklı! ✅
```

### Spring Arka Planda Ne Yapar?

**@Component ile Bean Oluşturma:**
```
1. Uygulama başlarken Spring @Component'i tarar
2. UserMapper instance'ı oluşturur (singleton)
3. Spring Application Context'e koyar
4. Service'de @Autowired ile inject eder

Örnek:
@Component
public class UserMapper { }
    ↓
Spring: new UserMapper() oluşturur
    ↓
Context'e kaydeder
    ↓
@Autowired UserMapper userMapper → Inject edilir
```

**@RequiredArgsConstructor ile Dependency Injection:**
```
@Component
@RequiredArgsConstructor
public class PostMapper {
    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;
}
    ↓
Lombok constructor oluşturur:
public PostMapper(UserMapper um, CategoryMapper cm) {
    this.userMapper = um;
    this.categoryMapper = cm;
}
    ↓
Spring constructor'ı görür
    ↓
UserMapper ve CategoryMapper bean'lerini bulur
    ↓
PostMapper'a inject eder
```

---

## 2. TODO APP VS BLOG API

### TODO App - Mapper Yoktu
```java
// Service'de elle mapping:
@Service
public class TodoService {
    public Todo createTodo(TodoRequest request) {
        Todo todo = new Todo();
        todo.setTitle(request.getTitle());  // Elle mapping
        todo.setCompleted(false);
        return todoRepository.save(todo);
    }
}
```
**Problemler:** Boilerplate, tekrar kod, service karmaşık

### Blog API - Mapper Pattern
```java
// UserMapper:
@Component
public class UserMapper {
    public User toEntity(UserCreateRequest request) { }
    public UserResponse toResponse(User user) { }
}

// Service:
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserMapper userMapper;  // ← Inject
    
    public UserResponse createUser(UserCreateRequest request) {
        User user = userMapper.toEntity(request);  // Temiz!
        return userMapper.toResponse(userRepository.save(user));
    }
}
```
**Avantajlar:** DRY, temiz service, kolay test

---

## 3. @COMPONENT ANNOTATION

### Ne İşe Yarar
Spring'e "bu class'ı bean yap" der

```java
@Component
public class UserMapper {
    // Spring bu class'tan singleton bean oluşturur
}
```

### Spring Ne Yapar

**Bean Lifecycle:**
```
1. Component Scanning:
   Spring başlarken @Component'leri tarar
   
2. Bean Creation:
   UserMapper instance'ı oluşturur
   Singleton scope (uygulama boyunca 1 instance)
   
3. Dependency Resolution:
   Eğer constructor'da dependency varsa inject eder
   
4. Bean Registration:
   Application Context'e kaydeder
   
5. Injection:
   @Autowired ile istendiğinde verir
```

---

## 4. @REQUIREDARGSCONSTRUCTOR (LOMBOK)

### Ne İşe Yarar
Final field'lar için constructor oluşturur

### Ne Zaman Kullanılır
Mapper içinde başka mapper'ları inject etmek için

**PostMapper Örneği:**
```java
@Component
@RequiredArgsConstructor  // ← Lombok annotation
public class PostMapper {
    
    // Final field'lar → constructor'a eklenir
    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    
    // Lombok oluşturur:
    // public PostMapper(UserMapper userMapper,
    //                   CategoryMapper categoryMapper,
    //                   TagMapper tagMapper) {
    //     this.userMapper = userMapper;
    //     this.categoryMapper = categoryMapper;
    //     this.tagMapper = tagMapper;
    // }
}
```

---

## 5. MAPPER METODLARI

### toResponse (Entity → Response DTO)

```java
public UserResponse toResponse(User user) {
    if (user == null)
        return null;  // Null check!

    return UserResponse.builder()
        .id(user.getId())
        .fullName(user.getFullName())
        .username(user.getUsername())
        .email(user.getEmail())
        .createdAt(user.getCreatedAt())
        .build();
}
```

### toEntity (Request DTO → Entity)

```java
public User toEntity(UserCreateRequest createRequest) {
    if (createRequest == null)
        return null;

    return User.builder()
        .email(createRequest.getEmail())
        .fullName(createRequest.getFullName())
        .username(createRequest.getUsername())
        .password(createRequest.getPassword())
        // ID yok → Database oluşturur
        // createdAt yok → @PrePersist oluşturur
        .build();
}
```

### updateEntityFromRequest (Partial Update)

```java
public void updateEntityFromRequest(UserUpdateRequest updateRequest, User user) {
    if (updateRequest == null || user == null)
        return;

    // Sadece null olmayan field'ları güncelle
    if (updateRequest.getEmail() != null)
        user.setEmail(updateRequest.getEmail());

    if (updateRequest.getPassword() != null)
        user.setPassword(updateRequest.getPassword());

    if (updateRequest.getFullName() != null)
        user.setFullName(updateRequest.getFullName());
}
```

### toResponseList (List Dönüşümü)

```java
public List<TagResponse> toResponseList(List<Tag> tags) {
    List<TagResponse> tagResponsesList = new ArrayList<>();

    if (tags != null) {
        for (Tag tag : tags) {
            TagResponse response = toResponse(tag);
            tagResponsesList.add(response);
        }
    }
    return tagResponsesList;
}
```

---

## 6. NESTED MAPPER KULLANIMI

**PostMapper:**
```java
@Component
@RequiredArgsConstructor
public class PostMapper {
    
    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    
    public PostResponse toResponse(Post post) {
        if (post == null)
            return null;

        return PostResponse.builder()
            .id(post.getId())
            .title(post.getTitle())
            
            // Nested entity'leri mapper ile dönüştür!
            .author(userMapper.toResponse(post.getAuthor()))
            .category(categoryMapper.toResponse(post.getCategory()))
            .tags(tagMapper.toResponseList(post.getTags()))
            .build();
    }
}
```

**Neden nested mapper:**
- Circular reference önlenir
- Password gibi hassas bilgiler dönmez
- Kod tekrarı önlenir

---

## 7. BEST PRACTICES

### ✅ İYİ

**1. Null check yap**
```java
if (user == null) return null;  // ✅
```

**2. Builder pattern kullan**
```java
return UserResponse.builder().build();  // ✅
```

**3. Nested entity'ler için mapper kullan**
```java
.author(userMapper.toResponse(post.getAuthor()))  // ✅
```

**4. Partial update için null check**
```java
if (request.getEmail() != null)
    user.setEmail(request.getEmail());  // ✅
```

---

### ❌ KÖTÜ

**1. Null check yapma**
```java
return UserResponse.builder()
    .id(user.getId())  // ❌ NPE riski!
    .build();
```

**2. Entity döndürme**
```java
.author(post.getAuthor())  // ❌ Entity döndü!
```

**3. Her field'ı güncelleme**
```java
user.setEmail(request.getEmail());  // ❌ null gelirse email null olur!
```

---

## 8. TODO APP → BLOG API GEÇİŞ

### TODO App'te Yoktu
- ❌ Mapper layer
- ❌ Entity ↔ DTO dönüşümü
- ❌ Service'de elle mapping

### Blog API'de Eklendi
- 🆕 Mapper layer
- 🆕 @Component annotation
- 🆕 toResponse, toEntity methodları
- 🆕 Nested mapper injection
- 🆕 List dönüşümü
- 🆕 Partial update support

---

## 9. SONRAKI ADIMLAR

✅ **Mapper Layer tamamlandı!**

Sırada:
1. **Service Layer** - Business logic
2. **Controller** - REST endpoints
3. **Exception Handling** - Global handler

Hangi modüle geçelim? 🚀
