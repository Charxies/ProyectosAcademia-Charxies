# Security OAuth2 (Spring security + Keycloak)

---
Proyecto: `03-security-oauth2` · Puerto usado: `8073` · Keycloak: `8090` (Docker) · Base de datos: `employee_directory` (Docker MySQL, solo empleados)

***Es de importancia recalcar que este proyecto no es de mi propiedad pero el analisis y 
explicacion si son de mi autoria (@Charxies).***

Dentro de este MD explicaremos como integramos **OAuth2** usando **Keycloak** como servidor de identidad,
sobre el modulo de Spring Security. Este proyecto es la evolucion directa de `02-security-jwt`, asi que lo
vamos a otra vez compar frente a este nuevo proyecto.

---
## 1. Conceptos

Seguimos con lo mismo de siempre: **autenticacion** (¿quien eres?) y **autorizacion**
(¿puedes hacer esto?). Lo que cambia esta vez es **quien se encarga** de la
autenticacion, en este caso nos apoyamos de Keycloack que es un software de identity management Open-source que permite single sign-on .

### ¿diferencia con el proyecto 02?

En `02-security-jwt` nuestra propia API hacia todo: guardabamos usuarios en nuestra base de datos (tablas
`members` y `roles`), validabamos la contraseña, y ademas **firmabamos** los tokens con nuestra propia
llave privada. O sea eramos juez y parte: emitiamos el boleto y tambien vigilabamos el boleto, haciamos todo nosotros desde puerta cerrada
lo cual es excelente si lo que buscas es un ambiente privado y cerrado.

Tambien esto en un proyecto de practica esta bien, pero en el mundo real trae problemas de escalabilidad: si tenemos 10 APIs
distintas, ¿cada una va a tener su propia tabla de usuarios y sus propias llaves RSA? Ademas, ¿que pasa si
el usuario quiere loguearse con Google o con su cuenta de Microsoft? Nuestra API ya no podria encargarse
de nada de eso.

### ¿Que es OAuth2 (y Keycloak)?

**OAuth2** es un estandar que separa estas responsabilidades en piezas distintas. Aqui la pieza que usamos
para esto es **Keycloak**: un servidor de identidad que corre aparte (en este proyecto, en un contenedor de
Docker, puerto `8090`). Keycloak es quien ahora:

- Guarda los usuarios y contraseñas.
- Guarda los roles.
- Firma los tokens con su propia llave.

Nuestra API (`security-oauth2`, puerto `8073`) ya **no hace nada de eso**. Se convierte en lo que se llama
un **Resource Server**: solo sabe **validar** tokens que alguien mas emitio, y decidir con base en ellos si
dejamos pasar o no la peticion. Ya no emitimos nada, ya no guardamos nada de usuarios.

##### Las diferencia clave vs el proyecto 02:

```
02-security-jwt: firmabamos nuestros propios tokens, con nuestra llave privada
03-security-oauth2: Keycloak ahora firma los tokens, nosotros solo validamos 
```

### El nombre tecnico de este cambio

Dejamos un comentario clave en `SecurityConfig.java`: esta API ahora se volvio un **RESOURCE SERVER puro**.
Comparemos y veamos todo lo que **desaparecio**:

- Ya no tenemos `UserDetailsService` → los usuarios ya no estan aqui ahora estan en Keycloak.
- Ya no tenemos `JwtEncoder` → ya no emitimos tokens.
- Ya no tenemos `JwtDecoder` manual → la llave publica se descarga sola del emisor.
- Ya no tenemos llaves RSA (`.pem`) en el proyecto → no hay nada que guardar ni que rotar.
- Ya no tenemos `AuthController` → el login pasa en Keycloak, no aqui.

---
## 2. Razon

Separamos "quien emite el token" de "quien lo valida" para resolver el problema de escalar a muchas APIs:
todas pueden confiar en el **mismo** Keycloak, sin que cada una tenga que mantener su propia tabla de
usuarios ni sus propias llaves. Ademas, con Keycloak ya tenemos de fabrica cosas como login social,
recuperacion de contraseña, multifactor, etc, que en el proyecto 02 hubieramos tenido que programar a mano, ya con esto
es mucho mas facil de mantener y escalar.

