

## Scripts

---
### Docker

#### == Creacion de containers ==

Utilizamos este script para generar un container en docker, con la imagen de **MYSQL 9.7**
este contenedor sera nuestra base de datos principal.


```powershell 
docker run --name mysql-9.7 -e MYSQL_ROOT_PASSWORD=admin -p 3306:3306 -d mysql:9.7 
```

Ya generada la base de datos utilizamos este siguiente script para generar un container 
con **PHPMyAdmin** para poder gestionar nuestra base de datos a travez de nuestro **Puerto 8000**

```powershell 
docker run --name mysql-9.7 -e MYSQL_ROOT_PASSWORD=admin -p 3306:3306 -d mysql:9.7 
```
---
### SQL

#### == Creacion Base de datos ==

Utilizamos estos siguientes "query's" dentro de nuesto gestor de SQL para generar la Base de datos
y generar un user admin tipo (springstudent = cualquier nombre que queramos)
````mysql
-- Dropea el user si ya existe en la DB
DROP USER if exists 'springstudent'@'%' ;

-- Crea el use springstudent y le da privilegios
CREATE USER 'springstudent'@'%' IDENTIFIED BY 'springstudent';

GRANT ALL PRIVILEGES ON * . * TO 'springstudent'@'%';
````

Ahora para generar nuestra base de datos, utilizamos lo siguiente: 
````mysql
CREATE DATABASE album_directory;
````
Para darle permiso a nuestro nuevo user y flushear los privilegios para no reiniciarlo: 
````mysql
GRANT ALL PRIVILEGES ON album_directory.* TO 'springstudent'@'%';
FLUSH PRIVILEGES;
````
#### == Creacion de tabla ==
Ahora apuntamos a nuestra base datos y creamos la tabla con nuestros datos que definimos en nuestro entity/dao:
````mysql
USE album_directory;

CREATE TABLE album (
    id INT AUTO_INCREMENT PRIMARY KEY,
    titulo VARCHAR(255) DEFAULT NULL,
    artista VARCHAR(255) DEFAULT NULL,
    genero VARCHAR(255)DEFAULT NULL,
    anio INT DEFAULT NULL
);
````
---

### Pruebas
#### == Pruebas con post ==

para hacer las pruebas con post utilizamos el siguiente 
script (lo voy a dejar formatteado y uno de prueba:

````bash
curl -X POST http://localhost:8080/api/albums ^
  -H "Content-Type: application/json" ^
  -d "{\"titulo\":\"Nutshell\",\"artista\":\"Alice in Chains\",\"genero\":\"Grunge\",\"anio\":1994}"
````
Formato de curl:
````bash
curl -X POST http://localhost:8080/api/albums ^
  -H "Content-Type: application/json" ^
  -d "{\"titulo\":\"\",\"artista\":\"\",\"genero\":\"\",\"anio\":}"
````