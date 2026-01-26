package com.song.my_pim.specification;

import com.song.my_pim.common.constants.ExportConstants;
import com.song.my_pim.config.ExportJobProperties;
import com.song.my_pim.entity.article.Article;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;


public class ArticleExportToXMLFileSpecification {
    private ArticleExportToXMLFileSpecification(){}

    public static Specification<Article> build(ExportJobProperties props, Integer client,
                                               Boolean includeDeletedOverride) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            boolean includeDeleted =
                    includeDeletedOverride != null
                            ? includeDeletedOverride
                            : props.isIncludeDeleted();

            if (!includeDeleted) {
                predicates.add(criteriaBuilder.isFalse(root.get(ExportConstants.DELETED)));
            }

            if (client != null) {
                predicates.add(criteriaBuilder.equal(root.get(ExportConstants.CLIENT), client));
            }

            // status1..4 , need to configure it in property file
            addStatusFilter(predicates, criteriaBuilder, root.get(ExportConstants.STATUS1), props.getStatuses().getStatus1());
            addStatusFilter(predicates, criteriaBuilder, root.get(ExportConstants.STATUS2), props.getStatuses().getStatus2());
            addStatusFilter(predicates, criteriaBuilder, root.get(ExportConstants.STATUS3), props.getStatuses().getStatus3());
            addStatusFilter(predicates, criteriaBuilder, root.get(ExportConstants.STATUS4), props.getStatuses().getStatus4());

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static void addStatusFilter(List<Predicate> predicates,
                                        jakarta.persistence.criteria.CriteriaBuilder cb,
                                        jakarta.persistence.criteria.Path<Integer> statusPath,
                                        List<Integer> allowed) {
        if (allowed != null && !allowed.isEmpty()) {
            predicates.add(statusPath.in(allowed));
        }
    }
}
