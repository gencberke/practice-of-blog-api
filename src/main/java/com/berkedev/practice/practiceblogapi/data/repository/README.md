# REPOSITORY MODÜLÜ - ÖĞRENME ÇIKTILARI

## 1. REPOSITORY NEDİR VE SPRING ARKADA NE YAPAR?

### Basit Tanım
Repository = Database ile konuşan katman. CRUD işlemleri ve query'ler buradan yapılır.

### Spring Data JPA Arka Planda Ne Yapar?

**Sihir (Magic) Nasıl Çalışır:**
```
1. Sen interface yazıyorsun (implement yok!)
2. Spring başlarken repository interface'leri tarar
3. Method isimlerini parse eder (findByUsername → find-By-Username)
4. JPQL query'si oluşturur (SELECT u FROM User u WHERE u.username = ?)
5. Runtime'da PROXY class oluşturur (implementation)
6. Bu proxy class'ı @Autowired ile inject eder

Örnek:
findByUsername("john")
    ↓
Spring proxy class method'u çağırır
    ↓
JPQL: SELECT u FROM User u WHERE u.username = ?
    ↓
SQL: SELECT * FROM users WHERE username = 'john'
    ↓
Entity döner
```

**Proxy Pattern:**
```java
// Sen yazıyorsun:
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}

// Spring runtime'da bunu oluşturuyor (görünmez):
public class UserRepositoryImpl implements UserRepository {
    @Override
    public Optional<User> findByUsername(String username) {
        // JPQL execute et
        // Result'u map et
        // Optional olarak dön
    }
}
```

---

## 2. TODO APP'TEKİ KULLANIM

### TodoRepository
```java
@Repository
public interface TodoRepository extends JpaRepository<Todo, Long> {

    @Query("SELECT e FROM Todo e WHERE e.id = :id")
    Todo getTodoById(@Param("id") Long id);
}
```

**Öğrenilenler:**
- ✅ JpaRepository extend etme
- ✅ @Repository annotation
- ✅ @Query ile custom JPQL
- ✅ @Param ile parametre binding
- ⚠️ Gereksiz @Query kullanımı (findById zaten var!)

**Daha iyi versiyon:**
```java
@Repository
public interface TodoRepository extends JpaRepository<Todo, Long> {
    // findById zaten JpaRepository'de var!
    // @Query gereksiz
}
```

---

## 3. BLOG API'DEKİ KULLANIM

### Yeni Öğrenilenler

Blog API'de TODO App'ten farklı olarak:
1. **Query Method Naming Convention** öğrenildi
2. **exists** methodları eklendi
3. **Optional** return type kullanıldı
4. **List** return type kullanıldı
5. **@Query** sadece kompleks query'ler için kullanıldı
6. **Method naming keywords** öğrenildi (findBy, existsBy, countBy)

### UserRepository

```java
@Repository
public interface UserRepository extends JpaRepository<User,Long> {
    
    // Method naming convention (Spring otomatik implement eder)
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
}
```

**Yeni kavramlar:**
- `existsBy`: Boolean döner (kayıt var mı?)
- `findBy`: Entity döner (tek kayıt bulma)
- `Optional`: Null-safe return (kayıt yoksa empty)

### PostRepository

```java
@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    // Basit method naming
    Optional<Post> findBySlug(String slug);
    boolean existsBySlug(String slug);
    List<Post> findByPublished(boolean published);
    List<Post> findByAuthorId(Long authorId);
    List<Post> findByCategoryId(Long categoryId);
    List<Post> findByPublishedAndCategoryId(boolean published, Long categoryId);
    List<Post> findByTitleContainingIgnoreCase(String keyword);
    
    // Kompleks query'ler için @Query
    @Query("SELECT p FROM Post p WHERE p.published = :published ORDER BY p.publishedAt DESC")
    List<Post> findPublishedPostsOrderedByDate(@Param("published") boolean published);

    @Query("SELECT COUNT(p) FROM Post p WHERE p.author.id = :authorId AND p.published = :published")
    long countPublishedPostsByAuthor(@Param("authorId") Long authorId, @Param("published") boolean published);
}
```

