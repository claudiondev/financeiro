    package com.claudio.financeiro.repository;

    import com.claudio.financeiro.model.Gasto;
    import org.springframework.data.jpa.repository.JpaRepository;
    import org.springframework.data.jpa.repository.Query;
    import org.springframework.data.repository.query.Param;

    import java.util.List;

    public interface GastoRepository extends JpaRepository<Gasto, Long> {

        // Este método é essencial para filtrar os gastos pelo dono da conta
        List<Gasto> findByUsuarioId(Long usuarioId);

        @Query("SELECT g FROM Gasto g WHERE g.usuario.id = :usuarioId " +
                "AND (:categoria IS NULL OR g.categoria = :categoria) " +
                "AND (:mes IS NULL OR MONTH(g.data) = :mes) " +
                "AND (:ano IS NULL OR YEAR(g.data) = :ano)")
        List<Gasto> findByFiltros(
                @Param("usuarioId") Long usuarioId,
                @Param("categoria") String categoria,
                @Param("mes") Integer mes,
                @Param("ano") Integer ano
        );
    }