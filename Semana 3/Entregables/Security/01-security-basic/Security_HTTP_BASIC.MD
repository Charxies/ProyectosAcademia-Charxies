# Security HTTP Basic (Spring security)

---
Proyecto: `01-security-basic` · Puerto usado: `8071` · Base de datos: `employee_directory` (Docker MySQL)

***Es de importancia recalcar que este proyecto no es de mi propiedad pero el analisis y 
explicacion si son de mi autoria (@Charxies).***

Dentro de este MD se explicara como se integro el modulo de **Spring Security** al proyecto, la razon, los pasos y explicacion de
lineas de codifgo para poder comprender bien el tema.

---
## 1. Conceptos

Antes de integrar estos temas es de importancia saber la diferencia entre **autorizacion y autenticacion**
ya que normalmente se confunden, ademas hay que aclarar que no todas las aplicaciones 
es necesario que tengan algun elemento de seguridad.

### Autenticación → "¿Quién eres? ¿de donde vienes?"

¿Que es la Autenticacion? el concepto es muy simple, en palabras muy resumidas es el proceso 
de ***verificar la identidad de alguien***.

En terminos dentro del ambito es cuando un usuario manda su **username y contraseña**, 
nuestro sistema lo compara contra lo que hay guardado (en este caso,una base de datos) y 
decide: ¿es esta persona quien dice ser, sí o no?

- Si las credenciales son correctas → la persona es quien dice ser **autenticada**.
- Si son incorrectas o no mandó ninguna → el servidor responde normalmente con un código
  HTTP **`401 Unauthorized`** **acceso no autorizado**.

### Autorización → "¿tu puedes hacer esto?"

Despues de ser verificado/autenticado sigue la parte de **autorización**, basicamente es la pregunta
 al usuario **¿Que puedes hacer, tienes permiso?**

Ejemplo, un usuario puede estar perfectamente autenticado (su usuario y contraseña son correctos) 
pero aun así no tener permiso para borrar un registro o crear uno nuevo, claro solo pasa si tuviera permisos.

- Si el usuario tiene el rol/permiso → se le permite continuar.
- Si no lo tiene → servidor responde con el código HTTP **`403 Forbidden`**

##### en resumen si algo falla se ve asi:
```
401 = no se quien eres      (fallo de AUTENTICACION)
403 = se quien eres, pero no puedes  (fallo de AUTORIZACION)
```

Ya con esta base ya se puede entender por qué es importante configurar
**dos cosas separadas**: de dónde salen los usuarios (autenticación) y qué
puede hacer cada uno (autorización).

---
## 2. Razon

Antes de agregar seguridad, **cualquier persona podía entrar a** `/api/employees` **y ver, crear o borrar empleados libremente**, 
sin pedir ninguna credencial. En este proyecto se agrega una capa de seguridad para que la API exija usuario y contraseña (autenticación), 
y además revise si ese usuario tiene permiso para la acción que está pidiendo (autorización).

---
## 3. Recursos

Para poder implementar lo anteriormente mencionado en nuestro proyecto utilizamos las siguientes dependencias de springboot:
``````html
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
``````
Cabe recalcar que en cuanto se agrega `spring-boot-starter-security` al
proyecto, **Spring Boot bloquea todo automáticamente** con una seguridad
genérica por defecto.Es por eso que el archivo `SecurityConfig.java` es donde se sobreescribe ese
comportamiento por defecto con reglas propias al proyecto.

---
## 4. Base de datos

Hay que entender que en vez de tener los usuarios escritos directo en Java, este proyecto los guarda
en dos tablas:

- `members` → guarda usuario, contraseña y si la cuenta está activa.
- `roles` → guarda los roles de cada usuario.

Lo anterior se ve algo parecido a la siguiente tabla:

| Usuario | Roles que tiene | Qué puede hacer   |
|---|---|-------------------|
| `john`  | `ROLE_EMPLOYEE` | lectura           |
| `mary`  | `ROLE_MANAGER` | crear / modificar |

**Ojito** en la base de datos el rol se guarda CON el prefijo
`ROLE_` (ejemplo `ROLE_EMPLOYEE`), pero en el código Java se escribe SIN
ese prefijo, ya que al asignar el role se le añade, esta es la forma correcta : `hasRole("MANAGER")`

---
## 5. Configuracion de seguridad: `SecurityConfig.java`

Nuestro archivo unico y central para toda la configuracion de seguridad (ojito creo es un singleton):
`SecurityConfig.java` (importante que tenga ese nombre)

Este archivo define dos "beans" (taggeado como ` @Bean`, tags que Spring usa para configurar el sistema), 
 cada uno resuelve una de las dos preguntas que se explicaron al principio: uno es para autenticación y el otro para autorización.

### 5.1 Authenticate BEAN
```java
@Bean
public UserDetailsService userDetailsService(DataSource theDataSource) {

    JdbcUserDetailsManager theUserDetailsManager = new JdbcUserDetailsManager(theDataSource);

    theUserDetailsManager.setUsersByUsernameQuery(
        "select user_id, pw, active from members where user_id=?");

    theUserDetailsManager.setAuthoritiesByUsernameQuery(
        "select user_id, role from roles where user_id=?");

    return theUserDetailsManager;
}
```
Este bean le dice a Spring exactamente cómo buscar un usuario: "cuando alguien
mande un user, búscalo así en `members`, y tambien busca sus roles en
`roles`". El `?` de cada consulta se reemplaza automáticamente por el
username que se envia a travez de la petición.

