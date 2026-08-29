# Security JWT (Spring security)

---
Proyecto: `02-security-jwt` · Puerto usado: `8072` · Base de datos: `employee_directory` (Docker MySQL)

***Es de importancia recalcar que este proyecto no es de mi propiedad pero el analisis y 
explicacion si son de mi autoria (@Charxies).***

Dentro de este MD se explicara como se integro **JWT (JSON Web Token)** sobre el modulo de Spring Security,
la razon, los pasos y explicacion de lineas de codigo para poder comprender bien el tema. Este proyecto es
la evolucion directa de `01-security-basic`, asi que se va a comparar bastante contra ese.

---
## 1. Conceptos

Antes de todo hay que recordar la base del proyecto anterior: **autenticacion** (¿quien eres?) y
**autorizacion** (¿puedes hacer esto?). Eso no cambia. Lo que cambia en este proyecto es **como se demuestra**
que ya pasaste la autenticacion en cada peticion.

### ¿el problema con HTTP Basic?

En el proyecto `01-security-basic` cada peticion mandaba usuario y contraseña, siempre, en cada request.
Eso significa que la contraseña viaja **una y otra vez** por la red, en cada `GET`, cada `POST`, todo.
Aunque este codificada en Base64 (facilmente reversible, como comprobamos en el anterior MD), el punto es que la
contraseña esta expuesta en **todas** las peticiones.

### ¿Que es un JWT?

Un **JWT (JSON Web Token)** es basicamente un "boleto" que el servidor te da **una sola vez**, despues de
que mandaste tu usuario y contraseña correctamente. A partir de ahi, ya no vuelves a mandar tu contraseña:
solo enseñas el boleto en cada peticion.

Un JWT tiene 3 partes, separadas por puntos (`.`):

```
header.payload.firma
```

- **header** → el algoritmo que se uso para firmar.
- **payload** → los datos (quien eres, tus roles, cuando expira, etc).
- **firma** → lo que garantiza que nadie lo modifico, un hash por asi decirlo.

**Importante decir que el JWT va firmado, NO cifrado**. Cualquiera que agarre el token puede leer el
payload sin necesitar ninguna llave, nada mas decodificando Base64 (lo que tiene adentro). Por eso, **nunca hay que meter datos secretos dentro de un JWT**.

### La firma como forma de proteger

Lo que SI esta protegido es que nadie mas pueda **crear** o **modificar** un token valido, porque la firma
se genera con **una llave privada que solo tiene el servidor**. Si alguien cambia aunque sea un caracter del
payload (por ejemplo, para darse el rol de ADMIN), la firma ya no coincide y el servidor lo rechaza. Esto
se prueba mas adelante con el script.

##### La diferencia clave contra el HTTP Basic:

```
01-security-basic: usuario + contraseña en CADA peticion (Authorization: Basic ...)
02-security-jwt: usuario + contraseña UNA VEZ en /login, despues solo el token (Authorization: Bearer ...)
```

---
## 2. Razon

El problema de mandar la contraseña en cada peticion no es solo de exposicion, tambien es de
**rendimiento**: cada vez que llega un `Authorization: Basic`, Spring tiene que ir a la base de datos a
validar usuario y contraseña otra vez. Con JWT eso pasa **una sola vez**, en el login; despues el servidor
solo valida la firma del token, sin tocar la base de datos.

---
## 3. Recursos

Para implementar JWT se necesitaron dependencias extra sobre las que ya traia el proyecto 01:

``````html
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<!-- Soporte JWT (firmar, validar, leer) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security-oauth2-resource-server</artifactId>
</dependency>
``````
Ademas se necesitan dos archivos que no existian en el proyecto 01: un par de llaves RSA.

```
src/main/resources/certs/private.pem
src/main/resources/certs/public.pem
```

- La **privada** firma los tokens (solo el servidor la tiene, nunca sale de ahi).
- La **publica** los valida (puede compartirse, no sirve para firmar nada).

Este paradigma se le llama asimetria (una llave firma, otra valida) esta es la base de la **criptografia de llave publica**,
y es lo que permite que cualquiera pueda comprobar que un token es legitimo sin poder falsificar uno nuevo.

---
## 4. Base de datos

Aqui no cambia nada. Siguen siendo las mismas tablas:

- `members` → usuario, contraseña, si esta activo.
- `roles` → los roles de cada usuario.

Lo que cambia es **cuando** se consultan: con HTTP Basic se consultaban en cada peticion, aqui solo se
consultan **una vez** teoricamente solo durante el login. Despues de eso, los roles van metidos dentro del token.

---
## 5. Configuracion de seguridad: `SecurityConfig.java`

Aqui esta la diferencia mas grande contra el proyecto anterior: en el 01 habia **una sola** cadena de
filtros (`filterChain`). En este proyecto hay **dos**, porque hay dos formas distintas de entrar a la
aplicacion:

