package org.example._1springdatajpa.rest;

import org.example._1springdatajpa.entity.Album;
import org.example._1springdatajpa.service.AlbumService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

public class AlbumRestController {
    private AlbumService albumService;
    private JsonMapper jsonMapper;

    @Autowired
    public AlbumRestController(AlbumService albumService, JsonMapper jsonMapper){
        this.albumService = albumService;
        this.jsonMapper = jsonMapper;
    }

    @GetMapping("/album")
    public List<Album> findAll(){return albumService.findAll();}
    @GetMapping("/album/{albumId}")
    public Album getAlbum(@PathVariable int albumId){
        Album album = albumService.AlbumFindID(albumId);
        if (album == null){throw new RuntimeException("No se encontro album ID: "+albumId);}
        return  album;
    }
}

