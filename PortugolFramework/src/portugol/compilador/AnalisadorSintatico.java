/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package portugol.compilador;

import javax.swing.JOptionPane;
import portugol.arvoresintatica.NoPrograma;
import portugol.arvoresintatica.NoExpressao;
import portugol.arvoresintatica.NoComando;
import portugol.arvoresintatica.NoComandoCondicao;
import portugol.arvoresintatica.NoComandoEnquantoFaca;
import portugol.arvoresintatica.NoComandoDeAte;
import portugol.arvoresintatica.NoNumeroInteiro;
import portugol.arvoresintatica.NoNumeroReal;
import portugol.arvoresintatica.NoExpressaoAritmetica;
import portugol.arvoresintatica.NoComandoAtribuicao;
import portugol.arvoresintatica.NoComandoLer;
import portugol.arvoresintatica.NoComandoEscrever;
import portugol.arvoresintatica.NoIdentificador;
import portugol.arvoresintatica.NoCadeiaCaracteres;
import portugol.arvoresintatica.NoExpressaoRelacional;
import portugol.arvoresintatica.TipoOperacaoAritmetica;
import portugol.arvoresintatica.NoBlocoComandos;
import portugol.arvoresintatica.TipoValor;
import portugol.lexico.Token;
import portugol.arvoresintatica.TipoRelacao;
import portugol.lexico.DadosIdentificador;
import portugol.lexico.Lexema;

/**
 *
 * @author Chessman Kennedy Faria Corrêa
 *
 * A tradutor realiza todas as tarefas relacionadas à fase de síntese do
 * compilador, sou seja, análise léxica, análise sintática, análise semântica,
 * e geração de código intermediário.
 */
public class AnalisadorSintatico {

    private Token token_a_frente;
    private AnalisadorLexico analisadorLexico;
    private TratadorErro tratadorErro;
    
    public AnalisadorSintatico (TabelaSimbolos tabelaSimbolos, TratadorErro tratadorErro){
        analisadorLexico = new AnalisadorLexico(tabelaSimbolos);
        this.tratadorErro = tratadorErro;
    }

    private void reconhecerToken(Token tokenEsperado) {
        if (token_a_frente != null){
            if (token_a_frente == tokenEsperado) {
                token_a_frente = analisadorLexico.obterToken();
            } else {
                /*
                * O tratador de erro esta usando execoes para emitir a mensagem
                * de erro e interromper a execucao do programa. O bloco catch
                * esta sendo usado para mostrar a mensagem de erro e evitar que
                * os metodos que chamam reconhecer tenham que fazer o tratamento
                * da execao ou propaga-la para frente.
                */
                try{
                   tratadorErro.emitirErroSintatico(tokenEsperado, token_a_frente, analisadorLexico.obterNumeroLinha());

                }catch(Exception e){
                    System.out.println(e.toString());
//                    JOptionPane.showMessageDialog(null, e.toString());
                }
            }
        } else {
                // Ver obervacao acima
                try{
                    tratadorErro.emitirErroLexico("",
                                                  analisadorLexico.obterNumeroLinha(),
                                                  analisadorLexico.obterNumeroColuna());
                } catch(Exception e) {
                    JOptionPane.showMessageDialog(null, e.toString());
                }
        }
    }

    private NoIdentificador reconhecerIdentificador(){
        int numeroLinhaIdentificador = analisadorLexico.obterNumeroLinha();

        DadosIdentificador dadosIdentificador = null;

        if (token_a_frente == Token.IDENTIFICADOR){
            dadosIdentificador = analisadorLexico.obterDadosIdentificador();
        }

        reconhecerToken(Token.IDENTIFICADOR);

        return new NoIdentificador(dadosIdentificador, numeroLinhaIdentificador);
    }

    private NoCadeiaCaracteres reconhecerCadeiaCaracteres(){

        int numeroLinha = analisadorLexico.obterNumeroLinha();
        String valor = analisadorLexico.obterLexema();

        reconhecerToken(Token.CADEIA_CARACTERES);
        return new NoCadeiaCaracteres(valor, numeroLinha);
    }

