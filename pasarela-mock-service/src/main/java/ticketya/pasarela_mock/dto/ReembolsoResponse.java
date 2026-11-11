package ticketya.pasarela_mock.dto;

public class ReembolsoResponse {
    private String reembolsoId;
    private String estado;
    private String mensaje;

    public ReembolsoResponse() {}

    public ReembolsoResponse(String reembolsoId, String estado, String mensaje) {
        this.reembolsoId = reembolsoId;
        this.estado = estado;
        this.mensaje = mensaje;
    }

    public String getReembolsoId() {
        return reembolsoId;
    }

    public void setReembolsoId(String reembolsoId) {
        this.reembolsoId = reembolsoId;
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

