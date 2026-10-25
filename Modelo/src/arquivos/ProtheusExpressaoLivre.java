package arquivos;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extrator dinâmico de Expressão Livre de relatórios Protheus (RDM).
 * Totalmente desacoplado - processa múltiplas expressões em memória.
 *
 * <p>Características:</p>
 * <ul>
 *   <li>Suporte a 1 ou N expressões livres no XML</li>
 *   <li>Extração automática de valores do ListParametro</li>
 *   <li>Processamento em memória - alta performance</li>
 *   <li>Totalmente desacoplado - apenas XML in, resultado out</li>
 * </ul>
 *
 * @author Equipe de Desenvolvimento
 * @version 5.0
 * @since 2024
 */
public final class ProtheusExpressaoLivre {

    // ===== CONSTANTES =====
    private static final String TAG_EXPRESSAO_LIVRE = "ExpressaoLivre";
    private static final String TAG_LISTA_PARAMETRO = "ListParametro";
    private static final String TAG_PARAMETRO = "P";
    private static final String TAG_TEXTO = "Texto";

    private static final String ATTR_NAME = "name";
    private static final String ATTR_NOME = "Nome";
    private static final String ATTR_ID_FILTRO = "IdFiltro";
    private static final String ATTR_ID_FILTRO_INI = "IdFiltroIni";
    private static final String ATTR_ID_FILTRO_FIM = "IdFiltroFim";
    private static final String ATTR_VALOR_INICIAL = "ValorInicial";
    private static final String ATTR_VALOR_FINAL = "ValorFinal";
    private static final String ATTR_TIPO = "Tipo";
    private static final String ATTR_ID = "Id";

    private static final String PLACEHOLDER_PREFIX = "${";
    private static final String PLACEHOLDER_SUFFIX = "}";
    private static final String PLACEHOLDER_PATTERN = "\\$\\{([^}]+)\\}";
    private static final String SUFFIX_INI = "_INI";
    private static final String SUFFIX_FIM = "_FIM";

    private static final String TIPO_DATA = "D";
    private static final String TIPO_NUMERICO = "N";

    // ===== MOCK JAVASCRIPT OTIMIZADO =====
    private static final String JS_MOCK =
            "var TJavaScript = {" +
                    "  fnExecutaMascara: function(m, v, u) {" +
                    "    if (!v) return '';" +
                    "    var s = String(v).replace(/\\D/g, '');" +
                    "    switch(m) {" +
                    "      case 'fnFormataData':" +
                    "        return s.length === 8 ? s.substr(6,2)+'/'+s.substr(4,2)+'/'+s.substr(0,4) : v;" +
                    "      case 'fnFormataValor':" +
                    "        var n = parseFloat(v.replace(/\\./g,'').replace(',','.'));" +
                    "        return isNaN(n) ? v : n.toFixed(2).replace('.',',');" +
                    "      case 'fnFormataCEP':" +
                    "        return s.length === 8 ? s.substr(0,5)+'-'+s.substr(5,3) : v;" +
                    "      case 'fnFormataCNPJ':" +
                    "        return s.length === 14 ? s.substr(0,2)+'.'+s.substr(2,3)+'.'+s.substr(5,3)+'/'+s.substr(8,4)+'-'+s.substr(12,2) : v;" +
                    "      case 'fnFormataCPF':" +
                    "        return s.length === 11 ? s.substr(0,3)+'.'+s.substr(3,3)+'.'+s.substr(6,3)+'-'+s.substr(9,2) : v;" +
                    "      default: return v;" +
                    "    }" +
                    "  }" +
                    "};";

    // ===== ENGINE SINGLETON =====
    private static final ScriptEngine SCRIPT_ENGINE;

    static {
        SCRIPT_ENGINE = new ScriptEngineManager().getEngineByName("nashorn");
        if (SCRIPT_ENGINE == null) {
            throw new IllegalStateException("Motor Nashorn não encontrado. Use JDK 8+");
        }
        try {
            SCRIPT_ENGINE.eval(JS_MOCK);
        } catch (ScriptException e) {
            throw new IllegalStateException("Erro ao inicializar JavaScript engine", e);
        }
    }

    // ===== CONSTRUTOR PRIVADO =====
    private ProtheusExpressaoLivre() {
        throw new UnsupportedOperationException("Classe utilitária");
    }

    // ================================================================
    //  API PÚBLICA - MÉTODOS DESACOPLADOS
    // ================================================================

