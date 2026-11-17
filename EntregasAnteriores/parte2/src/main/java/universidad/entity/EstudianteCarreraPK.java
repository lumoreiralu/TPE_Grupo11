package universidad.entity;
import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class EstudianteCarreraPK implements Serializable {
    private Integer idEstudiante;
    private Integer idCarrera;

    public EstudianteCarreraPK() {}

    public EstudianteCarreraPK(Integer idEstudiante, Integer idCarrera) {
        this.idEstudiante = idEstudiante;
        this.idCarrera = idCarrera;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass())
            return false;
        EstudianteCarreraPK that = (EstudianteCarreraPK) o;
        return Objects.equals(idEstudiante, that.idEstudiante) && Objects.equals(idCarrera, that.idCarrera);
    }

    @Override
    public int hashCode() {

        return Objects.hash(idEstudiante, idCarrera);
    }

    public Integer getIdEstudiante() {
        return idEstudiante;
    }

    public Integer getIdCarrera() {
        return idCarrera;
    }
}