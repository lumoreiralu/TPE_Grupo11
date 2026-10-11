# 🚀 TP3 - Ejercicio Integrador (API REST con Spring Boot)

Sistema backend desarrollado en **Java** con **Spring Boot** para la gestión y registro de estudiantes, carreras e inscripciones académicas, implementando servicios REST y persistencia con bases de datos relacionales.

---

## 🛠️ Tecnologías y Herramientas
* **Java** 
* **Spring Boot** (Spring Web, Spring Data JPA)
* **Hibernate**
* **MySQL** / Base de datos relacional
* **Postman** (para pruebas de la API REST)

---

## 📊 Modelo de Datos
El sistema se compone de tres entidades principales:
1. **Estudiante:** Información personal (DNI, nombre, apellido, edad, género, ciudad de residencia) y número de libreta universitaria (LU).
2. **Carrera:** Datos de la carrera (ID, nombre, duración).
3. **EstudianteCarrera (Tabla Intermedia):** Relación de inscripción que gestiona el año de inscripción, el estado de graduación y la vinculación entre estudiante y carrera.

---

## 🔌 Endpoints de la API REST (Servicios)

El sistema expone las siguientes operaciones implementadas según los requerimientos del trabajo práctico:

| Inciso | Operación | Método | Endpoint |
| :---: | :--- | :---: | :--- |
| **a** | Dar de alta un estudiante | `POST` | `/estudiantes` |
| **b** | Matricular estudiante en carrera | `POST` | `/estudianteCarrera` |
| **c** | Listar estudiantes ordenados por apellido | `GET` | `/estudiantes/ordenados-por-apellido` |
| **d** | Buscar estudiante por Libreta Universitaria | `GET` | `/estudiantes/{lu}` |
| **e** | Recuperar estudiantes por género | `GET` | `/estudiantes/{genero}` |
| **f** | Carreras con inscriptos (ordenadas por cantidad) | `GET` | `/carreras/con-inscriptos` |
| **g** | Estudiantes de una carrera filtrados por ciudad | `GET` | `/carreras/{id}/estudiantes?ciudad={ciudad}` |
| **h** | Reporte de carreras (inscriptos/egresados por año) | `GET` | `/carreras/reporte` |

---

## 🧪 Pruebas en Postman

A continuación se muestran ejemplos de las respuestas obtenidas al probar los servicios mediante Postman:

### 1. Dar de alta un estudiante (`POST /estudiantes`)
> 
> 
> 

### 2. Buscar estudiante por Libreta Universitaria (`GET /estudiantes/{lu}`)
> 
> 
> 

### 3. Listar estudiantes ordenados por apellido (`GET /estudiantes/ordenados-por-apellido`)
> 
> 
> ![Estudiantes Ordenados]



---

## ⚙️ Cómo ejecutar el proyecto

1. Clonar el repositorio:
   ```bash
   git clone <url-del-repositorio>
2. Configurar las credenciales de tu base de datos en el archivo:
   src/main/resources/application.properties:
   spring.datasource.url=jdbc:mysql://localhost:3306/db_estudiantes
  spring.datasource.username='root'
  spring.datasource.password=''
  spring.jpa.hibernate.ddl-auto=update
3. Ejecutar la aplicación desde tu entorno de desarrollo (IntelliJ IDEA, Eclipse, VS Code) o mediante Maven:
   mvn spring-boot:run
   