**Yeni öğrenilenler:**
- `And`: Birden fazla condition (WHERE ... AND ...)
- `Containing`: LIKE sorgusu (%keyword%)
- `IgnoreCase`: Case-insensitive arama
- `OrderBy`: Sıralama (method naming ile)
- `countBy`: COUNT(*) sorgusu

### CommentRepository

```java
@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // İlişkili entity'lerle sorgular
    List<Comment> findByPostId(Long postId);
    List<Comment> findByAuthorId(Long authorId);
    
    // Count methodları
    long countByPostId(Long postId);
    long countByAuthorId(Long authorId);
    
    // Custom JPQL - method naming çok uzun olacağı için
    @Query("SELECT c FROM Comment c WHERE c.post.id = :postId ORDER BY c.createdAt DESC")
    List<Comment> findPostCommentsOrderedByDate(@Param("postId") Long postId);
    
    @Query("SELECT c FROM Comment c WHERE c.author.id = :authorId ORDER BY c.createdAt DESC")
    List<Comment> findUserCommentsOrderedByDate(@Param("authorId") Long authorId);
}
```

---

## 4. JPREPOSITORY DETAYLARI

### JpaRepository Nedir?

**Interface hiyerarşisi:**
```
CrudRepository (CRUD işlemleri)
    ↓
PagingAndSortingRepository (Pagination + Sorting)
    ↓
JpaRepository (JPA specific + Batch operations)
```

### JpaRepository'den Gelen Hazır Methodlar

**CRUD Operations:**
```java
// Create/Update
<S extends T> S save(S entity);
<S extends T> List<S> saveAll(Iterable<S> entities);

// Read
Optional<T> findById(ID id);
List<T> findAll();
List<T> findAllById(Iterable<ID> ids);
boolean existsById(ID id);
long count();

// Delete
void deleteById(ID id);
void delete(T entity);
void deleteAll();
void deleteAllById(Iterable<? extends ID> ids);
```

**Örnekle kullanım:**
```java
// Save
User user = new User();
user.setUsername("john");
userRepository.save(user);  // INSERT veya UPDATE

// Find
Optional<User> user = userRepository.findById(1L);
List<User> allUsers = userRepository.findAll();

// Exists
boolean exists = userRepository.existsById(1L);

// Count
long userCount = userRepository.count();

// Delete
userRepository.deleteById(1L);
```

**Spring ne yapar:**
```
save(user) çağrılır
    ↓
Entity manager'a persist/merge
    ↓
@PrePersist veya @PreUpdate çalışır
    ↓
SQL: INSERT INTO users (...) VALUES (...)
    ↓
Generated ID entity'ye atanır
```

---

## 5. QUERY METHOD NAMING CONVENTION

### Temel Keywords

#### findBy
**Ne işe yarar:** SELECT sorgusu yapar

```java
// Tek field
Optional<User> findByUsername(String username);
// SQL: SELECT * FROM users WHERE username = ?

// Birden fazla field (AND)
Optional<User> findByUsernameAndEmail(String username, String email);
// SQL: SELECT * FROM users WHERE username = ? AND email = ?

// OR ile
List<User> findByUsernameOrEmail(String username, String email);
// SQL: SELECT * FROM users WHERE username = ? OR email = ?
```

#### existsBy
**Ne işe yarar:** Kayıt var mı kontrolü (boolean döner)

```java
boolean existsByUsername(String username);
// SQL: SELECT COUNT(*) > 0 FROM users WHERE username = ?

boolean existsByEmail(String email);
// SQL: SELECT COUNT(*) > 0 FROM users WHERE email = ?
```

**Neden kullanılır:** `findBy` yerine daha performanslı (entity oluşturmaz)

#### countBy
**Ne işe yarar:** Kayıt sayısını döner

```java
long countByPublished(boolean published);
// SQL: SELECT COUNT(*) FROM posts WHERE published = ?

long countByAuthorId(Long authorId);
// SQL: SELECT COUNT(*) FROM posts WHERE author_id = ?
```

