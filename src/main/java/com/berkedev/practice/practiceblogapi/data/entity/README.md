# ENTITY MODÜLÜ - ÖĞRENME ÇIKTILARI

## 1. ENTITY NEDİR VE SPRING ARKADA NE YAPAR?

### Basit Tanım
Entity = Veritabanı tablosunun Java class karşılığı

### Spring/Hibernate Arka Planda Ne Yapar?
```
1. Uygulama başlarken → @Entity class'ları taranır
2. Annotation'lar okunur → Metadata toplanır
3. SQL CREATE TABLE komutları oluşturulur
4. Database'de tablolar oluşturulur/güncellenir
5. Runtime'da Java kodunu SQL'e çevirir

Örnek:
user.setUsername("john") → UPDATE users SET username = 'john' WHERE id = ?
```

---

## 2. TODO APP VS BLOG API

### TODO App - Basit Entity
```java
@Entity
@Table(name = "todo")
public class Todo {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;
    
    private String title;
    private boolean completed;
    private LocalDateTime createdAt;
}
```
**Öğrenilenler:** Temel entity, lifecycle callbacks, tek tablo

### Blog API - İlişkili Entity'ler
```java
@Entity
@Table(name = "users")
public class User {
    @Id
    private Long id;
    
    @OneToMany(mappedBy = "author")
    private List<Post> posts;  // ← YENİ: İlişkiler!
}
```
**Yeni Öğrenilenler:** @OneToMany, @ManyToOne, @ManyToMany, FetchType, Cascade

---

## 3. ÖNEMLİ ANNOTATION'LAR

### @Entity
**Ne işe yarar:** Class'ı database tablosu olarak işaretler  
**Spring ne yapar:** Hibernate bu class'ı tarar ve tablo oluşturur

```java
@Entity
public class User { } // → CREATE TABLE user
```

---

### @Table
**Ne işe yarar:** Tablo ismini özelleştirme, constraint ekleme  
**Parametreler:**
- `name`: Tablo adı
- `uniqueConstraints`: Unique kolonlar
- `indexes`: Index'ler

```java
@Table(name = "users")  // PostgreSQL'de "user" reserved keyword!
```

**Neden "users"?** "user" PostgreSQL'de rezerve kelime, hata verir.

---

### @Id + @GeneratedValue
**Ne işe yarar:** Primary key tanımlama ve otomatik ID üretimi

```java
@Id
@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_seq")
@SequenceGenerator(name = "user_seq", sequenceName = "user_id_seq", allocationSize = 1)
private Long id;
```

**Parametreler:**
- `strategy`: ID nasıl üretilecek (SEQUENCE, IDENTITY, AUTO, TABLE)
- `generator`: Generator adı

**allocationSize = 1 neden?**
- Basit: Her insert'te 1 ID alır (1, 2, 3...)
- Production'da 50 kullan (performans için)

---

### @Column
**Ne işe yarar:** Column özelliklerini belirleme

**Önemli Parametreler:**
```java
@Column(
    nullable = false,          // NOT NULL
    unique = true,             // UNIQUE
    length = 255,              // VARCHAR(255)
    updatable = false,         // Güncellenemez
    columnDefinition = "TEXT"  // Custom SQL type
)
```

**Örnekler:**
```java
@Column(nullable = false, unique = true)
private String email;

@Column(nullable = false, updatable = false)
private LocalDateTime createdAt;  // Bir kez set edilir, değişmez

@Column(columnDefinition = "TEXT")
private String content;  // VARCHAR(255) yetmez, TEXT kullan
```

---

### @PrePersist / @PreUpdate
**Ne işe yarar:** Entity save/update edilmeden önce otomatik işlem

**Spring ne yapar:**
```
repository.save(user) çağrılır
    ↓
@PrePersist method çalışır → createdAt = now()
    ↓
INSERT INTO users (..., created_at) VALUES (..., '2025-10-03')
```

