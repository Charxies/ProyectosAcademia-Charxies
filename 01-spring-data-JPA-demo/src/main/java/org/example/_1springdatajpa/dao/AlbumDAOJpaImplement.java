package org.example._1springdatajpa.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.example._1springdatajpa.entity.Album;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AlbumDAOJpaImplement implements AlbumDAO{

    //entity manager para manejar lo que pediremos
    private EntityManager entityManager;

    //injeccion de constructor
    @Autowired
    public AlbumDAOJpaImplement(EntityManager entityManager){
        this.entityManager = entityManager;
    }

    //Hacer overrides de nuesta interfaz para definir comportamientos
    @Override
    public List<Album> findAll() {
        TypedQuery<Album> theQuery = entityManager.createQuery("from Album", Album.class);
        List<Album> albums = theQuery.getResultList();
        return albums;
    }

    @Override
    public Album AlbumFindID(int id) {
        Album album = entityManager.find(Album.class, id);
        return album;
    }

    @Override
    public Album albumSave(Album album) {
        Album newAlbum = entityManager.merge(album);
        return newAlbum;
    }

    @Override
    public void deleteByID(int id) {
        Album deletedAlbum = entityManager.find(Album.class,id);
        entityManager.remove(deletedAlbum);

    }
}