- `/api/auth/login` → se entra con usuario y contraseña (HTTP Basic), pero **solo se usa una vez**.
- `/api/employees` → se entra enseñando el token (Bearer), **en cada peticion**.

### 5.1 Autenticacion: taquilla (`/api/auth/**`)

```java
@Bean
@Order(1)
public SecurityFilterChain loginFilterChain(HttpSecurity http) throws Exception {

    http.securityMatcher("/api/auth/**");
    http.authorizeHttpRequests(configurer -> configurer.anyRequest().authenticated());
    http.httpBasic(Customizer.withDefaults());
    http.csrf(csrf -> csrf.disable());
    http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

    return http.build();
}
```

`securityMatcher("/api/auth/**")` le dice a esta cadena: "tu solo te encargas de estas rutas".
Como se puede notar, segumos usando `httpBasic()`, o sea que **aqui SI se manda usuario y contraseña**, pero es la
unica y sola parte de toda la aplicacion donde esto pasa.

### 5.2 Autorizacion: un pequeño cambio

```java
@Bean
@Order(2)
public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {

    http.authorizeHttpRequests(configurer -> configurer
            .requestMatchers(HttpMethod.GET,    "/api/employees").hasRole("EMPLOYEE")
            .requestMatchers(HttpMethod.GET,    "/api/employees/**").hasRole("EMPLOYEE")
            .requestMatchers(HttpMethod.POST,   "/api/employees").hasRole("MANAGER")
            .requestMatchers(HttpMethod.PUT,    "/api/employees").hasRole("MANAGER")
            .requestMatchers(HttpMethod.PATCH,  "/api/employees/**").hasRole("MANAGER")
            .requestMatchers(HttpMethod.DELETE, "/api/employees/**").hasRole("ADMIN")
            .anyRequest().authenticated());

    http.oauth2ResourceServer(oauth2 -> oauth2.jwt(
            jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

    http.csrf(csrf -> csrf.disable());
    http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

    return http.build();
}
```
las reglas de `authorizeHttpRequests(...)` son **exactamente las mismas**.
Lo que cambia ahora solo es esta siguiente linea de cod:

```java
http.oauth2ResourceServer(oauth2 -> oauth2.jwt(...));
```

Esta linea reemplaza al `httpBasic()`. A partir de aqui, **ya no aceptamos**
usuario y contraseña, ahora pedimos `Authorization: Bearer <token>`. Si intentamos mandar HTTP Basic contra
`/api/employees`, nos va rechazar con `401`, de ahora en adelante nos va a pedir el token.

### 5.3 Firmar y validar

```java
@Bean
public JwtEncoder jwtEncoder() {
    JWK jwk = new RSAKey.Builder(publicKey).privateKey(privateKey).build();
    JWKSource<SecurityContext> jwks = new ImmutableJWKSet<>(new JWKSet(jwk));
    return new NimbusJwtEncoder(jwks);
}

@Bean
public JwtDecoder jwtDecoder() {
    return NimbusJwtDecoder.withPublicKey(publicKey).build();
}
```

- `JwtEncoder` usamos la llave **privada** para **firmar** tokens nuevos
- `JwtDecoder` → usamos la llave **publica** para **validar** que el token que llega no se modifico

### 5.4 El traductor de roles: `jwtAuthenticationConverter()`

```java
private JwtAuthenticationConverter jwtAuthenticationConverter() {

    JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
    authoritiesConverter.setAuthoritiesClaimName("roles");
    authoritiesConverter.setAuthorityPrefix("");

    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);

    return converter;
}
```

Al parecer Spring busca un claim llamado `scope` dentro del
token y le pega el prefijo `SCOPE_` a cada rol que encuentra ahi. Lamentablemente como nuestro token no usa `scope`, sino un
claim propio de `roles` que **ya trae** nuestro prefijo `ROLE_` tenemos que manipularlo con `authoritiesConverter` esto se lee como: 

"Lee el claim **roles** `.setAuthoritiesClaimName("roles")`, y no le agregues ningun prefijo extra`.setAuthorityPrefix("")`, con esto
`hasRole("EMPLOYEE")` sigue funcionando exactamente igual que en el proyecto 01.

---
## 6. `AuthController.java` — la taquilla

Este archivo es nuevo, el unico lugar de toda la aplicacion donde todavia
recibiremos la contraseña.

```java
@PostMapping("/login")
public TokenResponse login(Authentication authentication) {

    Instant ahora = Instant.now();

    List<String> roles = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .filter(authority -> authority.startsWith("ROLE_"))
            .collect(Collectors.toList());

    JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("security-jwt")
            .issuedAt(ahora)
            .expiresAt(ahora.plusSeconds(ttlSeconds))
            .subject(authentication.getName())
            .claim("roles", roles)
            .build();

    JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();

    String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

    return new TokenResponse(token, "Bearer", ttlSeconds, authentication.getName());
}
```