```java
@PrePersist
void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = null;
}

@PreUpdate
void onUpdate() {
    updatedAt = LocalDateTime.now();
}
```

---

### @OneToMany
**Ne işe yarar:** 1 User → Birçok Post ilişkisi (Bire-Çok)

**Spring ne yapar:**
- Foreign key child tabloya konur (posts tablosuna user_id)
- Parent'tan child'lara erişim sağlar (user.getPosts())

**Temel Kullanım:**
```java
@OneToMany(mappedBy = "author", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
private List<Post> posts = new ArrayList<>();
```

### Parametreler Detaylı:

#### 1. mappedBy (Zorunlu)
**Ne işe yarar:** İlişkinin "sahibini" (owner) belirtir

**Neden gerekli:** Bidirectional ilişkide foreign key hangi tabloda?

```java
// User entity (inverse side)
@OneToMany(mappedBy = "author")  // Post'taki "author" field'ına bak
private List<Post> posts;

// Post entity (owner side)
@ManyToOne
@JoinColumn(name = "user_id")
private User author;  // ← mappedBy bunu işaret eder
```

**Database sonucu:**
```sql
-- users tablosunda foreign key YOK
CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    username VARCHAR(255)
);

-- posts tablosunda foreign key VAR
CREATE TABLE posts (
    id BIGINT PRIMARY KEY,
    title VARCHAR(255),
    user_id BIGINT,  -- ← Foreign key burada
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

#### 2. cascade
**Ne işe yarar:** Parent'taki işlemler child'lara da uygulansın mı?

**Seçenekler ve anlamları:**
```java
CascadeType.ALL        // Tüm işlemler cascade olur
CascadeType.PERSIST    // save() cascade olur
CascadeType.MERGE      // update() cascade olur
CascadeType.REMOVE     // delete() cascade olur
CascadeType.REFRESH    // refresh() cascade olur
CascadeType.DETACH     // detach() cascade olur
```

**Örnekle anlayalım:**
```java
@OneToMany(mappedBy = "author", cascade = CascadeType.ALL)
private List<Post> posts;

// Kullanım:
User user = new User();
user.setUsername("john");

Post post1 = new Post();
post1.setTitle("My First Post");

Post post2 = new Post();
post2.setTitle("My Second Post");

user.getPosts().add(post1);
user.getPosts().add(post2);

userRepository.save(user);  // Sadece user save ediyoruz

// CASCADE sayesinde:
// 1. User save edilir
// 2. post1 save edilir (otomatik)
// 3. post2 save edilir (otomatik)
// ✅ 3 INSERT query çalışır
```

**CascadeType.REMOVE örneği:**
```java
@OneToMany(mappedBy = "author", cascade = CascadeType.REMOVE)
private List<Post> posts;

User user = userRepository.findById(1L);
userRepository.delete(user);

// Sonuç:
// 1. User silinir
// 2. User'ın tüm postları da silinir (cascade)
// ⚠️ Dikkatli kullan!
```

**Ne zaman kullanmalı:**
- ✅ Strong ownership: Order → OrderItems (cascade = ALL)
- ✅ Parent olmadan child anlamsız: User → Posts (cascade = ALL)
- ⚠️ Shared child: Post → Tags (cascade kullanma!)

#### 3. orphanRemoval
**Ne işe yarar:** İlişkiden çıkarılan child database'den de silinsin mi?

**Orphan (yetim) ne demek:** Parent'tan koparılan child

```java
@OneToMany(mappedBy = "author", orphanRemoval = true)
private List<Post> posts;

// Kullanım:
User user = userRepository.findById(1L);
Post firstPost = user.getPosts().get(0);

user.getPosts().remove(firstPost);  // Collection'dan çıkar
userRepository.save(user);

