package org.example._2springdatamongodemo.rest;

import org.example._2springdatamongodemo.entity.Album;
import org.example._2springdatamongodemo.service.AlbumService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AlbumRestController {
    private AlbumService albumService;
    private JsonMapper jsonMapper;

    @Autowired
    public AlbumRestController(AlbumService albumService, JsonMapper jsonMapper){
        this.albumService = albumService;
        this.jsonMapper = jsonMapper;
    }

    @GetMapping("/album_directory")
    public List<Album> findAll(){return albumService.findAll();}
    @GetMapping("/album_directory/{albumId}")
    public Album getAlbum(@PathVariable int albumId){
        Album album = albumService.AlbumFindID(albumId);
        if (album == null){throw new RuntimeException("No se encontro album ID: "+albumId);}
        return  album;
    }
    @PostMapping("/album_directory")
    public Album postAlbum(@RequestBody Album album){
        album.setId(0);
        Album dbAlbum = albumService.albumSave(album);
        return  dbAlbum;
    }

    @PutMapping("/album_directory")
    public Album updateAlbum(@RequestBody Album album){

        Album dbAlbum = albumService.albumSave(album);
        return dbAlbum;
    }

    @PatchMapping("/album_directory/{albumId}")
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
    @DeleteMapping("/album_directory/{albumId}")
    public String deleteAlbum(@PathVariable int albumId) {

        Album tempAlbum = albumService.AlbumFindID(albumId);

        if (tempAlbum == null) {
            throw new RuntimeException("No se encontro album ID: " + albumId);
        }

        albumService.deleteByID(albumId);

        return "ID del album borrado: " + albumId;
    }
}

