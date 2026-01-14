package com.song.my_pim.entity.category;

import com.song.my_pim.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "category_node",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "ux_category_node_root_identifier",
                        columnNames = {"root_node", "node_identifier"}
                )
        },
        indexes = {
                @Index(name = "ix_category_node_parent", columnList = "parent_node"),
                @Index(name = "ix_category_node_root", columnList = "root_node"),
                @Index(name = "ix_category_node_root_path", columnList = "root_node,hierarchy_path")
        }
)
@Getter
@Setter
public class CategoryNode extends BaseEntity {

    /**
     * root of the tree:
     * - for root node: root_node points to itself (root_node = id)
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "root_node", nullable = false)
    private CategoryNode rootNode;

    @Column(name = "node_identifier", nullable = false, length = 120)
    private String identifier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_node")
    private CategoryNode parentNode;

    @OneToMany(mappedBy = "parentNode", fetch = FetchType.LAZY)
    @OrderBy("sortNo asc, id asc")
    private List<CategoryNode> children = new ArrayList<>();

    @Column(name = "name", length = 200)
    private String name;

    @Column(name = "level_no", nullable = false)
    private Integer levelNo = 0;

    @Column(name = "sort_no", nullable = false)
    private Integer sortNo = 0;

    @Column(name = "hierarchy_path", nullable = false, length = 2000)
    private String hierarchyPath;

    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    /* ====== optional helpers ====== */

    @Transient
    public boolean isRoot() {
        return parentNode == null;
    }
}
