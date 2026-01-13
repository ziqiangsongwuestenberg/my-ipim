package com.song.my_ipim.specification;

import com.song.my_ipim.config.ExportJobProperties;
import com.song.my_ipim.entity.article.Article;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;


public class ArticleExportToXMLFileSpecification {
    private ArticleExportToXMLFileSpecification(){}

    public static Specification<Article> build(ExportJobProperties props) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();

            if (!props.isIncludeDeleted()) {
                ps.add(cb.isFalse(root.get("deleted")));
            }

            if (props.getClient() != null) {
                ps.add(cb.equal(root.get("client"), props.getClient()));
            }

            // status1..4 , need to configure it in property file
            addStatusFilter(ps, cb, root.get("status1"), props.getStatuses().getStatus1());
            addStatusFilter(ps, cb, root.get("status2"), props.getStatuses().getStatus2());
            addStatusFilter(ps, cb, root.get("status3"), props.getStatuses().getStatus3());
            addStatusFilter(ps, cb, root.get("status4"), props.getStatuses().getStatus4());

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
