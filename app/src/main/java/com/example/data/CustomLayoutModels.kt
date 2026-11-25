package com.example.data

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

enum class CustomWidgetType {
    TOUCHPAD,
    KEY,
    SHORTCUT,
    MACRO,
    MEDIA,
    MOUSE_BUTTON,
}

@JsonClass(generateAdapter = true)
data class MacroStep(
    val action: String, // "KEY", "SHORTCUT", "TEXT", "DELAY", "MOUSE"
    val value: String,
)

@JsonClass(generateAdapter = true)
data class CustomLayoutWidget(
    val id: String,
    val label: String,
    val type: String,
    val colSpan: Int = 1,
    val heightUnits: Int = 1,
    val keyValue: String = "",
    val macroSteps: List<MacroStep> = emptyList(),
    val repeatCount: Int = 1, // 1 = once, >1 = N times, 0 = continuous loop
)

@JsonClass(generateAdapter = true)
data class CustomLayoutExportDto(
    val name: String,
    val description: String,
    val category: String,
    val widgets: List<CustomLayoutWidget>,
)

object CustomLayoutSerializer {
    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val dtoAdapter = moshi.adapter(CustomLayoutExportDto::class.java).indent("  ")
    private val dtoListType = Types.newParameterizedType(List::class.java, CustomLayoutExportDto::class.java)
    private val dtoListAdapter = moshi.adapter<List<CustomLayoutExportDto>>(dtoListType).indent("  ")
    private val widgetListType = Types.newParameterizedType(List::class.java, CustomLayoutWidget::class.java)
    private val widgetListAdapter = moshi.adapter<List<CustomLayoutWidget>>(widgetListType)

    fun serializeWidgets(widgets: List<CustomLayoutWidget>): String = widgetListAdapter.toJson(widgets)

    fun deserializeWidgets(json: String): List<CustomLayoutWidget> =
        runCatching { widgetListAdapter.fromJson(json).orEmpty() }.getOrDefault(emptyList())

    fun exportLayoutJson(dto: CustomLayoutExportDto): String = dtoAdapter.toJson(dto)

    fun importLayoutJson(json: String): CustomLayoutExportDto? =
        runCatching { dtoAdapter.fromJson(json) }.getOrNull()

    fun exportAllBundleJson(dtos: List<CustomLayoutExportDto>): String = dtoListAdapter.toJson(dtos)

    fun importAllBundleJson(json: String): List<CustomLayoutExportDto>? =
        runCatching { dtoListAdapter.fromJson(json) }.getOrNull()

