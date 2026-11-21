package volodea.fruitful.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import volodea.fruitful.config.security.CustomUserDetails;
import volodea.fruitful.dto.PageResponse;
import volodea.fruitful.dto.post.CreatePostRequest;
import volodea.fruitful.dto.post.PostResponse;
import volodea.fruitful.service.PostService;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {
    private final PostService postService;

    @GetMapping
    public PageResponse<PostResponse> getFeed(
            @RequestParam(defaultValue = "0") int page
            , @RequestParam(defaultValue = "20") int size
    ) {
        return postService.getFeed(page, size);
    }

    @PostMapping
    public ResponseEntity<PostResponse> createPost(
            @AuthenticationPrincipal CustomUserDetails principal
            , @Valid @RequestBody CreatePostRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(postService.createPost(principal.getId(), request));
    }

    @GetMapping("/me")
    public PageResponse<PostResponse> getMyPosts(
            @AuthenticationPrincipal CustomUserDetails principal
            , @RequestParam(defaultValue = "0") int page
            , @RequestParam(defaultValue = "20") int size
    ) {
        return postService.getPostsByAuthor(principal.getId(), page, size);
    }
}