### 5.2 Authorization BEAN

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

    http.authorizeHttpRequests(configurer -> configurer
        .requestMatchers(HttpMethod.GET, "/api/employees").hasRole("EMPLOYEE")
        .requestMatchers(HttpMethod.GET, "/api/employees/**").hasRole("EMPLOYEE")
        .requestMatchers(HttpMethod.POST, "/api/employees").hasRole("MANAGER")
        .requestMatchers(HttpMethod.PUT, "/api/employees").hasRole("MANAGER")
        .requestMatchers(HttpMethod.PATCH, "/api/employees/**").hasRole("MANAGER")
        .requestMatchers(HttpMethod.DELETE, "/api/employees/**").hasRole("ADMIN")
        .anyRequest().authenticated());

    http.httpBasic(Customizer.withDefaults());

    http.csrf(csrf -> csrf.disable());

    http.sessionManagement(session ->
        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

    return http.build();
}
```
Esta es la parte de **autorización**, aquí ya se asume que el usuario paso lo autenticado, y ahora se decide que tiene permitido hacer:
- **`authorizeHttpRequests(...)`**: esta seria la lista de reglas. Por
  ejemplo, un `GET` a `/api/employees` requiere el rol `EMPLOYEE`, mientras
  que un `DELETE` requiere `ADMIN`.
  La última línea,`anyRequest().authenticated()`, es una regla que dice: cualquier otra ruta
  que no esté especificada arriba, como mínimo exige estar autenticado.
- **`httpBasic(Customizer.withDefaults())`**: se activa el mecanismo de
  autenticación default de **HTTP Basic**.
- **`csrf().disable()`**: CSRF es una protección pensada para  **sesiones con cookies** , como no usamos cookies, esta proteccion es trivial para nosotros por eso se deshabilita.
- **`sessionCreationPolicy(SessionCreationPolicy.STATELESS)`**: esto indica que
  el servidor **no guarda ninguna sesión**. Cada petición debe traer sus
  propias credenciales, siempre, porque el servidor no "recuerda" a nadie
  entre una petición y otra, basicamente por eso se llama STATELESS, no guarda estados.

---
## 6. Permisos

Con el filter chain que esta definido en la anterior seccion la tabla de permisos se va viendo algo parecido a esto:
Esto es lo que queda definido en el `filterChain`:

| Método | Ruta | Rol que se necesita |
|---|---|---|
| `GET`    | `/api/employees` y `/api/employees/{id}` | `EMPLOYEE` |
| `POST`   | `/api/employees` | `MANAGER` |
| `PUT`    | `/api/employees` | `MANAGER` |
| `PATCH`  | `/api/employees/{id}` | `MANAGER` |
| `DELETE` | `/api/employees/{id}` | `ADMIN` |

En este caso de nuestos usuarios jhon y mary:

- Jhon seria capaz de leer (usar `GET`) pero solamente eso.
- Mary seria capaz de Crear/Modificar ( usar `POST` , `PUT` , `PATCH`)

---
## 7. Cuidado con HTTP ( mejor usar HTTPS)

Es importante saber que cuando estamos utilizando este modulo de HTTP, nuestros datos al fluir por nuestros **REST Header**
se ven de la siguiente manera:

```
Authorization: Basic am9objp0ZXN0MTIz
```

Esto no es seguro ya que al utilizar HTTP parece que estamos "cifrando" los datos, pero no es asi
solo estamos codificando los datos a `Base64` lo cual es muy facil de revertir, por lo cual
usar este tipo de modulo es inseguro por si solo.

facilmente podriamos pasar un, (piquele al codigo) para revertir los datos y conseguirlos:
````bash
([System.Text.Encoding]::UTF8.GetString([Convert]::FromBase64String("am9objp0ZXN0MTIz")))
````
Si alguien intercepta esa petición (por ejemplo en una red wifi publica, usando wireshark u otro sniffer), puede obtener la contraseña en texto plano sin mucho esfuerzo. Por
eso HTTP Basic **solo debería usarse junto con HTTPS** en un proyecto real.

---
## 8. Configuración en `application.properties`

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/employee_directory
spring.datasource.username=springstudent
spring.datasource.password=springstudent

server.port=8071
```
Dentro de nuestra configuracion denominamos el puerto que vamos a utilizar en este caso `8071`

---
## 9.  Ideas clave para recordar

- **Autenticación** = ¿quién eres? Si falla → `401`.
- **Autorización** = ¿puedes hacer esto? Si falla → `403`.
- En `SecurityConfig.java`, esas dos responsabilidades están separadas en dos
  beans distintos: `userDetailsService` (autenticación) y `filterChain`
  (autorización).
- En la base de datos los roles llevan el prefijo `ROLE_`; en el código Java no.
- Al ser una API sin cookies de sesión, se usa `STATELESS` y se desactiva CSRF.
- HTTP Basic no cifra, solo codifica en Base64. Necesita HTTPS para ser
  realmente seguro, **siempre hay que tener en cuenta la seguridad de traslado**.

  