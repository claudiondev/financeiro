package com.claudio.financeiro.service;

import com.claudio.financeiro.model.CategoriaGasto;

import java.util.Map;

/** Catálogo estático de dicas educacionais, uma por categoria de gasto. */
final class DicasEducacionaisCatalogo {

    private DicasEducacionaisCatalogo() {}

    private static final Map<CategoriaGasto, String> DICAS = Map.of(
            CategoriaGasto.ALIMENTACAO,
            "Planejar as refeições da semana e fazer uma lista antes de ir ao mercado costuma reduzir gastos por impulso.",
            CategoriaGasto.TRANSPORTE,
            "Compare o preço do combustível em apps como Preço da Hora antes de abastecer — a diferença entre postos costuma passar de 10%.",
            CategoriaGasto.MORADIA,
            "Revisar o plano de internet/TV a cada 12 meses pode revelar ofertas melhores que a operadora não oferece automaticamente pra quem já é cliente.",
            CategoriaGasto.LAZER,
            "Assinaturas de streaming que você não usa há mais de um mês são um bom ponto de partida pra cortar gasto recorrente sem sentir falta.",
            CategoriaGasto.SAUDE,
            "Farmácias populares e a versão genérica de medicamentos costumam custar bem menos que a referência, com o mesmo princípio ativo.",
            CategoriaGasto.EDUCACAO,
            "Antes de comprar um curso, vale checar se ele já não está incluso em alguma assinatura que você já paga.",
            CategoriaGasto.OUTROS,
            "Gastos que caem sempre em 'Outros' costumam esconder um padrão — vale revisar se merecem uma categoria própria pra facilitar o controle."
    );

    static String obterDica(CategoriaGasto categoria) {
        return DICAS.get(categoria);
    }
}
