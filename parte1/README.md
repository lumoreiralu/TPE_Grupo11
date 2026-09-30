# TP Integrador Parte 1 - JDBC

Proyecto de persistencia Java utilizando el patrón **DAO + Abstract Factory** para soportar múltiples motores de base de datos (MySQL y Apache Derby).

---

## 🛠️ Requisitos Previos

* **Java JDK:** 17 o superior.
* **Maven:** 3.8+ (incluido en la mayoría de los IDEs).
* **Base de Datos:** MySQL Server corriendo localmente (puerto `3306` por defecto).
* **IDE:** IntelliJ IDEA / Eclipse.

---

## 🚀 Configuración Inicial (Post-Pull)

Para evitar errores de compilación o fallos con las claves foráneas y rutas de archivos tras hacer un `pull`, sigue estos pasos:

### 1. Ubicación de Recursos (`CSV`)
Los archivos `.csv` deben estar dentro de la carpeta de recursos de Maven:

```text
src/
└── main/
    ├── java/
    └── resources/
        └── data/
            ├── clientes.csv
            ├── productos.csv
            ├── facturas.csv
            └── facturas-productos.csv
```


### 2. Base de Datos en MySQL
CREATE DATABASE IF NOT EXISTS db_cliente_factura;

### 3. Cambio de Motor de Base de Datos
El switcheo entre motores se maneja desde una única constante en la clase Main.java:
// Cambiar a DBType.DERBY o DBType.MYSQL según el caso
private static final DBType MOTOR = DBType.MYSQL;

### 4. Reglas Importantes de Desarrollo
Estructura de Tablas (DDL):
Todos los IDs (idCliente, idProducto, idFactura) se manejan como INT (sin AUTO_INCREMENT), ya que provienen directamente de los datasets CSV.
Si agregas o modificas una Foreign Key, asegúrate de mantener exactamente el mismo tipo de dato en la tabla referenciada.
Orden de Borrado y Carga de Datos:
Borrado (BorrarDatos): Primero las tablas hijas/de relación (factura_producto, facturas) y al final las independientes (productos, clientes).
Carga (CargarDatosIniciales): Primero las tablas independientes (clientes, productos) y al final las dependientes (facturas, factura_producto).

### 5. Ejecución del Proyecto
Ejecuta la clase Main.java. El flujo automatizado realizará:
Selección y publicación del motor de base de datos.
Limpieza de tablas existentes.
Lectura y carga masiva desde los archivos CSV vía Apache Commons CSV.
Verificación de lecturas y consultas.
