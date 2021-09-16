/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package portugol.compilador;

import portugol.arvoresintatica.NoComando;
import portugol.arvoresintatica.NoComandoAtribuicao;
import portugol.arvoresintatica.NoComandoCondicao;
import portugol.arvoresintatica.NoComandoDeAte;
import portugol.arvoresintatica.NoComandoEnquantoFaca;
import java.util.Iterator;
import javax.swing.JOptionPane;

import portugol.arvoresintatica.NoExpressao;
import portugol.arvoresintatica.NoExpressaoAritmetica;
import portugol.arvoresintatica.NoExpressaoRelacional;
import portugol.arvoresintatica.NoBlocoComandos;
import portugol.arvoresintatica.NomeComando;
import portugol.arvoresintatica.TipoExpressao;
import portugol.arvoresintatica.TipoValor;
import portugol.lexico.DadosIdentificador;
/**
 *
 * @author Kennedy
 */
public class AnalisadorSemantico {

    private TabelaSimbolos tabelaSimbolos;

    private TratadorErro tratadorErro;

    public AnalisadorSemantico(TabelaSimbolos tabelaSimbolos, TratadorErro tratadorErro){
        this.tabelaSimbolos = tabelaSimbolos;
        this.tratadorErro = tratadorErro;
    }

    public void verificarIdentificadores() throws Exception {
        Iterator<DadosIdentificador> itIdentificadores = tabelaSimbolos.obterDadosIdentificadores().iterator();
        while (itIdentificadores.hasNext()){
            DadosIdentificador identificador = itIdentificadores.next();
            if (identificador.obterTipo() == TipoValor.INDEFINIDO){
               tratadorErro.emitirErroSemanticoTipoIndefinido(identificador);
            }
        }
    }

    private TipoValor obterTipoExpressao(NoExpressao expressao) throws Exception {
        if ((expressao.obterTipo() == TipoExpressao.NUMERO_INTEIRO)||
            (expressao.obterTipo() == TipoExpressao.NUMERO_REAL) ||
            (expressao.obterTipo() == TipoExpressao.CADEIA_CARACTERES) ||
            (expressao.obterTipo() == TipoExpressao.IDENTIFICADOR)){
            return expressao.obterTipoValorExpressao();
        } else if (expressao.obterTipo() == TipoExpressao.EXPRESSAO_ARITMETICA){
            TipoValor tipoOperandoEsquerdo = obterTipoExpressao(((NoExpressaoAritmetica)expressao).obterOperandoEsquerdo());
            TipoValor tipoOperandoDireito = obterTipoExpressao(((NoExpressaoAritmetica)expressao).obterOperandoDireito());
            if (tipoOperandoEsquerdo != TipoValor.CADEIA_CARACTERES && tipoOperandoDireito != TipoValor.CADEIA_CARACTERES){

                if (tipoOperandoEsquerdo == TipoValor.REAL || tipoOperandoDireito == TipoValor.REAL){
                   return TipoValor.REAL;
               } else
                   return TipoValor.INTEIRO;

           } else {
               tratadorErro.emitirErroExpressaoTipoTexoNaoPermitida(expressao.obterNumeroLinha());
                //gerar erro aqui
           }
        }
        return TipoValor.INDEFINIDO;
    }

    private void verificarTipoExpressaoRelacional(NoExpressaoRelacional expressaoRelacional) throws Exception {
        TipoValor tipoExpressaoAritmeticaEsquerda = 
                 obterTipoExpressao(expressaoRelacional.obterOperandoEsquerdo());
        
        TipoValor tipoExpressaoAritmeticaDireita = 
                obterTipoExpressao (expressaoRelacional.obterOperandoDireito());
        
        if ((tipoExpressaoAritmeticaEsquerda == TipoValor.CADEIA_CARACTERES) ||
            (tipoExpressaoAritmeticaDireita  == TipoValor.CADEIA_CARACTERES)){
            tratadorErro.emitirErroExpressaoTipoTexoNaoPermitida(expressaoRelacional.obterNumeroLinha());
        }
    }