---
## 3. Recursos

Las dependencias en `pom.xml` son las mismas que usamos en el proyecto 02:

``````html
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security-oauth2-resource-server</artifactId>
</dependency>
``````

No necesitamos ninguna dependencia extra de Keycloak en el `pom.xml` de la API. Todo el trabajo pesado de
Keycloak vive en su propio contenedor, keycloack esta separado de nuestro proyecto Java.

### 3.1 desglosando el script `keycloak-setup.sh`

Con este script configuramos Keycloak desde cero, estos son los pasos que hace:

```bash
kc create realms -s realm=academy -s enabled=true
```

Creamos un **realm** llamado `academy`. Un realm en Keycloak es como un espacio aislado de usuarios/roles
propio.

```bash
kc create clients -r academy -s clientId=employee-api -s enabled=true \
  -s publicClient=true -s directAccessGrantsEnabled=true -s standardFlowEnabled=true \
  -s 'redirectUris=["http://localhost:8073/*"]'
```

Registramos a nuestra API como un **client** dentro de ese realm. Atencion con
`directAccessGrantsEnabled=true`: esto habilita que podamos pedir un token mandando usuario y contraseña
directo por curl (el llamado **Resource Owner Password Grant**).

> El flujo de verdad es Authorization Code + PKCE, con navegador. Esto solo lo habilitamos para poder
> practicar en la terminal.

O sea que la forma en la que probamos en este proyecto (con `curl` y usuario/contraseña) **no es como lo
haria uno en produccion**, ahi normalmente el usuario ve una pantalla de login de Keycloak en el navegador.

```bash
for r in EMPLOYEE MANAGER ADMIN; do kc create roles -r academy -s name=$r; done
```

Crea los 3 roles de siempre, esta vez dentro de Keycloak (ya no en una tabla `roles` propia).

```bash
kc set-password -r academy --username $u --new-password test123 --temporary=false
```

`--temporary=false`: es obligatorio ponerlo. Sin eso, Keycloak marca la contraseña como "hay
que cambiarla al primer login" y el login por curl nos falla con un mensaje que no ayuda nada:
`"Account is not fully set up"`.

---
## 4. Base de datos

Aqui si hay un cambio real. Como bien lo anotaron `application.properties`:

```properties
# Fijate en lo que YA NO esta aqui: esta base de datos solo guarda EMPLEADOS.
# Las tablas members y roles ya no se usan: los usuarios viven en Keycloak.
```

La base de datos MySQL (`employee_directory`) sigue existiendo, pero ahora **solo** guardamos la tabla
`employee` (los datos de negocio). Las tablas `members` y `roles` que usabamos en los proyectos 01 y 02 ya
no le sirven a esta API para nada: toda la identidad de los usuarios vive del lado de Keycloak.

---
## 5. Configuracion de seguridad: `SecurityConfig.java`

