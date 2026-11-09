package ticketya.pasarela_mock.dto;

import java.math.BigDecimal;

public class PagoRequest {

    private BigDecimal monto;
    private String ordenId;
    private String metodoPago;

    public PagoRequest() {
    }

    public PagoRequest(BigDecimal monto, String ordenId, String metodoPago) {
        this.monto = monto;
        this.ordenId = ordenId;
        this.metodoPago = metodoPago;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public String getOrdenId() {
        return ordenId;
    }

    public void setOrdenId(String ordenId) {
        this.ordenId = ordenId;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }

}