    /**
     * Processa todas as expressões livres do XML.
     *
     * @param xml Conteúdo XML do relatório
     * @return Lista de resultados (pode ter 1 ou mais)
     * @throws ExpressaoLivreException em caso de erro
     */
    public static List<ResultadoExpressao> processarTodas(String xml)
            throws ExpressaoLivreException {
        return processarTodas(xml, null);
    }

    /**
     * Processa todas as expressões livres com valores extras.
     *
     * @param xml Conteúdo XML do relatório
     * @param valoresExtras Valores adicionais para substituição
     * @return Lista de resultados (pode ter 1 ou mais)
     * @throws ExpressaoLivreException em caso de erro
     */
    public static List<ResultadoExpressao> processarTodas(String xml, Map<String, String> valoresExtras)
            throws ExpressaoLivreException {
        try {
            // 1. Parse XML em memória
            Document doc = parseXmlEmMemoria(xml);

            // 2. Extrair valores dos parâmetros
            Map<String, String> valores = extrairValores(doc);

            // 3. Mesclar valores extras
            if (valoresExtras != null) {
                valoresExtras.forEach((k, v) -> {
                    if (v != null) valores.put(k.toUpperCase(), v);
                });
            }

            // 4. Extrair e processar todas as expressões
            return processarExpressoes(doc, valores);

        } catch (Exception e) {
            throw new ExpressaoLivreException("Erro ao processar XML", e);
        }
    }

    /**
     * Processa apenas a primeira expressão livre encontrada.
     *
     * @param xml Conteúdo XML do relatório
     * @return Resultado da primeira expressão
     * @throws ExpressaoLivreException em caso de erro
     */
    public static String processarPrimeira(String xml)
            throws ExpressaoLivreException {
        List<ResultadoExpressao> resultados = processarTodas(xml, null);
        if (resultados.isEmpty()) {
            throw new ExpressaoLivreException("Nenhuma expressão encontrada");
        }
        return resultados.get(0).getResultado();
    }

    /**
     * Processa expressão específica por ID.
     *
     * @param xml Conteúdo XML do relatório
     * @param idExpressao ID da expressão a ser processada
     * @return Resultado da expressão
     * @throws ExpressaoLivreException em caso de erro
     */
    public static String processarPorId(String xml, String idExpressao)
            throws ExpressaoLivreException {
        List<ResultadoExpressao> resultados = processarTodas(xml, null);
        return resultados.stream()
                .filter(r -> idExpressao.equals(r.getId()))
                .findFirst()
                .map(ResultadoExpressao::getResultado)
                .orElseThrow(() -> new ExpressaoLivreException(
                        "Expressão com ID '" + idExpressao + "' não encontrada"
                ));
    }

    /**
     * Processa expressões filtrando por nome.
     *
     * @param xml Conteúdo XML do relatório
     * @param nomeExpressao Nome da expressão (atributo name)
     * @return Lista de resultados com esse nome
     * @throws ExpressaoLivreException em caso de erro
     */
    public static List<ResultadoExpressao> processarPorNome(String xml, String nomeExpressao)
            throws ExpressaoLivreException {
        List<ResultadoExpressao> todos = processarTodas(xml, null);
        List<ResultadoExpressao> filtrados = new ArrayList<>();
        for (ResultadoExpressao r : todos) {
            if (nomeExpressao.equals(r.getNome())) {
                filtrados.add(r);
            }
        }
        return filtrados;
    }

    // ================================================================
    //  MÉTODOS INTERNOS - PROCESSAMENTO
    // ================================================================

