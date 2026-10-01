package br.ufg.pas.estoque.repository;

import br.ufg.pas.estoque.model.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    /**
     * Decremento atômico: a verificação de saldo e a baixa acontecem no mesmo
     * comando SQL. Assim, mesmo com várias instâncias do serviço ou requisições
     * concorrentes, o estoque nunca fica negativo.
     *
     * @return 1 se reservou, 0 se não havia saldo suficiente
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Produto p SET p.quantidade = p.quantidade - :quantidade "
            + "WHERE p.id = :id AND p.quantidade >= :quantidade")
    int reservar(@Param("id") Long id, @Param("quantidade") int quantidade);

    /** Operação de compensação (Saga): devolve ao estoque uma reserva desfeita. */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Produto p SET p.quantidade = p.quantidade + :quantidade WHERE p.id = :id")
    int liberar(@Param("id") Long id, @Param("quantidade") int quantidade);
}