#### deleteBy
**Ne işe yarar:** Koşula göre siler

```java
void deleteByUsername(String username);
// SQL: DELETE FROM users WHERE username = ?

long deleteByPublished(boolean published);
// SQL: DELETE FROM posts WHERE published = ?
// Silinen kayıt sayısını döner
```

### Gelişmiş Keywords

#### Containing / Like
**Ne işe yarar:** LIKE sorgusu (%keyword%)

```java
List<Post> findByTitleContaining(String keyword);
// SQL: SELECT * FROM posts WHERE title LIKE '%keyword%'

List<User> findByUsernameContaining(String keyword);
// SQL: SELECT * FROM users WHERE username LIKE '%keyword%'
```

#### IgnoreCase
**Ne işe yarar:** Case-insensitive arama

```java
Optional<User> findByUsernameIgnoreCase(String username);
// SQL: SELECT * FROM users WHERE LOWER(username) = LOWER(?)

List<Post> findByTitleContainingIgnoreCase(String keyword);
// SQL: SELECT * FROM posts WHERE LOWER(title) LIKE LOWER('%keyword%')
```

#### StartingWith / EndingWith
**Ne işe yarar:** Başlangıç/bitiş ile arama

```java
List<User> findByUsernameStartingWith(String prefix);
// SQL: SELECT * FROM users WHERE username LIKE 'prefix%'

List<User> findByUsernameEndingWith(String suffix);
// SQL: SELECT * FROM users WHERE username LIKE '%suffix'
```

#### Between
**Ne işe yarar:** Aralık sorgusu

```java
List<Post> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
// SQL: SELECT * FROM posts WHERE created_at BETWEEN ? AND ?
```

#### LessThan / GreaterThan
**Ne işe yarar:** Küçüktür/büyüktür sorgusu

```java
List<Post> findByCreatedAtAfter(LocalDateTime date);
// SQL: SELECT * FROM posts WHERE created_at > ?

List<Post> findByCreatedAtBefore(LocalDateTime date);
// SQL: SELECT * FROM posts WHERE created_at < ?
```

#### In
**Ne işe yarar:** IN clause sorgusu

```java
List<User> findByUsernameIn(List<String> usernames);
// SQL: SELECT * FROM users WHERE username IN (?, ?, ?)
```

#### OrderBy
**Ne işe yarar:** Sıralama ekler

```java
List<Post> findByPublishedOrderByCreatedAtDesc(boolean published);
// SQL: SELECT * FROM posts WHERE published = ? ORDER BY created_at DESC

List<Post> findByAuthorIdOrderByTitleAsc(Long authorId);
// SQL: SELECT * FROM posts WHERE author_id = ? ORDER BY title ASC
```

### İlişkili Entity Sorguları

```java
// Author ilişkisinden sorgulama
List<Post> findByAuthorId(Long authorId);
// SQL: SELECT * FROM posts WHERE author_id = ?

List<Post> findByAuthorUsername(String username);
// SQL: SELECT p FROM Post p WHERE p.author.username = ?
// (Spring otomatik JOIN yapar)

// Category ilişkisinden
List<Post> findByCategoryId(Long categoryId);
List<Post> findByCategoryName(String categoryName);

// Birden fazla ilişki
List<Post> findByPublishedAndCategoryId(boolean published, Long categoryId);
// SQL: SELECT * FROM posts WHERE published = ? AND category_id = ?
```

---

## 6. @QUERY ANNOTATION

### Ne Zaman Kullanılır?

**@Query kullan:**
- ✅ Method adı çok uzun olacaksa (5+ keyword)
- ✅ JOIN gerekiyorsa
- ✅ GROUP BY, HAVING gerekiyorsa
- ✅ Subquery gerekiyorsa
- ✅ Complex aggregation
- ✅ Native SQL gerekiyorsa

**Method naming kullan:**
- ✅ Basit sorgular (1-3 keyword)
- ✅ Tek tablo sorguları
- ✅ Standart CRUD işlemleri