    fun starterLayouts(): List<CustomLayoutExportDto> = listOf(
        CustomLayoutExportDto(
            name = "Edição & Produtividade",
            description = "Touchpad compacto + atalhos rápidos e macro de salvar/atualizar",
            category = "Trabalho",
            widgets = listOf(
                CustomLayoutWidget("w1", "Touchpad", CustomWidgetType.TOUCHPAD.name, colSpan = 4, heightUnits = 2),
                CustomLayoutWidget("w2", "Copiar", CustomWidgetType.SHORTCUT.name, colSpan = 1, keyValue = "CTRL+C"),
                CustomLayoutWidget("w3", "Colar", CustomWidgetType.SHORTCUT.name, colSpan = 1, keyValue = "CTRL+V"),
                CustomLayoutWidget("w4", "Desfazer", CustomWidgetType.SHORTCUT.name, colSpan = 1, keyValue = "CTRL+Z"),
                CustomLayoutWidget("w5", "Salvar", CustomWidgetType.SHORTCUT.name, colSpan = 1, keyValue = "CTRL+S"),
                CustomLayoutWidget("w6", "Alt+Tab", CustomWidgetType.SHORTCUT.name, colSpan = 2, keyValue = "ALT+TAB"),
                CustomLayoutWidget(
                    id = "w7",
                    label = "Macro: Salvar + F5",
                    type = CustomWidgetType.MACRO.name,
                    colSpan = 2,
                    macroSteps = listOf(
                        MacroStep("SHORTCUT", "CTRL+S"),
                        MacroStep("DELAY", "200"),
                        MacroStep("KEY", "F5"),
                    )
                ),
            )
        ),
        CustomLayoutExportDto(
            name = "Controle de Jogos WASD",
            description = "Teclas direcionais WASD, Espaço, Shift e clique rápido",
            category = "Jogos",
            widgets = listOf(
                CustomLayoutWidget("g1", "ESC", CustomWidgetType.KEY.name, colSpan = 1, keyValue = "ESC"),
                CustomLayoutWidget("g2", "W", CustomWidgetType.KEY.name, colSpan = 2, keyValue = "W"),
                CustomLayoutWidget("g3", "E (Ação)", CustomWidgetType.KEY.name, colSpan = 1, keyValue = "E"),
                CustomLayoutWidget("g4", "A", CustomWidgetType.KEY.name, colSpan = 1, keyValue = "A"),
                CustomLayoutWidget("g5", "S", CustomWidgetType.KEY.name, colSpan = 2, keyValue = "S"),
                CustomLayoutWidget("g6", "D", CustomWidgetType.KEY.name, colSpan = 1, keyValue = "D"),
                CustomLayoutWidget("g7", "Shift", CustomWidgetType.KEY.name, colSpan = 1, keyValue = "SHIFT"),
                CustomLayoutWidget("g8", "Pular (Espaço)", CustomWidgetType.KEY.name, colSpan = 2, keyValue = "SPACE"),
                CustomLayoutWidget("g9", "Clique Esq.", CustomWidgetType.MOUSE_BUTTON.name, colSpan = 1, keyValue = "LEFT_CLICK"),
                CustomLayoutWidget("g10", "Mirar / Câmera", CustomWidgetType.TOUCHPAD.name, colSpan = 4, heightUnits = 2),
            )
        ),
    )

