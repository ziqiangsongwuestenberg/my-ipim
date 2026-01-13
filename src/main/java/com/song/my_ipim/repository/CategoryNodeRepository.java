package com.song.my_ipim.repository;

import com.song.my_ipim.entity.category.CategoryNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CategoryNodeRepository extends JpaRepository<CategoryNode, Long> {

    List<CategoryNode> findByRootNode_IdAndHierarchyPathStartingWith(Long rootNodeId, String pathPrefix);

    List<CategoryNode> findByParentNodeIsNullOrderByIdAsc();

    List<CategoryNode> findByParentNode_IdOrderByIdAsc(Long parentId);


    @Query("""
        select c from CategoryNode c
        where lower(c.identifier) like lower(concat('%', :kw, '%'))
           or lower(c.name) like lower(concat('%', :kw, '%'))
        order by c.id
    """)
    List<CategoryNode> search(@Param("kw") String kw);
}
