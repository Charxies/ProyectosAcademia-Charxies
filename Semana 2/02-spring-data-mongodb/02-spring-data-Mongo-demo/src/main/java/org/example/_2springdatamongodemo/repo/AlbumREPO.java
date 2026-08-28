package org.example._2springdatamongodemo.repo;

import org.example._2springdatamongodemo.entity.Album;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AlbumREPO extends MongoRepository<Album, String> {
    //interfaz para definir contrato, Mongo repo ya viene
    // con todos los metodos incluidos

}