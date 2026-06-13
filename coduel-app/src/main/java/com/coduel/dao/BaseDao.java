package com.coduel.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.util.List;

public abstract class BaseDao<T> {

    @PersistenceContext
    private EntityManager em;

    private final Class<T> entityClass;

    protected BaseDao(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    protected EntityManager em() {
        return em;
    }

    public T selectById(Long id) {
        return em.find(entityClass, id);
    }

    public T persist(T entity) {
        em.persist(entity);
        return entity;
    }

    public void flush() {
        em.flush();
    }

    /** Offset pagination (0-based page), ordered by id — reusable by any DAO. */
    public List<T> selectPage(int page, int size) {
        return em.createQuery("SELECT e FROM " + entityClass.getSimpleName() + " e ORDER BY e.id", entityClass)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();
    }

    public long count() {
        return em.createQuery("SELECT COUNT(e) FROM " + entityClass.getSimpleName() + " e", Long.class)
                .getSingleResult();
    }

    protected <R> TypedQuery<R> createQuery(String jpql, Class<R> resultClass) {
        return em.createQuery(jpql, resultClass);
    }

    protected T selectSingleOrNull(TypedQuery<T> query) {
        List<T> results = query.setMaxResults(1).getResultList();
        return results.isEmpty() ? null : results.get(0);
    }
}
