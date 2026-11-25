package com.example.hid

import java.text.Normalizer
import kotlin.math.abs
import kotlin.math.min

data class AutocorrectSuggestion(
    val originalWord: String,
    val replacement: String,
    val isPrimaryCorrection: Boolean,
)

/**
 * Ultra-fast, zero-cost, offline Smart Autocorrect & Accent Restoration Engine (PT-BR + EN).
 * Corrects missing Portuguese accents, common abbreviations, and typos in real time while typing
 * both in Direct Bluetooth HID mode (via automatic backspace + replacement) and Text Field mode.
 */
object SmartAutocorrectEngine {

    // Direct instant corrections (typos, shortcuts, missing accents)
    private val directCorrections: Map<String, String> = mapOf(
        // Portuguese shortcuts & common typos
        "vc" to "você",
        "vcs" to "vocês",
        "voce" to "você",
        "voces" to "vocês",
        "nao" to "não",
        "tb" to "também",
        "tbm" to "também",
        "tambem" to "também",
        "pq" to "porque",
        "qnd" to "quando",
        "mto" to "muito",
        "mt" to "muito",
        "obg" to "obrigado",
        "obgd" to "obrigado",
        "blz" to "beleza",
        "td" to "tudo",
        "cmg" to "comigo",
        "ctg" to "contigo",
        "hj" to "hoje",
        "msg" to "mensagem",
        "msgs" to "mensagens",
        "vlw" to "valeu",
        "pfv" to "por favor",
        "flw" to "falou",
        "eh" to "é",
        "ja" to "já",
        "so" to "só",
        "ate" to "até",
        "sao" to "são",
        "entao" to "então",
        "estao" to "estão",
        "esta" to "está",
        "estava" to "estava",
        "alguem" to "alguém",
        "ninguem" to "ninguém",
        "porem" to "porém",
        "alem" to "além",
        "apos" to "após",
        "atras" to "atrás",
        "atraves" to "através",
        "voce" to "você",
        "nois" to "nós",
        "nosso" to "nosso",
        "irmao" to "irmão",
        "mae" to "mãe",
        "coracao" to "coração",
        "atencao" to "atenção",
        "informacao" to "informação",
        "configuracao" to "configuração",
        "configuracoes" to "configurações",
        "conexao" to "conexão",
        "conexoes" to "conexões",
        "apresentacao" to "apresentação",
        "navegacao" to "navegação",
        "solucao" to "solução",
        "opcao" to "opção",
        "opcoes" to "opções",
        "funcao" to "função",
        "funcoes" to "funções",
        "versao" to "versão",
        "reuniao" to "reunião",
        "decisao" to "decisão",
        "musica" to "música",
        "video" to "vídeo",
        "videos" to "vídeos",
        "numero" to "número",
        "numeros" to "números",
        "codigo" to "código",
        "codigos" to "códigos",
        "rapido" to "rápido",
        "rapida" to "rápida",
        "facil" to "fácil",
        "dificil" to "difícil",
        "proximo" to "próximo",
        "proxima" to "próxima",
        "ultimo" to "último",
        "ultima" to "última",
        "pagina" to "página",
        "paginas" to "páginas",
        "area" to "área",
        "televisao" to "televisão",
        "computdor" to "computador",
        "computadr" to "computador",
        "tecldo" to "teclado",
        "bota" to "botão",
        "botao" to "botão",
        "botoes" to "botões",
        "padrao" to "padrão",
        "horario" to "horário",
        "relatorio" to "relatório",
        "usuario" to "usuário",
        "senhas" to "senhas",
        "seguranca" to "segurança",
        "espaco" to "espaço",
        "comeco" to "começo",
        "servico" to "serviço",
        "endereco" to "endereço",
        "preco" to "preço",
        "diferenca" to "diferença",
        "maca" to "maçã",
        "cafe" to "café",
        "agua" to "água",
        "saude" to "saúde",
        "familia" to "família",
        "historia" to "história",
        "memoria" to "memória",
        "bateria" to "bateria",
        "silencio" to "silêncio",
        "camera" to "câmera",
        "fisico" to "físico",
        "numerico" to "numérico",
        "multimidia" to "multimídia",
        "aereo" to "aéreo",
        "sensivel" to "sensível",
        "disponivel" to "disponível",
        "possivel" to "possível",
        "incrivel" to "incrível",
        "otimo" to "ótimo",
        "otima" to "ótima",
        "ola" to "olá",
        "amanha" to "amanhã",
        "sabado" to "sábado",
        "terca" to "terça",
        "marco" to "março",
        "parabens" to "parabéns",
        "tamben" to "também",
        "entao" to "então",
        "comigo" to "comigo",
        // English common typos
        "teh" to "the",
        "recieve" to "receive",
        "seperate" to "separate",
        "definately" to "definitely",
        "occured" to "occurred",
        "untill" to "until",
        "wierd" to "weird",
        "dont" to "don't",
        "cant" to "can't",
        "wont" to "won't",
        "im" to "I'm",
        "ive" to "I've",
        "didnt" to "didn't",
        "doesnt" to "doesn't",
        "isnt" to "isn't",
        "keybord" to "keyboard",
        "bluethooth" to "bluetooth",
        "compuer" to "computer",
        "presntation" to "presentation",
    )