    private NoNumeroInteiro reconhecerNumeroInteiro(){

        int numeroLinha = analisadorLexico.obterNumeroLinha();
        int valor = Integer.parseInt(analisadorLexico.obterLexema());

        reconhecerToken(Token.NUMERO_INTEIRO);
        return new NoNumeroInteiro(valor, numeroLinha);
    }

    private NoNumeroReal reconhecerNumeroReal(){

        int numeroLinha = analisadorLexico.obterNumeroLinha();
        float valor = Float.parseFloat(analisadorLexico.obterLexema());

        reconhecerToken(Token.NUMERO_REAL);
        return new NoNumeroReal(valor, numeroLinha);
    }


    public TipoValor obterTipoValorExpressao(String lexema){
        if (lexema.equalsIgnoreCase(Lexema.REAL)){
            return TipoValor.REAL;
        } else if (lexema.equalsIgnoreCase(Lexema.INTEIRO)){
            return TipoValor.INTEIRO;
        } else if (lexema.equalsIgnoreCase(Lexema.CADEIA_CARACTERES)){
            return TipoValor.CADEIA_CARACTERES;
        } else {
            return TipoValor.INDEFINIDO;
        }
         
    }
    /* Reconhece a produção: declaracao -> identificador : tipo_identificador fim_comando
     * Reconhece uma declaracao de variavel. Esta sendo aproveitada para
     * a especificacao do tipo da variavel. Além de verificar a sintaxe da
     * declaração, o tipo do identificador também é definido. Isso corresponde
     * ao registro do tipo do identificador na tabela de símbolos. Também vale
     * a pena notar que não existe necessidade de representar as variáveis como
     * uma árvore sintática, uma vez que os dados de tipo dos identificadores
     * serão obtidos da tabela de símbolos.
     */
    private void reconhecerDeclaracao(){
        if (token_a_frente != null){
            //É preciso salvar o identificador para que o seu tipo possa ser
            //definido.
            NoIdentificador identificador = reconhecerIdentificador();
            reconhecerToken(Token.DOIS_PONTOS);
            //É necessário manter o tipo para a definição do tipo da veriável
            //após o fim da análise sintática de declaração.
            TipoValor tipoValorVariavel = obterTipoValorExpressao(analisadorLexico.obterLexema());
            reconhecerToken(Token.TIPO_VARIAVEL);
//            reconhecerToken(Token.IDENTIFICADOR);
            identificador.definirTipoValor(tipoValorVariavel);
        }
    }

    /* Reconhece a produção: bloco_declaracoes -> declaracao*
     * Como a quantidade de declarações de variáveis é indefinida, é necessário
     * um laço para identificar cada declaração. Observar que o laço é executado
     * enquanto existir um token e quanto este token for diferente de IDETIFICADOR.
     * A presença deste token indica que terminou o bloco de declarações.
     */
    public void reconhecerBlocoDeclaracoes(){
        while ((token_a_frente != null) && (token_a_frente == Token.IDENTIFICADOR)){
            reconhecerDeclaracao();
        }
    }

    //Reconhece a produção: programa -> programa bloco_declaracoes bloco_comandos fim_comando
    private NoPrograma reconhecerPrograma(){
        reconhecerToken(Token.PROGRAMA);
        NoIdentificador identificador = reconhecerIdentificador();
        identificador.definirTipoValor(TipoValor.CADEIA_CARACTERES);
        reconhecerBlocoDeclaracoes();
        NoBlocoComandos blocoComandos = reconhecerBlocoComandos();

        return new NoPrograma(identificador, blocoComandos);
        //O último ponto-e-vírgula esta sendo ignorado.
    }

