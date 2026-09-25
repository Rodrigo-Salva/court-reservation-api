# 🔌 Referencia de la API

Resumen de todos los endpoints por módulo. La documentación interactiva (esquemas de request y response, pruebas en vivo) está en **Swagger UI**: `http://localhost:8080/swagger-ui.html`.

**Contenido:** [Convenciones](#-convenciones) · [Autenticación](#-autenticación) · [Módulos](#-módulos) · [Matriz de acceso](#-matriz-de-acceso) · [Códigos de error](#-códigos-de-error)

---

## 📌 Convenciones

- **Base URL:** `http://localhost:8080` · todas las rutas cuelgan de `/api`.
- **Formato:** JSON (`Content-Type: application/json`). Fechas `yyyy-MM-dd`, horas `HH:mm`.
- **Autenticación:** cabecera `Authorization: Bearer <token>`.
- **Paginación:** `?page=0&size=20` (tamaño máx. 100). Respuesta: `{ content, page, size, totalElements, totalPages }`.

**Leyenda de acceso**

| Icono | Significa |
|:---:|---|
| 🌐 | Público (sin token) |
| 🔑 | Cualquier usuario autenticado |
| 🧑‍💼 | Personal de sede: `RECEPTIONIST` (solo lo indicado), `VENUE_ADMIN`, `ADMIN`, `SUPER_ADMIN` — **acotado a su sede** |
| 🛠️ | Solo `ADMIN` (y `SUPER_ADMIN` donde se indica) |

---

## 🔐 Autenticación

| Método | Ruta | Acceso | Descripción |
|---|---|:---:|---|
| `POST` | `/api/auth/register` | 🌐 | Crea una cuenta `USER` y devuelve el token. Rechaza email o teléfono duplicados |
| `POST` | `/api/auth/login` | 🌐 | Devuelve `{ token, name, role }`. **429** tras 5 intentos fallidos (con `Retry-After`) |
| `POST` | `/api/auth/logout` | 🔑 | Revoca todos los tokens del usuario |

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"demo01@sportsbooking.com","password":"demo123"}'
# → { "token": "eyJhbGciOi…", "name": "Lucía Fernández", "role": "USER" }
```

---

## 🧩 Módulos

### 📅 Reservas · `/api/bookings`

| Método | Ruta | Acceso | Descripción |
|---|---|:---:|---|
| `POST` | `/` | 🔑 | Crea una reserva (con `usesPackage` opcional). Con pago obligatorio queda `PENDIENTE` |
| `POST` | `/recurrent` | 🔑 | Crea reservas semanales (2–12 semanas) |
| `GET` | `/{id}` | 🔑 | Detalle (titular o personal) |
| `GET` | `/user/{userId}` · `/user/{userId}/status/{status}` · `/user/{userId}/future` | 🔑 | Reservas del usuario (solo el titular o personal) |
| `GET` | `/court/{courtId}/availability?date=` | 🌐 | Horarios libres y ocupados de una cancha |
| `GET` | `/check-overlap` | 🔑 | ¿Hay solapamiento en ese horario? |
| `PUT` | `/cancel` | 🔑 | Cancela con la penalización que corresponda |
| `PUT` | `/{id}/reschedule` | 🔑 | Reprograma una reserva confirmada |
| `GET` | `/{id}/check-in-code` | 🔑 | Código del QR de check-in (titular o personal) |
| `PUT` | `/{id}/check-in` | 🧑‍💼 | Check-in con el código y el ID de reserva |
| `POST` | `/check-in/scan` | 🧑‍💼 | Check-in **solo con el código** leído del QR |
| `PATCH` | `/{id}/no-show` | 🧑‍💼 | Marca no-show (penalización 100 %) |
| `GET` | `/` | 🧑‍💼 | Todas las reservas (de su sede) |
| `GET` | `/search?status=&q=&page=&size=` | 🧑‍💼 | Búsqueda paginada por estado y texto |

### 🏟️ Canchas · `/api/courts`

| Método | Ruta | Acceso | Descripción |
|---|---|:---:|---|
| `GET` | `/` · `/sport-type/{sport}` · `/search?name=` · `/price-range` | 🌐 | Catálogo público de canchas activas |
| `GET` | `/{id}` | 🔑 | Detalle de una cancha |
| `GET` | `/all` | 🧑‍💼 (`VENUE_ADMIN`+) | Todas, incluso inactivas (de su sede) |
| `POST` | `/` | 🧑‍💼 (`VENUE_ADMIN`+) | Crea una cancha; el personal de sede la crea siempre en **su** sede |
| `PUT` | `/{id}` | 🧑‍💼 (`VENUE_ADMIN`+) | Edita o **mueve de sede** (`venueId`; solo admin global) |
| `PATCH` | `/{id}/activate` · `DELETE /{id}` | 🧑‍💼 (`VENUE_ADMIN`+) | Activa o desactiva |

### 🏢 Sedes · `/api/venues`

| Método | Ruta | Acceso | Descripción |
|---|---|:---:|---|
| `GET` | `/` | 🌐 | Sedes activas |
| `GET` | `/all` | 🛠️ `ADMIN`/`SUPER_ADMIN` | Todas |
| `POST` · `PUT /{id}` · `PATCH /{id}/activate` · `DELETE /{id}` | | 🛠️ `ADMIN`/`SUPER_ADMIN` | Gestión de sedes |

### 🚧 Bloqueos de cancha · `/api/court-blocks`

| Método | Ruta | Acceso | Descripción |
|---|---|:---:|---|
| `GET` | `/court/{courtId}?date=` | 🧑‍💼 (incluye `RECEPTIONIST`) | Bloqueos de un día |
| `POST` | `/` | 🧑‍💼 (`VENUE_ADMIN`+) | Crea un bloqueo (mantenimiento, feriado o evento) |
| `DELETE` | `/{id}` | 🧑‍💼 (`VENUE_ADMIN`+) | Lo desactiva |

### 📦 Paquetes · `/api/packages` y `/api/user-packages`

| Método | Ruta | Acceso | Descripción |
|---|---|:---:|---|
| `GET` | `/api/packages` · `/{id}` · `/best-discount` · `/best-price` | 🔑 | Catálogo de paquetes activos |
| `GET` | `/api/packages/all` | 🛠️ | Todos, incluso inactivos |
| `POST` · `PUT /{id}` · `PATCH /{id}/activate` · `DELETE /{id}` | | 🛠️ | Gestión de paquetes |
| `POST` | `/api/user-packages/purchase` | 🔑 | Compra un paquete |
| `GET` | `/api/user-packages/user/{userId}` · `/active` · `/best-available` · `/{id}` | 🔑 | Paquetes del usuario |

### ⏳ Lista de espera · `/api/waiting-list`

| Método | Ruta | Acceso | Descripción |
|---|---|:---:|---|
| `POST` | `/` | 🔑 | Se anota en la lista de un horario |
| `GET` | `/{id}` · `/user/{userId}` | 🔑 | Sus solicitudes |
| `DELETE` | `/{id}` | 🔑 | Se retira |
| `GET` | `/court/{courtId}/pending` | 🛠️ | Cola pendiente de una cancha |

### 💳 Pagos · `/api/payments`

| Método | Ruta | Acceso | Descripción |
|---|---|:---:|---|
| `POST` | `/` | 🔑 | Paga una reserva (simulado). Si estaba `PENDIENTE`, la confirma |
| `GET` | `/my` | 🔑 | Mis pagos |
| `GET` | `/?status=&q=&page=&size=` | 🧑‍💼 (`VENUE_ADMIN`+) | Transacciones paginadas + totales cobrado y reembolsado |
| `PATCH` | `/{id}/refund` | 🧑‍💼 (`VENUE_ADMIN`+) | Reembolsa un pago aprobado |

### ⭐ Reseñas · `/api/court-reviews`

| Método | Ruta | Acceso | Descripción |
|---|---|:---:|---|
| `GET` | `/court/{courtId}` | 🌐 | Reseñas visibles de una cancha |
| `POST` | `/` | 🔑 | Califica (requiere una reserva completada) |
| `GET` | `/moderation?filter=&q=&page=&size=` | 🧑‍💼 (`VENUE_ADMIN`+) | Todas las reseñas, paginadas (`filter`: `hidden` o `visible`) |
| `PATCH` | `/{id}/hide` · `/{id}/show` | 🧑‍💼 (`VENUE_ADMIN`+) | Oculta o vuelve a mostrar |
| `DELETE` | `/{id}` | 🧑‍💼 (`VENUE_ADMIN`+) | Elimina |

### 🤝 Partidos abiertos · `/api/open-matches`

| Método | Ruta | Acceso | Descripción |
|---|---|:---:|---|
| `GET` | `/` | 🔑 | Partidos abiertos |
| `GET` | `/mine` | 🔑 | Los que publiqué |
| `POST` | `/` | 🔑 | Publica una reserva confirmada propia |
| `POST` | `/{id}/join` | 🔑 | Solicita unirse |
| `GET` | `/{id}/requests` | 🔑 creador | Solicitudes recibidas |
| `PATCH` | `/{id}/requests/{requestId}/accept` · `/reject` | 🔑 creador | Acepta o rechaza |

### 👥 Equipos · `/api/teams`

| Método | Ruta | Acceso | Descripción |
|---|---|:---:|---|
| `GET` | `/` | 🔑 | Equipos donde soy propietario o miembro |
| `POST` | `/` | 🔑 | Crea un equipo |
| `GET` | `/{id}/members` | 🔑 miembro | Integrantes |
| `DELETE` | `/{id}/members/{userId}` | 🔑 propietario | Quita a un integrante |
| `DELETE` | `/{id}/leave` | 🔑 miembro | Abandona el equipo |
| `POST` | `/{id}/invitations` | 🔑 propietario | Invita por email `{ "email": "…" }` |
| `GET` | `/{id}/invitations` | 🔑 propietario | Invitaciones pendientes |
| `DELETE` | `/{id}/invitations/{invitationId}` | 🔑 propietario | Cancela una invitación |
| `GET` | `/invitations/mine` | 🔑 | Invitaciones que recibí |
| `POST` | `/invitations/{invitationId}/accept` · `/decline` | 🔑 invitado | Responde |

### 🏆 Torneos · `/api/tournaments`

| Método | Ruta | Acceso | Descripción |
|---|---|:---:|---|
| `GET` | `/` · `/{id}/matches` · `/{id}/ranking` | 🌐 | Consulta pública |
| `POST` | `/{id}/enroll` | 🔑 | Se inscribe |
| `POST` | `/` | 🧑‍💼 (`VENUE_ADMIN`+) | Crea un torneo (en su sede, o global si es admin global) |
| `POST` | `/{id}/fixtures` | 🧑‍💼 (`VENUE_ADMIN`+) | Genera el fixture round-robin |
| `PATCH` | `/{id}/matches/{matchId}/result` | 🧑‍💼 (`VENUE_ADMIN`+) | Registra el resultado |

### 🔔 Notificaciones · `/api/notifications`

| Método | Ruta | Acceso | Descripción |
|---|---|:---:|---|
| `GET` | `/my` | 🔑 | Mis notificaciones |
| `PATCH` | `/{id}/read` · `/read-all` | 🔑 | Marca como leídas |

### 📊 Reportes · `/api/reports`

| Método | Ruta | Acceso | Descripción |
|---|---|:---:|---|
| `GET` | `/operational?startDate=&endDate=&venueId=&courtId=&sportType=` | 🧑‍💼 (`VENUE_ADMIN`+) | Totales, ingresos, cancelaciones, no-show, horas pico y desgloses diario, por cancha y por deporte |
| `GET` | `/operational/export?…&format=pdf\|xlsx\|csv` | 🧑‍💼 (`VENUE_ADMIN`+) | Descarga el reporte (rango máx. 1 año) |

El personal de sede solo obtiene datos de su sede; pedir otra sede responde **403**.

### 🧾 Auditoría · `/api/audit-logs`

| Método | Ruta | Acceso | Descripción |
|---|---|:---:|---|
| `GET` | `/?page=&size=` | 🧑‍💼 (`VENUE_ADMIN`+) | Acciones registradas, más recientes primero (de su sede) |

### 👤 Usuarios · `/api/users`

| Método | Ruta | Acceso | Descripción |
|---|---|:---:|---|
| `GET` | `/{id}` · `/email/{email}` | 🔑 | Solo el propio usuario o un administrador |
| `PUT` | `/{id}` | 🔑 | Edita el propio perfil |
| `GET` | `/page?q=&active=&page=&size=` | 🛠️ `ADMIN` | Usuarios paginados con búsqueda por nombre, email o teléfono |
| `GET` | `/all` · `/` · `/search` · `/membership/{tipo}` · `/exists/*` | 🛠️ `ADMIN` | Listados y consultas |
| `PATCH` | `/{id}/membership` · `/{id}/activate` · `DELETE /{id}` | 🛠️ `ADMIN` | Membresía y estado |
| `POST` | `/staff` | 🛠️ `ADMIN`/`SUPER_ADMIN` | Crea personal de sede (`VENUE_ADMIN` o `RECEPTIONIST`) con su sede |
| `PATCH` | `/{id}/venue` | 🛠️ `ADMIN`/`SUPER_ADMIN` | Asigna sede y rol de personal a un usuario existente |

---

## 🗂️ Matriz de acceso

Resumen por rol (✅ permitido · — no permitido · 🏢 solo su sede):

| Capacidad | Público | `USER` | `RECEPTIONIST` | `VENUE_ADMIN` | `ADMIN` | `SUPER_ADMIN` |
|---|:---:|:---:|:---:|:---:|:---:|:---:|
| Ver catálogo de canchas, sedes, torneos y reseñas | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Reservar, pagar, paquetes, lista de espera, comunidad | — | ✅ | ✅ | ✅ | ✅ | ✅ |
| Listar reservas y buscarlas | — | — | 🏢 | 🏢 | ✅ | ✅ |
| Check-in (manual o QR) y no-show | — | — | 🏢 | 🏢 | ✅ | ✅ |
| Ver bloqueos de cancha | — | — | 🏢 | 🏢 | ✅ | ✅ |
| Crear y quitar bloqueos | — | — | — | 🏢 | ✅ | ✅ |
| Gestionar canchas | — | — | — | 🏢 | ✅ | ✅ |
| Pagos: listar y reembolsar | — | — | — | 🏢 | ✅ | ✅ |
| Reseñas: moderar | — | — | — | 🏢 | ✅ | ✅ |
| Reportes y auditoría | — | — | — | 🏢 | ✅ | ✅ |
| Torneos: crear, fixture y resultados | — | — | — | 🏢 | ✅ | ✅ |
| Gestionar sedes | — | — | — | — | ✅ | ✅ |
| Crear personal y asignar sede | — | — | — | — | ✅ | ✅ |
| Gestionar paquetes | — | — | — | — | ✅ | — |
| Gestionar usuarios (listar, membresía, activar) | — | — | — | — | ✅ | — |
| Cola de lista de espera por cancha | — | — | — | — | ✅ | — |

> 🏢 = el sistema filtra los datos y rechaza con **403** cualquier recurso de otra sede. Un miembro del personal sin sede asignada no accede a ninguno.

---

## ⚠️ Códigos de error

Todos los errores comparten esta forma:

```json
{
  "status": 403,
  "error": "Prohibido",
  "message": "No tiene permiso para realizar esta operación",
  "timestamp": "2026-09-25T10:15:30",
  "path": "/api/courts/7"
}
```

| Código | Cuándo |
|:---:|---|
| `400` | Validación fallida, regla de negocio incumplida, JSON o parámetro con formato inválido |
| `401` / `403` | Sin token, token inválido o revocado / sin permiso o recurso de otra sede |
| `404` | Recurso o ruta inexistente |
| `429` | Demasiados intentos de login (cabecera `Retry-After` en segundos) |