en el proyecto 02 teniamos **dos** cadenas de filtros (una para el
login, otra para la API). Aqui **regresamos a una sola cadena**, porque ya no tenemos ningun endpoint de
login dentro de esta aplicacion, ahora pasamos todo a keycloak (que agusto).

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

    http.authorizeHttpRequests(configurer -> configurer
            .requestMatchers(HttpMethod.GET,    "/api/employees").hasRole("EMPLOYEE")
            .requestMatchers(HttpMethod.GET,    "/api/employees/**").hasRole("EMPLOYEE")
            .requestMatchers(HttpMethod.POST,   "/api/employees").hasRole("MANAGER")
            .requestMatchers(HttpMethod.PUT,    "/api/employees").hasRole("MANAGER")
            .requestMatchers(HttpMethod.PATCH,  "/api/employees/**").hasRole("MANAGER")
            .requestMatchers(HttpMethod.DELETE, "/api/employees/**").hasRole("ADMIN")
            .anyRequest().authenticated());

    http.oauth2ResourceServer(oauth2 -> oauth2.jwt(
            jwt -> jwt.jwtAuthenticationConverter(keycloakConverter())));

    http.csrf(csrf -> csrf.disable());
    http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

    return http.build();
}
```

**las reglas de `authorizeHttpRequests(...)` son
IDENTICAS a las que usamos en los proyectos**. Con esto nos queda claro que la autorizacion y la
autenticacion en verdad se pueden parecer en los anteriores proyectos, pero bien separadas en este diseño.

### 5.1 La llave publica cambia

En el proyecto 02 teniamos que declarar manualmente los beans `JwtEncoder` y `JwtDecoder`, apuntando a
archivos `.pem`. Aqui **ya no se ncecesita nada de eso**. Todo el trabajo lo hace una sola linea en
`application.properties`:

```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8090/realms/academy
```

en el propio archivo explica el por que:

> Con esto le decimos a Spring: "los tokens los emite ese servidor de alla". Spring va solo a esa URL,
> descarga la configuracion (`/.well-known/openid-configuration`), de ahi saca la direccion del JWKS y de
> ahi baja la llave publica. No hay que escribir NADA de criptografia: ni llaves, ni encoder, ni decoder.

dejamos que Spring haga todo ese trabajo solo, en automatico, nada mas con esa URL.

### 5.2 El traductor de roles: `keycloakConverter()`

Aqui si tuvimos que ajustar algo, porque Keycloak no acomoda los roles donde Spring los busca por defecto.

```java
private Converter<Jwt, AbstractAuthenticationToken> keycloakConverter() {

    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(SecurityConfig::extraerRoles);

    return converter;
}

@SuppressWarnings("unchecked")
private static Collection<GrantedAuthority> extraerRoles(Jwt jwt) {

    Map<String, Object> realmAccess = jwt.getClaim("realm_access");

    if (realmAccess == null || realmAccess.get("roles") == null) {
        return List.of();
    }

    List<String> roles = (List<String>) realmAccess.get("roles");

    return roles.stream()
            .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
            .toList();
}
```

El problema es que Keycloak mete los roles **anidados**, dentro de un claim llamado `realm_access`, y
**sin** el prefijo `ROLE_`:

```json
"realm_access": { "roles": ["EMPLOYEE", "MANAGER", "default-roles-academy"] }
```

En el proyecto 02 el ajuste era sencillo (`setAuthoritiesClaimName` + `setAuthorityPrefix`), porque el
claim era plano. Aqui, como esta anidado un nivel mas adentro, ya no funciona igual.
Ahora tuvimos que escribir un metodo propio (`extraerRoles`) que:

1. Saca el mapa `realm_access` del token.
2. Si no existe o no trae `roles`, regresamos una lista vacia (por seguridad, para no tronar).
3. Si existe, agarramos la lista de roles y le pegamos el prefijo `ROLE_` a cada uno.

Gracias a esto, `hasRole("EMPLOYEE")` sigue funcionando exactamente igual,
sin tocar ni una linea de las reglas de autorizacion.

---
## 6. ¿`AuthController`?

En el proyecto 02 teniamos un archivo `AuthController.java` que se encargaba del `/api/auth/login`. En este
proyecto **ese archivo lo quitamos por completo**. El login ya no pasa dentro de nuestra aplicacion: pasa
contra Keycloak, directamente al endpoint de token que expone Keycloak
(`/realms/academy/protocol/openid-connect/token`).

---
## 7. Permisos

Otra vez la tabla de permisos nos queda **identica** a la de los proyectos 01 y 02:

| Método | Ruta | Rol que se necesita |
|---|---|---|
| `GET`    | `/api/employees` y `/api/employees/{id}` | `EMPLOYEE` |
| `POST`   | `/api/employees` | `MANAGER` |
| `PUT`    | `/api/employees` | `MANAGER` |
| `PATCH`  | `/api/employees/{id}` | `MANAGER` |
| `DELETE` | `/api/employees/{id}` | `ADMIN` |

Ahora los roles salen de un token que **Keycloak** firmo, y nosotros solo lo traducimos con
`extraerRoles`.

---
## 8. Token

### 8.1 Pedir el token — ahora se lo pedimos a Keycloak

```bash
curl -s -X POST http://localhost:8090/realms/academy/protocol/openid-connect/token \
  -d grant_type=password -d client_id=employee-api \
  -d username=john -d password=test123
