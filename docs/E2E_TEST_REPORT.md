# Reporte E2E — Anti-gastos boludos

**Fecha:** 29 de junio de 2026  
**Dispositivo:** Emulador Pixel 9 Pro XL (API 35)  
**Versión:** 1.0 (versionCode 1)

---

## Resumen

| Capa | Resultado |
|------|-----------|
| Unit tests (26) | ✅ PASS |
| Lint debug | ✅ PASS (0 errors, warnings menores) |
| assembleDebug | ✅ OK (29 MB APK) |
| bundleRelease | ✅ OK (20 MB AAB firmado) |
| Instalación emulador | ✅ OK |
| Launch MainActivity | ✅ Sin crash |
| Deeplink `antigastos://crush/1` | ✅ OK |
| Monkey 500 eventos | ✅ 0 FATAL |
| Room DB creada | ✅ `antigastos.db` |

**Veredicto:** apto para internal testing en Play Console.

---

## Tests automatizados (26)

| Suite | Tests | Qué valida |
|-------|-------|------------|
| `ProductionSmokeTest` | 9 | 27 personajes, packs, chat offline, rutas, intents |
| `LocalPersonaEngineTest` | 6 | Motor offline, moods, frases |
| `PersonaQuickSuggestionsTest` | 2 | Chips ↔ ChatIntent |
| `CopyEngineTest` | 3 | Motor de copy |
| `WorkerNotificationStateTest` | 6 | Anti-spam notificaciones |

Comando: `./gradlew :app:testDebugUnitTest`

---

## Pruebas en emulador

```bash
# Instalación
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Launch
adb shell am start -n com.antigastos.boludos/.MainActivity

# Stress
adb shell monkey -p com.antigastos.boludos --throttle 300 500
```

| Prueba | Resultado |
|--------|-----------|
| Cold start | App en foreground, sin FATAL |
| Deeplink crush | Intent entregado, sin crash |
| Monkey 500 taps | 0 excepciones fatales |
| Base de datos | Room crea `antigastos.db` |

---

## Checklist manual (pendiente en dispositivo real)

Probar en teléfono físico antes de producción:

- [ ] Onboarding completo + quiz personaje
- [ ] Cargar / editar / borrar gasto
- [ ] Chat personaje IA off (chips offline)
- [ ] Chat personaje IA on (Firebase)
- [ ] Donar → Mercado Pago
- [ ] UMP / consentimiento ads (región EEA si aplica)
- [ ] Biometría activar / cancelar / salir
- [ ] Widget home + refresco al cargar gasto
- [ ] Notificaciones (activar toggle → pedir permiso)
- [ ] Ruleta + Lotería + rewarded ads
- [ ] Ajustes → privacidad → abrir URL web

---

## Artefactos de release

| Archivo | Tamaño |
|---------|--------|
| `app/build/outputs/bundle/release/app-release.aab` | ~20 MB |
| `app/build/outputs/apk/debug/app-debug.apk` | ~29 MB |

---

## Issues conocidos (no bloqueantes)

| Issue | Severidad |
|-------|-----------|
| `fallbackToDestructiveMigration` — update con schema nuevo borra datos | Media |
| R8 desactivado en release | Baja |
| 39 warnings lint (deps desactualizadas, Compose modifier order) | Baja |
| Sin tests instrumentados Espresso | Info |

---

## Correcciones aplicadas durante el E2E

- Lint `MissingPermission` en notificaciones (`Notifier`, `StreakStatusNotifier`)
- Nuevo `ProductionSmokeTest` (cobertura 27 personajes)
