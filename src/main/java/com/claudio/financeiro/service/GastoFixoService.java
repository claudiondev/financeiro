package com.claudio.financeiro.service;

import com.claudio.financeiro.dto.CriarGastoFixoRequest;
import com.claudio.financeiro.dto.GastoFixoDTO;
import com.claudio.financeiro.model.Gasto;
import com.claudio.financeiro.model.GastoFixo;
import com.claudio.financeiro.model.StatusGastoFixo;
import com.claudio.financeiro.model.Usuario;
import com.claudio.financeiro.repository.GastoFixoRepository;
import com.claudio.financeiro.repository.GastoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GastoFixoService {

    private static final Logger log = LoggerFactory.getLogger(GastoFixoService.class);

    // Mesmo limiar usado no status (VENCENDO) e no disparo do alerta/e-mail — um só lugar,
    // sem número mágico duplicado (ver spec "Fora de escopo"/"Lembrete de pagamento").
    private static final int DIAS_LIMIAR_ALERTA = 3;

    private final GastoFixoRepository gastoFixoRepository;
    private final GastoRepository gastoRepository;
    private final JavaMailSender mailSender;

    // Desligado até o .env ter credenciais de e-mail reais (ver application.properties).
    // O aviso dentro do app não depende disso — só o e-mail em si fica pausado.
    @Value("${app.email.lembrete-habilitado:false}")
    private boolean lembretePorEmailHabilitado;

    public GastoFixoService(GastoFixoRepository gastoFixoRepository, GastoRepository gastoRepository,
                            JavaMailSender mailSender) {
        this.gastoFixoRepository = gastoFixoRepository;
        this.gastoRepository = gastoRepository;
        this.mailSender = mailSender;
    }

    public GastoFixoDTO criar(CriarGastoFixoRequest request, Usuario usuarioLogado) {
        GastoFixo fixo = paraEntidade(request, usuarioLogado);
        return toDTOComStatus(gastoFixoRepository.save(fixo), LocalDate.now());
    }

    public GastoFixoDTO atualizar(Long id, CriarGastoFixoRequest request, Long usuarioId) {
        GastoFixo fixo = buscarComOwnership(id, usuarioId);
        fixo.setCategoria(request.getCategoria());
        fixo.setValor(request.getValor());
        fixo.setDescricao(request.getDescricao());
        fixo.setDiaVencimento(request.getDiaVencimento());
        fixo.setDataInicio(request.getDataInicio());
        return toDTOComStatus(gastoFixoRepository.save(fixo), LocalDate.now());
    }

    // Os gastos já gerados a partir desse molde permanecem (histórico não é apagado) —
    // a FK gasto_fixo_id fica NULL neles (ON DELETE SET NULL, migração V7).
    public void deletar(Long id, Long usuarioId) {
        buscarComOwnership(id, usuarioId);
        gastoFixoRepository.deleteById(id);
    }

    // Pausar preserva o molde (diferente de deletar) — para de gerar gasto novo todo mês,
    // mas pode ser reativado depois sem perder categoria/valor/dia de vencimento configurados.
    public GastoFixoDTO pausar(Long id, Long usuarioId) {
        GastoFixo fixo = buscarComOwnership(id, usuarioId);
        fixo.setAtivo(false);
        return toDTOComStatus(gastoFixoRepository.save(fixo), LocalDate.now());
    }

    public GastoFixoDTO reativar(Long id, Long usuarioId) {
        GastoFixo fixo = buscarComOwnership(id, usuarioId);
        fixo.setAtivo(true);
        return toDTOComStatus(gastoFixoRepository.save(fixo), LocalDate.now());
    }

    public List<GastoFixoDTO> listarComStatus(Long usuarioId) {
        LocalDate hoje = LocalDate.now();
        garantirGastosDoMesGerados(usuarioId, hoje.getMonthValue(), hoje.getYear());

        return gastoFixoRepository.findByUsuarioId(usuarioId).stream()
                .map(fixo -> toDTOComStatus(fixo, hoje))
                .collect(Collectors.toList());
    }

    /** Usado pelo aviso no Resumo. Dispara o e-mail de lembrete como efeito colateral (ver método privado). */
    public List<GastoFixoDTO> listarPendentesAlerta(Usuario usuarioLogado) {
        List<GastoFixoDTO> pendentes = listarComStatus(usuarioLogado.getId()).stream()
                .filter(dto -> dto.getStatusMesAtual() == StatusGastoFixo.VENCENDO || dto.getStatusMesAtual() == StatusGastoFixo.ATRASADO)
                .collect(Collectors.toList());

        pendentes.forEach(dto -> enviarLembretePorEmailSeNecessario(dto, usuarioLogado));
        return pendentes;
    }

    /**
     * Gera, se ainda não existir, o Gasto do mês/ano para cada GastoFixo ativo do usuário.
     * Idempotente — chamar de novo no mesmo mês não duplica nada. Sem rotina agendada: é
     * chamada em toda leitura de gastos de um mês (calcularResumo, filtrarGastos, evolução),
     * porque o servidor não roda 24/7 hoje.
     */
    @Transactional
    public void garantirGastosDoMesGerados(Long usuarioId, int mes, int ano) {
        YearMonth referencia = YearMonth.of(ano, mes);

        for (GastoFixo fixo : gastoFixoRepository.findByUsuarioIdAndAtivoTrue(usuarioId)) {
            if (fixo.getDataInicio().isAfter(referencia.atEndOfMonth())) {
                continue; // ainda não começou a valer nesse mês
            }

            LocalDate inicioDoMes = referencia.atDay(1);
            LocalDate fimDoMes = referencia.atEndOfMonth();
            if (gastoRepository.existsByGastoFixoIdAndDataBetween(fixo.getId(), inicioDoMes, fimDoMes)) {
                continue; // já gerado
            }

            Gasto gasto = new Gasto();
            gasto.setDescricao(fixo.getDescricao());
            gasto.setValor(fixo.getValor());
            gasto.setCategoria(fixo.getCategoria());
            gasto.setUsuario(fixo.getUsuario());
            gasto.setGastoFixo(fixo);
            gasto.setPago(false);
            gasto.setData(diaAjustado(referencia, fixo.getDiaVencimento()));

            // Duas requisições concorrentes (ex.: Resumo e o alerta de contas vencendo,
            // carregados em paralelo) podem passar pelo "já existe?" acima ao mesmo tempo,
            // antes de qualquer uma salvar. A constraint única (gasto_fixo_id, data) do
            // banco (migração V11) rejeita a segunda gravação — aqui só ignoramos: a outra
            // requisição já gerou o gasto do mês, não há nada a fazer.
            try {
                gastoRepository.save(gasto);
            } catch (DataIntegrityViolationException e) {
                log.info("Gasto do mês já gerado por outra requisição concorrente pra gastoFixo id={}", fixo.getId());
            }
        }
    }

    // Best-effort: o alerta no app (a lista em si) não pode depender do e-mail funcionar.
    // Se o SMTP não estiver configurado ou falhar, loga e segue — não derruba a requisição.
    private void enviarLembretePorEmailSeNecessario(GastoFixoDTO dto, Usuario usuarioLogado) {
        if (!lembretePorEmailHabilitado) return;
        if (dto.getGastoDoMesId() == null) return;

        Gasto gasto = gastoRepository.findById(dto.getGastoDoMesId()).orElse(null);
        if (gasto == null) return;

        LocalDate hoje = LocalDate.now();
        if (hoje.equals(gasto.getUltimoLembreteEnviadoEm())) return; // já mandou hoje

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(usuarioLogado.getEmail());
            message.setSubject("Conta a vencer: " + dto.getDescricao());
            message.setText(
                    "A conta \"" + dto.getDescricao() + "\" no valor de R$ " + dto.getValor()
                            + " vence em " + dto.getDataVencimentoMesAtual()
                            + ". Não esqueça de marcar como paga no app depois de pagar."
            );
            mailSender.send(message);

            gasto.setUltimoLembreteEnviadoEm(hoje);
            gastoRepository.save(gasto);
        } catch (Exception e) {
            log.warn("Não foi possível enviar o lembrete por e-mail para gasto fixo id={}: {}", dto.getId(), e.getMessage());
        }
    }

    private GastoFixoDTO toDTOComStatus(GastoFixo fixo, LocalDate hoje) {
        YearMonth mesAtual = YearMonth.from(hoje);
        LocalDate dataVencimento = diaAjustado(mesAtual, fixo.getDiaVencimento());

        // .findFirst() (não um único resultado direto): ver nota de findByGastoFixoIdAndDataBetweenOrderByIdAsc
        // no repository — precisa tolerar uma eventual duplicata sem quebrar a tela inteira.
        Gasto gastoDoMes = gastoRepository
                .findByGastoFixoIdAndDataBetweenOrderByIdAsc(fixo.getId(), mesAtual.atDay(1), mesAtual.atEndOfMonth())
                .stream()
                .findFirst()
                .orElse(null);

        StatusGastoFixo status;
        Long gastoDoMesId = null;
        if (gastoDoMes == null) {
            status = StatusGastoFixo.PENDENTE;
        } else {
            gastoDoMesId = gastoDoMes.getId();
            status = calcularStatus(gastoDoMes.isPago(), gastoDoMes.getData(), hoje);
        }

        return new GastoFixoDTO(
                fixo.getId(),
                fixo.getCategoria(),
                fixo.getValor(),
                fixo.getDescricao(),
                fixo.getDiaVencimento(),
                fixo.getDataInicio(),
                fixo.isAtivo(),
                status,
                dataVencimento,
                gastoDoMesId
        );
    }

    private StatusGastoFixo calcularStatus(boolean pago, LocalDate dataVencimento, LocalDate hoje) {
        if (pago) return StatusGastoFixo.PAGO;
        if (dataVencimento.isBefore(hoje)) return StatusGastoFixo.ATRASADO;
        if (!dataVencimento.isAfter(hoje.plusDays(DIAS_LIMIAR_ALERTA))) return StatusGastoFixo.VENCENDO;
        return StatusGastoFixo.PENDENTE;
    }

    // Dia 31 num mês que só tem 28/29/30 dias cai no último dia do mês.
    private LocalDate diaAjustado(YearMonth mes, int diaVencimento) {
        int ultimoDia = mes.lengthOfMonth();
        return mes.atDay(Math.min(diaVencimento, ultimoDia));
    }

    private GastoFixo paraEntidade(CriarGastoFixoRequest request, Usuario usuarioLogado) {
        GastoFixo fixo = new GastoFixo();
        fixo.setCategoria(request.getCategoria());
        fixo.setValor(request.getValor());
        fixo.setDescricao(request.getDescricao());
        fixo.setDiaVencimento(request.getDiaVencimento());
        fixo.setDataInicio(request.getDataInicio());
        fixo.setUsuario(usuarioLogado);
        return fixo;
    }

    private GastoFixo buscarComOwnership(Long id, Long usuarioId) {
        GastoFixo fixo = gastoFixoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gasto fixo não encontrado"));

        if (!fixo.getUsuario().getId().equals(usuarioId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado");
        }

        return fixo;
    }
}
