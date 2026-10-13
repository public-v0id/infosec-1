package ru.se.ifmo.infosec.lab1.repository;

import ru.se.ifmo.infosec.lab1.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {
}