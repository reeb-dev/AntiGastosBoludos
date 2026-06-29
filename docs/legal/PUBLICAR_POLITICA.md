# Publicar la política de privacidad (URL para Play Console)

Play Store exige una **URL pública** accesible sin instalar la app. El repositorio del código puede ser **privado**; la política debe estar en un sitio público.

## Opción A — GitHub Pages (recomendada)

1. Creá un repositorio **público** solo para legal, por ejemplo `antigastos-legal`, o usá GitHub Pages en un repo público.
2. Subí `docs/legal/privacy_policy.html` como `index.html` en la rama `gh-pages`.
3. URL resultante: `https://TU_USUARIO.github.io/antigastos-legal/`
4. Pegá esa URL en Play Console → Política de privacidad.

## Opción B — Gist público

1. Creá un [Gist](https://gist.github.com) público con el contenido de `privacy_policy.html`.
2. Usá la URL raw del gist (GitHub → Raw).

## Opción C — Google Sites / Notion público

Subí el HTML o copiá el texto a una página pública.

## Sincronizar con la app

Cuando actualices la política:

1. Editá `docs/legal/privacy_policy.html` (fuente de verdad).
2. Copiá el mismo archivo a `app/src/main/assets/privacy_policy.html`.
3. Actualizá `app/src/main/res/values/strings.xml` → `privacy_policy_url` con la URL pública.

La app muestra la versión embebida en **Ajustes → Privacidad**; la URL es para Google Play.
