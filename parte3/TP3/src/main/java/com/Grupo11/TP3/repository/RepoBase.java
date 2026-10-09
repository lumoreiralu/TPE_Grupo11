package com.Grupo11.TP3.repository;

/*Repositorio base(interface) común al resto de repositorios que extiende de Repository
 * @param <T> Tipo de la entidad que manipula
 * @param <ID> Identificador único de la entidad que manipula
*/

import org.hibernate.internal.util.Optional;
import org.springframework.data.repository.NoRepositoryBean;

import java.io.Serializable;
import java.util.List;

@NoRepositoryBean
public interface RepoBase <T, ID extends Serializable> extends org.springframework.data.repository.Repository<T,ID> {
    /**
     * Elimina una entidad de la BD.
     * @param deleted entidad a borrar.
     */
    void delete( T deleted);

    /**
     * Retorna un listado de las entidades almacenadas.
     * @return Listado de entidades.
     */
    List<T> findAll();

    /**
     * Busca y devuelve una entidad de acuerdo al id ingresado por parámetro.
     * @param id Identificador único de la entidad.
     * @return Entidad que coicide con el id ingresado.
     */
    Optional<T> findById(Long id);

    /**
     * Indica si existe la entidad con el id ingresado por parámetro.
     * @param id Identificador único de la entidad.
     * @return True en caso de existir, caso contraio, false.
     */
    boolean existsById(Long id);

    /**
     * Elimina una entidad correspondiente al id ingresado por parámetro.
     * @param id Identificador único de la entidad.
     */
    void deleteById(Long id);

    /**
     * Persiste una entidad ingresada por parámetro.
     * @param persisted entidad a persistir
     * @return retorna la entidad persistida con el id asignado.
     */
    T save( T persisted);
}
