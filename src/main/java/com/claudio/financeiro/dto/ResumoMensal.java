package com.claudio.financeiro.dto;

public class ResumoMensal {
    private Double totalSalario;
    private Double totalGasto;
    private Double saldo;
    private String mensagem;


    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }




    public Double getTotalSalario() {
        return totalSalario;
    }

    public void setTotalSalario(Double totalSalario) {
        this.totalSalario = totalSalario;
    }


    public Double getTotalGasto() {
        return totalGasto;
    }

    public void setTotalGasto(Double totalGasto) {
        this.totalGasto = totalGasto;
    }

    public Double getSaldo() {
        return saldo;
    }

    public void setSaldo(Double saldo) {
        this.saldo = saldo;
    }

}
