package org.example._1springdatajpa.service;

import org.example._1springdatajpa.entity.Album;
import java.util.List;
//interfaz para definir contrato (mismo que DAO)
public interface AlbumService {
    List<Album> findAll();
    Album AlbumFindID(int id);
    Album albumSave(Album album);
    void deleteByID(int id);
}