    // Core high-frequency vocabulary for fuzzy Levenshtein typo matching & completion
    private val coreDictionary: List<String> = listOf(
        "você", "não", "também", "quando", "porque", "muito", "obrigado", "obrigada",
        "computador", "teclado", "mouse", "bluetooth", "controle", "remoto", "televisão",
        "configuração", "conexão", "apresentação", "navegação", "multimídia", "aplicativo",
        "mensagem", "trabalho", "projeto", "reunião", "relatório", "planilha", "documento",
        "arquivo", "sistema", "internet", "navegador", "pesquisa", "endereço", "usuário",
        "senha", "segurança", "dispositivo", "pareado", "conectado", "desconectado",
        "volume", "música", "vídeo", "pausa", "avançar", "voltar", "próximo", "anterior",
        "página", "número", "código", "leitor", "câmera", "layout", "atalho", "macro",
        "rápido", "fácil", "ótimo", "melhor", "agora", "depois", "sempre", "nunca",
        "hoje", "amanhã", "ontem", "semana", "horário", "minuto", "segundo", "preciso",
        "quero", "posso", "fazer", "enviar", "copiar", "colar", "desfazer", "refazer",
        "abrir", "fechar", "salvar", "editar", "apagar", "limpar", "buscar", "selecionar",
        "hello", "world", "computer", "keyboard", "bluetooth", "remote", "control",
        "connect", "device", "settings", "media", "volume", "search", "window", "screen",
        "presentation", "slide", "scanner", "barcode", "custom", "layout", "shortcut",
    )

    private val normalizedDictionary: Map<String, String> =
        coreDictionary.associateBy { stripAccents(it.lowercase()) }

    fun stripAccents(input: String): String {
        val norm = Normalizer.normalize(input, Normalizer.Form.NFD)
        return norm.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
    }

    private fun matchCasing(original: String, corrected: String): String {
        if (original.isEmpty() || corrected.isEmpty()) return corrected
        return when {
            original.all { it.isUpperCase() } && original.length > 1 -> corrected.uppercase()
            original.first().isUpperCase() -> corrected.replaceFirstChar { it.uppercase() }
            else -> corrected
        }
    }

    /**
     * Computes live autocorrect & completion suggestions for the current word being typed.
     */
    fun getSuggestions(currentWord: String, maxResults: Int = 3): List<AutocorrectSuggestion> {
        val trimmed = currentWord.trim()
        if (trimmed.length < 2) return emptyList()
        val lower = trimmed.lowercase()
        val stripped = stripAccents(lower)

        val results = mutableListOf<AutocorrectSuggestion>()
        val seen = mutableSetOf<String>()

        // 1. Direct exact dictionary correction (highest priority)
        directCorrections[lower]?.let { corr ->
            val cased = matchCasing(trimmed, corr)
            if (cased != trimmed) {
                results.add(AutocorrectSuggestion(trimmed, cased, isPrimaryCorrection = true))
                seen.add(cased.lowercase())
            }
        }

        // 2. Accent restoration from normalized dictionary
        normalizedDictionary[stripped]?.let { accented ->
            val cased = matchCasing(trimmed, accented)
            if (cased != trimmed && seen.add(cased.lowercase())) {
                results.add(
                    AutocorrectSuggestion(
                        originalWord = trimmed,
                        replacement = cased,
                        isPrimaryCorrection = results.isEmpty(),
                    )
                )
            }
        }

        // 3. Prefix completions for fast typing
        if (results.size < maxResults && stripped.length >= 3) {
            coreDictionary
                .asSequence()
                .filter { stripAccents(it.lowercase()).startsWith(stripped) && it.lowercase() != lower }
                .take(maxResults - results.size)
                .forEach { candidate ->
                    val cased = matchCasing(trimmed, candidate)
                    if (seen.add(cased.lowercase())) {
                        results.add(AutocorrectSuggestion(trimmed, cased, isPrimaryCorrection = false))
                    }
                }
        }

        // 4. Fuzzy Levenshtein typo correction (edit distance <= 2)
        if (results.size < maxResults && stripped.length >= 4) {
            val bestFuzzy = coreDictionary
                .asSequence()
                .filter { abs(it.length - stripped.length) <= 2 }
                .map { it to levenshtein(stripped, stripAccents(it.lowercase())) }
                .filter { it.second in 1..2 }
                .sortedBy { it.second }
                .take(maxResults - results.size)

            bestFuzzy.forEach { (candidate, dist) ->
                val cased = matchCasing(trimmed, candidate)
                if (cased != trimmed && seen.add(cased.lowercase())) {
                    results.add(
                        AutocorrectSuggestion(
                            originalWord = trimmed,
                            replacement = cased,
                            isPrimaryCorrection = results.isEmpty() && dist == 1,
                        )
                    )
                }
            }
        }

        return results.take(maxResults)
    }

    /**
     * Returns the best auto-correction when the user presses space/punctuation, or null if unchanged.
     */
    fun autoCorrectWord(word: String): String? {
        val primary = getSuggestions(word, maxResults = 1).firstOrNull { it.isPrimaryCorrection }
        return primary?.replacement
    }

    private fun levenshtein(a: String, b: String): Int {
        val dp = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            var prev = i - 1
            dp[0] = i
            for (j in 1..b.length) {
                val temp = dp[j]
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[j] = min(min(dp[j] + 1, dp[j - 1] + 1), prev + cost)
                prev = temp
            }
        }
        return dp[b.length]
    }
}
