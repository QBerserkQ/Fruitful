package volodea.fruitful.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import volodea.fruitful.dto.PageResponse;
import volodea.fruitful.dto.post.CreatePostRequest;
import volodea.fruitful.dto.post.PostResponse;
import volodea.fruitful.dto.user.UserSummaryResponse;
import volodea.fruitful.entity.Post;
import volodea.fruitful.entity.User;
import volodea.fruitful.exception.UserNotFoundException;
import volodea.fruitful.repository.PostRepository;
import volodea.fruitful.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Transactional
    public PostResponse createPost(Long authorId, CreatePostRequest request) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + authorId));

        Post post = Post.builder()
                .author(author)
                .content(request.content().trim())
                .build();

        return toPostResponse(postRepository.save(post));
    }

    private PostResponse toPostResponse(Post post) {
        return new PostResponse(
                post.getId(), post.getContent()
                , post.getCreatedAt()
                , new UserSummaryResponse(post.getAuthor().getId(), post.getAuthor().getUsername())
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> getFeed(int page, int size) {
       Pageable pageable = toPageable(page, size);

        Page<Post> posts = postRepository.findAllByOrderByCreatedAtDescIdDesc(pageable);

        return PageResponse.from(posts.map(this::toPostResponse));
    }

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> getPostsByAuthor(Long authorId, int page, int size) {
        Pageable pageable = toPageable(page, size);

        Page<Post> posts = postRepository.findByAuthor_IdOrderByCreatedAtDescIdDesc(authorId, pageable);

        return PageResponse.from(posts.map(this::toPostResponse));
    }

    private Pageable toPageable(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.clamp(size, 1, 20);

        return PageRequest.of(safePage, safeSize);
    }

}
