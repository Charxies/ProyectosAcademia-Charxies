package org.spring_data_jpa_jwt.dao;
import org.spring_data_jpa_jwt.entity.Album;
import java.util.List;

public interface AlbumDAO {
    //interfaz para definir contrato
    List<Album> findAll();
    Album AlbumFindID(int id);
    Album albumSave(Album album);
    void deleteByID(int id);
}