    /* Reconhece a produção: bloco_comandos -> inicio comando* fim_comando
     * Observar que o primeiro token é usado para determinar qual método
     * de produção será executado.
    */
    private NoBlocoComandos reconhecerBlocoComandos(){
        int numeroLinhaBloco = analisadorLexico.obterNumeroLinha();
        reconhecerToken(Token.INICIO);
        NoBlocoComandos listaComandosLocal = new NoBlocoComandos(numeroLinhaBloco);
        NoComando comando = null;
        boolean temComando = true;
        while (analisadorLexico.temCodigo() && temComando){
            switch (token_a_frente){
                case LER:
                    comando = reconhecerComandoLer();
                    break;
                case ESCREVER:
                    comando = reconhecerComandoEscrever();
                    break;
                case SE:
                    comando = reconhecerComandoCondicao();
                    break;
                case ENQUANTO:
                    comando = reconhecerComandoEnquantoFaca();
                    break;
                case DE:
                    comando = reconhecerComandoDeAteFaca();
                    break;
                case IDENTIFICADOR:
                case ATRIBUICAO:
                    comando = reconhecerComandoAtribuicao();
                    break;
                default:
                    temComando = false;
            }
            if (temComando) {
                listaComandosLocal.incluirOperacao(comando);
            }
        }
        reconhecerToken(Token.FIM);
        return listaComandosLocal;
    }

    private TipoRelacao obterValorRelacao(String lexema){
        TipoRelacao valorRelacao;
        if (lexema.equals(Lexema.RELACAO_IGUAL)){
            valorRelacao = TipoRelacao.IGUAL;
        } else if (lexema.equals(Lexema.RELACAO_DIFERENTE)){
            valorRelacao = TipoRelacao.DIFERENTE;
        } else if (lexema.equals(Lexema.RELACAO_MAIOR)){
            valorRelacao = TipoRelacao.MAIOR;
        } else if (lexema.equals(Lexema.RELACAO_MENOR)){
            valorRelacao = TipoRelacao.MENOR;
        } else if (lexema.equals(Lexema.RELACAO_MAIOR_IGUAL)){
            valorRelacao = TipoRelacao.MAIOR_IGUAL;
        } else {
            valorRelacao = TipoRelacao.MENOR_IGUAL;
        }
        return valorRelacao;
    }
    private NoExpressaoRelacional reconhecerExpressaoRelacional(){
        int numeroLinhaExpressao = analisadorLexico.obterNumeroLinha();

        NoExpressao operandoEsquerdo = reconhecerExpressaoAritmetica();
        TipoRelacao valorRelacao = obterValorRelacao(analisadorLexico.obterLexema());
        reconhecerToken(Token.RELACAO);
        NoExpressao operandoDireito = reconhecerExpressaoAritmetica();

        return new NoExpressaoRelacional(valorRelacao, operandoEsquerdo, operandoDireito, numeroLinhaExpressao);
    }


    private NoExpressao reconhecerFator(){
        NoExpressao simbolo = null;
        switch (token_a_frente){
            case NUMERO_INTEIRO:
                simbolo = reconhecerNumeroInteiro();
                break;
            case NUMERO_REAL:
                simbolo = reconhecerNumeroReal();
                break;
            case ABRE_PARENTESES:
                reconhecerToken(Token.ABRE_PARENTESES);
                simbolo = reconhecerExpressaoAritmetica();
                reconhecerToken(Token.FECHA_PARENTESES);
                break;
            case IDENTIFICADOR:
                simbolo = reconhecerIdentificador();
        }
        return simbolo;
    }

    private  NoExpressao reconhecerTermo(){
        NoExpressao operandoEsquerdo = reconhecerFator();
        return reconhecerTermo2(operandoEsquerdo);
    }

