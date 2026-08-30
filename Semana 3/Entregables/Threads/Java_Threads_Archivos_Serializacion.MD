# Threading, Archivos y Serializacion en Java

---
***Estas son notas propias de estudio, aqui explico y comparo estos tres temas de Java 
que suelen aparecer juntos porque en la practica se usan combinados (@Charxies).***

Dentro de este MD vamos a ver tres cosas que a primera vista parecen distintas pero que en un
programa real casi siempre se juntan: **hilos (threading)**, **manejo de archivos** y
**serializacion**. Un ejemplo tipico: un hilo aparte que guarda un objeto serializado a disco
sin trabar el hilo principal.

---
## 1. Threading (hilos)

### ¿Que es un hilo?

Un **hilo ( en ingles thread)** es una linea de ejecucion independiente dentro de un programa. Por
defecto, cuando corremos un programa en Java, todo pasa en **un solo hilo** ( el
`main`). Si queremos que dos cosas pasen "al mismo tiempo" (o casi, dependiendo del CPU),
necesitamos abrir aun mas hilos .

### hilo nuevo: extender `Thread` o implementar `Runnable`?

```java
// extendiendo Thread
class MiHilo extends Thread {
    @Override
    public void run() {
        System.out.println("Corriendo en: " + Thread.currentThread().getName());
    }
}

// Runnable()
Runnable tarea = () -> System.out.println("Corriendo en: " + Thread.currentThread().getName());
Thread hilo = new Thread(tarea);
hilo.start(); // este es start() no run()
```

Si llamamos a `hilo.run()` directamente, este **NO** crea ningun hilo nuevo, 
simplemente se ejecuta ese codigo en el hilo actual. Al parecer el metodo que realmente manda a correr un hilo nuevo es `hilo.start()`.

Se prefiere `Runnable` sobre extender `Thread` porque en Java solo podemos heredar de
una clase, y si ya extendimos `Thread` no podemos heredar de nada mas. Con `Runnable`
mantenemos esa puerta abierta. (no sabia esto hasta que investigue mas, pero tiene mucho sentido)

###  la concurrencia

Cuando dos hilos leen y escriben la **misma variable** al mismo tiempo, podemos terminar con
resultados inconsistentes. A esto se le llama una **condicion de carrera (race condition)**.

```java
class Contador {
    private int valor = 0;

    public void incrementar() {
        valor++; // se puede ver como una sola operacion pero en realidad son 3
    }

    public int getValor() {
        return valor;
    }
}
```

la instruccion `valor++` parece solo una instruccion, pero en realidad son tres pasos: leer el valor, sumarle
1, y volver a guardarlo. Si dos hilos hacen esto exactamente al mismo tiempo, uno de los  incrementos se puede perder.

### `synchronized` como solucion

```java
public synchronized void incrementar() {
    valor++;
}
```

Con `synchronized` basicamente le decimos a Java: "Un metodo, por objeto a la vez". Los demas hilos que quieran entrar se quedan esperando su
turno de ahi **se sincornizan**. Esto soluciona la condicion de carrera, pero tiene un costo, si abusamos de
`synchronized` en todos lados, perdemos la ventaja de tener varios hilos, porque terminan
esperandose unos a otros todo el tiempo y terminan haciendo un deadlock.

### `ExecutorService` la solucion mas moderna

Crear hilos a mano (`new Thread()`) uno por uno no es muy escalable. Lo que se usa hoy en dia es
un **pool de hilos** (thread pool), administrado por un `ExecutorService`:

```java
ExecutorService pool = Executors.newFixedThreadPool(4); // 4 hilos disponibles

for (int i = 0; i < 10; i++) {
    int tareaId = i;
    pool.submit(() -> {
        System.out.println("Tarea " + tareaId + " en " + Thread.currentThread().getName());
    });
}

pool.shutdown(); // avisamos que ya no vamos a mandar mas tareas
```

Aqui se puede ver como mandamos 10 tareas pero solo tenemos 4 hilos reales: el pool va reutilizando
esos 4 hilos conforme se van desocupando. Esto es mucho mas eficiente que crear 10 hilos
nuevos, porque crear un hilo tiene un costo real de memoria y de tiempo.

hayq que tener cuidado con `pool.shutdown()` porque esto no mata los hilos de golpe, nada mas le dice al pool
"ya no aceptes tareas nuevas, pero termina las que ya tienes pendientes", esto deja que pasen metodos en proceso que ya no quieres que entren.

### Hilos asincronos: `Future` y `CompletableFuture`

Todo lo que vimos arriba con `pool.submit(Runnable)` tiene un problema: si nuestra tarea
**regresa un resultado**, no hay forma facil de recuperarlo, porque `Runnable` no regresa
nada `( es void)`. Para eso existe `Callable<T>`, que si regresa un valor, y `Future<T>`, que es
como un "recibo" que nos entrega el `ExecutorService` de inmediato, y que mas adelante podemos
cambiar por el resultado real.

