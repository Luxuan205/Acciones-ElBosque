package com.accioneselbosque.auth.repository;

import com.accioneselbosque.auth.model.AccountStatus;
import com.accioneselbosque.auth.model.Investor;
import com.accioneselbosque.auth.model.SubscriptionType;
import org.springframework.data.jpa.domain.Specification;

public class InvestorSpecification {

    private InvestorSpecification() {}

    public static Specification<Investor> hasEmailContaining(String email) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%");
    }

    public static Specification<Investor> hasNameContaining(String name) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("fullName")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<Investor> hasStatus(String status) {
        return (root, query, cb) -> {
            try {
                AccountStatus statusEnum = AccountStatus.valueOf(status.toUpperCase());
                return cb.equal(root.get("accountStatus"), statusEnum);
            } catch (IllegalArgumentException e) {
                return cb.conjunction();
            }
        };
    }

    public static Specification<Investor> hasSubscriptionType(String type) {
        return (root, query, cb) -> {
            try {
                SubscriptionType subscriptionEnum = SubscriptionType.valueOf(type.toUpperCase());
                return cb.equal(root.get("subscriptionType"), subscriptionEnum);
            } catch (IllegalArgumentException e) {
                return cb.conjunction();
            }
        };
    }
}