// orphanRemoval = true sayesinde:
// 1. Post user'ın listesinden çıkar
// 2. Post database'den de SİLİNİR
// DELETE FROM posts WHERE id = ?
```

**orphanRemoval vs CascadeType.REMOVE farkı:**
```java
// Senaryo 1: Parent silinir
@OneToMany(orphanRemoval = true)
userRepository.delete(user);
// ✅ Child'lar silinir

@OneToMany(cascade = CascadeType.REMOVE)
userRepository.delete(user);
// ✅ Child'lar silinir

// Senaryo 2: Collection'dan çıkarılır
@OneToMany(orphanRemoval = true)
user.getPosts().remove(post);
// ✅ Post database'den silinir

@OneToMany(cascade = CascadeType.REMOVE)
user.getPosts().remove(post);
// ❌ Post database'den silinmez (sadece ilişki kopar)
```

**Ne zaman kullanmalı:**
- ✅ Child parent olmadan anlamsız (User → Posts)
- ✅ Strong ownership ilişkilerinde
- ❌ Shared child'larda kullanma (Post → Tags)

#### 4. fetch
**Ne işe yarar:** Child'lar ne zaman yüklenecek?

**LAZY (Önerilen):**
```java
@OneToMany(fetch = FetchType.LAZY)
private List<Post> posts;

User user = userRepository.findById(1L);
// SQL: SELECT * FROM users WHERE id = 1
// posts henüz yüklenmedi ✅

List<Post> posts = user.getPosts();  // ŞİMDİ yüklenir
// SQL: SELECT * FROM posts WHERE user_id = 1
```

**EAGER (Önerilmez):**
```java
@OneToMany(fetch = FetchType.EAGER)
private List<Post> posts;

User user = userRepository.findById(1L);
// SQL: SELECT u.*, p.* FROM users u 
//      LEFT JOIN posts p ON u.id = p.user_id 
//      WHERE u.id = 1
// posts HEMEN yüklendi ⚠️ gereksiz!
```

**Neden LAZY tercih edilmeli:**
- 🚀 Performance: Sadece ihtiyaç olduğunda yüklenir
- 💾 Memory: Gereksiz data yüklenmez
- ⚡ N+1 problem riski azalır

---

### @ManyToOne
**Ne işe yarar:** Birçok Post → 1 User ilişkisi (Çoka-Bir)

**Spring ne yapar:**
- Foreign key bu entity'nin tablosuna konur (post tablosuna user_id)
- Child'dan parent'a erişim sağlar (post.getAuthor())

**Temel Kullanım:**
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id", nullable = false)
private User author;
```

### Parametreler Detaylı:

#### 1. fetch (ÇOK ÖNEMLİ!)
**⚠️ DİKKAT:** @ManyToOne'da default FetchType.EAGER! (Tehlikeli)

**LAZY (Mutlaka belirt):**
```java
@ManyToOne(fetch = FetchType.LAZY)  // ✅ LAZY kullan
private User author;

Post post = postRepository.findById(1L);
// SQL: SELECT * FROM posts WHERE id = 1
// author henüz yüklenmedi

User author = post.getAuthor();  // ŞİMDİ yüklenir
// SQL: SELECT * FROM users WHERE id = (post'un user_id'si)
```

**EAGER (Default, kullanma):**
```java
@ManyToOne  // fetch = FetchType.EAGER default! ⚠️
private User author;

Post post = postRepository.findById(1L);
// SQL: SELECT p.*, u.* FROM posts p 
//      LEFT JOIN users u ON p.user_id = u.id
//      WHERE p.id = 1
// author HEMEN yüklendi (gereksiz JOIN)
```

**N+1 Problem örneği:**
```java
// EAGER ile:
List<Post> posts = postRepository.findAll();  // 100 post

// Query'ler:
// 1. SELECT * FROM posts  (100 post gelir)
// 2-101. Her post için: SELECT * FROM users WHERE id = ?
// TOPLAM: 101 query! ⚠️ PERFORMANS SORUNU

// LAZY ile:
List<Post> posts = postRepository.findAll();
// Sadece 1 query: SELECT * FROM posts
// author'lara erişmedikçe ekstra query yok ✅
```