### JPQL (Java Persistence Query Language)

**Temel kullanım:**
```java
@Query("SELECT u FROM User u WHERE u.username = :username")
Optional<User> findByUsername(@Param("username") String username);
```

**Entity ismi kullanılır (tablo adı değil):**
```java
// ✅ DOĞRU: Entity adı
@Query("SELECT u FROM User u WHERE u.email = :email")
Optional<User> findByEmail(@Param("email") String email);

// ❌ YANLIŞ: Tablo adı
@Query("SELECT u FROM users u WHERE u.email = :email")  // Hata!
```

**Field ismi kullanılır (column adı değil):**
```java
// ✅ DOĞRU: Field adı
@Query("SELECT p FROM Post p WHERE p.published = :published")
List<Post> findPublished(@Param("published") boolean published);

// ❌ YANLIŞ: Column adı
@Query("SELECT p FROM Post p WHERE p.is_published = :published")  // Hata!
```

### JOIN Sorguları

```java
// Implicit JOIN (Spring otomatik yapar)
@Query("SELECT p FROM Post p WHERE p.author.username = :username")
List<Post> findByAuthorUsername(@Param("username") String username);

// Explicit JOIN
@Query("SELECT p FROM Post p JOIN p.author a WHERE a.username = :username")
List<Post> findByAuthorUsername2(@Param("username") String username);

// LEFT JOIN
@Query("SELECT p FROM Post p LEFT JOIN p.comments c WHERE p.id = :postId")
Post findPostWithComments(@Param("postId") Long postId);

// JOIN FETCH (N+1 problem çözümü)
@Query("SELECT p FROM Post p JOIN FETCH p.author WHERE p.id = :id")
Optional<Post> findByIdWithAuthor(@Param("id") Long id);
```

**JOIN FETCH ne işe yarar:**
```java
// JOIN FETCH yok (N+1 problem)
@Query("SELECT p FROM Post p WHERE p.published = true")
List<Post> findPublished();

// Kullanım:
List<Post> posts = postRepository.findPublished();  // 1 query
for (Post post : posts) {
    post.getAuthor().getUsername();  // Her post için 1 query!
}
// TOPLAM: 1 + N query ⚠️

// JOIN FETCH var (tek query)
@Query("SELECT p FROM Post p JOIN FETCH p.author WHERE p.published = true")
List<Post> findPublishedWithAuthor();

// Kullanım:
List<Post> posts = postRepository.findPublishedWithAuthor();  // 1 query
for (Post post : posts) {
    post.getAuthor().getUsername();  // Ekstra query yok!
}
// TOPLAM: 1 query ✅
```

### Aggregation Sorguları

```java
// COUNT
@Query("SELECT COUNT(p) FROM Post p WHERE p.author.id = :authorId")
long countByAuthorId(@Param("authorId") Long authorId);

// COUNT with condition
@Query("SELECT COUNT(p) FROM Post p WHERE p.author.id = :authorId AND p.published = :published")
long countPublishedPostsByAuthor(@Param("authorId") Long authorId, @Param("published") boolean published);

// SUM
@Query("SELECT SUM(p.viewCount) FROM Post p WHERE p.author.id = :authorId")
Long getTotalViewsByAuthor(@Param("authorId") Long authorId);

// AVG
@Query("SELECT AVG(p.viewCount) FROM Post p")
Double getAverageViews();

// GROUP BY
@Query("SELECT p.author.username, COUNT(p) FROM Post p GROUP BY p.author.username")
List<Object[]> countPostsPerAuthor();
```

### Native SQL Query

**Ne zaman kullanılır:** Database-specific özellikler gerektiğinde

```java
@Query(value = "SELECT * FROM posts WHERE published = :published", nativeQuery = true)
List<Post> findPublishedNative(@Param("published") boolean published);

// PostgreSQL specific (INTERVAL)
@Query(value = "SELECT * FROM posts WHERE created_at > NOW() - INTERVAL '7 days'", nativeQuery = true)
List<Post> findPostsFromLastWeek();

// PostgreSQL JSON operations
@Query(value = "SELECT * FROM posts WHERE metadata->>'featured' = 'true'", nativeQuery = true)
List<Post> findFeaturedPosts();
```

