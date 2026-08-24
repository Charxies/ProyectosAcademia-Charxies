package org.example._2springdatamongodemo.service;

import org.example._2springdatamongodemo.entity.Album;

import java.util.List;

//interfaz para definir contrato (mismo que DAO)
public interface AlbumService {
    List<Album> findAll();
    Album albumFindID(String id);
    Album albumSave(Album album);
    void deleteByID(String id);
}