#### 2. cascade
**Ne işe yarar:** Child'daki işlemler parent'a yansısın mı?

**⚠️ @ManyToOne'da cascade genellikle KULLANILMAZ!**

```java
// BAD: ManyToOne'da cascade kullanma
@ManyToOne(cascade = CascadeType.ALL)  // ❌ Tehlikeli
private User author;

Post post = new Post();
post.setAuthor(new User("john"));  // Yeni user
postRepository.save(post);

// Sonuç:
// 1. Post save edilir
// 2. User da save edilir (cascade)
// ⚠️ Muhtemelen istemediğimiz davranış! User zaten var olmalı
```

**Neden kullanılmaz:**
- Parent (User) genellikle zaten database'de var
- Yeni parent oluşturmak nadiren istenir
- Shared parent'larda tehlikeli

**Nadir kullanım örneği:**
```java
// Strong ownership: OrderItem → Order
@ManyToOne(cascade = {CascadeType.PERSIST})
private Order order;
// OrderItem save edilince Order da save olur (nadir senaryo)
```

#### 3. optional
**Ne işe yarar:** Bu ilişki zorunlu mu?

```java
// Zorunlu ilişki (NULL olamaz)
@ManyToOne(optional = false)
@JoinColumn(name = "user_id", nullable = false)
private User author;
// Her post'un mutlaka bir author'u olmalı

// Opsiyonel ilişki (NULL olabilir)
@ManyToOne(optional = true)  // default
@JoinColumn(name = "category_id")
private Category category;
// Post category'siz olabilir
```

---

### @JoinColumn
**Ne işe yarar:** Foreign key column'unun özelliklerini belirler

**Spring ne yapar:** Database'de foreign key column oluşturur

**Temel Kullanım:**
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id", nullable = false)
private User author;
```

### Parametreler Detaylı:

#### 1. name
**Ne işe yarar:** Foreign key column adını belirler

```java
@JoinColumn(name = "user_id")
private User author;

// Database'de:
// ALTER TABLE posts ADD COLUMN user_id BIGINT
```

**Default davranış (name vermezsen):**
```java
@ManyToOne
private User author;

// Default: author_id (fieldName_id)
```

#### 2. nullable
**Ne işe yarar:** Foreign key NULL olabilir mi?

```java
// NULL olamaz (zorunlu ilişki)
@JoinColumn(name = "user_id", nullable = false)
private User author;
// ALTER TABLE posts ADD COLUMN user_id BIGINT NOT NULL

// NULL olabilir (opsiyonel ilişki)
@JoinColumn(name = "category_id", nullable = true)
private Category category;
// ALTER TABLE posts ADD COLUMN category_id BIGINT
```

#### 3. unique
**Ne işe yarar:** Bu foreign key unique olmalı mı? (One-to-One için)

```java
@JoinColumn(name = "user_id", unique = true)
private User author;
// Her user sadece 1 post yazabilir (nadir kullanım)
```

#### 4. foreignKey
**Ne işe yarar:** Foreign key constraint'in adını özelleştirir

```java
@JoinColumn(
    name = "user_id",
    foreignKey = @ForeignKey(name = "fk_post_user")
)
private User author;

// Database'de:
// CONSTRAINT fk_post_user FOREIGN KEY (user_id) REFERENCES users(id)
```

**Neden önemli:** Database migration'larda constraint adı gerekir

#### 5. referencedColumnName (Nadir)
**Ne işe yarar:** Parent'ın hangi column'una referans verilecek?

**Default:** Primary key'e (id) referans verir

```java
// Normal (id'ye referans)
@JoinColumn(name = "user_id")
private User author;
// FOREIGN KEY (user_id) REFERENCES users(id)