    private NoExpressao reconhecerTermo2(NoExpressao operandoEsquerdo){
        int numeroLinhaTermo = analisadorLexico.obterNumeroLinha();
        if (token_a_frente == Token.MULTIPLICACAO){
            reconhecerToken(Token.MULTIPLICACAO);
            NoExpressao operandoDireito = reconhecerFator();
            return new NoExpressaoAritmetica(TipoOperacaoAritmetica.MULTIPLICACAO,
                                             operandoEsquerdo,
                                             reconhecerTermo2(operandoDireito),
                                             numeroLinhaTermo);
        } else if (token_a_frente == Token.DIVISAO){
            reconhecerToken(Token.DIVISAO);
            NoExpressao operandoDireito = reconhecerFator();
            return new NoExpressaoAritmetica(TipoOperacaoAritmetica.DIVISAO,
                                             operandoEsquerdo,
                                             reconhecerTermo2(operandoDireito),
                                             numeroLinhaTermo);
        }
        return operandoEsquerdo;
    }

    private NoExpressao reconhecerExpressaoAritmetica(){
        NoExpressao termo = reconhecerTermo();
        return reconhecerExpressaoAritmetica2(termo);
    }

    private  NoExpressao reconhecerExpressaoAritmetica2(NoExpressao operandoEsquerdo){
        int numeroLinhaExpressao = analisadorLexico.obterNumeroLinha();
        if (token_a_frente == Token.ADICAO){
            reconhecerToken(Token.ADICAO);
            NoExpressao operandoDireito = reconhecerTermo();
            return new NoExpressaoAritmetica(TipoOperacaoAritmetica.ADICAO,
                                             operandoEsquerdo,
                                             reconhecerExpressaoAritmetica2(operandoDireito),
                                             numeroLinhaExpressao);
        } else if (token_a_frente == Token.SUBTRACAO){
            reconhecerToken(Token.SUBTRACAO);
            NoExpressao operandoDireito = reconhecerTermo();
            return new NoExpressaoAritmetica(TipoOperacaoAritmetica.SUBTRACAO,
                                             operandoEsquerdo,
                                             reconhecerExpressaoAritmetica2(operandoDireito),
                                             numeroLinhaExpressao);
        }
        return operandoEsquerdo;
    }

    public NoPrograma executar(String codigoFonte){
        analisadorLexico.definirCodigo(codigoFonte);
        if (analisadorLexico.temCodigo()){
           token_a_frente = analisadorLexico.obterToken();
           return reconhecerPrograma();
        }
        return null;
    }

    public NoComandoLer reconhecerComandoLer(){
        int numeroLinhaTokenLer = analisadorLexico.obterNumeroLinha();
        
        reconhecerToken(Token.LER);
        
        NoIdentificador identificador = reconhecerIdentificador();
        
        if (token_a_frente == Token.IDENTIFICADOR) {
            reconhecerToken(Token.IDENTIFICADOR);
        } else {
            reconhecerToken(Token.LER);
        }
 
        return new NoComandoLer(identificador, numeroLinhaTokenLer);
    }

    public NoComandoEscrever reconhecerComandoEscrever(){
        int numeroLinhaComandoEscrever = analisadorLexico.obterNumeroLinha();
        NoExpressao operando;
        reconhecerToken(Token.ESCREVER);

        switch (token_a_frente) {
            case IDENTIFICADOR:
                operando = reconhecerIdentificador();
                break;
            case NUMERO_INTEIRO:
                operando = reconhecerNumeroInteiro();
                break;
            case NUMERO_REAL:
                operando = reconhecerNumeroReal();
                break;
            default:
                operando = reconhecerCadeiaCaracteres();
        }

        return new NoComandoEscrever(operando, numeroLinhaComandoEscrever);
    }