    /**
     * Processa todas as expressões encontradas no XML.
     */
    private static List<ResultadoExpressao> processarExpressoes(Document doc, Map<String, String> valores)
            throws ExpressaoLivreException, ScriptException {

        List<ResultadoExpressao> resultados = new ArrayList<>();
        NodeList expressoes = doc.getElementsByTagName(TAG_EXPRESSAO_LIVRE);

        for (int i = 0; i < expressoes.getLength(); i++) {
            Element el = (Element) expressoes.item(i);
            String nome = el.getAttribute(ATTR_NAME);

            // Processa cada texto dentro da expressão
            NodeList textos = el.getElementsByTagName(TAG_TEXTO);
            for (int j = 0; j < textos.getLength(); j++) {
                Element textoEl = (Element) textos.item(j);
                String id = textoEl.getAttribute(ATTR_ID);
                String conteudo = textoEl.getTextContent().trim();

                if (conteudo.isEmpty()) continue;

                // Processa a expressão
                String processada = substituirPlaceholders(conteudo, valores);
                String resultado = executarJavaScript(processada);

                // Cria objeto resultado
                ResultadoExpressao resultadoObj = new ResultadoExpressao();
                resultadoObj.setId(id.isEmpty() ? "expr_" + i + "_" + j : id);
                resultadoObj.setNome(nome);
                resultadoObj.setExpressaoOriginal(conteudo);
                resultadoObj.setExpressaoProcessada(processada);
                resultadoObj.setResultado(resultado);
                resultadoObj.setIndice(i);
                resultadoObj.setSubIndice(j);

                resultados.add(resultadoObj);
            }
        }

        if (resultados.isEmpty()) {
            throw new ExpressaoLivreException("Nenhuma expressão válida encontrada no XML");
        }

        return resultados;
    }

    /**
     * Extrai valores dos parâmetros do XML.
     */
    private static Map<String, String> extrairValores(Document doc) {
        Map<String, String> valores = new LinkedHashMap<>();
        NodeList params = doc.getElementsByTagName(TAG_PARAMETRO);

        for (int i = 0; i < params.getLength(); i++) {
            Element p = (Element) params.item(i);
            String nome = p.getAttribute(ATTR_NOME);

            if (nome == null || nome.isEmpty()) continue;

            String valIni = p.getAttribute(ATTR_VALOR_INICIAL);
            String valFim = p.getAttribute(ATTR_VALOR_FINAL);
            String idIni = p.getAttribute(ATTR_ID_FILTRO_INI);
            String idFim = p.getAttribute(ATTR_ID_FILTRO_FIM);
            String tipo = p.getAttribute(ATTR_TIPO);
            String idFiltro = p.getAttribute(ATTR_ID_FILTRO);

            // Mapeia valores
            if (!valIni.isEmpty()) {
                valores.put(nome, converterValor(valIni, tipo));
                if (!idIni.isEmpty()) {
                    valores.put(nome + SUFFIX_INI, converterValor(valIni, tipo));
                }
            }

            if (!valFim.isEmpty()) {
                if (!idFim.isEmpty()) {
                    valores.put(nome + SUFFIX_FIM, converterValor(valFim, tipo));
                }
                if (!valIni.isEmpty() && !valores.containsKey(nome)) {
                    valores.put(nome, converterValor(valFim, tipo));
                }
            }

            if (!idFiltro.isEmpty() && !valores.containsKey(nome) && !valIni.isEmpty()) {
                valores.put(nome, converterValor(valIni, tipo));
            }
        }

        return valores;
    }

    /**
     * Converte valor conforme tipo.
     */
    private static String converterValor(String valor, String tipo) {
        if (valor == null || valor.isEmpty()) return valor;
        if (TIPO_NUMERICO.equals(tipo)) {
            return valor.replace(".", "").replace(",", ".");
        }
        return valor;
    }

    /**
     * Substitui placeholders na expressão.
     */
    private static String substituirPlaceholders(String texto, Map<String, String> valores) {
        if (valores == null || valores.isEmpty()) return texto;

        String resultado = texto;
        for (Map.Entry<String, String> entry : valores.entrySet()) {
            String chave = entry.getKey();
            String valor = Objects.toString(entry.getValue(), "");
            resultado = resultado.replace(PLACEHOLDER_PREFIX + chave + PLACEHOLDER_SUFFIX, valor);
            resultado = resultado.replace(PLACEHOLDER_PREFIX + chave.toUpperCase() + PLACEHOLDER_SUFFIX, valor);
        }

        return resultado;
    }

    /**
     * Executa JavaScript.
     */
    private static String executarJavaScript(String expressao) throws ScriptException {
        Object resultado = SCRIPT_ENGINE.eval(expressao);
        return Objects.toString(resultado, "");
    }

