package volodea.fruitful.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import volodea.fruitful.dto.PageResponse;
import volodea.fruitful.dto.post.CreatePostRequest;
import volodea.fruitful.dto.post.PostResponse;
import volodea.fruitful.entity.Post;
import volodea.fruitful.entity.User;
import volodea.fruitful.exception.UserNotFoundException;
import volodea.fruitful.repository.PostRepository;
import volodea.fruitful.repository.UserRepository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks
    private PostService postService;

    @Test
    void createPost_savesPostWithAuthorAndTrimmedContent() {
        User author = user();
        when(userRepository.findById(1L)).thenReturn(Optional.of(author));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        PostResponse response = postService.createPost(1L, new CreatePostRequest("  hello  "));

        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(captor.capture());
        assertThat(captor.getValue().getAuthor()).isSameAs(author);
        assertThat(captor.getValue().getContent()).isEqualTo("hello");
        assertThat(response.content()).isEqualTo("hello");
        assertThat(response.author().username()).isEqualTo("vova");
    }

    @Test
    void createPost_userNotFound_throwsAndSavesNothing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> postService.createPost(99L, new CreatePostRequest("hi")))
                .isInstanceOf(UserNotFoundException.class);

        verify(postRepository, never()).save(any());
    }

    @ParameterizedTest
    @CsvSource({
            "0, 1000, 0, 20",   // size too big -> 20
            "0, 0,    0, 1",    // size too small -> 1
            "-5, 10,  0, 10",   // negative page -> 0
            "3, 10,   3, 10"    // normal values stay as they are
    })
    void getFeed_clampsPageAndSize(int page, int size, int expectedPage, int expectedSize) {
        when(postRepository.findAllByOrderByCreatedAtDescIdDesc(any(Pageable.class)))
                .thenReturn(Page.empty());

        postService.getFeed(page, size);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(postRepository).findAllByOrderByCreatedAtDescIdDesc(captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(expectedPage);
        assertThat(captor.getValue().getPageSize()).isEqualTo(expectedSize);
    }

    @Test
    void getFeed_mapsPostsToResponse() {
        Post post = Post.builder()
                .id(5L)
                .author(user())
                .content("hello")
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
        when(postRepository.findAllByOrderByCreatedAtDescIdDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(post), PageRequest.of(0, 20), 1));

        PageResponse<PostResponse> result = postService.getFeed(0, 20);

        assertThat(result.content().getFirst().id()).isEqualTo(5L);
        assertThat(result.content().getFirst().author().username()).isEqualTo("vova");
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.hasNext()).isFalse();
    }

    private User user() {
        User u = new User();   // adapt if User has no setters
        u.setId(1L);
        u.setUsername("vova");
        return u;
    }
}