    private NoComandoCondicao reconhecerComandoCondicao(){
        int numeroLinhaComandoCondicao = analisadorLexico.obterNumeroLinha();
        
        this.reconhecerToken(Token.SE);
        this.reconhecerToken(Token.ABRE_PARENTESES);
        
        NoExpressaoRelacional expressao = this.reconhecerExpressaoRelacional();
        
        this.reconhecerToken(Token.FECHA_PARENTESES);
        this.reconhecerToken(Token.ENTAO);
        
        NoBlocoComandos blocoComandos = this.reconhecerBlocoComandos();
        NoComando comandoSenao = null;
        
        if (token_a_frente == Token.SENAO) {
            this.reconhecerToken(Token.SENAO);
            
            if (token_a_frente == Token.INICIO) {
                comandoSenao = this.reconhecerBlocoComandos();
            } else if (token_a_frente == Token.SE) {
                comandoSenao = this.reconhecerComandoCondicao();
            } else {
                // TRATAR O ERRO
            }
        }
        
        this.reconhecerToken(Token.FIM_COMANDO);
        
        return new NoComandoCondicao(expressao, blocoComandos, comandoSenao,
            numeroLinhaComandoCondicao);
    }

    private NoComandoEnquantoFaca reconhecerComandoEnquantoFaca(){
        int numeroLinhaComandoEnquantoFaca = analisadorLexico.obterNumeroLinha();
        
        reconhecerToken(Token.ENQUANTO);
        reconhecerToken(Token.ABRE_PARENTESES);
        NoExpressaoRelacional expressaoRelacional = reconhecerExpressaoRelacional();
        reconhecerToken(Token.FECHA_PARENTESES);
        reconhecerToken(Token.FACA);
        NoBlocoComandos blocoComandos = reconhecerBlocoComandos();
        reconhecerToken(Token.FIM_COMANDO);

        return new NoComandoEnquantoFaca(expressaoRelacional, blocoComandos, numeroLinhaComandoEnquantoFaca);
    }

    private NoComandoDeAte reconhecerComandoDeAteFaca(){
        int numeroLinhaComandoDeAte = analisadorLexico.obterNumeroLinha();

        reconhecerToken(Token.DE);
        NoIdentificador identificador = reconhecerIdentificador();
        reconhecerToken(Token.ATRIBUICAO);
        NoNumeroInteiro posicaoInicial = reconhecerNumeroInteiro();
        reconhecerToken(Token.ATE);
        NoNumeroInteiro posicaoFinal = reconhecerNumeroInteiro();
        reconhecerToken(Token.FACA);
        NoBlocoComandos blocoComandos = reconhecerBlocoComandos();
        reconhecerToken(Token.FIM_COMANDO);
        
        return new NoComandoDeAte(identificador, posicaoInicial, posicaoFinal, blocoComandos, numeroLinhaComandoDeAte);
    }

    private NoComandoAtribuicao reconhecerComandoAtribuicao(){
        int numeroLinhaAtribuicao = analisadorLexico.obterNumeroLinha();
        NoIdentificador identificador = new NoIdentificador(analisadorLexico.obterDadosIdentificador(), numeroLinhaAtribuicao);
        
        if (token_a_frente != Token.ATRIBUICAO) {
            identificador = reconhecerIdentificador();
        } else {
            this.reconhecerToken(Token.ATRIBUICAO);
        }
        
        NoExpressao expressao = null;
        
        switch (token_a_frente) {
            case IDENTIFICADOR:
                expressao = reconhecerIdentificador();
                break;
            case NUMERO_INTEIRO:
                expressao = reconhecerNumeroInteiro();
                break;
            case NUMERO_REAL:
                expressao = reconhecerNumeroReal();
                break;
            case ADICAO:
            case SUBTRACAO:
                expressao = reconhecerExpressaoAritmetica();
                break;
            case ATRIBUICAO:
                expressao = reconhecerComandoAtribuicao().obterExpressao();
                break;
            case ESCREVER:
                break;
        }
        
        if (token_a_frente == Token.FIM_COMANDO) {
            this.reconhecerToken(Token.FIM_COMANDO);
        } else if (token_a_frente == Token.ADICAO) {
            this.reconhecerToken(Token.ADICAO);
        } else  if (token_a_frente == Token.ATRIBUICAO) {
            this.reconhecerToken(Token.ATRIBUICAO);
        }
        
        return new NoComandoAtribuicao(identificador, expressao, numeroLinhaAtribuicao);
    }
}
