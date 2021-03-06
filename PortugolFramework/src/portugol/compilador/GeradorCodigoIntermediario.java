/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package portugol.compilador;

import portugol.arvoresintatica.NoBlocoComandos;
import java.util.ArrayList;
import java.util.Iterator;
import portugol.arvoresintatica.NoComando;
import portugol.arvoresintatica.NoComandoAtribuicao;
import portugol.arvoresintatica.NoComandoCondicao;
import portugol.arvoresintatica.NoComandoDeAte;
import portugol.arvoresintatica.NoComandoEnquantoFaca;
import portugol.arvoresintatica.NoComandoEscrever;
import portugol.arvoresintatica.NoComandoLer;
import portugol.arvoresintatica.NoExpressao;
import portugol.arvoresintatica.NoExpressaoAritmetica;
import portugol.arvoresintatica.NoExpressaoRelacional;
import portugol.arvoresintatica.NoIdentificador;
import portugol.arvoresintatica.NoNumeroInteiro;
import portugol.arvoresintatica.NomeComando;
import portugol.arvoresintatica.TipoOperacaoAritmetica;
import portugol.intermediario.Instrucao;
import portugol.intermediario.InstrucaoEscrever;
import portugol.intermediario.InstrucaoIrPara;
import portugol.intermediario.InstrucaoLer;
import portugol.intermediario.InstrucaoAritmetica;
import portugol.intermediario.InstrucaoAtribuir;
import portugol.intermediario.InstrucaoRelacional;
import portugol.intermediario.InstrucaoSeFalso;
import portugol.intermediario.Rotulo;
import portugol.lexico.DadosIdentificador;
import portugol.arvoresintatica.TipoRelacao;

/**
 *
 * @author Kennedy
 */
public class GeradorCodigoIntermediario {

    private ArrayList<Instrucao> instrucoes;
    private int numeroRotulo = 0;
    private int numeroVariavelTemporaria = 0;


    public GeradorCodigoIntermediario(){
        instrucoes = new ArrayList<Instrucao>();
    }

    public  ArrayList<Instrucao> executar (NoBlocoComandos listaComandos) throws Exception {
        traduzirComandos(listaComandos);
        return instrucoes;
    }

    private Rotulo criarRotulo(){
        numeroRotulo++;
        return new Rotulo(numeroRotulo);
    }

    private NoIdentificador criarVariavelTemporaria(){
        numeroVariavelTemporaria++;
        DadosIdentificador dadosIdentificador = new DadosIdentificador("temp_"+numeroVariavelTemporaria);

        return new NoIdentificador(dadosIdentificador, 0);
    }

    public void traduzirComandos(NoBlocoComandos listaComandos) throws Exception {
        Iterator it = listaComandos.obterOperacoes().iterator();
        while (it.hasNext()){
            NoComando comando = (NoComando)it.next();
            switch(comando.obterNome()){
               case LER:
                    traduzirComandoLer((NoComandoLer)comando);
                    break;
               case ESCREVER:
                    traduzirComandoEscrever((NoComandoEscrever)comando);
                    break;
                case LACO_DE_ATE:
                    traduzirComandoDeAte((NoComandoDeAte)comando);
                    break;
                case LACO_ENQUANTO:
                    traduzirComandoEnquantoFaca((NoComandoEnquantoFaca)comando);
                    break;
                case BLOCO_COMANDOS:
                    traduzirComandos((NoBlocoComandos)comando);
                    break;
                case ATRIBUICAO:
                    traduzirComandoAtribuicao((NoComandoAtribuicao)comando);
                    break;
                case CONDICAO:
                    traduzirComandoCondicao((NoComandoCondicao)comando);
            }
        }
    }
    
    private NoExpressao traduzirExpressao(NoExpressao operando){
        
        switch (operando.obterTipo()){
            case NUMERO_INTEIRO:
            case NUMERO_REAL:
            case IDENTIFICADOR:
                return operando;
            default:
                return traduzirExpressaoAritmetica((NoExpressaoAritmetica)operando);
        }
    }

    private NoIdentificador traduzirExpressaoAritmetica(NoExpressaoAritmetica expressaoAritmetica){
        
        NoExpressao operandoEsquerdo = traduzirExpressao(expressaoAritmetica.obterOperandoEsquerdo());
        NoExpressao operandoDireito  = traduzirExpressao(expressaoAritmetica.obterOperandoDireito());

        /* A tradu????o de uma express??o aritm??tica para a forma de tr??s endere??os
         * requer a cria????o de uma vari??vel tempor??ria (representada pela
         * classe NoIdentificador) para armazenar o resultado da opera????o entre
         * dois operadores. A vari??vel criada ?? retornada, de forma que
         * possa ser usada em outra express??o aritm??tica de tr??s endere??os.
         */ 
        NoIdentificador identificador = criarVariavelTemporaria();

        InstrucaoAritmetica instrucaoAritmetica = new InstrucaoAritmetica(expressaoAritmetica.obterCodigoOperacao(),
                                                                          identificador,
                                                                          operandoEsquerdo,
                                                                          operandoDireito);
        instrucoes.add(instrucaoAritmetica);

        return identificador;
    }

