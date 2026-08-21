package org.example._1springdatajpa.dao;
import org.example._1springdatajpa.entity.Album;
import java.util.List;

public interface albumDAO {
    //interfaz para definir contrato
    List<Album> findAll();
    Album AlbumFindID(int id);
    Album albumSave(Album album);
    void deleteByID(int id);
}
