package portugol.compilador;

import portugol.lexico.Token;
import portugol.lexico.DadosIdentificador;
import portugol.lexico.Lexema;

/**
 *
 * @author Kennedy
 */
public class AnalisadorLexico {

    private char[] vetorCodigo;
    private int posicaoInicial = 0;
    private int posicaoFinal = 0;
    private int numeroLinha = 1;
    private String lexema;

    private TabelaSimbolos tabelaSimbolos;

    public AnalisadorLexico(TabelaSimbolos tabelaSimbolos){
        this.tabelaSimbolos = tabelaSimbolos;
        this.lexema = "";
    }

    public String obterLexema(){
        return lexema;
    }


    public DadosIdentificador obterDadosIdentificador(){
        return (DadosIdentificador)tabelaSimbolos.obterDadosToken(lexema);
    }
    public void definirCodigo(String codigo){
        /*
         * O simbolo $ esta sendo usado como indicador de final de codigo. Esta
         * tecnica facilita a varredura do vetor de caracteres.
         */
        vetorCodigo = codigo.concat("$").toCharArray();
        posicaoInicial = 0;
        posicaoFinal = 0;
        numeroLinha = 1;
        tabelaSimbolos.iniciar();

    }

    public int obterNumeroLinha(){
        return numeroLinha;
    }

    public int obterNumeroColuna(){
        return posicaoInicial;
    }

    public boolean temCodigo(){
        return (vetorCodigo[posicaoFinal] != '$');
    }

    /**
     * O método saltarEspacos avança a posicaoFinal para a próxima posição
     * onde existe um caractere considerado como válido pela gramática. É
     * importante notar que quando um caractere de salto de linha é encontrado
     * (\n), o atributo numeroLinha é incrementado.
     */
    public void saltarEspacos(){
       boolean caractereEspaco = (vetorCodigo[posicaoFinal] == ' ') || (vetorCodigo[posicaoFinal] == '\n');
       while ((posicaoFinal < vetorCodigo.length) && caractereEspaco){

           if (vetorCodigo[posicaoFinal]== '\n'){
                   numeroLinha++;
           }

           posicaoFinal++;
           caractereEspaco = (vetorCodigo[posicaoFinal] == ' ') || (vetorCodigo[posicaoFinal] == '\n');
        }
    }

    public Token obterToken(){
        int estado = 0;
        lexema = "";

        saltarEspacos();

        posicaoInicial = posicaoFinal;

        while (vetorCodigo[posicaoFinal]!= '$'){
            switch (estado){
                case 0:
                    if (Character.isLetter(vetorCodigo[posicaoFinal])) {
                        estado = 1;
                    } else if (vetorCodigo[posicaoFinal] == '<') {
                        estado = 7;
                    } else if (vetorCodigo[posicaoFinal] == '>') {
                        estado = 11;
                    } else if (vetorCodigo[posicaoFinal] == '=') {
                        estado = 14;
                    } else if (vetorCodigo[posicaoFinal] == ';') {
                        estado = 15;
                    } else if (vetorCodigo[posicaoFinal] == ':') {
                        estado = 16;
                    } else if (vetorCodigo[posicaoFinal] == '(') {
                        estado = 19;
                    } else if (vetorCodigo[posicaoFinal] == ')') {
                        estado = 20;
                    } else if (vetorCodigo[posicaoFinal] == '"') {
                        estado = 21;
                    } else if (vetorCodigo[posicaoFinal] == '+') {
                        estado = 23;
                    } else if (vetorCodigo[posicaoFinal] == '-') {
                        estado = 24;
                    } else if (vetorCodigo[posicaoFinal] == '*') {
                        estado = 25;
                    } else if (vetorCodigo[posicaoFinal] == '/') {
                        estado = 26;
                    }
                    
                    break;
                case 1:
                    if (!Character.isLetterOrDigit(vetorCodigo[posicaoFinal])) {
                        estado = 2;
                    }
                    
                    break;
                case 2:
                    lexema = String.copyValueOf(vetorCodigo, posicaoInicial, posicaoFinal - posicaoInicial - 1);
                    
                    return tabelaSimbolos.obterToken(lexema);
                case 3:
                    if (!Character.isDigit(vetorCodigo[posicaoFinal])) {
                        if (vetorCodigo[posicaoFinal] == '.'){
                            estado = 4;
                        } else {
                            estado = 6;
                        }
                    }
                    
                    break;
                case 4:
                    if (!Character.isDigit(vetorCodigo[posicaoFinal])) {
                        estado = 5;
                    }
                    
                    break;
                case 5:
                    return Token.NUMERO_REAL;
                case 6:
                    return Token.NUMERO_INTEIRO;
                case 7:
                    if (vetorCodigo[posicaoFinal] == '=') {
                        estado = 8;
                    } else if (vetorCodigo[posicaoFinal] == '>') {
                        estado = 9;
                    } else {
                        estado = 10;
                    }
                    
                    break;
                case 8:
                    lexema = Lexema.RELACAO_MENOR_IGUAL;
                    return tabelaSimbolos.obterToken(lexema);
                case 9:
                    lexema = Lexema.RELACAO_DIFERENTE;
                    return tabelaSimbolos.obterToken(lexema);
                case 10:
                    lexema = Lexema.RELACAO_MENOR;
                    return tabelaSimbolos.obterToken(lexema);
                case 11:
                    if (vetorCodigo[posicaoFinal] == '=') {
                        estado = 12;
                    } else {
                        estado = 13;
                    }       
                    
                    break;
                case 12:
                    lexema = Lexema.RELACAO_MAIOR_IGUAL;
                    return tabelaSimbolos.obterToken(lexema);
                case 13:
                    lexema = Lexema.RELACAO_MAIOR;
                    return tabelaSimbolos.obterToken(lexema);
                case 14:
                    lexema = Lexema.RELACAO_IGUAL;
                    return tabelaSimbolos.obterToken(lexema);
                case 15:
                    lexema = Lexema.FIM_COMANDO;
                    return tabelaSimbolos.obterToken(lexema);
                case 16:
                    if (vetorCodigo[posicaoFinal] == '=') {
                        estado = 17;
                    } else {
                        estado = 18;
                    }
                    
                    break;
                case 17:
                    return Token.ATRIBUICAO;
                case 18:
                    return Token.DOIS_PONTOS;
                case 19:
                    return Token.ABRE_PARENTESES;
                case 20:
                    return Token.FECHA_PARENTESES;
                case 21:
                    if (vetorCodigo[posicaoFinal] == '"') {
                        estado = 22;
                    }
                    
                    break;
                case 22:
                    return Token.CADEIA_CARACTERES;
                case 23:
                    return Token.ADICAO;
                case 24:
                    return Token.SUBTRACAO;
                case 25:
                    return Token.MULTIPLICACAO;
                case 26:
                    return Token.DIVISAO;
            }
            
        posicaoFinal++;
        }

        return null;
    }
}
