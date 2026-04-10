package org.example.catholicsouvenircustomorder.controller;

import lombok.RequiredArgsConstructor;
import org.example.catholicsouvenircustomorder.model.Tag;
import org.example.catholicsouvenircustomorder.service.TagService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tag")
@RequiredArgsConstructor
public class TagController {
    private final TagService tagService;

    @GetMapping("/{tagId}")
    public Tag findById(@PathVariable("tagId") UUID tagId) {
        return tagService.findById(tagId);
    }

    @PostMapping
    public Tag create(
            @AuthenticationPrincipal UUID accountId,
            @RequestBody Tag tag) {
        return tagService.create(accountId, tag);
    }

    @DeleteMapping("/{tagId}")
    public void delete(
            @AuthenticationPrincipal UUID accountId,
            @PathVariable("tagId") UUID tagId) {
        tagService.delete(accountId, tagId);
    }

    @GetMapping("")
    public List<Tag> findAll() {
        return tagService.getAllTags();
    }
}
