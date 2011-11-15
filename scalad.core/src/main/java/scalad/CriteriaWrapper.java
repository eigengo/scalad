package scalad;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

/**
 * @author janmachacek
 */
class CriteriaWrapper {

	static <T> TypedQuery<T> getQuery(Query queryBuilder, EntityManager entityManager, Class<T> entityType) {
		final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		final CriteriaQuery<T> criteriaQuery = cb.createQuery(entityType);
		final Root<T> root = criteriaQuery.from(entityType);

		final Predicate predicate = cb.like(root.<String>get(queryBuilder.property()), queryBuilder.value().get().toString());

		criteriaQuery.where(predicate);

		return entityManager.createQuery(criteriaQuery);
	}
}
