package org.example._2springdatamongodemo.service;

import org.example._2springdatamongodemo.entity.Album;
import org.example._2springdatamongodemo.repo.AlbumREPO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AlbumServiceImplement implements AlbumService{

    private AlbumREPO albumREPO;

    @Autowired
    public AlbumServiceImplement(AlbumREPO albumREPO){
        this.albumREPO = albumREPO;
    }

    @Override
    public List<Album> findAll() {
        return albumREPO.findAll();
    }

    @Override
    public Album albumFindID(String id) {
        return albumREPO.findById(id).orElse(null);
    }
    @Override
    public Album albumSave(Album album) {
        return albumREPO.save(album);
    }
    @Override
    public void deleteByID(String id) { albumREPO.deleteById(id);}
}