    /**
     * Parseia XML em memória.
     */
    private static Document parseXmlEmMemoria(String xml)
            throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        byte[] bytes = xml.getBytes(StandardCharsets.ISO_8859_1);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);

        return builder.parse(inputStream);
    }

    // ================================================================
    //  MÉTODOS DE DEBUG
    // ================================================================

    /**
     * Retorna informações detalhadas para debug.
     */
    public static String debug(String xml) throws ExpressaoLivreException {
        try {
            Document doc = parseXmlEmMemoria(xml);
            Map<String, String> valores = extrairValores(doc);
            List<ResultadoExpressao> resultados = processarExpressoes(doc, valores);

            StringBuilder sb = new StringBuilder();
            sb.append("========== DEBUG EXPRESSÕES ==========\n\n");

            sb.append("VALORES ENCONTRADOS:\n");
            valores.forEach((k, v) -> sb.append("  ").append(k).append(" = ").append(v).append("\n"));

            sb.append("\nEXPRESSÕES PROCESSADAS (").append(resultados.size()).append("):\n\n");
            for (ResultadoExpressao r : resultados) {
                sb.append("--- ").append(r.getNome());
                if (!r.getId().isEmpty()) {
                    sb.append(" [ID: ").append(r.getId()).append("]");
                }
                sb.append(" ---\n");
                sb.append("  Original: ").append(r.getExpressaoOriginal()).append("\n");
                sb.append("  Processada: ").append(r.getExpressaoProcessada()).append("\n");
                sb.append("  Resultado: ").append(r.getResultado()).append("\n\n");
            }

            return sb.toString();

        } catch (Exception e) {
            throw new ExpressaoLivreException("Erro no debug: " + e.getMessage(), e);
        }
    }

    // ================================================================
    //  CLASSE DE RESULTADO
    // ================================================================

    /**
     * Representa o resultado de uma expressão processada.
     */
    public static class ResultadoExpressao {
        private String id;
        private String nome;
        private String expressaoOriginal;
        private String expressaoProcessada;
        private String resultado;
        private int indice;
        private int subIndice;

        // Getters e Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getNome() { return nome; }
        public void setNome(String nome) { this.nome = nome; }

        public String getExpressaoOriginal() { return expressaoOriginal; }
        public void setExpressaoOriginal(String expressaoOriginal) { this.expressaoOriginal = expressaoOriginal; }

        public String getExpressaoProcessada() { return expressaoProcessada; }
        public void setExpressaoProcessada(String expressaoProcessada) { this.expressaoProcessada = expressaoProcessada; }

        public String getResultado() { return resultado; }
        public void setResultado(String resultado) { this.resultado = resultado; }

        public int getIndice() { return indice; }
        public void setIndice(int indice) { this.indice = indice; }

        public int getSubIndice() { return subIndice; }
        public void setSubIndice(int subIndice) { this.subIndice = subIndice; }

        @Override
        public String toString() {
            return String.format("ResultadoExpressao{id='%s', nome='%s', resultado='%s'}",
                    id, nome, resultado);
        }
    }

    // ================================================================
    //  EXEMPLOS DE USO
    // ================================================================

    public static void main(String[] args) {
        try {
            // ===== XML COM MÚLTIPLAS EXPRESSÕES =====
            String xmlMultiplas = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n" +
                    "<Param>\n" +
                    "  <ListParametro>\n" +
                    "    <P Nome=\"pDT_INI\" ValorInicial=\"20260801\" Tipo=\"D\"/>\n" +
                    "    <P Nome=\"pDT_FIM\" ValorInicial=\"20260820\" Tipo=\"D\"/>\n" +
                    "    <P Nome=\"pVALOR\" ValorInicial=\"1500.50\" Tipo=\"N\"/>\n" +
                    "    <P Nome=\"pNOME\" ValorInicial=\"João Silva\" Tipo=\"C\"/>\n" +
                    "  </ListParametro>\n" +
                    "  \n" +
                    "  <!-- Expressão 1: Formata data -->\n" +
                    "  <ExpressaoLivre name=\"Periodo\">\n" +
                    "    <Texto Id=\"PERIODO_FORMATADO\">\n" +
                    "      TJavaScript.fnExecutaMascara('fnFormataData', '${PDT_INI}', false) + ' a ' +\n" +
                    "      TJavaScript.fnExecutaMascara('fnFormataData', '${PDT_FIM}', false)\n" +
                    "    </Texto>\n" +
                    "  </ExpressaoLivre>\n" +
                    "  \n" +
                    "  <!-- Expressão 2: Valor formatado -->\n" +
                    "  <ExpressaoLivre name=\"Valor\">\n" +
                    "    <Texto Id=\"VALOR_FORMATADO\">\n" +
                    "      'R$ ' + TJavaScript.fnExecutaMascara('fnFormataValor', '${PVALOR}', false)\n" +
                    "    </Texto>\n" +
                    "  </ExpressaoLivre>\n" +
                    "  \n" +
                    "  <!-- Expressão 3: Mensagem completa -->\n" +
                    "  <ExpressaoLivre name=\"Mensagem\">\n" +
                    "    <Texto Id=\"MSG_COMPLETA\">\n" +
                    "      'Relatório de ' + '${PNOME}' + ' - Período: ' +\n" +
                    "      TJavaScript.fnExecutaMascara('fnFormataData', '${PDT_INI}', false) + ' a ' +\n" +
                    "      TJavaScript.fnExecutaMascara('fnFormataData', '${PDT_FIM}', false) +\n" +
                    "      ' - Valor Total: R$ ' + TJavaScript.fnExecutaMascara('fnFormataValor', '${PVALOR}', false)\n" +
                    "    </Texto>\n" +
                    "  </ExpressaoLivre>\n" +
                    "  \n" +
                    "  <!-- Expressão 4: Cálculo -->\n" +
                    "  <ExpressaoLivre name=\"Calculo\">\n" +
                    "    <Texto Id=\"CALCULO_SIMPLES\">\n" +
                    "      var valor = parseFloat('${PVALOR}'.replace(',', '.'));\n" +
                    "      var desconto = valor * 0.1;\n" +
                    "      'Desconto de 10%: R$ ' + TJavaScript.fnExecutaMascara('fnFormataValor', String(desconto), false)\n" +
                    "    </Texto>\n" +
                    "  </ExpressaoLivre>\n" +
                    "</Param>";

            // ===== EXEMPLO 1: Processar todas as expressões =====
            System.out.println("=== PROCESSAR TODAS AS EXPRESSÕES ===\n");
            List<ResultadoExpressao> resultados = processarTodas(xmlMultiplas);

            for (ResultadoExpressao r : resultados) {
                System.out.printf("ID: %s | Nome: %s | Resultado: %s%n",
                        r.getId(), r.getNome(), r.getResultado());
            }

            // ===== EXEMPLO 2: Processar apenas a primeira =====
            System.out.println("\n=== PROCESSAR PRIMEIRA EXPRESSÃO ===");
            String primeira = processarPrimeira(xmlMultiplas);
            System.out.println("Resultado: " + primeira);

            // ===== EXEMPLO 3: Processar por ID =====
            System.out.println("\n=== PROCESSAR POR ID ===");
            String porId = processarPorId(xmlMultiplas, "VALOR_FORMATADO");
            System.out.println("Resultado: " + porId);

            // ===== EXEMPLO 4: Processar por Nome =====
            System.out.println("\n=== PROCESSAR POR NOME ===");
            List<ResultadoExpressao> porNome = processarPorNome(xmlMultiplas, "Mensagem");
            for (ResultadoExpressao r : porNome) {
                System.out.println("Resultado: " + r.getResultado());
            }

            // ===== EXEMPLO 5: Com valores extras =====
            System.out.println("\n=== COM VALORES EXTRAS ===");
            Map<String, String> extras = new HashMap<>();
            extras.put("PDT_INI", "20260901");
            extras.put("PDT_FIM", "20260915");
            extras.put("PVALOR", "2500.75");
            extras.put("PNOME", "Maria Santos");

            List<ResultadoExpressao> comExtras = processarTodas(xmlMultiplas, extras);
            for (ResultadoExpressao r : comExtras) {
                if ("MSG_COMPLETA".equals(r.getId())) {
                    System.out.println("Com override: " + r.getResultado());
                }
            }

            // ===== DEBUG =====
            System.out.println("\n" + debug(xmlMultiplas));

            // ===== TESTE DE PERFORMANCE =====
            System.out.println("=== TESTE DE PERFORMANCE ===");
            long inicio = System.nanoTime();
            for (int i = 0; i < 1000; i++) {
                processarTodas(xmlMultiplas);
            }
            long fim = System.nanoTime();
            System.out.printf("1000 execuções em %dms%n", (fim - inicio) / 1_000_000);

        } catch (ExpressaoLivreException e) {
            System.err.println("Erro: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

/**
 * Exceção personalizada.
 */
class ExpressaoLivreException extends Exception {
    private static final long serialVersionUID = 1L;

    public ExpressaoLivreException(String msg) {
        super(msg);
    }

    public ExpressaoLivreException(String msg, Throwable cause) {
        super(msg, cause);
    }
}