**⚠️ Native SQL dezavantajları:**
- Database-specific (taşınabilir değil)
- Entity field names yerine column names
- Type-safe değil

---

## 7. OPTIONAL RETURN TYPE

### Neden Optional?

**Problem:**
```java
// BAD: Null döner
User findByUsername(String username);

User user = userRepository.findByUsername("nonexistent");
user.getEmail();  // NullPointerException! ⚠️
```

**Çözüm:**
```java
// GOOD: Optional döner
Optional<User> findByUsername(String username);

Optional<User> userOpt = userRepository.findByUsername("nonexistent");
if (userOpt.isPresent()) {
    User user = userOpt.get();
    // güvenli kullanım
}
```

### Optional Kullanımı

```java
// 1. isPresent() + get()
Optional<User> userOpt = userRepository.findByUsername("john");
if (userOpt.isPresent()) {
    User user = userOpt.get();
    System.out.println(user.getEmail());
}

// 2. ifPresent()
userOpt.ifPresent(user -> System.out.println(user.getEmail()));

// 3. orElse()
User user = userOpt.orElse(new User());  // Default değer

// 4. orElseGet()
User user = userOpt.orElseGet(() -> createDefaultUser());

// 5. orElseThrow()
User user = userOpt.orElseThrow(() -> new UserNotFoundException("User not found"));

// 6. map()
String email = userOpt
    .map(User::getEmail)
    .orElse("no-email");
```

---

## 8. BEST PRACTICES

### ✅ İYİ

**1. Optional kullan (findBy için)**
```java
Optional<User> findByUsername(String username);  // ✅
Optional<User> findByEmail(String email);  // ✅
```

**2. Method naming kısa tutmaya çalış**
```java
// ✅ Okunabilir
findByPublished(boolean published)

// ⚠️ Uzun ama anlaşılır
findByPublishedAndCategoryId(boolean published, Long categoryId)

// ❌ Çok uzun, @Query kullan
findByPublishedAndCategoryIdAndCreatedAtBetweenOrderByCreatedAtDesc(...)
```

**3. @Query'de JPQL kullan (native SQL yerine)**
```java
// ✅ JPQL (database agnostic)
@Query("SELECT u FROM User u WHERE u.username = :username")

// ⚠️ Native SQL (sadece gerektiğinde)
@Query(value = "SELECT * FROM users WHERE username = ?", nativeQuery = true)
```

**4. JOIN FETCH kullan (N+1 önlemek için)**
```java
@Query("SELECT p FROM Post p JOIN FETCH p.author WHERE p.id = :id")
Optional<Post> findByIdWithAuthor(@Param("id") Long id);
```

**5. Parametre binding (@Param kullan)**
```java
@Query("SELECT u FROM User u WHERE u.username = :username")
Optional<User> findByUsername(@Param("username") String username);  // ✅

// ⚠️ Pozisyonel parametre (okunabilirlik düşük)
@Query("SELECT u FROM User u WHERE u.username = ?1")
Optional<User> findByUsername(String username);
```

---

### ❌ KÖTÜ

**1. Null return (Optional kullanmamak)**
```java
User findByUsername(String username);  // ❌ Null dönebilir
```

**2. Gereksiz @Query**
```java
// ❌ Gereksiz @Query
@Query("SELECT u FROM User u WHERE u.id = :id")
Optional<User> findById(@Param("id") Long id);

// ✅ findById zaten JpaRepository'de var!
```