// Farklı column'a referans (nadir)
@JoinColumn(
    name = "user_email", 
    referencedColumnName = "email"
)
private User author;
// FOREIGN KEY (user_email) REFERENCES users(email)
```

---

### @ManyToMany
**Ne işe yarar:** Post ↔ Tag ilişkisi (Çoka-Çok)

**Spring ne yapar:**
- 3 tablo oluşturur: posts, tags, post_tags (ara tablo)
- Ara tablo otomatik yönetilir
- İki yönlü erişim sağlar

**Neden ara tablo gerekli:**
```
1 Post → Birden fazla Tag (Java, Spring, Database)
1 Tag → Birden fazla Post (Java tag'i 10 post'ta, Spring tag'i 5 post'ta)

→ Doğrudan foreign key çalışmaz!
→ Ara tablo gerekli: post_tags
```

### Owner vs Inverse Side

**ManyToMany'de 2 taraf var:**

#### Owner Side (Post entity):
```java
@ManyToMany(fetch = FetchType.LAZY)
@JoinTable(
    name = "post_tags",
    joinColumns = @JoinColumn(name = "post_id"),
    inverseJoinColumns = @JoinColumn(name = "tag_id")
)
private List<Tag> tags = new ArrayList<>();
```

#### Inverse Side (Tag entity):
```java
@ManyToMany(mappedBy = "tags", fetch = FetchType.LAZY)
private List<Post> posts = new ArrayList<>();
```

**Fark ne:**
- **Owner:** @JoinTable ile ara tabloyu tanımlar
- **Inverse:** mappedBy ile owner'ı işaret eder
- **Database'de:** Sadece 1 ara tablo oluşur

### Database Sonucu:
```sql
-- posts tablosu
CREATE TABLE posts (
    id BIGINT PRIMARY KEY,
    title VARCHAR(255),
    content TEXT
    -- tag_id YOK! ✅
);

-- tags tablosu  
CREATE TABLE tags (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255)
    -- post_id YOK! ✅
);

-- post_tags ara tablosu (JPA otomatik oluşturur)
CREATE TABLE post_tags (
    post_id BIGINT,
    tag_id BIGINT,
    PRIMARY KEY (post_id, tag_id),  -- Composite key
    FOREIGN KEY (post_id) REFERENCES posts(id),
    FOREIGN KEY (tag_id) REFERENCES tags(id)
);
```

**Ara tabloda örnek data:**
```sql
-- Post 1: "Spring Tutorial" → Java, Spring tag'leri
-- Post 2: "Database Guide" → Database, Spring tag'leri

post_tags:
post_id | tag_id
--------|-------
   1    |   1    -- Post 1 → Java
   1    |   2    -- Post 1 → Spring
   2    |   3    -- Post 2 → Database
   2    |   2    -- Post 2 → Spring (Spring tag'i 2 post'ta!)
```

---

### @ManyToMany Parametreleri:

#### 1. mappedBy (Inverse side'da zorunlu)
**Ne işe yarar:** Owner side'ı işaret eder

```java
// Owner (Post entity)
@ManyToMany
@JoinTable(...)
private List<Tag> tags;  // ← "tags" field adı

// Inverse (Tag entity)
@ManyToMany(mappedBy = "tags")  // ← Post'taki "tags" field'ını işaret et
private List<Post> posts;
```

**Neden gerekli:** JPA'ya "Post entity ara tabloyu yönetir" demiş olursun

#### 2. fetch
**Ne işe yarar:** İlişkili entity'ler ne zaman yüklenecek?

**LAZY (Default, doğru):**
```java
@ManyToMany(fetch = FetchType.LAZY)
private List<Tag> tags;

Post post = postRepository.findById(1L);
// SQL: SELECT * FROM posts WHERE id = 1
// tags henüz yüklenmedi ✅

List<Tag> tags = post.getTags();  // ŞİMDİ yüklenir
// SQL: SELECT t.* FROM tags t
//      JOIN post_tags pt ON t.id = pt.tag_id
//      WHERE pt.post_id = 1
```

**EAGER (Kullanma):**
```java
@ManyToMany(fetch = FetchType.EAGER)
private List<Tag> tags;