    private NoExpressao traduzirExpressaoRelacional(NoExpressaoRelacional expressaoRelacional) throws Exception {
        
        NoExpressao operandoEsquerdo = traduzirExpressao(expressaoRelacional.obterOperandoEsquerdo());
        NoExpressao operandoDireito = traduzirExpressao(expressaoRelacional.obterOperandoDireito());

        NoIdentificador operandoResultado = criarVariavelTemporaria();

        InstrucaoRelacional instrucaoRelacional = new InstrucaoRelacional(expressaoRelacional.obterRelacao(),
                                                                          operandoResultado,
                                                                          operandoEsquerdo,
                                                                          operandoDireito);

        instrucoes.add(instrucaoRelacional);
        return operandoResultado;
    }

    //M??todos para completar ---------------------------------------------------
    private void traduzirComandoLer(NoComandoLer comandoLer){
        instrucoes.add(new InstrucaoLer(comandoLer.obterIdentificador()));
    }

    private void traduzirComandoEscrever(NoComandoEscrever comandoEscrever){
        instrucoes.add(new InstrucaoEscrever(comandoEscrever.obterOperando()));
    }

    
    private void traduzirComandoCondicao(NoComandoCondicao comandoCondicao) throws Exception {
        Rotulo saidaSe = this.criarRotulo();
        NoExpressao opRel = this.traduzirExpressaoRelacional(comandoCondicao.obterExpressaoRelacional());
        InstrucaoSeFalso instrucaoSeFalso = new InstrucaoSeFalso(opRel, saidaSe);

        instrucoes.add(instrucaoSeFalso);
        
        traduzirComandos(comandoCondicao.obterBlocoComandos());

        if(comandoCondicao.obterComandoSenao() != null) {
            instrucoes.add(saidaSe);

            NoComando senao = comandoCondicao.obterComandoSenao();

            if(senao instanceof NoBlocoComandos) {
                traduzirComandos((NoBlocoComandos) senao);
            } else {
                Rotulo saidaSeNao = this.criarRotulo();
                NoComandoCondicao senaoComandoCondicao = (NoComandoCondicao) senao;
                NoExpressao opRelSeNao = this.traduzirExpressaoRelacional(senaoComandoCondicao.obterExpressaoRelacional());
                InstrucaoSeFalso instrucaoSeFalsoSeNao = new InstrucaoSeFalso(opRelSeNao, saidaSeNao);
                instrucoes.add(instrucaoSeFalsoSeNao);
                traduzirComandos((senaoComandoCondicao).obterBlocoComandos());
                instrucoes.add(saidaSeNao);
            }
        }
    }

    private void traduzirComandoEnquantoFaca(NoComandoEnquantoFaca comandoEnquantoFaca) throws Exception{
        Rotulo retorno = this.criarRotulo();
        Rotulo saida = this.criarRotulo();
        
        instrucoes.add(retorno);
        
        NoExpressao noExpressao = this.traduzirExpressaoRelacional(comandoEnquantoFaca.obterExpressaoRelacional());
        InstrucaoSeFalso instrucaoSeFalso = new InstrucaoSeFalso(noExpressao, saida);
        
        instrucoes.add(instrucaoSeFalso);
        this.traduzirComandos(comandoEnquantoFaca.obterListaComandos());
        
        InstrucaoIrPara instrucaoIrPara = new InstrucaoIrPara(retorno);
        
        instrucoes.add(instrucaoIrPara);
        instrucoes.add(saida);
    }

    private void traduzirComandoDeAte(NoComandoDeAte comandoDeAte) throws Exception {
        NoExpressao incremento = new NoNumeroInteiro(1, 0);
        NoIdentificador tempRel = this.criarVariavelTemporaria();
        Rotulo retorno = this.criarRotulo();
        Rotulo saida = this.criarRotulo();

        InstrucaoAtribuir instrucaoAtribuir = new InstrucaoAtribuir(comandoDeAte.obterLimiteInicial(), comandoDeAte.obterIdentificador());
        instrucoes.add(instrucaoAtribuir);
        instrucoes.add(retorno);
        
        InstrucaoRelacional instrucaoRelacional = new InstrucaoRelacional(TipoRelacao.MENOR_IGUAL, tempRel, comandoDeAte.obterIdentificador(), comandoDeAte.obterLimiteFinal());
        instrucoes.add(instrucaoRelacional);
        instrucoes.add(new InstrucaoSeFalso(tempRel, saida));

        this.traduzirComandos(comandoDeAte.obterBlocoComandos());

        InstrucaoAritmetica instrucaoAritmetica = new InstrucaoAritmetica(TipoOperacaoAritmetica.ADICAO, comandoDeAte.obterIdentificador(), comandoDeAte.obterIdentificador(), incremento);
        instrucoes.add(instrucaoAritmetica);
        instrucoes.add(new InstrucaoIrPara(retorno));
        instrucoes.add(saida);
    }

    private void traduzirComandoAtribuicao(NoComandoAtribuicao comandoAtribuicao) throws Exception {
        if(comandoAtribuicao.obterExpressao() instanceof NoExpressaoAritmetica) {
            NoIdentificador noId = traduzirExpressaoAritmetica((NoExpressaoAritmetica) comandoAtribuicao.obterExpressao());
            instrucoes.add(new InstrucaoAtribuir(noId, comandoAtribuicao.obterIdentificador()));
        } else {
            instrucoes.add(new InstrucaoAtribuir(comandoAtribuicao.obterExpressao(), comandoAtribuicao.obterIdentificador()));
        }
    }
}