**3. Select * kullanımı (native SQL'de)**
```java
// ❌ Tüm column'ları çeker (gereksiz data)
@Query(value = "SELECT * FROM posts", nativeQuery = true)
List<Post> findAll();

// ✅ Sadece gerekli field'lar
@Query("SELECT new com.example.dto.PostSummary(p.id, p.title) FROM Post p")
List<PostSummary> findAllSummaries();
```

**4. Method naming çok uzun**
```java
// ❌ Okunamaz
findByPublishedAndAuthorIdAndCategoryIdAndCreatedAtBetweenOrderByCreatedAtDesc(...)

// ✅ @Query kullan
@Query("SELECT p FROM Post p WHERE ...")
List<Post> findFilteredPosts(...);
```

---

## 9. GEÇMİŞ SOHBETTEN ÖNERİLER

### 1. Gereksiz @Query Kullanımı

**Senin kodin (TODO App):**
```java
@Query("SELECT e FROM Todo e WHERE e.id = :id")
Todo getTodoById(@Param("id") Long id);
```

**Önerim:**
```java
// @Query gereksiz, findById zaten var!
// Sadece şunu kullan:
Optional<Todo> todo = todoRepository.findById(id);
```

**Neden:** JpaRepository zaten `findById` sağlıyor, @Query gereksiz

### 2. Spring Data JPA Method Naming Magic

**Açıkladığım:**
Spring, method isimlerini runtime'da parse ederek otomatik implementation oluşturur:

```
findByUsername
  ↓ (parse)
find - By - Username
  ↓ (JPQL)
SELECT u FROM User u WHERE u.username = ?
  ↓ (Proxy class)
Implementation oluşturulur
```

### 3. @Query Ne Zaman Kullanılmalı

**Önerim:**
- Basit sorgular: Method naming
- Kompleks sorgular: @Query
- 4+ keyword: @Query düşün
- 5+ keyword: Kesinlikle @Query

**Örnekler:**
```java
// Basit: Method naming ✅
findByUsername(String username)

// Orta: Method naming hala OK ✅
findByPublishedAndCategoryId(boolean published, Long categoryId)

// Kompleks: @Query kullan ✅
@Query("SELECT c FROM Comment c WHERE c.post.id = :postId ORDER BY c.createdAt DESC")
List<Comment> findPostCommentsOrderedByDate(@Param("postId") Long postId);
```

### 4. Optional vs Entity Return

**Önerim:**
- `findBy`: Optional kullan (tek kayıt, null olabilir)
- `existsBy`: boolean
- Query'ler (List): List kullan

```java
// ✅ DOĞRU
Optional<User> findByUsername(String username);
boolean existsByEmail(String email);
List<User> findByNameContaining(String keyword);

// ❌ YANLIŞ
User findByUsername(String username);  // Null riski
```

### 5. JPQL vs Native SQL

**Önerim:**
- Default: JPQL kullan (database agnostic)
- Database-specific feature gerekiyorsa: Native SQL

```java
// ✅ JPQL (önerilen)
@Query("SELECT p FROM Post p WHERE p.published = :published")

// ⚠️ Native SQL (sadece gerektiğinde)
@Query(value = "SELECT * FROM posts WHERE published = ?", nativeQuery = true)
```

---

## 10. TODO APP → BLOG API GEÇİŞ

### TODO App'te Vardı
- ✅ JpaRepository extend etme
- ✅ @Repository annotation
- ✅ @Query kullanımı (gereksiz kullanım)
- ✅ @Param ile parametre binding

### Blog API'de Eklendi
- 🆕 Query Method Naming Convention
- 🆕 Optional return type
- 🆕 exists methods (existsByUsername, existsByEmail)
- 🆕 List return type (findAll variants)
- 🆕 İlişkili entity sorguları (findByAuthorId, findByCategoryId)
- 🆕 Gelişmiş keywords (Containing, IgnoreCase, OrderBy)
- 🆕 COUNT queries (countByPostId, countByAuthorId)
- 🆕 Doğru @Query kullanımı (sadece kompleks query'ler için)

---

## 11. SONRAKI ADIMLAR

✅ **Repository Layer tamamlandı!**

Sırada:
1. **Service Layer** - Business logic, transaction management
2. **DTO & Mapper** - Entity-DTO dönüşümleri
3. **Controller** - REST endpoints
4. **Validation** - Input validation
5. **Exception Handling** - Global exception handler

Hangi modüle geçelim? 🚀