```java
ExecutorService pool = Executors.newFixedThreadPool(2);

Callable<Integer> tarea = () -> {
    Thread.sleep(2000); // hacemos que se tarde 2 seg
    return 42;
};

Future<Integer> resultado = pool.submit(tarea); // esto regresa de inmediato, no bloquea

Integer valor = resultado.get(); // AQUI SI se bloquea hasta que termine el resultado
System.out.println(valor);
```

**Ojito**: `pool.submit(tarea)` no bloquea nada, regresa el `Future` al instante. Lo que
bloquea es `resultado.get()`, porque ahi si le estamos pidiendo el valor ya calculado. Si
llamamos `.get()` demasiado pronto, el hilo que lo llama se queda esperando a que el otro hilo
termine.

`Future` funciona, pero es bastante limitado: no podemos encadenar acciones despues de que
termine, ni combinar varios `Future` entre si de forma comoda. Para eso, desde Java 8 tenemos
`CompletableFuture`, que es la forma moderna de manejar hilos asincronos:

```java
CompletableFuture<Integer> futuro = CompletableFuture.supplyAsync(() -> {
    // esto corre en otro hilo, tomado de un pool interno por defecto
    return 42;
});

futuro
    .thenApply(valor -> valor * 2)              // transformamos el resultado (86)
    .thenAccept(resultadoFinal -> System.out.println("Resultado: " + resultadoFinal))
    .exceptionally(error -> {                     // si algo truena en cualquier paso anterior
        System.out.println("Algo fallo: " + error.getMessage());
        return null;
    });
```

Lo que hace especial a `CompletableFuture` es que podemos **encadenar** pasos sin bloquear en
ningun momento con `.get()`. Cada paso (`thenApply`, `thenAccept`, `thenCompose`...) se ejecuta
solo, de forma asincrona, cuando el paso anterior termina. Nosotros preferimos
`CompletableFuture` sobre `Future` a la hora de encadenar trabajo asincrono, porque con
`Future` terminariamos llamando `.get()` en varios lugares y bloqueando el hilo cada vez.

Tambien se pueden combinar varios `CompletableFuture` entre si:

```java
CompletableFuture<Integer> futuroA = CompletableFuture.supplyAsync(() -> 10);
CompletableFuture<Integer> futuroB = CompletableFuture.supplyAsync(() -> 20);

CompletableFuture<Integer> suma = futuroA.thenCombine(futuroB, (a, b) -> a + b);

System.out.println(suma.join()); // 30, .join() es como .get() pero sin excepcion checked
```

### Virtual Threads (hilos virtuales, desde Java 21 con project loom que se me hizo interesante)

Todo lo que vimos hasta ahora (`Thread`, `ExecutorService`) trabaja con **hilos de plataforma
(platform threads)**: cada hilo de Java esta amarrado 1 a 1 con un hilo real del sistema
operativo. Crear miles de estos hilos es caro, porque cada uno es memoria reservada y el sistema
operativo tiene que andar cambiando de contexto entre todos ellos.

Aparecieron desde Java 21, los **Virtual Threads**, son hilos manejados por la propia JVM, mucho mas
ligeros, que **no** estan amarrados 1 a 1 con un hilo del sistema operativo. La JVM puede correr
**miles o millones** de hilos virtuales sobre un puñado de hilos reales.
---
## 2. Manejo de archivos

### Dos Formas: `java.io` y `java.nio.file`

Java tiene dos formas de trabajar con archivos: la clasica (`java.io.File`, `FileReader`,
`FileWriter`...) y la moderna (`java.nio.file.Path`, `Files`). aqui un ejemplo de como se utiliza path y Files:

```java
Path path = Path.of("datos/empleados.txt");

// Escribir todo el contenido de una vez
Files.writeString(path, "John,Doe,john@correo.com\n");

// Leer todo el contenido de una vez
String contenido = Files.readString(ruta);
System.out.println(contenido);

// Leer linea por linea (mejor para archivos grandes)
List<String> lineas = Files.readAllLines(ruta);
lineas.forEach(System.out::println);
```

Esto reemplaza bastante codigo que antes se tenia que escribir a mano con `BufferedReader` y
`try/catch`, personalmente me tocaron esos tiempos.

### `try-with-resources`: cerrar archivos sin olvidarnos

Cuando abrimos un archivo, siempre tenemos que **cerrarlo** despues, si no se queda un recurso
del sistema operativo ocupado que puede causar fugas de memoria o
que el archivo se quede bloqueado.

