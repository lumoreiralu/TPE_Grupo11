package ticketya.pasarela_mock.dto;

public class PagoResponse {

    private String transaccionId;
    private String estado;
    private String mensaje;

    public PagoResponse() {
    }

    public PagoResponse(String transaccionId, String estado, String mensaje) {
        this.transaccionId = transaccionId;
        this.estado = estado;
        this.mensaje = mensaje;
    }

    public String getTransaccionId() {
        return transaccionId;
    }

    public void setTransaccionId(String transaccionId) {
        this.transaccionId = transaccionId;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

}
