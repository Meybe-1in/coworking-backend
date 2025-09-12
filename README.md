# Levantar el proyecto con Docker


## Descripción

Este README explica cómo levantar **coworking-backend** usando Docker y Docker Compose. Incluye requisitos, ejemplo de `.env`, comandos para levantar la aplicación y endpoints principales.
---

## Requisitos previos

* Tener instalado **Docker** (motor/daemon).
* Tener instalado **Docker Compose** (o usar `docker compose` si usas el plugin v2).

Comprueba las versiones con:

```bash
docker --version
docker compose version    # o: docker-compose --version
```
---

## Clonar el repositorio

HTTPS:

```bash
git clone https://github.com/Meybe-1in/coworking-backend.git
```

Entrar en la carpeta del proyecto:

```bash
cd coworking-backend
```

## Variables de entorno

Crea un archivo `.env` en la raíz del proyecto (NUNCA subir credenciales reales al repo). Ejemplo mínimo:

```env
# .env (ejemplo)
POSTGRES_DB=coworking_db
POSTGRES_USER=coworking_user
POSTGRES_PASSWORD=secret
POSTGRES_PORT=5432
POSTGRES_HOST=postgres

PGADMIN_DEFAULT_EMAIL=admin@admin.com
PGADMIN_DEFAULT_PASSWORD=admin
```

Ajusta valores según tu entorno/local.
---

## Levantar con Docker Compose

Construir imágenes y levantar contenedores:

```bash
# si usas docker compose v1:
docker-compose up --build
# si usas el plugin v2 (recomendado):
docker compose up --build

# para ejecutarlo en background (detached):
docker-compose up --build -d
```

Parar y borrar contenedores/redes/volúmenes anónimos:

```bash
docker-compose down
```
---

## Endpoints principales (ejemplo)

Nota: actualiza esta sección si la API tiene otro prefijo o rutas distintas. Se muestra un ejemplo de endpoints comunes para la API de contactos:

- POST /auth/login
- POST /auth/register
- GET /users (protegido con JWT)

Base URL local por defecto: `http://localhost:8080` (ajusta si tu `docker-compose` expone otro puerto).