Ojito porque este metodo **no recibe** un `@RequestBody` con usuario y contraseña.
Eso es porque para cuando este metodo se ejecuta, la **cadena de autenticacion** ya valido el HTTP Basic contra la base
de datos. Si las credenciales estuvieran mal, el request nunca llegaria hasta aqui (se hubiera cortado
antes con `401`).

---
## 7. Permisos

La tabla de permisos es **la misma** a la del proyecto 01, no hay cambios de nada:

| Método | Ruta | Rol que se necesita |
|---|---|---|
| `GET`    | `/api/employees` y `/api/employees/{id}` | `EMPLOYEE` |
| `POST`   | `/api/employees` | `MANAGER` |
| `PUT`    | `/api/employees` | `MANAGER` |
| `PATCH`  | `/api/employees/{id}` | `MANAGER` |
| `DELETE` | `/api/employees/{id}` | `ADMIN` |

Lo unico que cambia es **de donde** salen esos roles, ahora en vez de la DB ahora salen del Token

---
## 8. Token

En esta seccion que sigue describire que se puede hacer con el token que se genera

### 8.1 Conseguir el token

```bash
curl -s -u john:test123 -X POST http://localhost:8072/api/auth/login
```

Esto devuelve un JSON con el `accessToken`. A partir de aqui, ya no se vuelve a mandar `john:test123` en
ningun lado, solo el token.

### 8.2 Ver que lleva dentro (sin necesitar ninguna llave)

Hay una funcion `payload()` que agarra la parte de en medio del token (el payload) y la
decodifica de Base64, sin firma ni nada. Esto demuestra en la practica que **el JWT no es secreto**, nada
mas esta firmado. Por eso nunca hay que meter contraseñas o datos sensibles dentro de el, el mismo problema de HTTP Basic en cuando a "cifrado".

### 8.3 Usar el token

```bash
curl -H "Authorization: Bearer $TJ" http://localhost:8072/api/employees
```

Ya no se manda `-u usuario:pass`, se manda el token en el header `Authorization`, con el prefijo `Bearer`.

### 8.4 Lo que YA NO funciona

digamos tiramos un curl:  
```bash
curl -u jhon:test123 http://localhost:8072/api/employees
#daria un -> 401
```

Aunque ***Jhon*** sea ADMIN y su contraseña sea correcta, la cadena de autorizacion (la que protege `/api/employees`)
**ya no entiende HTTP Basic**, solo Bearer. La contraseña dejo de funcionar de esta manera.

### 8.5 Manipular el token

se puede agarrar un token valido y cambiar **un caracter de en medio de la firma**.

Tambien se puede armar un **token falso**, reescribiendo el payload para decir `"roles":["ROLE_ADMIN"]`, pero
dejando la firma original (que ya no corresponde a ese payload nuevo)

En ambos casos el servidor responderia `401`, **Se puede reescribir el payload todo lo que se quiera, pero
sin la llave privada es imposible recalcular una firma valida**, y el servidor lo detecta al instante. Esto
es justo lo que protege a JWT de que alguien se auto-otorgue permisos.

### 8.6 Caducidad del token

Siemrpe se tiene que terminar recordando que el proyecto tiene un `jwt.ttl-seconds` configurado en
`application.properties` (en este caso`3600` = 1 hora). Si se arranca con un TTL bajo se tendria que esperar mas de lo que dice el TTL para verlo caducar en vivo (65 segundos en vez de 3), porque
Spring tiene por defecto ***60 segundos de desfase*** (`JwtTimestampValidator`).

---
## 9. Configuración en `application.properties`

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/employee_directory
spring.datasource.username=springstudent
spring.datasource.password=springstudent

server.port=8072

rsa.private-key=classpath:certs/private.pem
rsa.public-key=classpath:certs/public.pem

jwt.ttl-seconds=3600
```

Se sigue usando la misma base de datos que en el proyecto 01, solo cambia el puerto (`8072` en vez de
`8071`). Se agregan las rutas a las llaves RSA y cuanto dura un
token antes de caducar.

---
## 10. Ideas clave para recordar

- La diferencia de fondo con el proyecto 01: la contraseña viaja **una sola vez** (al login), no en cada
  peticion.
- Un JWT va **firmado, no cifrado**: cualquiera puede leer el payload, nadie puede falsificar la firma sin
  la llave privada.
- Hay **dos cadenas de filtros**: una para `/api/auth/**` (HTTP Basic, taquilla) y otra para todo lo demas
  (Bearer, torniquete).
- La firma se hace con la llave **privada** (`JwtEncoder`), se valida con la llave **publica**
  (`JwtDecoder`). Es una via de un solo sentido: firmar solo lo puede hacer el servidor.
- Las reglas de autorizacion (`hasRole(...)`) no cambian respecto al proyecto 01, lo que cambia es de donde
  salen los roles.
- Nunca hay que meter datos secretos dentro del payload de un JWT.
- HTTP Basic ya no funciona contra `/api/employees` una vez que se activa `oauth2ResourceServer`, aunque
  las credenciales sean correctas.