    /**
     * Galeria de Layouts Prontos (estilo Stream Deck) para programas populares.
     */
    fun streamDeckGallery(): List<CustomLayoutExportDto> = listOf(
        CustomLayoutExportDto(
            name = "OBS Studio Deck",
            description = "Cenas, gravação, stream, microfone mudo e replay instantâneo",
            category = "Stream Deck",
            widgets = listOf(
                CustomLayoutWidget("obs1", "Cena 1 (Cam)", CustomWidgetType.SHORTCUT.name, colSpan = 2, keyValue = "CTRL+SHIFT+F1"),
                CustomLayoutWidget("obs2", "Cena 2 (Tela)", CustomWidgetType.SHORTCUT.name, colSpan = 2, keyValue = "CTRL+SHIFT+F2"),
                CustomLayoutWidget("obs3", "Cena 3 (Pausa)", CustomWidgetType.SHORTCUT.name, colSpan = 2, keyValue = "CTRL+SHIFT+F3"),
                CustomLayoutWidget("obs4", "Mutar Mic", CustomWidgetType.SHORTCUT.name, colSpan = 2, keyValue = "CTRL+SHIFT+M"),
                CustomLayoutWidget("obs5", "Gravar REC", CustomWidgetType.SHORTCUT.name, colSpan = 2, keyValue = "CTRL+SHIFT+R"),
                CustomLayoutWidget("obs6", "Iniciar Live", CustomWidgetType.SHORTCUT.name, colSpan = 2, keyValue = "CTRL+SHIFT+S"),
                CustomLayoutWidget("obs7", "Salvar Replay", CustomWidgetType.SHORTCUT.name, colSpan = 4, keyValue = "CTRL+SHIFT+B"),
            )
        ),
        CustomLayoutExportDto(
            name = "Adobe Photoshop",
            description = "Pincel, borracha, zoom, camadas, desfazer e exportar rápido",
            category = "Design",
            widgets = listOf(
                CustomLayoutWidget("ps1", "Pincel (B)", CustomWidgetType.KEY.name, colSpan = 1, keyValue = "B"),
                CustomLayoutWidget("ps2", "Borracha (E)", CustomWidgetType.KEY.name, colSpan = 1, keyValue = "E"),
                CustomLayoutWidget("ps3", "Mover (V)", CustomWidgetType.KEY.name, colSpan = 1, keyValue = "V"),
                CustomLayoutWidget("ps4", "Laço (L)", CustomWidgetType.KEY.name, colSpan = 1, keyValue = "L"),
                CustomLayoutWidget("ps5", "Desfazer", CustomWidgetType.SHORTCUT.name, colSpan = 2, keyValue = "CTRL+Z"),
                CustomLayoutWidget("ps6", "Nova Camada", CustomWidgetType.SHORTCUT.name, colSpan = 2, keyValue = "CTRL+SHIFT+N"),
                CustomLayoutWidget("ps7", "Zoom -", CustomWidgetType.SHORTCUT.name, colSpan = 1, keyValue = "CTRL+-"),
                CustomLayoutWidget("ps8", "Ajustar Tela", CustomWidgetType.SHORTCUT.name, colSpan = 2, keyValue = "CTRL+0"),
                CustomLayoutWidget("ps9", "Zoom +", CustomWidgetType.SHORTCUT.name, colSpan = 1, keyValue = "CTRL+="),
                CustomLayoutWidget("ps10", "Salvar PSD", CustomWidgetType.SHORTCUT.name, colSpan = 4, keyValue = "CTRL+S"),
            )
        ),
        CustomLayoutExportDto(
            name = "Premiere Pro Edição",
            description = "Lâmina de corte, play J/K/L, marcador e exportar vídeo",
            category = "Vídeo",
            widgets = listOf(
                CustomLayoutWidget("pr1", "Rebobinar (J)", CustomWidgetType.KEY.name, colSpan = 1, keyValue = "J"),
                CustomLayoutWidget("pr2", "Pausar (K)", CustomWidgetType.KEY.name, colSpan = 2, keyValue = "K"),
                CustomLayoutWidget("pr3", "Avançar (L)", CustomWidgetType.KEY.name, colSpan = 1, keyValue = "L"),
                CustomLayoutWidget("pr4", "Cortar (Ctrl+K)", CustomWidgetType.SHORTCUT.name, colSpan = 2, keyValue = "CTRL+K"),
                CustomLayoutWidget("pr5", "Seleção (V)", CustomWidgetType.KEY.name, colSpan = 1, keyValue = "V"),
                CustomLayoutWidget("pr6", "Lâmina (C)", CustomWidgetType.KEY.name, colSpan = 1, keyValue = "C"),
                CustomLayoutWidget("pr7", "Marcador (M)", CustomWidgetType.KEY.name, colSpan = 2, keyValue = "M"),
                CustomLayoutWidget("pr8", "Exportar (Ctrl+M)", CustomWidgetType.SHORTCUT.name, colSpan = 2, keyValue = "CTRL+M"),
            )
        ),
        CustomLayoutExportDto(
            name = "Microsoft Excel Pro",
            description = "AutoSoma, filtros, negrito, moeda e macro de salvar planilha",
            category = "Produtividade",
            widgets = listOf(
                CustomLayoutWidget("ex1", "AutoSoma", CustomWidgetType.SHORTCUT.name, colSpan = 2, keyValue = "ALT+="),
                CustomLayoutWidget("ex2", "Filtro", CustomWidgetType.SHORTCUT.name, colSpan = 2, keyValue = "CTRL+SHIFT+L"),
                CustomLayoutWidget("ex3", "Negrito", CustomWidgetType.SHORTCUT.name, colSpan = 1, keyValue = "CTRL+B"),
                CustomLayoutWidget("ex4", "Copiar", CustomWidgetType.SHORTCUT.name, colSpan = 1, keyValue = "CTRL+C"),
                CustomLayoutWidget("ex5", "Colar", CustomWidgetType.SHORTCUT.name, colSpan = 1, keyValue = "CTRL+V"),
                CustomLayoutWidget("ex6", "Desfazer", CustomWidgetType.SHORTCUT.name, colSpan = 1, keyValue = "CTRL+Z"),
                CustomLayoutWidget("ex7", "Data Hoje", CustomWidgetType.SHORTCUT.name, colSpan = 2, keyValue = "CTRL+;"),
                CustomLayoutWidget("ex8", "Salvar", CustomWidgetType.SHORTCUT.name, colSpan = 2, keyValue = "CTRL+S"),
            )
        ),
        CustomLayoutExportDto(
            name = "VS Code Developer",
            description = "Paleta de comandos, busca global, terminal, comentar e formatar",
            category = "Dev",
            widgets = listOf(
                CustomLayoutWidget("vs1", "Paleta (F1)", CustomWidgetType.KEY.name, colSpan = 2, keyValue = "F1"),
                CustomLayoutWidget("vs2", "Abrir Arquivo", CustomWidgetType.SHORTCUT.name, colSpan = 2, keyValue = "CTRL+P"),
                CustomLayoutWidget("vs3", "Buscar Projeto", CustomWidgetType.SHORTCUT.name, colSpan = 2, keyValue = "CTRL+SHIFT+F"),
                CustomLayoutWidget("vs4", "Comentar", CustomWidgetType.SHORTCUT.name, colSpan = 2, keyValue = "CTRL+/"),
                CustomLayoutWidget("vs5", "Formatar", CustomWidgetType.SHORTCUT.name, colSpan = 2, keyValue = "SHIFT+ALT+F"),
                CustomLayoutWidget("vs6", "Executar (F5)", CustomWidgetType.KEY.name, colSpan = 2, keyValue = "F5"),
                CustomLayoutWidget("vs7", "Salvar Tudo", CustomWidgetType.SHORTCUT.name, colSpan = 4, keyValue = "CTRL+S"),
            )
        ),
        CustomLayoutExportDto(
            name = "Spotify & Música",
            description = "Reprodução, curtir faixa, volume rápido e modo aleatório",
            category = "Mídia",
            widgets = listOf(
                CustomLayoutWidget("sp1", "Anterior", CustomWidgetType.MEDIA.name, colSpan = 1, keyValue = "PREV"),
                CustomLayoutWidget("sp2", "Play / Pause", CustomWidgetType.MEDIA.name, colSpan = 2, keyValue = "PLAY_PAUSE"),
                CustomLayoutWidget("sp3", "Próxima", CustomWidgetType.MEDIA.name, colSpan = 1, keyValue = "NEXT"),
                CustomLayoutWidget("sp4", "Vol -", CustomWidgetType.MEDIA.name, colSpan = 1, keyValue = "VOL_DOWN"),
                CustomLayoutWidget("sp5", "Mudo", CustomWidgetType.MEDIA.name, colSpan = 2, keyValue = "MUTE"),
                CustomLayoutWidget("sp6", "Vol +", CustomWidgetType.MEDIA.name, colSpan = 1, keyValue = "VOL_UP"),
                CustomLayoutWidget("sp7", "Aleatório", CustomWidgetType.SHORTCUT.name, colSpan = 2, keyValue = "CTRL+S"),
                CustomLayoutWidget("sp8", "Repetir", CustomWidgetType.SHORTCUT.name, colSpan = 2, keyValue = "CTRL+R"),
            )
        ),
        CustomLayoutExportDto(
            name = "YouTube Cinema",
            description = "Pausa (K), -10s (J), +10s (L), Legendas (C), Cinema (T) e Tela Cheia (F)",
            category = "Mídia",
            widgets = listOf(
                CustomLayoutWidget("yt1", "-10s (J)", CustomWidgetType.KEY.name, colSpan = 1, keyValue = "J"),
                CustomLayoutWidget("yt2", "Play / Pause (K)", CustomWidgetType.KEY.name, colSpan = 2, keyValue = "K"),
                CustomLayoutWidget("yt3", "+10s (L)", CustomWidgetType.KEY.name, colSpan = 1, keyValue = "L"),
                CustomLayoutWidget("yt4", "Legendas (C)", CustomWidgetType.KEY.name, colSpan = 2, keyValue = "C"),
                CustomLayoutWidget("yt5", "Cinema (T)", CustomWidgetType.KEY.name, colSpan = 1, keyValue = "T"),
                CustomLayoutWidget("yt6", "Cheia (F)", CustomWidgetType.KEY.name, colSpan = 1, keyValue = "F"),
                CustomLayoutWidget("yt7", "Mudo (M)", CustomWidgetType.KEY.name, colSpan = 2, keyValue = "M"),
                CustomLayoutWidget("yt8", "Próximo Vídeo", CustomWidgetType.SHORTCUT.name, colSpan = 2, keyValue = "SHIFT+N"),
            )
        ),
    )
}
