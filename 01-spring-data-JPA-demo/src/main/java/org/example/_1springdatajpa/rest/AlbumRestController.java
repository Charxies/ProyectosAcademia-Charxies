package org.example._1springdatajpa.rest;

import org.example._1springdatajpa.entity.Album;
import org.example._1springdatajpa.service.AlbumService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

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
    @PostMapping("/album")
    public Album postAlbum(@RequestBody Album album){
        album.setId(0);
        Album dbAlbum = albumService.albumSave(album);
        return  dbAlbum;
    }

    @PutMapping("/album")
    public Album updateAlbum(@RequestBody Album album){

        Album dbAlbum = albumService.albumSave(album);
        return dbAlbum;
    }

    @PatchMapping("/album/{albumId}")
    public  Album patchAlbum(@PathVariable int albumId, @RequestBody Map<String, Object> patchPayload){
        Album tempAlbum = albumService.AlbumFindID(albumId);
        if (tempAlbum == null) {
            throw new RuntimeException("No se encontro album ID: " + albumId);
        }
        if (patchPayload.containsKey("id")) {
            throw new RuntimeException(
                    "AlbumID no puede ser modificado. remueve 'id' del body request.");
        }
        Album patchedAlbum = jsonMapper.updateValue(tempAlbum, patchPayload);
        Album dbAlbum = albumService.albumSave(patchedAlbum);

        return dbAlbum;
    }
}

