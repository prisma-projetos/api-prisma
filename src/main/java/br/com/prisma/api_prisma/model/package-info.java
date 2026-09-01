/**
 * Entidades JPA que espelham o schema do banco do pipeline.
 *
 * <p><b>Somente leitura.</b> Nenhuma classe aqui tem setter, e isso e
 * deliberado: quem escreve neste banco e o pipeline (scraper, matching de
 * entidades e classificador). Esta API apenas consulta. O Hibernate popula os
 * objetos por acesso direto aos campos -- as anotacoes de mapeamento estao nos
 * atributos, nao nos getters --, entao a ausencia de setters nao atrapalha a
 * hidratacao e impede que a aplicacao altere um estado que nao lhe pertence.
 *
 * <p>Os nomes das tabelas e colunas seguem o banco existente, em portugues e
 * com a nomenclatura do TSE ({@code nr_titulo_eleitoral_candidato},
 * {@code sq_candidato}), em vez de serem traduzidos: renomear aqui obrigaria
 * quem lê o codigo a manter duas nomenclaturas na cabeca ao investigar uma
 * consulta no banco.
 *
 * <p>Uma coluna do schema real fica de fora de proposito: {@code nr_cpf_candidato}
 * e {@code ds_email} nao sao mapeados, para que nao possam vazar por uma
 * resposta da API por descuido.
 */
package br.com.prisma.api_prisma.model;
