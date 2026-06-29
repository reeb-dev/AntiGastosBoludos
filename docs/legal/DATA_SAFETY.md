# Data safety — Google Play Console

Guía para completar el formulario **Seguridad de los datos** de Anti-gastos boludos.

## Resumen

| ¿La app recopila o comparte datos? | **Sí** (anuncios, analytics, IA opcional) |
| ¿Todos los datos son opcionales? | Gastos locales: necesarios para la función. IA y notificaciones: opcionales. |
| ¿Cifrado en tránsito? | **Sí** (HTTPS hacia Google y APIs) |
| ¿Los usuarios pueden pedir borrado? | **Sí** — borrar gastos en app o desinstalar |

## Tipos de datos

### No se comparten (solo en el dispositivo)

- Información financiera: gastos, metas, presupuestos
- Información personal: nombre mostrado, nombre de pareja (modo pareja local)
- Historial de chat con personajes

Declarar como **recopilados** pero **no compartidos** si Play lo pide para “datos en el dispositivo”, o indicar que no salen del dispositivo según el wizard actual.

### Compartidos con terceros

| Tipo | Proveedor | Obligatorio | Finalidad |
|------|-----------|-------------|-----------|
| Identificadores del dispositivo (Advertising ID) | Google AdMob | Sí, si hay ads | Publicidad |
| Datos de diagnóstico / uso | Firebase Analytics | Sí | Estadísticas |
| Otros contenidos generados por el usuario (prompts) | Google (Gemini / Firebase AI) | No — solo si el usuario activa IA | Funcionalidad |

### Condicional

| Tipo | Cuándo | Proveedor |
|------|--------|-----------|
| Búsqueda en la app (keywords GIF) | `localStickersOnly = false` | Giphy / Tenor / Klipy |
| Contenido de imagen (memes) | `localStickersOnly = false` | meme-api.com |

## Permisos relevantes

- `INTERNET`, `AD_ID`, `POST_NOTIFICATIONS`, `USE_BIOMETRIC`, `VIBRATE`

## Política de privacidad

- URL pública: ver `docs/legal/PUBLICAR_POLITICA.md`
- Copia en app: `app/src/main/assets/privacy_policy.html`

## Donaciones

No hay procesamiento de pagos in-app. Las transferencias a Mercado Pago son iniciadas por el usuario fuera de la app.
