# Misiones Diarias

Mod de Fabric para **Minecraft 26.3**: cada día real cada jugador recibe **3 misiones** (fácil, media y difícil)
y cada semana un **desafío semanal**. Pagan **Fichas** del mod Arequipa (o esmeraldas si no está instalado, 1 por cada 5 fichas).

## Instalar
Fabric Loader 0.19.5+ y Fabric API para 26.3, y `build/libs/misiones-diarias-0.2.1.jar` en `mods`
(en el servidor **y** en cada cliente).

## Cómo se abre
- **Tecla J** (se puede cambiar en Opciones → Controles → Misiones Diarias).
- **Botón del libro** arriba a la derecha del inventario (tecla E).
- También: `/misiones`, el **Tablón de Misiones** (8 tablas + libro) o la **Libreta de Misiones** (libro + pluma + brújula).

## Mientras juegas
- **Panel lateral** a la derecha de la pantalla con tus misiones y su barra de progreso (se apaga en el panel: "Panel lateral: No").
- Aviso emergente cuando completas una misión, y el progreso aparece arriba de la barra de items.

## Tipos de misión (el progreso se cuenta solo)
| Tipo | Ejemplo | Cómo cuenta |
|---|---|---|
| Minar | "Mina 8 menas de diamante", "Tala 40 troncos" | bloques que rompes en supervivencia |
| Cazar | "Elimina 10 zombis", "Elimina 30 monstruos (cualquiera)" | criaturas que derrotas |
| Fabricar | "Fabrica 48 antorchas", "Cocina 15 filetes", "Prepara 4 Pisco Sour" | lo que fabricas o cocinas desde que empieza la misión (mesa, horno, estaciones de Arequipa/Tecsup) |
| Estadística | "Pesca 5 peces", "Cría 6 animales", "Comercia 5 veces", "Recorre 900 bloques", "Encanta 2 objetos" | desde que empieza la misión |
| Juntar | "Junta 8 diamantes" | los tienes en el inventario y **se entregan al reclamar** |
| Entregar | "Lleva 32 antorchas al punto marcado" | llegar a menos de 6 bloques de las coordenadas con los objetos |
| Explorar | "Explora una jungla" | entrar al bioma |

## Recompensas
- Fácil 12-18 fichas, media 25-37, difícil 50-75 **+ premio extra** (diamantes, manzanas doradas, esmeraldas...).
- **Desafío semanal** (se renueva cada lunes): 150-220 fichas + premio grande (netherite, tótem, manzana encantada...).
  Ejemplos: "Elimina 150 monstruos", "Recorre 10 000 bloques", "Derrota al Capitán Barbanube".
- **Bono diario**: completar las 3 del día da +30 fichas. **Racha**: cada día seguido completo suma +10 % (máx. +50 %).
- **Cambiar**: una vez al día puedes cambiar una misión que no te guste. **Reclamar todo** cobra todo de un click.
- Las misiones son distintas para cada jugador. Si tienes Arequipa, Tecsup o Mazmorras del Cielo, aparecen misiones de esos mods.

## Desarrollo
```
gradlew-d.cmd build              # jar en build/libs
gradlew-d.cmd runGametest        # 9 pruebas (generación, minar, cazar, entregar, fabricar, juntar, monstruos, semanal)
gradlew-d.cmd runClientGameTest  # capturas del panel, del panel lateral y del botón del inventario
python tools/gen_assets.py       # modelos, receta, traducciones
```