Post post = postRepository.findById(1L);
// SQL: SELECT p.*, t.* FROM posts p
//      LEFT JOIN post_tags pt ON p.id = pt.post_id
//      LEFT JOIN tags t ON pt.tag_id = t.id
//      WHERE p.id = 1
// tags HEMEN yüklendi ⚠️
```

#### 3. cascade
**⚠️ ManyToMany'de cascade DİKKATLİ KULLAN!**

**Problem:**
```java
// BAD: CascadeType.REMOVE kullanma
@ManyToMany(cascade = CascadeType.REMOVE)  // ❌ Tehlikeli
private List<Tag> tags;

Post post = postRepository.findById(1L);
postRepository.delete(post);

// Sonuç:
// 1. Post silinir
// 2. Post'un tag'leri de silinir ⚠️
// 3. Diğer post'lar bu tag'leri kullanıyordu, onlar da kayboldu!
```

**Doğru yaklaşım:**
```java
// GOOD: Sadece gerekli cascade'ler
@ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})  // ✅
private List<Tag> tags;

// Veya hiç cascade kullanma
@ManyToMany
private List<Tag> tags;
```

**Ne zaman hangi cascade:**
```java
// CascadeType.PERSIST: Yeni tag oluştururken kullanışlı
Post post = new Post();
Tag newTag = new Tag("NewTag");
post.getTags().add(newTag);
postRepository.save(post);
// newTag da otomatik save edilir ✅

// CascadeType.MERGE: Tag güncellenirken
Tag tag = post.getTags().get(0);
tag.setName("Updated");
postRepository.save(post);
// tag da otomatik update edilir ✅

// CascadeType.REMOVE: KULLANMA! ❌
// Shared data silinir
```

---

### @JoinTable
**Ne işe yarar:** Ara tablonun özelliklerini belirler

**Sadece owner side'da kullanılır!**

**Temel Kullanım:**
```java
@ManyToMany
@JoinTable(
    name = "post_tags",
    joinColumns = @JoinColumn(name = "post_id"),
    inverseJoinColumns = @JoinColumn(name = "tag_id")
)
private List<Tag> tags;
```

### Parametreler Detaylı:

#### 1. name
**Ne işe yarar:** Ara tablo adını belirler

```java
@JoinTable(name = "post_tags")
// CREATE TABLE post_tags (...)
```

**Default davranış (name vermezsen):**
```java
@ManyToMany
private List<Tag> tags;
// Default: post_tag (entityName_fieldName)
```

#### 2. joinColumns
**Ne işe yarar:** Owner entity'nin foreign key'ini tanımlar

```java
@JoinTable(
    name = "post_tags",
    joinColumns = @JoinColumn(name = "post_id")  // Owner (Post) foreign key
)
```

**Ne demek:** Bu ara tabloda "post_id" column'u Post entity'yi işaret eder

**Database sonucu:**
```sql
CREATE TABLE post_tags (
    post_id BIGINT,  -- ← joinColumns
    tag_id BIGINT,
    FOREIGN KEY (post_id) REFERENCES posts(id)
);
```

#### 3. inverseJoinColumns
**Ne işe yarar:** Diğer entity'nin (inverse) foreign key'ini tanımlar

```java
@JoinTable(
    name = "post_tags",
    joinColumns = @JoinColumn(name = "post_id"),
    inverseJoinColumns = @JoinColumn(name = "tag_id")  // Inverse (Tag) foreign key
)
```

**Ne demek:** Bu ara tabloda "tag_id" column'u Tag entity'yi işaret eder

**Database sonucu:**
```sql
CREATE TABLE post_tags (
    post_id BIGINT,
    tag_id BIGINT,  -- ← inverseJoinColumns
    FOREIGN KEY (tag_id) REFERENCES tags(id)
);
```

#### 4. uniqueConstraints (Opsiyonel)
**Ne işe yarar:** Ara tabloda unique constraint ekler

```java
@JoinTable(
    name = "post_tags",
    joinColumns = @JoinColumn(name = "post_id"),
    inverseJoinColumns = @JoinColumn(name = "tag_id"),
    uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "tag_id"})
)
```

**Ne demek:** Aynı post-tag çifti 2 kez eklenemez

**Database sonucu:**
```sql
CREATE TABLE post_tags (
    post_id BIGINT,
    tag_id BIGINT,
    PRIMARY KEY (post_id, tag_id),  -- Zaten unique
    UNIQUE (post_id, tag_id)  -- Ekstra unique constraint (genellikle gereksiz)
);
```

**Not:** Primary key zaten (post_id, tag_id) olduğu için uniqueConstraints genellikle gereksiz

#### 5. indexes (Opsiyonel)
**Ne işe yarar:** Ara tabloya index ekler (performance için)

```java
@JoinTable(
    name = "post_tags",
    joinColumns = @JoinColumn(name = "post_id"),
    inverseJoinColumns = @JoinColumn(name = "tag_id"),
    indexes = {
        @Index(name = "idx_post_tags_post", columnList = "post_id"),
        @Index(name = "idx_post_tags_tag", columnList = "tag_id")
    }
)
```

**Ne işe yarar:** Query performansı artırır

```sql
-- Index sayesinde bu query hızlı:
SELECT * FROM post_tags WHERE post_id = 1;  -- idx_post_tags_post kullanır

