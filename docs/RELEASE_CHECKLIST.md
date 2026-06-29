# Checklist de publicación — Play Store

## Antes de subir el AAB

- [ ] `local.properties` con `DONATION_ALIAS`, `DONATION_CBU`, `DONATION_HOLDER` (no commitear datos reales en `donation_config.xml`)
- [ ] **No** embeber `GEMINI_API_KEY` en release — el build de release la fuerza vacía; usar Firebase AI (`google-services.json`)
- [ ] `google-services.json` en `app/` (no va a git; copiar en CI/máquina de release)
- [ ] Keystore release en `local.properties`:
  ```properties
  RELEASE_STORE_FILE=/ruta/release.keystore
  RELEASE_STORE_PASSWORD=...
  RELEASE_KEY_ALIAS=...
  RELEASE_KEY_PASSWORD=...
  ```
- [ ] Compilar: `./gradlew :app:bundleRelease` (falla si no hay `RELEASE_STORE_FILE`)
- [ ] Probar el AAB en un dispositivo real (internal testing)

## Play Console

- [ ] Crear app `com.antigastos.boludos`
- [ ] URL de política de privacidad (ver `docs/legal/PUBLICAR_POLITICA.md`)
- [ ] Completar Data safety (`docs/legal/DATA_SAFETY.md`)
- [ ] Clasificación de contenido (lenguaje informal argentino, simulación de ruleta/lotería, anuncios)
- [ ] Capturas de pantalla (teléfono 6.7")
- [ ] Icono 512×512 (`app/src/main/ic_launcher-playstore.png`)
- [ ] AdMob: app vinculada + **mensaje GDPR/UMP** configurado en consola (Privacy & messaging)
- [ ] Declarar **contiene anuncios** y completar formulario de ads

## Repositorio privado

```bash
# Si aún no hay remote:
git remote add origin git@github.com:reeb-dev/AntiGastosBoludos.git
git push -u origin HEAD

# Hacer el repo privado (GitHub CLI):
gh repo edit TU_USUARIO/AntiGastosBoludos --visibility private
```

O en GitHub: **Settings → General → Danger zone → Change visibility → Private**.

## Post-lanzamiento

- [ ] Planificar migraciones Room reales antes de v2 (hoy `fallbackToDestructiveMigration` borra datos al actualizar esquema)
- [ ] Monitorear crashes en Play Console
- [ ] Revisar política de anuncios si hay rechazo por densidad de ads
