# Sugerencias — Anti-gastos boludos

Recomendaciones priorizadas a partir de las auditorías de producción (junio 2026).  
Estado del código: compila debug/release, 8 tests unitarios OK.

---

## Resumen ejecutivo

| Área | Estado |
|------|--------|
| Funcionalidad core | Lista |
| Offline (personajes sin IA) | Implementado (`LocalPersonaEngine`) |
| Legal / política | Publicada en GitHub Pages |
| Play Console | Pendiente manual |
| Cambios locales sin commit | ~20 archivos — commitear antes del AAB |

**Veredicto:** listo para **internal testing**; producción pública requiere consola + keystore.

---

## Crítico — antes de publicar en Play

| # | Acción | Responsable |
|---|--------|-------------|
| 1 | Completar `local.properties`: `DONATION_*`, keystore `RELEASE_*` | Dev |
| 2 | `./gradlew :app:bundleRelease` y probar AAB en dispositivo real | Dev |
| 3 | Play Console → URL privacidad: `https://reeb-dev.github.io/antigastos-legal/` | Dev |
| 4 | Completar **Data safety** (`docs/legal/DATA_SAFETY.md`) | Dev |
| 5 | AdMob → **Privacy & messaging** (GDPR/UMP) vinculado a la app | Dev |
| 6 | Declarar **contiene anuncios** + formulario de ads | Dev |
| 7 | Clasificación IARC: ruleta/lotería **simulada**, sin dinero real | Dev |
| 8 | **Commit + push** de cambios pendientes (offline, UMP, backup, widget) | Dev |

---

## Alta prioridad — código

| # | Sugerencia | Por qué | Esfuerzo |
|---|------------|---------|----------|
| A1 | Commitear y pushear rama actual al repo privado | Trazabilidad del release | Bajo |
| A2 | Probar chat con IA **off** y chips de sugerencias | Valida motor offline en producción | Bajo |
| A3 | Probar flujo UMP en región EEA (VPN o dispositivo test) | Rechazo común en UE sin consentimiento | Medio |
| A4 | Restringir API key Firebase en Google Cloud (package + SHA-1 release) | Reduce abuso del `google-services.json` | Medio |
| A5 | Proyecto Firebase **dedicado** (hoy comparte `noteeureka` con otra app) | Aislamiento y cuotas | Medio |
| A6 | Configurar `GIPHY_API_KEY` o `KLIPY_API_KEY` en release | Tenor depreca jun 2026 | Bajo |

---

## Media prioridad — calidad y datos

| # | Sugerencia | Detalle |
|---|------------|---------|
| M1 | Migraciones Room reales antes de **v2** | Hoy `fallbackToDestructiveMigration` borra todo al cambiar schema |
| M2 | Documentar en changelog riesgo de pérdida de datos en updates | Ya parcialmente en política de privacidad |
| M3 | Habilitar R8 (`isMinifyEnabled = true`) + reglas ProGuard | Room, Compose, Ads, Firebase |
| M4 | No persistir `geminiApiKey` en Room sin cifrado, o quitar en prod | Campo en `SettingsEntity` |
| M5 | “Borrar todos los gastos” → opción de wipe completo (chat, logros) | Evita datos huérfanos |
| M6 | Widget: usar singleton `AppDatabase` en lugar de `AppDatabase.create()` | Menos riesgo de locks |
| M7 | Limpiar legacy: `cloudId`, `getByCloudId`, `postFriendUpdate`, canal social | Código muerto post-Firestore |
| M8 | Sincronizar política en GitHub Pages tras cada cambio en `docs/legal/` | `cp` + push a `antigastos-legal` |

---

## Baja prioridad — pulido post-v1

| # | Sugerencia |
|---|------------|
| B1 | Renombrar paquete `ui.auth` → `ui.biometric` (solo contiene `BiometricGate`) |
| B2 | Unificar doble splash (`installSplashScreen` + `ArgSplash` 1.6s) |
| B3 | Aviso si biometría activa pero el dispositivo no tiene hardware |
| B4 | Indicador en chat cuando la respuesta es offline vs IA |
| B5 | Email de soporte dedicado al producto (hoy `jesusseep@gmail.com` personal) |
| B6 | Eliminar o archivar `functions/` del repo (marcado DEPRECATED) |
| B7 | Tests de integración WorkManager |
| B8 | SQLCipher o `EncryptedSharedPreferences` para datos sensibles |

---

## Play Console — checklist rápido

- [ ] App `com.antigastos.boludos` creada
- [ ] Política de privacidad (URL pública)
- [ ] Data safety (AdMob AD_ID, Analytics, meme-api condicional, Gemini opcional)
- [ ] Clasificación de contenido (13+, ads, lenguaje, simulación ruleta)
- [ ] Store listing: capturas 6.7", icono 512×512, descripción clara
- [ ] Internal testing → closed testing → producción
- [ ] AdMob vinculado: `ca-app-pub-9783550633423906~4815321949`

---

## Release — comandos

```bash
# Verificar tests
./gradlew :app:testDebugUnitTest

# AAB (requiere RELEASE_STORE_FILE en local.properties)
./gradlew :app:bundleRelease

# Salida
ls -la app/build/outputs/bundle/release/
```

---

## Motor offline (ya implementado)

- `LocalPersonaEngine` — chat, saludos, frases Home, consejos sin red
- `ChatIntent` — detecta intención por keywords
- `PersonaCopyPacks.pick()` — evita repetir frases
- IA en nube **opcional**: Ajustes → Frases con IA

**Sugerencia de prueba:** desactivar IA, abrir chat del tachero, usar chips “¿Cómo voy?”, “Delivery”, “Analizame”.

---

## Contacto y referencias

| Doc | Uso |
|-----|-----|
| [RELEASE_CHECKLIST.md](RELEASE_CHECKLIST.md) | Publicación AAB |
| [legal/DATA_SAFETY.md](legal/DATA_SAFETY.md) | Formulario Play |
| [legal/PUBLICAR_POLITICA.md](legal/PUBLICAR_POLITICA.md) | URL pública |
| Repo app (privado) | https://github.com/reeb-dev/AntiGastosBoludos |
| Política (pública) | https://reeb-dev.github.io/antigastos-legal/ |
