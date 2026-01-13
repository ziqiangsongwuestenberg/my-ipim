package com.song.my_ipim.service.categoryService;

import com.song.my_ipim.dto.category.CategoryNodeDto;
import com.song.my_ipim.entity.category.CategoryNode;
import com.song.my_ipim.repository.CategoryNodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryTreeService {

    private final CategoryNodeRepository repo;

    public List<CategoryNodeDto> getFullTree() {
        List<CategoryNode> all = repo.findAll();

        Map<Long, List<CategoryNode>> byParentId = all.stream()
                .collect(Collectors.groupingBy(n ->
                        n.getParentNode() == null ? 0L : n.getParentNode().getId()
                ));

        List<CategoryNode> roots = byParentId.getOrDefault(0L, List.of());

        return roots.stream()
                .map(r -> toDtoRecursive(r, byParentId))
                .toList();
    }

    public List<CategoryNodeDto> getChildren(Long parentId) {
        return repo.findByParentNode_IdOrderByIdAsc(parentId).stream()
                .map(n -> new CategoryNodeDto(n.getId(), n.getIdentifier(), n.getName(), List.of()))
                .toList();
    }

    public List<CategoryNodeDto> search(String kw) {
        return repo.search(kw).stream()
                .map(n -> new CategoryNodeDto(n.getId(), n.getIdentifier(), n.getName(), List.of()))
                .toList();
    }

    private CategoryNodeDto toDtoRecursive(CategoryNode node, Map<Long, List<CategoryNode>> byParentId) {
        List<CategoryNode> children = byParentId.getOrDefault(node.getId(), List.of());

        List<CategoryNodeDto> childDtos = children.stream()
                .map(c -> toDtoRecursive(c, byParentId))
                .toList();

        return new CategoryNodeDto(
                node.getId(),
                node.getIdentifier(),
                node.getName(),
                new ArrayList<>(childDtos)
        );
    }
}