    private void verificarTipoComandoCondicao(NoComandoCondicao comandoCondicao) throws Exception {
       verificarTipoExpressaoRelacional(comandoCondicao.obterExpressaoRelacional());
       verificarTipoComandos(comandoCondicao.obterBlocoComandos());
       if (comandoCondicao.obterComandoSenao() != null){
            if (comandoCondicao.obterComandoSenao().obterNome() == NomeComando.CONDICAO){
                verificarTipoComandoCondicao((NoComandoCondicao)comandoCondicao.obterComandoSenao());
            } else {
                verificarTipoComandos((NoBlocoComandos)comandoCondicao.obterComandoSenao());
            }
       }
    }

    private void verificarComandoDeAte(NoComandoDeAte comandoDeAte) throws Exception {
       int valorInicial = comandoDeAte.obterLimiteInicial().obterValor();
       int valorFinal = comandoDeAte.obterLimiteFinal().obterValor();
       if (valorFinal < valorInicial){
            tratadorErro.emitirErroLimitesComandoDeAte(comandoDeAte);
       } else {
           verificarTipoComandos(comandoDeAte.obterBlocoComandos());
       }
       
    }

    private void verificarComandoEnquantoFaca(NoComandoEnquantoFaca comandoEnquantoFaca) throws Exception{
       verificarTipoExpressaoRelacional(comandoEnquantoFaca.obterExpressaoRelacional());
       verificarTipoComandos(comandoEnquantoFaca.obterListaComandos());
    }

    private void verificarComandoAtribuicao(NoComandoAtribuicao comandoAtribuicao) throws Exception {
        TipoValor tipoIdentificador = 
                comandoAtribuicao.obterIdentificador().obterTipoValorExpressao();
        
        TipoValor tipoExpressao = 
                obterTipoExpressao(comandoAtribuicao.obterExpressao());
        
        if (tipoIdentificador != tipoExpressao) {
            /* A única diferença permitida é o identificador ser real e
             * a expressão gerar um resultado inteiro.
             */
            if (tipoIdentificador == TipoValor.REAL) {
               
                if (tipoExpressao != TipoValor.INTEIRO) {
                   tratadorErro.emitirErroSemanticoTipoValorEsperado(tipoIdentificador,
                                                                     tipoExpressao,
                                                                     comandoAtribuicao.obterNumeroLinha());
               }

           } else {
                   tratadorErro.emitirErroSemanticoTipoValorEsperado(tipoIdentificador,
                                                                     tipoExpressao,
                                                                     comandoAtribuicao.obterNumeroLinha());
           }
        }
    }

    public void verificarTipoComandos(NoBlocoComandos listaComandos) throws Exception {
        Iterator it = listaComandos.obterOperacoes().iterator();
        while (it.hasNext()){
            NoComando operacao = (NoComando)it.next();
            switch(operacao.obterNome()){
                case LACO_DE_ATE:
                    verificarComandoDeAte((NoComandoDeAte)operacao);
                    break;
                case LACO_ENQUANTO:
                    verificarComandoEnquantoFaca((NoComandoEnquantoFaca)operacao);
                    break;
                case BLOCO_COMANDOS:
                    verificarTipoComandos((NoBlocoComandos)operacao);
                    break;
                case ATRIBUICAO:
                    verificarComandoAtribuicao((NoComandoAtribuicao)operacao);
                    break;
                case CONDICAO:
                    verificarTipoComandoCondicao((NoComandoCondicao)operacao);
            }
        }
    }

    public void executar(NoBlocoComandos listaComandos){
        try{
            verificarIdentificadores();
            verificarTipoComandos(listaComandos);
        } catch(Exception e){
            JOptionPane.showMessageDialog(null, e.toString());
        }
    }
}

