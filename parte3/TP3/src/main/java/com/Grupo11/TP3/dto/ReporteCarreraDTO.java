package universidad.dto;

public class ReporteCarreraDTO {
    private String nombreCarrera;
    private int anio;
    private long cantidadInscriptos;
    private long cantidadEgresados;

    public ReporteCarreraDTO(String nombreCarrera, int anio, long cantidadInscriptos, long cantidadEgresados){
        this.nombreCarrera=nombreCarrera;
        this.anio = anio;
        this.cantidadInscriptos = cantidadInscriptos;
        this.cantidadEgresados = cantidadEgresados;
    }

    public String getNombreCarrera() {
        return this.nombreCarrera;
    }

    public int getAnio() {
        return anio;
    }

    public long getCantidadInscriptos() {
        return cantidadInscriptos;
    }

    public long getCantidadEgresados() {
        return cantidadEgresados;
    }

    public void setCantidadEgresados(long cantidadEgresados) {
        this.cantidadEgresados = cantidadEgresados;
    }

// le da formato al texto de salida como una tabla
    @Override
    public String toString() {
        return String.format("%-25s | Año: %-4d | Inscriptos: %-4d | Egresados: %-4d",
                nombreCarrera, anio, cantidadInscriptos, cantidadEgresados);
    }

}