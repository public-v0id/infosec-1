package ru.se.ifmo.infosec.lab1.controller;

import org.apache.commons.text.StringEscapeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.se.ifmo.infosec.lab1.dto.PostResponse;
import ru.se.ifmo.infosec.lab1.entity.Post;
import ru.se.ifmo.infosec.lab1.repository.PostRepository;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostRepository postRepository;

    public PostController(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @GetMapping
    public List<PostResponse> getPosts() {
        return postRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private PostResponse toResponse(Post post) {
        return new PostResponse(
                post.getId(),
                StringEscapeUtils.escapeHtml4(post.getTitle()),
                StringEscapeUtils.escapeHtml4(post.getContent())
        );
    }
}