SELECT * FROM post_tags WHERE tag_id = 5;   -- idx_post_tags_tag kullanır
```

---

### ManyToMany Kullanım Örneği:

```java
// 1. Yeni post ve tag'ler oluştur
Post post = new Post();
post.setTitle("Spring Boot Tutorial");

Tag javaTag = new Tag();
javaTag.setName("Java");

Tag springTag = new Tag();
springTag.setName("Spring");

// 2. İlişki kur
post.getTags().add(javaTag);
post.getTags().add(springTag);

// 3. Save et
postRepository.save(post);

// Sonuç:
// INSERT INTO posts (...) VALUES (...)
// INSERT INTO tags (name) VALUES ('Java')
// INSERT INTO tags (name) VALUES ('Spring')
// INSERT INTO post_tags (post_id, tag_id) VALUES (1, 1)  -- Java
// INSERT INTO post_tags (post_id, tag_id) VALUES (1, 2)  -- Spring
```

**Bidirectional sync için helper method (opsiyonel):**
```java
public class Post {
    public void addTag(Tag tag) {
        tags.add(tag);
        tag.getPosts().add(this);  // Ters tarafı da sync et
    }
    
    public void removeTag(Tag tag) {
        tags.remove(tag);
        tag.getPosts().remove(this);
    }
}
```

---

## 4. LOMBOK ANNOTATIONS

### @Getter / @Setter
Otomatik getter/setter oluşturur
```java
@Getter @Setter
private String username;
// → getUsername(), setUsername() otomatik oluşur
```

### @NoArgsConstructor
JPA için gerekli! Parametresiz constructor oluşturur
```java
@NoArgsConstructor
// → public User() {}
```

### @AllArgsConstructor
Tüm field'larla constructor
```java
@AllArgsConstructor
// → public User(Long id, String username, ...) {}
```

### @Builder
Builder pattern (fluent API)
```java
@Builder
User user = User.builder()
    .username("john")
    .email("john@mail.com")
    .build();
