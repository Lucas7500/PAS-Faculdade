package br.ufg.pas.pedido.client;

import br.ufg.pas.pedido.exception.EstoqueIndisponivelException;
import br.ufg.pas.pedido.exception.EstoqueInsuficienteException;
import br.ufg.pas.pedido.exception.ProdutoInexistenteException;
import br.ufg.pas.pedido.observability.CorrelationIdFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Comunicação síncrona (REST) com o Estoque Service. O Pedido Service nunca
 * acessa o banco do Estoque: usa apenas a interface HTTP publicada por ele.
 */
@Component
public class EstoqueClient {

    private final RestTemplate restTemplate;
    private final String estoqueUrl;

    public EstoqueClient(RestTemplate restTemplate, @Value("${estoque.service.url}") String estoqueUrl) {
        this.restTemplate = restTemplate;
        this.estoqueUrl = estoqueUrl;
    }

    public void reservar(Long produtoId, int quantidade, String correlationId) {
        enviar(estoqueUrl + "/produtos/" + produtoId + "/reservar", quantidade, correlationId);
    }

    /** Compensação da Saga: desfaz uma reserva já realizada. */
    public void liberar(Long produtoId, int quantidade, String correlationId) {
        enviar(estoqueUrl + "/produtos/" + produtoId + "/liberar", quantidade, correlationId);
    }

    private void enviar(String url, int quantidade, String correlationId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(CorrelationIdFilter.HEADER, correlationId);
        HttpEntity<Map<String, Integer>> entity = new HttpEntity<>(Map.of("quantidade", quantidade), headers);

        try {
            restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode() == HttpStatus.CONFLICT) {
                throw new EstoqueInsuficienteException();
            }
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ProdutoInexistenteException();
            }
            throw new EstoqueIndisponivelException("Estoque Service respondeu " + e.getStatusCode(), e);
        } catch (ResourceAccessException e) {
            throw new EstoqueIndisponivelException("Estoque Service inacessível", e);
        }
    }
}
