package org.example._1springdatajpa.service;

import jakarta.transaction.Transactional;
import org.example._1springdatajpa.dao.AlbumDAO;
import org.example._1springdatajpa.entity.Album;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AlbumServiceImplement implements AlbumService{

    private AlbumDAO albumDAO;

    @Autowired
    public AlbumServiceImplement(AlbumDAO albumDAO){
        this.albumDAO = albumDAO;
    }

    @Override
    public List<Album> findAll() {
        return albumDAO.findAll();
    }

    @Override
    public Album AlbumFindID(int id) {
        return albumDAO.AlbumFindID(id);
    }
    @Transactional
    @Override
    public Album albumSave(Album album) {
        return albumDAO.albumSave(album);
    }
    @Transactional
    @Override
    public void deleteByID(int id) { albumDAO.deleteByID(id);}
}
