        package com.claudio.financeiro.dto;

        import lombok.AllArgsConstructor;
        import lombok.Data;
        import lombok.NoArgsConstructor;

        import java.time.LocalDate;

        @Data
        @NoArgsConstructor@AllArgsConstructor

        public class GastoDTO {
            private Long id;
            private String descricao;
            private Double valor;
            private String categoria;
            private LocalDate data;
            private Long usuarioId;

        }