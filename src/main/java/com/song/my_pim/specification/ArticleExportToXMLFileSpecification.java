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

        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();

            boolean includeDeleted =
                    includeDeletedOverride != null
                            ? includeDeletedOverride
                            : props.isIncludeDeleted();

            if (!includeDeleted) {
                ps.add(cb.isFalse(root.get(ExportConstants.DELETED)));
            }

            if (client != null) {
                ps.add(cb.equal(root.get(ExportConstants.CLIENT), client));
            }

            // status1..4 , need to configure it in property file
            addStatusFilter(ps, cb, root.get(ExportConstants.STATUS1), props.getStatuses().getStatus1());
            addStatusFilter(ps, cb, root.get(ExportConstants.STATUS2), props.getStatuses().getStatus2());
            addStatusFilter(ps, cb, root.get(ExportConstants.STATUS3), props.getStatuses().getStatus3());
            addStatusFilter(ps, cb, root.get(ExportConstants.STATUS4), props.getStatuses().getStatus4());

            return cb.and(ps.toArray(new Predicate[0]));
        };
    }

    private static void addStatusFilter(List<Predicate> ps,
                                        jakarta.persistence.criteria.CriteriaBuilder cb,
                                        jakarta.persistence.criteria.Path<Integer> statusPath,
                                        List<Integer> allowed) {
        if (allowed != null && !allowed.isEmpty()) {
            ps.add(statusPath.in(allowed));
        }
    }
}
