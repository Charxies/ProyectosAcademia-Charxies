package org.example._1springdatajpa.rest;

import org.example._1springdatajpa.service.AlbumService;
import tools.jackson.databind.json.JsonMapper;

public class AlbumRestController {
    private AlbumService albumService;
    private JsonMapper jsonMapper;

    @Autowired
    public AlbumRestController(AlbumService albumService, JsonMapper jsonMapper){
        this.albumService = albumService;
        this.jsonMapper = jsonMapper;
    }


}