```java
// Forma vieja, propensa a que se nos olvide cerrar en un catch/return temprano
BufferedReader br = new BufferedReader(new FileReader("datos.txt"));
try {
    String linea = br.readLine();
    // ...
} finally {
    br.close();
}

// Forma moderna: try-with-resources
try (BufferedReader br = new BufferedReader(new FileReader("datos.txt"))) {
    String linea = br.readLine();
    // ...
} // Java cierra "br" solo, aunque truene una excepcion adentro
```

### Streams de bytes vs streams de caracteres

- `InputStream` / `OutputStream` → trabajan con **bytes**, sirven para cualquier tipo de
  archivo (imagenes, PDFs, ejecutables, objetos serializados...).
- `Reader` / `Writer` → trabajan con **caracteres** (texto), y ya manejan la codificacion
  (UTF-8, etc) por nosotros.

Si vamos a leer texto, usamos `Reader`/`Writer`. Si vamos a leer cualquier otra cosa (o vamos a
guardar objetos serializados, que es el siguiente tema), usamos `InputStream`/`OutputStream`.

---
## 3. Serializacion

### ¿Que es serializar?

**Serializar** es convertir un objeto de Java (que vive en memoria RAM) en una secuencia de
bytes, para poder guardarlo en un archivo o mandarlo por la red. **Deserializar** es el proceso
inverso: agarrar esos bytes y reconstruir el objeto original.

```java
class Empleado implements Serializable {
    private String nombre;
    private int edad;

    // constructor, getters, setters...
}
```

Para que una clase se pueda serializar, tiene que implementar la interfaz `Serializable`.
**Ojito**: `Serializable` no tiene ningun metodo que implementar, es lo que se llama una
**"marker interface"** (interfaz marcadora), solo le sirve a Java para saber "esta clase esta
autorizada para convertirse en bytes".

### Guardar el objeto en disco

```java
Empleado emp = new Empleado("John", 30);

try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("empleado.dat"))) {
    oos.writeObject(emp);
}
```

### Leer un objeto desde disco

```java
try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream("empleado.dat"))) {
    Empleado emp = (Empleado) ois.readObject(); // hay que castear, regresa Object
}
```

### `serialVersionUID`: el detalle que nadie explica bien la primera vez

```java
class Empleado implements Serializable {
    private static final long serialVersionUID = 1L;
    // ...
}
```

Cuando serializamos un objeto, Java guarda por dentro un numero de version de la clase. Si
despues modificamos la clase (le agregamos un campo nuevo, por ejemplo) y tratamos de
deserializar un archivo viejo, Java compara ese numero. Si no coincide, nos avienta una
`InvalidClassException` en vez de dejarnos leer datos corruptos o desactualizados por
accidente.

### `transient` que es?  lo que NO queremos serializar

```java
class Sesion implements Serializable {
    private String usuario;
    private transient String passwordEnMemoria; // esto NUNCA se guarda en el archivo
}
```

La palabra clave `transient` le dice a Java "este campo no lo incluyas cuando serialices el
objeto". bastante util al usarlo para contraseñas, conexiones abiertas o cualquier cosa que no tenga sentido guardar como bytes (o que
seria un riesgo de seguridad guardar).

Cuando deserializamos de vuelta, ese campo `transient` **regresa con su valor por defecto**
(`null` para objetos, `0` para numeros, etc), no con el valor que tenia antes de guardarlo.

---
## 4. Ideas clave para recordar

- Un hilo se manda a correr con `.start()`, nunca con `.run()` directamente.
- `synchronized` evita condiciones de carrera, pero abusar de el nos quita el beneficio de
  tener varios hilos.
- Para muchas tareas cortas, mejor usar un `ExecutorService` (pool de hilos) que crear hilos a
  mano uno por uno.
- `Future` nos regresa un "recibo" de una tarea que corre en otro hilo; `.get()` bloquea hasta
  que el resultado este listo.
- `CompletableFuture` es la forma moderna de encadenar y combinar trabajo asincrono sin tener
  que bloquear con `.get()` en cada paso.
- Los **Virtual Threads** (Java 21) son hilos ligeros manejados por la JVM, ideales para tareas
  que pasan mucho tiempo esperando I/O; no ayudan en tareas puras de CPU.
- Preferimos `java.nio.file` (`Path`, `Files`) sobre la API vieja de `java.io.File` cuando se
  puede.
- `try-with-resources` cierra los recursos solo, aunque truene una excepcion adentro. Cualquier
  clase `AutoCloseable` puede usarse ahi.
- Para serializar, la clase tiene que implementar `Serializable` (marker interface, sin
  metodos).
- Siempre definir nuestro propio `serialVersionUID` a mano, para controlar cuando rompemos
  compatibilidad entre versiones.
- `transient` excluye un campo de la serializacion; al deserializar regresa con su valor por
  defecto.