```

la URL ya no es la de la API (`8073`), es la de Keycloak (`8090`). Nuestra API ahora solo se entera cuando ese token llega 
despues en un`Authorization: Bearer`.

### 8.2 Usar el token contra la API

```bash
curl -H "Authorization: Bearer $TJ" http://localhost:8073/api/employees
```

Esto no cambia nada respecto al proyecto 02: seguimos mandando el token como `Bearer`. Lo unico que cambio
es quien lo emite.

### 8.3 HTTP Basic ya no existe

```bash
curl -u susan:test123 http://localhost:8073/api/employees
# -> 401
```
 Aqui HTTP Basic **ni siquiera podria funcionar aunque quisieramos**, porque nuestra API **ya no tiene tabla de usuarios ni contraseñas**. No hay
nada contra que comparar `susan:test123`.

### 8.4 token valido... emisor equivocado

Esta es la prueba que mas me parecio importante de todo el script. Si tomamos un token **valido** del
proyecto 02 (`localhost:8072`, firmado con la llave privada de ESE proyecto) y lo mandamos contra nuestra
API del proyecto 03:

```bash
curl -H "Authorization: Bearer $TOKEN_DEL_PROYECTO_02" http://localhost:8073/api/employees
# -> 401
```

Aunque el token sea un JWT perfectamente valido, con firma correcta, con roles correctos, nuestra API lo
rechaza. La razon es clave para entender OAuth2 de verdad:

> La confianza no es en "tener un JWT": es en **QUIEN lo firmo**.

Nuestro proyecto keycloak solo confia en tokens firmados por la llave publica que descargamos de **Keycloak**
(`localhost:8090`). Un token firmado por otra llave privada (la del proyecto 02) es, para efectos de esta
API, papel mojado, aunque tecnicamente sea un JWT bien formado.

---
## 9. Configuración en `application.properties`

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/employee_directory
spring.datasource.username=springstudent
spring.datasource.password=springstudent

server.port=8073

spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8090/realms/academy
```

Seguimos usando la misma base de datos que en los proyectos anteriores (solo que ya no usamos las tablas
`members`/`roles`), y cambiamos el puerto (`8073`). La linea nueva y mas importante de todo el archivo es
el `issuer-uri`, que es lo unico que le tenemos que decir a la API para que confie en Keycloak.

---
## 10. Ideas clave para recordar

- La diferencia de fondo con el proyecto 02: ahora **Keycloak** es quien guarda usuarios, contraseñas y
  firma los tokens. Nuestra API ya no hace nada de eso, solo valida.
- Nuestra API se convirtio en un **Resource Server puro**: no tenemos `UserDetailsService`, no tenemos
  `JwtEncoder`, no tenemos `AuthController`, no tenemos llaves `.pem` propias.
- Con una sola linea (`issuer-uri`), dejamos que Spring descargue solo toda la configuracion y la llave
  publica del emisor. No tenemos que programar nada de criptografia a mano.
- Las reglas de autorizacion (`hasRole(...)`) nos quedaron identicas en los 3 proyectos: la autorizacion es
  independiente de como hagamos la autenticacion.
- Keycloak anida los roles dentro de `realm_access.roles` y sin prefijo, por eso tuvimos que escribir un
  traductor de roles propio (`extraerRoles`).
- La base de datos de nuestra API ya solo guarda datos de negocio (empleados), no identidad de usuarios.
- Lo mas importante de todo el proyecto: la confianza en un JWT no es "es un JWT valido", es "¿quien lo
  firmo?". Un token de otro emisor, aunque sea valido, lo rechazamos.
