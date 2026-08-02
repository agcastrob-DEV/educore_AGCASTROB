-- EduCore · esquema P2 (MariaDB). Se ejecuta una vez al inicializar el contenedor.
-- Herencia de Estudiante mapeada single-table (columna `tipo`).
--
-- Esqueleto mínimo a propósito: solo `estudiante` es la implementación de referencia
-- (completa y funcional, ver EstudianteRepoSql). Empleado, Edificio/Aula, Sección y
-- Matrícula son P1 de cada grupo — cada equipo diseña y agrega sus propias tablas aquí
-- (con sus FKs hacia `estudiante` donde corresponda) al completar su esquema.

SET NAMES utf8mb4;

CREATE TABLE estudiante (
  id              INT AUTO_INCREMENT PRIMARY KEY,
  tipo            VARCHAR(20)  NOT NULL,          -- 'REGULAR' | 'BECADO'
  nombre          VARCHAR(100) NOT NULL,
  apellidos       VARCHAR(100) NOT NULL,
  email           VARCHAR(150) NOT NULL,
  carnet          VARCHAR(50)  NOT NULL,
  porcentaje_beca DECIMAL(3,2) NULL               -- solo becados (0.00 .. 1.00)
);

CREATE TABLE IF NOT EXISTS empleado (
  id            INT AUTO_INCREMENT PRIMARY KEY,
  nombre        VARCHAR(100) NOT NULL,
  apellidos     VARCHAR(100) NOT NULL,
  email         VARCHAR(100) NOT NULL,
  salario       DOUBLE NOT NULL,
  fecha_ingreso DATE NOT NULL,
  tipo          VARCHAR(30) NOT NULL
);

CREATE TABLE IF NOT EXISTS edificio (
  id     INT AUTO_INCREMENT PRIMARY KEY,
  codigo VARCHAR(50) UNIQUE NOT NULL,
  nombre VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS aula (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  numero      VARCHAR(50) NOT NULL,
  capacidad   INT NOT NULL,
  tipo        VARCHAR(30) NOT NULL,
  edificio_id INT NOT NULL,
  FOREIGN KEY (edificio_id) REFERENCES edificio(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS seccion (
  id         INT AUTO_INCREMENT PRIMARY KEY,
  codigo     VARCHAR(50) UNIQUE NOT NULL,
  nombre     VARCHAR(100) NOT NULL,
  docente_id INT NOT NULL,
  aula_id    INT NOT NULL,
  FOREIGN KEY (docente_id) REFERENCES empleado(id),
  FOREIGN KEY (aula_id) REFERENCES aula(id)
);

CREATE TABLE IF NOT EXISTS matricula (
  id            INT AUTO_INCREMENT PRIMARY KEY,
  estudiante_id INT NOT NULL,
  seccion_id    INT NOT NULL,
  FOREIGN KEY (estudiante_id) REFERENCES estudiante(id),
  FOREIGN KEY (seccion_id) REFERENCES seccion(id),
  UNIQUE(estudiante_id, seccion_id)
);



-- ── Datos semilla ────────────────────────────────────────────────────────────
INSERT INTO estudiante (tipo, nombre, apellidos, email, carnet, porcentaje_beca) VALUES
  ('REGULAR', 'Ana',   'Rojas Mora',   'ana.rojas@uam.edu',   '202410000001', NULL),
  ('REGULAR', 'Luis',  'Castro Vega',  'luis.castro@uam.edu', '202410000002', NULL),
  ('BECADO',  'Marta', 'Solis Pena',   'marta.solis@uam.edu', '202410000003', 0.50);
