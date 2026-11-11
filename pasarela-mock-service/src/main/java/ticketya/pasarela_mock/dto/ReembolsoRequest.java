package ticketya.pasarela_mock.dto;

import java.math.BigDecimal;

public class ReembolsoRequest {
    private String transaccionId;
    private BigDecimal monto;

    public ReembolsoRequest() {}

    public ReembolsoRequest(String transaccionId, BigDecimal monto) {
        this.transaccionId = transaccionId;
        this.monto = monto;
    }

    public String getTransaccionId() {
        return transaccionId;
    }

    public void setTransaccionId(String transaccionId) {
        this.transaccionId = transaccionId;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }
}

