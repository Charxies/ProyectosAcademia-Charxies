package org.spring_data_jpa_jwt.service;

import org.spring_data_jpa_jwt.entity.Album;
import java.util.List;
//interfaz para definir contrato (mismo que DAO)
public interface AlbumService {
    List<Album> findAll();
    Album AlbumFindID(int id);
    Album albumSave(Album album);
    void deleteByID(int id);
}
