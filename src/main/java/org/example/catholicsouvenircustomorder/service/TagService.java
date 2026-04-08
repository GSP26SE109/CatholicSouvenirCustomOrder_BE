package org.example.catholicsouvenircustomorder.service;

import org.example.catholicsouvenircustomorder.model.Tag;

import java.util.List;
import java.util.UUID;

public interface TagService {
    Tag create(UUID accountID, Tag tag);

    void delete(UUID accountId, UUID id);

    Tag findById(UUID id);

    List<Tag> resolveTags(List<String> tagNames);

    List<Tag> getAllTags();
}