```

**⚠️ @Builder kullanırken @NoArgsConstructor + @AllArgsConstructor gerekli!**

---

## 5. BEST PRACTICES

### ✅ İYİ

**1. FetchType.LAZY kullan**
```java
@ManyToOne(fetch = FetchType.LAZY)  // ✅
@OneToMany(fetch = FetchType.LAZY)  // ✅
```
**Neden:** Performance, N+1 problem önlenir

**2. ArrayList initialize et**
```java
@OneToMany(mappedBy = "author")
private List<Post> posts = new ArrayList<>();  // ✅
```
**Neden:** NullPointerException önlenir

**3. @Getter/@Setter kullan, @Data kullanma**
```java
@Getter @Setter @NoArgsConstructor  // ✅
```
**Neden:** @Data infinite loop yapar (toString, equals)

**4. updatable = false kullan**
```java
@Column(nullable = false, updatable = false)
private LocalDateTime createdAt;  // ✅
```
**Neden:** Timestamp'ler değişmemeli

**5. Helper methods yaz**
```java
public void addPost(Post post) {
    posts.add(post);
    post.setAuthor(this);  // İki tarafı sync et
}
```
**Neden:** Bidirectional ilişki sync kalır

---

### ❌ KÖTÜ

**1. FetchType.EAGER**
```java
@ManyToOne(fetch = FetchType.EAGER)  // ❌
```
**Neden kötü:** N+1 problem, gereksiz JOIN'ler

**2. Cascade = ALL her yerde**
```java
@ManyToMany(cascade = CascadeType.ALL)  // ❌
```
**Neden kötü:** Shared data silinebilir

**3. @Data entity'lerde**
```java
@Entity @Data  // ❌
```
**Neden kötü:** Infinite loop (toString), lazy loading exception

**4. ArrayList initialize etmeme**
```java
private List<Post> posts;  // ❌ (null)
```
**Neden kötü:** NullPointerException

**5. İlişkiyi tek taraflı set etme**
```java
post.setAuthor(user);  // ❌ (sadece bir taraf)
// user.getPosts() boş kalır!
```

---

## 6. GEÇMİŞ SOHBETTEN ÖNERİLER

### 1. PostgreSQL "user" Problemi
**Önerim:** `@Table(name = "users")` kullan
```java
@Table(name = "users")  // "user" reserved keyword
```

### 2. Category Entity'de @Data Hatası
**Senin kodin:**
```java
@Data  // ❌ Infinite loop riski
```

**Önerim:**
```java
@Getter
@Setter
@NoArgsConstructor  // ✅
```

### 3. FetchType.LAZY Vurgusu
Her @ManyToOne ve @OneToMany'de LAZY belirttim. Default EAGER tehlikeli, performans problemi yaratır.

### 4. TEXT Column Definition
Blog content ve comment için:
```java
@Column(columnDefinition = "TEXT")  // VARCHAR(255) yetmez
private String content;
```

### 5. Helper Methods Önerisi
Bidirectional ilişkilerde:
```java
public void addPost(Post post) {
    posts.add(post);
    post.setAuthor(this);
}
```

### 6. allocationSize = 1
Öğrenme projelerinde predictable ID'ler için:
```java
@SequenceGenerator(allocationSize = 1)
```

---

## 7. TODO APP → BLOG API GEÇİŞ

### TODO App'te Vardı
- ✅ @Entity, @Table, @Id
- ✅ @Column parametreleri
- ✅ @PrePersist, @PreUpdate
- ✅ Lombok basics

### Blog API'de Eklendi
- 🆕 @OneToMany, @ManyToOne, @ManyToMany
- 🆕 mappedBy, cascade, orphanRemoval
- 🆕 FetchType.LAZY stratejisi
- 🆕 @JoinColumn, @JoinTable
- 🆕 Bidirectional ilişkiler
- 🆕 Helper methods

---

## 8. SONRAKI ADIMLAR

✅ **Entity'ler tamamlandı!**

Sırada:
1. **Repository Layer** - Spring Data JPA, custom queries
2. **Service Layer** - Business logic, transaction management
3. **DTO & Mapper** - Entity-DTO dönüşümleri
4. **Controller** - REST endpoints
5. **Validation** - Input validation, custom validators

Hangi modüle geçelim? 🚀