package org.example.catholicsouvenircustomorder.service.imp;

import lombok.AllArgsConstructor;
import org.example.catholicsouvenircustomorder.exception.ResourceNotFoundException;
import org.example.catholicsouvenircustomorder.model.Tag;
import org.example.catholicsouvenircustomorder.repository.TagRepository;
import org.example.catholicsouvenircustomorder.service.TagService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class TagServiceImp implements TagService {
    private final TagRepository tagRepository;
    @Override
    public Tag create(Tag tag) {
        return tagRepository.save(tag);
    }

    @Override
    public void delete(UUID id) {
        Tag tag = findById(id);
        tagRepository.delete(tag);
    }

    @Override
    public Tag findById(UUID id) {
        return tagRepository.findById(id).orElseThrow(()-> new ResourceNotFoundException("Tag không tồn tại"));
    }
    @Override
    public List<Tag> resolveTags(List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) return new ArrayList<>();

        List<Tag> existingTags = tagRepository.findByNameIn(tagNames);

        Set<String> existingNames = existingTags.stream()
                .map(Tag::getName)
                .collect(Collectors.toSet());

        List<Tag> newTags = tagNames.stream()
                .filter(name -> !existingNames.contains(name))
                .map(name -> {
                    Tag tag = new Tag();
                    tag.setName(name);
                    return tag;
                })
                .toList();

        tagRepository.saveAll(newTags);

        existingTags.addAll(newTags);
        return existingTags;
    }
}
