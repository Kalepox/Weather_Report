package com.weather.report.repositories;

import java.util.List;

import com.weather.report.persistence.PersistenceManager;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

/**
 * Generic repository exposing basic CRUD operations backed by the persistence
 * layer.
 * <p>
 * Concrete repositories extend/compose this class to centralise common database
 * access
 * logic for all entities, as described in the README.
 *
 * @param <T>  entity type
 * @param <ID> identifier (primary key) type
 */
public class CRUDRepository<T, ID> {

  protected Class<T> entityClass;

  /**
   * Builds a repository for the given entity class.
   *
   * @param entityClass entity class handled by this repository
   */
  public CRUDRepository(Class<T> entityClass) {
    this.entityClass = entityClass;
  }

  /**
   * Given an entity class retrieves the name of the entity to be used in the
   * queries.
   * 
   * @return the name of the entity (to be used in queries)
   */
  protected String getEntityName() {
    Entity ea = entityClass.getAnnotation(jakarta.persistence.Entity.class);
    if (ea == null)
      throw new IllegalArgumentException("Class " + this.entityClass.getName() + " must be annotated as @Entity");
    if (ea.name().isEmpty())
      return this.entityClass.getSimpleName();
    return ea.name();
  }
  /**
   * Persists a new entity instance.
   *
   * @param entity entity to persist
   * @return persisted entity
   */
  public T create(T entity) {
    EntityManager em = PersistenceManager.getEntityManager();
    try{
      em.getTransaction().begin();// starts the search
      em.persist(entity);// finds aplace in DB for the entity
      em.getTransaction().commit();//inserts to the db
      return entity;
    }
    catch(Exception e){
      if(em.getTransaction().isActive()){
        em.getTransaction().rollback();
      }// if anything wrong happens it reverts the DB
      throw e;
    }finally{
      em.close();// closes the manager
    }
  }

  /**
   * Reads a single entity by identifier.
   *
   * @param id entity identifier (primary key)
   * @return found entity or {@code null} if absent
   */
  public T read(ID id) {
    EntityManager em = PersistenceManager.getEntityManager();// get the manager
    try{
      return em.find(entityClass, id);// returns the entity found by id
      // .find() is deined for EntitiyManager class you can go to Decleration for more details
    }
    finally{
      em.close();
    }
  }

  /**
   * Reads all entities of the managed type.
   *
   * @return list of all entities
   */
  public List<T> read() {
      EntityManager em = PersistenceManager.getEntityManager();
    try{
      String command =  "SELECT e FROM " + getEntityName() + " e"; // this sent to jpql to get data 
      TypedQuery<T> query = em.createQuery(command, entityClass); // turns into the data into java query
      return query.getResultList();// returns the query list
    }
    finally{
      em.close();
    }
  }

  /**
   * Updates an existing entity.
   *
   * @param entity entity with new state
   * @return updated entity
   */

  public T update(T entity) {// catch clause used for write operations

    EntityManager em = PersistenceManager.getEntityManager();
    try{
      em.getTransaction().begin();// starts
      T updated = em.merge(entity);// merges with the normal DB
      em.getTransaction().commit(); // commit the change to DB
      return(updated);// returns the updated DB
    }
    catch(Exception e){
      if(em.getTransaction().isActive()){
        em.getTransaction().rollback();
      }
      throw e;
    }
    finally{
      em.close();
    }

  }

  /**
   * Deletes an entity by identifier (primary key).
   *
   * @param id entity identifier (primary key)
   * @return deleted entity
   */
  public T delete(ID id) {

    EntityManager em = PersistenceManager.getEntityManager();
    try{
      em.getTransaction().begin();
      T entity = em.find(entityClass, id);// searchs for the entity
        if (entity != null) {// checks if the entity exitsts, we cannot use isPresent() because its not defined here
          em.remove(entity);
        }
      em.getTransaction().commit();
      return entity;
    } 
    catch(Exception e){
      if (em.getTransaction().isActive()) {
      em.getTransaction().rollback();
      }
      throw e;
    }
    finally{
      em.close();
    }

  }

}
