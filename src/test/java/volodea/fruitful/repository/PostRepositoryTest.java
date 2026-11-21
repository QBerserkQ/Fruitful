package volodea.fruitful.repository;

import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;
import volodea.fruitful.entity.Post;
import volodea.fruitful.entity.Role;
import volodea.fruitful.entity.User;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Transactional
class PostRepositoryTest {

    @Autowired PostRepository postRepository;
    @Autowired UserRepository userRepository;
    @Autowired
    EntityManager em;

    private User author;
    private User other;

    @BeforeEach
    void setUp() {
        author = newUser("author");
        other = newUser("other");
    }

    @Test
    void save_fillsIdAndCreatedAt() {
        Post saved = postRepository.saveAndFlush(
                Post.builder().author(author).content("hello").build());

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void feed_returnsNewestFirst_andLoadsAuthors() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        savePost(author, "old", now.minusMinutes(2));
        savePost(other, "middle", now.minusMinutes(1));
        savePost(author, "new", now);
        em.flush();
        em.clear(); // forget everything, so the entity graph is really tested

        Page<Post> page = postRepository.findAllByOrderByCreatedAtDescIdDesc(PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(Post::getContent)
                .containsExactly("new", "middle", "old");
        assertThat(page.getContent())
                .allSatisfy(p -> assertThat(Hibernate.isInitialized(p.getAuthor())).isTrue());
    }

    @Test
    void feed_sameCreatedAt_higherIdFirst() {
        OffsetDateTime t = OffsetDateTime.now(ZoneOffset.UTC);
        savePost(author, "first", t);
        savePost(author, "second", t);
        em.flush();
        em.clear();

        Page<Post> page = postRepository.findAllByOrderByCreatedAtDescIdDesc(PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(Post::getContent)
                .containsExactly("second", "first");
    }

    @Test
    void feed_paging_splitsResults() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        savePost(author, "1", now.minusMinutes(3));
        savePost(author, "2", now.minusMinutes(2));
        savePost(author, "3", now.minusMinutes(1));

        Page<Post> first = postRepository.findAllByOrderByCreatedAtDescIdDesc(PageRequest.of(0, 2));
        Page<Post> second = postRepository.findAllByOrderByCreatedAtDescIdDesc(PageRequest.of(1, 2));

        assertThat(first.getContent()).hasSize(2);
        assertThat(first.getTotalElements()).isEqualTo(3);
        assertThat(first.hasNext()).isTrue();
        assertThat(second.getContent()).hasSize(1);
        assertThat(second.hasNext()).isFalse();
    }

    @Test
    void findByAuthor_returnsOnlyThatAuthorsPosts() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        savePost(author, "mine-1", now.minusMinutes(1));
        savePost(other, "not-mine", now);
        savePost(author, "mine-2", now);

        Page<Post> page = postRepository
                .findByAuthor_IdOrderByCreatedAtDescIdDesc(author.getId(), PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(Post::getContent)
                .containsExactlyInAnyOrder("mine-1", "mine-2");
    }

    private User newUser(String name) {
        User u = new User();                       // adapt to how ConversationRepositoryTest creates users
        u.setUsername(name);
        u.setEmail(name + "@test.com");
        u.setPasswordHash("hash");
        u.setRole(Role.USER);                      // adjust the role type/value
        return userRepository.save(u);
    }

    private Post savePost(User a, String content, OffsetDateTime createdAt) {
        return postRepository.save(
                Post.builder().author(a).content(content).createdAt(createdAt).build());
    }
}